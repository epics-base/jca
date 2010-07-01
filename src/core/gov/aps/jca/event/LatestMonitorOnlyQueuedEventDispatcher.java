package gov.aps.jca.event;

import gov.aps.jca.JCALibrary;
import gov.aps.jca.configuration.Configurable;
import gov.aps.jca.configuration.Configuration;
import gov.aps.jca.configuration.ConfigurationException;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This EventDispatcher uses an internal thread to dispatch events and overrides older (obsolete) monitor values.
 */
public class LatestMonitorOnlyQueuedEventDispatcher extends AbstractEventDispatcher implements
        Runnable, Configurable {
    
    static protected int _count = 0;

    protected volatile boolean _killed = false;
    protected int _priority = Thread.NORM_PRIORITY;
    protected Thread _dispatcherThread;
    protected List _queue;
    protected int _queueLimit = 100;

    protected Map _sourcesEventCount;
    protected int _limit = 5;
    
    protected Map _overrideMap;

    protected String _monitorOutput = null;
    protected Thread _monitorThread;
    
    public LatestMonitorOnlyQueuedEventDispatcher() {
        _dispatcherThread = new Thread(this, "LatestMonitorOnlyQueuedEventDispatcher-" + (_count++));
        _dispatcherThread.setDaemon(true);
        setPriority(JCALibrary.getInstance().getPropertyAsInt(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".priority", _priority));
        _queue = new ArrayList();
        _queueLimit = JCALibrary.getInstance().getPropertyAsInt(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".queue_limit", _queueLimit);
        if (_queueLimit < 10)
        	_queueLimit = 10;

        _sourcesEventCount = new HashMap();
        _limit = JCALibrary.getInstance().getPropertyAsInt(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".channel_queue_limit", _limit);
        if (_limit < 3)
        	_limit = 3;
    
        _overrideMap = new HashMap();
        
        _monitorOutput = JCALibrary.getInstance().getProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".monitor_output", _monitorOutput);
        if (_monitorOutput != null)
        {
        	_monitorThread = new Thread(
        			new Runnable() {
		        		public void run() {
		        			runMonitoring();
		        		}
		        	}, _dispatcherThread.getName() + " monitor");
        	_monitorThread.start();
        }
        
        _dispatcherThread.start();
    }

    abstract class Event {
        CAEvent _ev;
        final EventListener _listener;
        final Object[] _listeners;
        final Object _overrideId;

        Event(CAEvent ev, Object[] listeners) {
        	this(ev, listeners, null);
        }
        
        Event(CAEvent ev, Object[] listeners, Object overrideId) {
            _ev = ev;
            _listener = null;
            _listeners = listeners;
            _overrideId = overrideId;
        }

        Event(CAEvent ev, EventListener listener) {
        	this(ev, listener, null);
        }
        
        Event(CAEvent ev, EventListener listener, Object overrideId) {
            _ev = ev;
            _listener = listener;
            _listeners = null;
            _overrideId = overrideId;
        }

        abstract public void dispatch();
    }

    protected void nonBlockingQueueEvent(Event ev)
    {
    	queueEvent(ev, true);
	}    

    protected void queueEvent(Event ev)
    {
    	queueEvent(ev, false);
	}    

    protected void queueEvent(Event ev, boolean doNotBlockRequired)
    {
    	if (_killed) return;

        // increment counter, will block if limit will be reached
    	// avoid deadlock allowing recursive queue-ing 
    	boolean doNotBlock = doNotBlockRequired || (Thread.currentThread() == _dispatcherThread);
    	incrementSyncCounter(ev, doNotBlock);

        synchronized (_queue)
        {
        	while (!doNotBlock && _queue.size() >= _queueLimit && !_killed)
        	{
				try {
					_queue.wait();
				} catch (InterruptedException e) { }
        	}
        	
        	if (_killed) return;

        	_queue.add(ev);
            // notify event arrival
            _queue.notifyAll();
        }
    }

    /**
	 * @param ev
	 * @return
	 */
	private final void incrementSyncCounter(Event ev, boolean doNotBlock) {
    	final Object source = ev._ev.getSource();

    	SynchronizedLimitedInt sli;
    	synchronized (_sourcesEventCount)
		{
    		// NOTE: hash code of source should not change !!!
    		sli = (SynchronizedLimitedInt)_sourcesEventCount.get(source);
    		if (sli == null) {
    			_sourcesEventCount.put(source, new SynchronizedLimitedInt(1, _limit));
    			return;
    		}
		}
    	// this will block when limit is reached
    	// (NOTE: it might happen that sli is removed/destroyed here)
		sli.increment(doNotBlock);
	}

    /**
	 * @param ev
	 * @return
	 */
	private final void decrementSyncCounter(Event ev) {
    	final Object source = ev._ev.getSource();

    	synchronized (_sourcesEventCount)
		{
    		final SynchronizedLimitedInt sli = (SynchronizedLimitedInt)_sourcesEventCount.get(source);
    		if (sli != null && sli.decrement() <= 0)
    		{
    			_sourcesEventCount.remove(source);
    			sli.destroy();
    		}
		}
	}

	/**
     * Process events in the queue as they are added. The queue must be
     * synchronized for thread safety but we must be careful not to block for
     * too long or else new events get blocked from being added to the queue
     * causing long delays. The design is to process events in batches. This
     * allows for the most efficient use of computer cycles since much of the
     * time the queue will not be blocked.
     * 
     * This method was modified by tap on 6/17/2004 to allow both efficient event processing and thread safety.
     * Later optimized by msekoranja.
     */
    public void run()
    {
        // eventBatch is local and only referenced by a single thread so we
        // don't have to synchronize it
        int eventsToProcess = 0;
        Event[] eventBatch = new Event[0];

        while (!_killed)
        {
            try
            {
                // for performance reasons we don't want to block for too long
                // synchronize _queue for thread safety
                // copy all of the new queued events to the local batch and clear _queue
                synchronized (_queue)
                {
                    // wait for new requests
                    while (!_killed && _queue.isEmpty())
                        _queue.wait();
                    
                    if (!_killed)
                    {
                        eventsToProcess = _queue.size();
                        // create new instance of batch array only if necessary
                        if (eventsToProcess > eventBatch.length)
                            eventBatch = new Event[eventsToProcess];
                        
                        // only copy (will not recreate array)                          
                        _queue.toArray(eventBatch);
                        _queue.clear();

                        // notify queue clean-up
                        _queue.notifyAll();
                    }
                }
                
                // process all events in the local batch until it is empty
                for (int i = 0; !_killed && i < eventsToProcess; i++)
                {
                    // catch all exceptions, so that one buggy listener does not harm the others  
                	final Event event = eventBatch[i];
                	try
                    {
                		// remove from override id
                    	final Object overrideId = eventBatch[i]._overrideId;
                    	if (overrideId != null)
                    	{
                    		synchronized (_overrideMap) {
                    			_overrideMap.remove(overrideId);
							}
                    	}
                	
                    	event.dispatch();
                    }
                    catch (Throwable th) {
                        th.printStackTrace();
                    }
                    
                    decrementSyncCounter(eventBatch[i]);
                    
				    eventBatch[i] = null;	// allow to be gc'ed
                    Thread.yield();
                }
                
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    public void dispose()
    {
        _killed = true;
        _dispatcherThread = null;

        // notify _queue
        synchronized (_queue)
        {
            _queue.notifyAll();
        }

        // destroy all locks
        synchronized (_sourcesEventCount)
		{
        	Iterator iter = _sourcesEventCount.values().iterator();
        	while (iter.hasNext())
        		((SynchronizedLimitedInt)iter.next()).destroy();
        	
        	_sourcesEventCount.clear();
		}

        // clear _overrideMap
        synchronized (_overrideMap)
        {
            _overrideMap.clear();
        }
    }

    public void configure(Configuration conf) throws ConfigurationException
    {
        int priority = getPriority();
        try
        {
            priority = conf.getChild("priority").getValueAsInteger();
        } catch (Exception ex)
        {
            priority = conf.getAttributeAsInteger("priority", priority);
        }
        setPriority(priority);
    }

    public int getPriority()
    {
        return _priority;
    }

    public void setPriority(int priority)
    {
        if (_killed)
        {
            throw new IllegalStateException("Dispatcher thread has been killed");
        }
        _priority = priority;
        _dispatcherThread.setPriority(_priority);
    }

    public void dispatch(ContextMessageEvent ev, List listeners)
    {
    	if (_killed) return;
    	nonBlockingQueueEvent(new Event(ev, listeners.toArray()) {
            public void dispatch()
            {
                for (int t = 0; t < _listeners.length; ++t)
                {
                    if (_listeners[t] instanceof ContextMessageListener)
                    {
                        ((ContextMessageListener) _listeners[t])
                                .contextMessage((ContextMessageEvent) _ev);
                    }
                }
            }
        });
    }

    public void dispatch(ContextExceptionEvent ev, List listeners)
    {
    	if (_killed) return;
    	nonBlockingQueueEvent(new Event(ev, listeners.toArray()) {
            public void dispatch()
            {
                for (int t = 0; t < _listeners.length; ++t)
                {
                    if (_listeners[t] instanceof ContextExceptionListener)
                    {
                        ((ContextExceptionListener) _listeners[t])
                                .contextException((ContextExceptionEvent) _ev);
                    }
                }
            }
        });
    }

    public void dispatch(ConnectionEvent ev, List listeners)
    {
    	if (_killed) return;
        nonBlockingQueueEvent(new Event(ev, listeners.toArray()) {
            public void dispatch()
            {
                for (int t = 0; t < _listeners.length; ++t)
                {
                    if (_listeners[t] instanceof ConnectionListener)
                    {
                        ((ConnectionListener) _listeners[t])
                                .connectionChanged((ConnectionEvent) _ev);
                    }
                }
            }
        });
    }

    public void dispatch(AccessRightsEvent ev, List listeners)
    {
    	if (_killed) return;
    	nonBlockingQueueEvent(new Event(ev, listeners.toArray()) {
            public void dispatch()
            {
                for (int t = 0; t < _listeners.length; ++t)
                {
                    if (_listeners[t] instanceof AccessRightsListener)
                    {
                        ((AccessRightsListener) _listeners[t])
                        		.accessRightsChanged((AccessRightsEvent) _ev);
                    }
                }
            }
        });
    }

    public void dispatch(MonitorEvent ev, List listeners)
    {
    	if (_killed) return;
    	
    	// object creation opt. tweak
	    if (listeners.size() == 1){
	    	dispatch(ev, (MonitorListener)listeners.get(0));
	    	return;
	    }

    	// override check, add to override map or override old event
    	synchronized (_overrideMap) {
    		Event existingEvent = (Event)_overrideMap.get(ev.getSource());
    		if (existingEvent != null) {
    			existingEvent._ev = ev;
    		}
    		else
    		{
    			final Event event = new Event(ev, listeners.toArray(), ev.getSource()) {
    	            public void dispatch()
    	            {
    	                for (int t = 0; t < _listeners.length; ++t)
    	                {
    	                    if (_listeners[t] instanceof MonitorListener)
    	                    {
    	                        ((MonitorListener) _listeners[t])
    	                        		.monitorChanged((MonitorEvent) _ev);
    	                    }
    	                }
    	            }
    			};
    			_overrideMap.put(ev.getSource(), event);
    		    queueEvent(event);
    		}
    	}
    }

    public void dispatch(GetEvent ev, List listeners)
    {
    	if (_killed) return;
        queueEvent(new Event(ev, listeners.toArray()) {
            public void dispatch()
            {
                for (int t = 0; t < _listeners.length; ++t)
                {
                    if (_listeners[t] instanceof GetListener)
                    {
                        ((GetListener) _listeners[t]).getCompleted((GetEvent) _ev);
                    }
                }
            }
        });
    }

    public void dispatch(PutEvent ev, List listeners)
    {
    	if (_killed) return;
        queueEvent(new Event(ev, listeners.toArray()) {
            public void dispatch()
            {
                for (int t = 0; t < _listeners.length; ++t)
                {
                    if (_listeners[t] instanceof PutListener)
                    {
                        ((PutListener) _listeners[t]).putCompleted((PutEvent) _ev);
                    }
                }
            }
        });
    }

  public void dispatch( ContextMessageEvent ev, ContextMessageListener cml) {
  	if (_killed) return;
  	nonBlockingQueueEvent(new Event(ev, cml) {
        public void dispatch()
        {
            try {
          		((ContextMessageListener)_listener).contextMessage((ContextMessageEvent) _ev);
          	} catch (Throwable th) {
          		th.printStackTrace();
          	}
        }
    });

  }

  public void dispatch( ContextExceptionEvent ev, ContextExceptionListener cel ) {
  	if (_killed) return;
  	nonBlockingQueueEvent(new Event(ev, cel) {
        public void dispatch()
        {
            try {
          		((ContextExceptionListener)_listener).contextException((ContextExceptionEvent) _ev);
          	} catch (Throwable th) {
          		th.printStackTrace();
          	}
        }
    });
  }

  public void dispatch( ConnectionEvent ev, ConnectionListener cl ) {
  	if (_killed) return;
    nonBlockingQueueEvent(new Event(ev, cl) {
        public void dispatch()
        {
            try {
          		((ConnectionListener)_listener).connectionChanged((ConnectionEvent) _ev);
          	} catch (Throwable th) {
          		th.printStackTrace();
          	}
        }
    });
  }

  public void dispatch( AccessRightsEvent ev, AccessRightsListener arl ) {
  	if (_killed) return;
  	nonBlockingQueueEvent(new Event(ev, arl) {
        public void dispatch()
        {
            try {
          		((AccessRightsListener)_listener).accessRightsChanged((AccessRightsEvent) _ev);
          	} catch (Throwable th) {
          		th.printStackTrace();
          	}
        }
    });
  }
  
  public void dispatch(MonitorEvent ev, MonitorListener ml)
  {
  	if (_killed) return;

  	// override check, add to override map or override old event
   	Event event = null;
   	synchronized (_overrideMap) 
   	{
  		Event existingEvent = (Event)_overrideMap.get(ev.getSource());
  		if (existingEvent != null && existingEvent._listener == ml) 
      	{
  			existingEvent._ev = ev;
  		}
  		else
  		{
  			event = new Event(ev, ml, ev.getSource()) 
		        {
	  	            public void dispatch()
	  	            {
						try  {
		  	                 ((MonitorListener)_listener).monitorChanged((MonitorEvent) _ev);
		  	          	} 
	                    catch (Throwable th)  {
	                       th.printStackTrace();
		  	          	}
	  	            }
	  			};
  			_overrideMap.put(ev.getSource(), event);
  		}
  	}

   	if( event != null ) 
    	queueEvent(event);
  }

  public void dispatch( GetEvent ev, GetListener gl ) {
  	if (_killed) return;
    queueEvent(new Event(ev, gl) {
        public void dispatch()
        {
            try {
          		((GetListener)_listener).getCompleted((GetEvent) _ev);
          	} catch (Throwable th) {
          		th.printStackTrace();
          	}
        }
    });
  }

  public void dispatch( PutEvent ev, PutListener pl ) {
  	if (_killed) return;
    queueEvent(new Event(ev, pl) {
        public void dispatch()
        {
            try {
          		((PutListener)_listener).putCompleted((PutEvent) _ev);
          	} catch (Throwable th) {
          		th.printStackTrace();
          	}
        }
    });
  }

  /**
   * Monitors queue and outputs logs to the <code>monitor_log</code>.
   */
  public void runMonitoring()
  {
	  if (_monitorOutput == null)
		  return;
	  
	  PrintStream output;
	  if (_monitorOutput.equals("stdout"))
		  output = System.out;
	  else if (_monitorOutput.equals("stderr"))
		  output = System.err;
	  else
	  {
		  try {
			output = new PrintStream(_monitorOutput);
		} catch (FileNotFoundException e) {
			System.err.println("Failed to open monitoring output file '" + _monitorOutput + "', falling back to stdout.");
			e.printStackTrace();
			output = System.out;
		}
	  }
	  
	  final SimpleDateFormat ISO8601FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");		
      while (!_killed)
      {
    	  synchronized (_queue) {
        	  output.println(ISO8601FORMAT.format(new Date()) + "\t" + _queue.size());
    	  }
    	  output.flush();
    	  
    	 try {
			Thread.sleep(1000);
		} catch (InterruptedException e) { /* noop */ }
      }
	  
      output.close();
  }

}