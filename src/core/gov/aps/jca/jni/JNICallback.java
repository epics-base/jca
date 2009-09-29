package gov.aps.jca.jni;

import gov.aps.jca.event.*;
import java.util.*;


abstract class JNICallback {
  protected EventDispatcher _dispatcher;
  protected List _listeners;

  protected JNICallback(EventDispatcher dispatcher, EventListener listener) {
    this(dispatcher, Arrays.asList(new EventListener[] {listener}));
  }
  protected JNICallback(EventDispatcher dispatcher, List listeners) {
    _dispatcher= dispatcher;
    _listeners = listeners;
  }

  public void dispatch( ContextMessageEvent ev ) {
    _dispatcher.dispatch(ev,_listeners);
  }

  public void dispatch( ContextExceptionEvent ev) {
    _dispatcher.dispatch(ev,_listeners);
  }

  public void dispatch( ConnectionEvent ev){
    _dispatcher.dispatch(ev,_listeners);
  }

  public void dispatch( AccessRightsEvent ev){
    _dispatcher.dispatch(ev,_listeners);
  }

  public void dispatch( MonitorEvent ev){
    _dispatcher.dispatch(ev,_listeners);
  }

  public void dispatch( GetEvent ev){
    _dispatcher.dispatch(ev,_listeners);
  }

  public void dispatch( PutEvent ev){
    _dispatcher.dispatch(ev,_listeners);
  }

}