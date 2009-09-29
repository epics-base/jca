

package gov.aps.jca.event;

import java.util.*;


/**
 * This EventDispatcher will forward all events directly to the listeners. The
 * listeners will be called by the underlying JCA callback's thread. JCA methods
 * call (Context, Channel, Monitor) are not allowed within the listener's
 * callback methods and could result in an unpredictable behavior. Listener's
 * method should delegate all JCA calls to another thread.
 * 
 * 
 * @author Eric Boucher
 * @author Matej Sekoranja
 * @version 1.0
 */
public class DirectEventDispatcher extends AbstractEventDispatcher {

  public void dispatch( ContextMessageEvent ev, List listeners ) {
    Object[] list= listeners.toArray();

    for(int t=0; t<list.length; ++t) {
      if(list[t] instanceof ContextMessageListener) {
      	try {
      		((ContextMessageListener)list[t]).contextMessage(ev);
	  	} catch (Throwable th) {
	  		th.printStackTrace();
	  	}
      }
    }
  }

  public void dispatch( ContextExceptionEvent ev, List listeners ) {
    Object[] list= listeners.toArray();

    for(int t=0; t<list.length; ++t) {
      if(list[t] instanceof ContextExceptionListener) {
      	try {
      		((ContextExceptionListener)list[t]).contextException(ev);
	  	} catch (Throwable th) {
	  		th.printStackTrace();
	  	}
      }
    }
  }

  public void dispatch( ConnectionEvent ev, List listeners ) {
    Object[] list= listeners.toArray();

    for(int t=0; t<list.length; ++t) {
      if(list[t] instanceof ConnectionListener) {
      	try {
      		((ConnectionListener)list[t]).connectionChanged(ev);
	  	} catch (Throwable th) {
	  		th.printStackTrace();
	  	}
      }
    }
  }

  public void dispatch( AccessRightsEvent ev, List listeners ) {
    Object[] list= listeners.toArray();

    for(int t=0; t<list.length; ++t) {
      if(list[t] instanceof AccessRightsListener) {
      	try {
      		((AccessRightsListener)list[t]).accessRightsChanged(ev);
    	} catch (Throwable th) {
      		th.printStackTrace();
      	}
      }
    }
  }

  public void dispatch( MonitorEvent ev, List listeners ) {
    Object[] list= listeners.toArray();

    for(int t=0; t<list.length; ++t) {
      if(list[t] instanceof MonitorListener) {
      	try {
      		((MonitorListener)list[t]).monitorChanged(ev);
    	} catch (Throwable th) {
      		th.printStackTrace();
      	}
      }
    }
  }

  public void dispatch( GetEvent ev, List listeners ) {
    Object[] list= listeners.toArray();

    for(int t=0; t<list.length; ++t) {
      if(list[t] instanceof GetListener) {
      	try {
      		((GetListener)list[t]).getCompleted(ev);
      	} catch (Throwable th) {
      		th.printStackTrace();
      	}
      }
    }
  }

  public void dispatch( PutEvent ev, List listeners ) {
    Object[] list= listeners.toArray();

    for(int t=0; t<list.length; ++t) {
      if(list[t] instanceof PutListener) {
      	try {
      		((PutListener)list[t]).putCompleted(ev);
      	} catch (Throwable th) {
      		th.printStackTrace();
      	}
      }
    }
  }


  public void dispatch( ContextMessageEvent ev, ContextMessageListener cml) {
  	try {
  		cml.contextMessage(ev);
  	} catch (Throwable th) {
  		th.printStackTrace();
  	}
  }

  public void dispatch( ContextExceptionEvent ev, ContextExceptionListener cel ) {
	try {
  		cel.contextException(ev);
  	} catch (Throwable th) {
  		th.printStackTrace();
  	}
  }

  public void dispatch( ConnectionEvent ev, ConnectionListener cl ) {
	try {
  		cl.connectionChanged(ev);
  	} catch (Throwable th) {
  		th.printStackTrace();
  	}
  }

  public void dispatch( AccessRightsEvent ev, AccessRightsListener arl ) {
	try {
  		arl.accessRightsChanged(ev);
  	} catch (Throwable th) {
  		th.printStackTrace();
  	}
  }

  public void dispatch( MonitorEvent ev, MonitorListener ml ) {
	try {
  		ml.monitorChanged(ev);
  	} catch (Throwable th) {
  		th.printStackTrace();
  	}
  }

  public void dispatch( GetEvent ev, GetListener gl ) {
	try {
  		gl.getCompleted(ev);
  	} catch (Throwable th) {
  		th.printStackTrace();
  	}
  }

  public void dispatch( PutEvent ev, PutListener pl ) {
	try {
  		pl.putCompleted(ev);
  	} catch (Throwable th) {
  		th.printStackTrace();
  	}
  }

}


