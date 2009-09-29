/**********************************************************************

 *

 *      Original Author: Eric Boucher

 *      Date:            05/05/2003

 *

 *      Experimental Physics and Industrial Control System (EPICS)

 *

 *      Copyright 1991, the University of Chicago Board of Governors.

 *

 *      This software was produced under  U.S. Government contract

 *      W-31-109-ENG-38 at Argonne National Laboratory.

 *

 *      Beamline Controls & Data Acquisition Group

 *      Experimental Facilities Division

 *      Advanced Photon Source

 *      Argonne National Laboratory

 *

 *

 * $Id: AccessRightsEvent.java,v 1.2 2004-08-12 07:24:05 msekoranja Exp $

 *

 * Modification Log:

 * 01. 05/07/2003  erb  initial development

 *

 */





package gov.aps.jca.event;



import gov.aps.jca.*;



/**

 * An event which indicates a change in a Channel read and write access properties.

 *

 * @see Channel

 */

public class AccessRightsEvent extends CAEvent  {

  protected boolean _read;

  protected boolean _write;





  /**

   * Constructs an AccessRightEvent object.

   *

   * @param channel the event source.

   * @param read the new read access value.

   * @param write the new write access value.

   */

  public AccessRightsEvent(Channel channel, boolean read, boolean write) {

    super(channel);

    _read=read;

    _write=write;

  }



  /**

   * Returns the read access value associated with this event.

   */

  public boolean getReadAccess() {

    return _read;

  }



  /**

   * Returns the write access value associated with this event.

   */

  public boolean getWriteAccess() {

    return _write;

  }



}



