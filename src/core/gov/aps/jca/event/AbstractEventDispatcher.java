package gov.aps.jca.event;

import java.util.*;


abstract public class AbstractEventDispatcher implements EventDispatcher {

  public void dispatch(ContextMessageEvent ev, ContextMessageListener listener) {
    dispatch(ev, Arrays.asList(new ContextMessageListener[] { listener }));
  }

  public void dispatch(ContextExceptionEvent ev, ContextExceptionListener listener) {
    dispatch(ev, Arrays.asList(new ContextExceptionListener[] { listener }));
  }

  public void dispatch(ConnectionEvent ev, ConnectionListener listener) {
    dispatch(ev, Arrays.asList(new ConnectionListener[] { listener }));
  }

  public void dispatch(AccessRightsEvent ev, AccessRightsListener listener) {
    dispatch(ev, Arrays.asList(new AccessRightsListener[] { listener }));
  }

  public void dispatch(MonitorEvent ev, MonitorListener listener) {
    dispatch(ev, Arrays.asList(new MonitorListener[] { listener }));
  }

  public void dispatch(GetEvent ev, GetListener listener) {
    dispatch(ev, Arrays.asList(new GetListener[] { listener }));
  }

  public void dispatch(PutEvent ev, PutListener listener) {
    dispatch(ev, Arrays.asList(new PutListener[] { listener }));
  }
  
  public void dispose() { /* noop */ }
}