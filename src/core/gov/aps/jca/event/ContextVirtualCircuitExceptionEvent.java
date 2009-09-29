package gov.aps.jca.event;

import java.net.InetAddress;

import gov.aps.jca.*;

/**
 * An event which indicates an virtual circuit exception in a Context.
 *
 * Virtual circuit context exceptions occur when it becomes disconnected or becomes unresponsive.
 *
 * @see gov.aps.jca.Context
 */

public class ContextVirtualCircuitExceptionEvent extends CAEvent {

  Context ctxt;
  CAStatus _status;
  InetAddress _virtual_circuit;
  
  public ContextVirtualCircuitExceptionEvent( Context ctxt, InetAddress virtual_circuit, CAStatus status ) {

    super( ctxt );

    _virtual_circuit = virtual_circuit;
    _status = status;
  }

  /**
   * @return Returns the status.
   */
  public CAStatus getStatus() {
 	return _status;
  }

 /**
  * @return Returns the virtual circuit.
  */
  public InetAddress getVirtualCircuit() {
	return _virtual_circuit;
  }

}
