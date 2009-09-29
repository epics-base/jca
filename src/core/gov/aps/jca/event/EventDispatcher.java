package gov.aps.jca.event;

import java.util.*;


public interface EventDispatcher {

  public void dispatch( ContextMessageEvent ev , ContextMessageListener listener);
  public void dispatch( ContextMessageEvent ev , List listeners);

  public void dispatch( ContextExceptionEvent ev, ContextExceptionListener listener );
  public void dispatch( ContextExceptionEvent ev, List listeners );

  public void dispatch( ConnectionEvent ev, ConnectionListener listener );
  public void dispatch( ConnectionEvent ev, List listeners );

  public void dispatch( AccessRightsEvent ev, AccessRightsListener listener );
  public void dispatch( AccessRightsEvent ev, List listeners );

  public void dispatch( MonitorEvent ev , MonitorListener listener);
  public void dispatch( MonitorEvent ev , List listeners);

  public void dispatch( GetEvent ev , GetListener listener );
  public void dispatch( GetEvent ev , List listeners );

  public void dispatch( PutEvent ev , PutListener listener);
  public void dispatch( PutEvent ev , List listeners);
  
  public void dispose();

}