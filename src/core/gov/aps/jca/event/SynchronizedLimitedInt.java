package gov.aps.jca.event;

/**
 * Limited sync int var.
 **/
public class SynchronizedLimitedInt {

  protected int value_;

  protected int limit_;

  protected final Object lock_;
  
  protected boolean destroyed = false;

  /**
   * Make a new SynchronizedLimitedInt with the given initial value,
   * and using its own internal lock.
   **/
  public SynchronizedLimitedInt(int initialValue, int limit) {
    lock_ = this;
    value_ = initialValue;
    limit_ = limit;
  }

  /**
   * Make a new SynchronizedLimitedInt with the given initial value,
   * and using the supplied lock.
   **/
  public SynchronizedLimitedInt(int initialValue, int limit, Object lock) {
    lock_ = lock;
    value_ = initialValue;
    limit_ = limit;
  }

  /**
   * Return the current value
   **/
  public final int get() { synchronized(lock_) { return value_; } }

  /**
   * Increment the value.
   * @return the new value
   **/
  public int increment() {
  	return increment(false);
  }
  
  /**
   * Increment the value.
   * @return the new value
   **/
  public int increment(boolean doNotBlock) {
    synchronized (lock_) {
    	if (!doNotBlock)
    	{
	    	// limit is reached, wait
	    	while (value_ >= limit_ && !destroyed)
	    	{
	    		try {
					lock_.wait();
				} catch (InterruptedException e) { /* noop */ }
	    	}
    	}
	    return ++value_;
    }
  }

  /**
   * Decrement the value.
   * @return the new value
   **/
  public int decrement() {
    synchronized (lock_) {
      int oldValue = value_;
      --value_;
      // just below limit, notify
      if (oldValue == limit_)
      	lock_.notify();
      return value_;
    }
  }
  
  /**
   * Destroy this instance - wakeup and disable all sync waits.
   */
  public void destroy()
  {
  	synchronized (lock_)
	{
  	  	destroyed = true;
  		lock_.notifyAll();
	}
  }

  public String toString() { return String.valueOf(get()); }

}

