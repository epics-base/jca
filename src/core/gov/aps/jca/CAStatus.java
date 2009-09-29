/**********************************************************************
 *
 *      Original Author: Eric Boucher
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
 * $Id: CAStatus.java,v 1.4 2006-08-23 13:37:07 msekoranja Exp $
 *
 */

package gov.aps.jca;

/** Enumeration class representing Channel Access Status codes.
 * @author Eric Boucher
 */
public class CAStatus extends gov.aps.jca.ValuedEnum {
  static private java.util.Map _map= new java.util.HashMap();
  
  /** Normal successful completion. */  
  static public final CAStatus NORMAL= new CAStatus("NORMAL", 0, CASeverity.SUCCESS, "Normal successful completion");
  /** Maximum simultaneous IOC connections exceed. */  
  static public final CAStatus MAXIOC= new CAStatus("MAXIOC", 1, CASeverity.ERROR, "Maximum simultaneous IOC connections exceeded");
  /** Unknown internet host. */  
  static public final CAStatus UKNHOST= new CAStatus("UKNHOST", 2, CASeverity.ERROR, "Unknown internet host");
  /** Unknown internet service. */  
  static public final CAStatus UKNSERV= new CAStatus("UKNSERV", 3, CASeverity.ERROR, "Unknown internet service");
  /** Unable to allocate a new socket. */  
  static public final CAStatus SOCK= new CAStatus("SOCK", 4, CASeverity.ERROR, "Unable to allocate a new socket");
  /** Unable to connect to internet host or service. */  
  static public final CAStatus CONN= new CAStatus("CONN", 5, CASeverity.WARNING, "Unable to connect to internet host or service");
  /** Unable to allocate additional dynamic memory. */  
  static public final CAStatus ALLOCMEM= new CAStatus("ALLOCMEM", 6, CASeverity.WARNING, "Unable to allocate additional dynamic memory");
  /** Unknown IO channel. */  
  static public final CAStatus UKNCHAN= new CAStatus("UKNCHAN", 7, CASeverity.WARNING, "Unknown IO channel");
  /** Record field specified inappropriate for channel. */  
  static public final CAStatus UKNFIELD= new CAStatus("UKNFIELD", 8, CASeverity.WARNING, "Record field specified inappropriate for channel specified");
  /** The requested transfer is greater than available memory or MAX_ARRAY_BYTES. */  
  static public final CAStatus TOLARGE= new CAStatus("TOLARGE", 9, CASeverity.WARNING, "The requested transfer is greater than available memory or EPICS_CA_MAX_ARRAY_BYTES");
  /** User specified timeout on IO operation expired. */  
  static public final CAStatus TIMEOUT= new CAStatus("TIMEOUT", 10, CASeverity.WARNING, "User specified timeout on IO operation expired");
  /** Feature planned but not supported at this time. */  
  static public final CAStatus NOSUPPORT= new CAStatus("NOSUPPORT", 11, CASeverity.WARNING, "Sorry, that feature is planned but not supported at this time");
  /** Supplied string is unusually large. */  
  static public final CAStatus STRTOBIG= new CAStatus("STRTOBIG", 12, CASeverity.WARNING, "The supplied string is unusually large");
  /** Request ignored because the specified channel is disconnected. */  
  static public final CAStatus DISCONNCHID= new CAStatus("DISCONNCHID", 13, CASeverity.ERROR, "The request was ignored because the specified channel is disconnected");
  /** The data type specified is invalid. */  
  static public final CAStatus BADTYPE= new CAStatus("BADTYPE", 14, CASeverity.ERROR, "The data type specifed is invalid");
  /** Remote channel not found. */  
  static public final CAStatus CHIDNOTFND= new CAStatus("CHIDNOTFND", 15, CASeverity.INFO, "Remote Channel not found");
  /** Unable to locate all user specified channels. */  
  static public final CAStatus CHIDRETRY= new CAStatus("CHIDRETRY", 16, CASeverity.INFO, "Unable to locate all user specified channels");
  /** Channel Access internal failure. */  
  static public final CAStatus INTERNAL= new CAStatus("INTERNAL", 17, CASeverity.FATAL, "Channel Access Internal Failure");
  /** The requested local DB operation failed. */  
  static public final CAStatus DBLCLFAIL= new CAStatus("DBLCLFAIL", 18, CASeverity.WARNING, "The requested local DB operation failed");
  /** Could not perform a database value get for that channel. */  
  static public final CAStatus GETFAIL= new CAStatus("GETFAIL", 19, CASeverity.WARNING, "Could not perform a database value get for that channel");
  /** Could not perform a database value put for that channel. */  
  static public final CAStatus PUTFAIL= new CAStatus("PUTFAIL", 20, CASeverity.WARNING, "Could not perform a database value put for that channel");
  /** Could not perform a database monitor add for that channel. */  
  static public final CAStatus ADDFAIL= new CAStatus("ADDFAIL", 21, CASeverity.WARNING, "Could not perform a database monitor add for that channel");
  /** Count requested inappropriate for that channel. */  
  static public final CAStatus BADCOUNT= new CAStatus("BADCOUNT", 22, CASeverity.WARNING, "Count requested inappropriate for that channel");
  /** The supplied string has improper format. */  
  static public final CAStatus BADSTR= new CAStatus("BADSTR", 23, CASeverity.ERROR, "The supplied string has improper format");
  /** Virtual circuit disconneted. */  
  static public final CAStatus DISCONN= new CAStatus("DISCONN", 24, CASeverity.WARNING, "Virtual circuit disconnect");
  /** identical process variable name on pultiple servers. */  
  static public final CAStatus DBLCHNL= new CAStatus("DBLCHNL", 25, CASeverity.WARNING, "Identical process variable name on multiple servers");
  /** The CA method called is inappropriate for use within an event handler. */  
  static public final CAStatus EVDISALLOW= new CAStatus("EVDISALLOW", 26, CASeverity.ERROR, "The CA routine called is inappropriate for use within an event handler");
  /** Database value get for that channel failed during channel search. */  
  static public final CAStatus BUILDGET= new CAStatus("BUILDGET", 27, CASeverity.WARNING, "Database value get for that channel failed during channel search");
  /** Unable to initialize without the vxWorks VX_FP_TASK task option set. */  
  static public final CAStatus NEEDSFP= new CAStatus("NEEDSFP", 28, CASeverity.WARNING, "Unable to initialize without the vxWorks VX_FP_TASK task option set");
  /** Event queue overflow has prevented first pass event after event add. */  
  static public final CAStatus OVEVFAIL= new CAStatus("OVEVFAIL", 29, CASeverity.WARNING, "Event queue overflow has prevented first pass event after event add");
  /** Bad monitor subscription identifier. */  
  static public final CAStatus BADMONID= new CAStatus("BADMONID", 30, CASeverity.ERROR, "bad monitor subscription identifier");
  /** Remote channel has new network address. */  
  static public final CAStatus NEWADDR= new CAStatus("NEWADDR", 31, CASeverity.WARNING, "Remote channel has new network address");
  /** New or resumed network connection. */  
  static public final CAStatus NEWCONN= new CAStatus("NEWCONN", 32, CASeverity.INFO, "New or resumed network connection");
  /** Specified task isn't a member of a CA Context. */  
  static public final CAStatus NOCACTX= new CAStatus("NOCACTX", 33, CASeverity.WARNING, "Specified task isnt a member of a CA context");
  /** Attempt to use a defunct CA feature failed. */  
  static public final CAStatus DEFUNCT= new CAStatus("DEFUNCT", 34, CASeverity.FATAL, "Attempt to use defunct CA feature failed");
  /** The supplied string is empty. */  
  static public final CAStatus EMPTYSTR= new CAStatus("EMPTYSTR", 35, CASeverity.WARNING, "The supplied string is empty");
  /** Unable to spawn the CA repeater thread- auto reconnect will fail. */  
  static public final CAStatus NOREPEATER= new CAStatus("NOREPEATER", 36, CASeverity.WARNING, "Unable to spawn the CA repeater thread- auto reconnect will fail");
  /** No channel id match for search reply- search reply ignored. */  
  static public final CAStatus NOCHANMSG= new CAStatus("NOCHANMSG", 37, CASeverity.WARNING, "No channel id match for search reply- search reply ignored");
  /** Reseting dead connection- will try to reconnect. */  
  static public final CAStatus DLCKREST= new CAStatus("DLCKREST", 38, CASeverity.WARNING, "Reseting dead connection- will try to reconnect");
  /** Server (IOC) has fallen behind or is not responding- still waiting. */  
  static public final CAStatus SERVBEHIND= new CAStatus("SERVBEHIND", 39, CASeverity.WARNING, "Server (IOC) has fallen behind or is not responding- still waiting");
  /** No internet interface with broadcast available. */  
  static public final CAStatus NOCAST= new CAStatus("NOCAST", 40, CASeverity.WARNING, "No internet interface with broadcast available");
  /** The monitor selection mask supplied is empty or inappropriate. */  
  static public final CAStatus BADMASK= new CAStatus("BADMASK", 41, CASeverity.ERROR, "The monitor selection mask supplied is empty or inappropriate");
  /** IO operations have completed. */  
  static public final CAStatus IODONE= new CAStatus("IODONE", 42, CASeverity.INFO, "IO operations have completed");
  /** IO operations are in progress. */  
  static public final CAStatus IOINPROGESS= new CAStatus("IOINPROGESS", 43, CASeverity.INFO, "IO operations are in progress");
  /** Invalid synchronous group identifier. */  
  static public final CAStatus BADSYNCGRP= new CAStatus("BADSYNCGRP", 44, CASeverity.ERROR, "Invalid synchronous group identifier");
  /** Put callback timed out. */  
  static public final CAStatus PUTCBINPROG= new CAStatus("PUTCBINPROG", 45, CASeverity.ERROR, "Put callback timed out");
  /** Read access denied. */  
  static public final CAStatus NORDACCESS= new CAStatus("NORDACCESS", 46, CASeverity.WARNING, "Read access denied");
  /** Write access denied. */  
  static public final CAStatus NOWTACCESS= new CAStatus("NOWTACCESS", 47, CASeverity.WARNING, "Write access denied");
  /** This anachhronistic feature of CA is no longer supported. */  
  static public final CAStatus ANACHRONISM= new CAStatus("ANACHRONISM", 48, CASeverity.ERROR, "Sorry, that anachronistic feature of CA is no longer supported");
  /** The search/beacon request address list was empty after initialization. */  
  static public final CAStatus NOSEARCHADDR= new CAStatus("NOSEARCHADDR", 49, CASeverity.WARNING, "The search/beacon request address list was empty after initialization");
  /** Data conversion between client's type and the server's type failed. */  
  static public final CAStatus NOCONVERT= new CAStatus("NOCONVERT", 50, CASeverity.WARNING, "Data conversion between client's type and the server's type failed");
  /** Invalid channel identifier. */  
  static public final CAStatus BADCHID= new CAStatus("BADCHID", 51, CASeverity.ERROR, "Invalid channel identifier");
  /** Invalid callback method. */  
  static public final CAStatus BADFUNCPTR= new CAStatus("BADFUNCPTR", 52, CASeverity.ERROR, "Invalid function pointer");
  /** Thread is already attached to a client context. */  
  static public final CAStatus ISATTACHED= new CAStatus("ISATTACHED", 53, CASeverity.WARNING, "Thread is already attached to a client context");
  /** No supprt in service. */  
  static public final CAStatus UNAVAILINSERV= new CAStatus("UNAVAILINSERV", 54, CASeverity.WARNING, "No support in service");
  /** User destroyed channel. */  
  static public final CAStatus CHANDESTROY= new CAStatus("CHANDESTROY", 55, CASeverity.WARNING, "User destroyed channel");
  /** Priority out of range. */  
  static public final CAStatus BADPRIORITY= new CAStatus("BADPRIORITY", 56, CASeverity.ERROR, "Priority out of range");
  /** Preemptive callback not enabled - additional threads may not join. */  
  static public final CAStatus NOTTHREADED= new CAStatus("NOTTHREADED", 57, CASeverity.ERROR, "Preemptive callback not enabled - additional threads may not join");
  /** Client's protocol revision does not support transfers exceeding 16k bytes */
  static public final CAStatus ARRAY16KCLIENT= new CAStatus("ARRAY16KCLIENT", 58, CASeverity.WARNING, "Client's protocol revision does not support transfers exceeding 16k bytes");
  /** Virtual circuit connection sequence aborted */  
  static public final CAStatus CONNSEQTMO= new CAStatus("CONNSEQTMO", 59, CASeverity.WARNING, "Virtual circuit connection sequence aborted");
  /** Virtual circuit connection unresponsive */
  static public final CAStatus UNRESPTMO= new CAStatus("UNRESPTMO", 60, CASeverity.WARNING, "Virtual circuit connection unresponsive");
  
  // not-so-nice optimization (using hashmap would be nicer, but not very fast) 
  static CAStatus[] _cachedTypesByValue;
  
  static {
      // cache all types by value
      // values are all are all unsigned short
      int maxValue = 0;
      for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
          CAStatus t= (CAStatus) it.next();
          if (t.getValue() > maxValue)
              maxValue = t.getValue(); 
        }
      
      _cachedTypesByValue = new CAStatus[maxValue + 1];
      for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
          CAStatus t= (CAStatus) it.next();
          if (t.getValue() >= 0)
              _cachedTypesByValue[t.getValue()] = t;
        }
  }

  private CASeverity _severity;
  private String _msg;
  
  /** Creates a new instance of CAStatus */
  protected CAStatus(String name, int value, CASeverity severity, String msg) {
    super(name,value,_map);
    _severity= severity;
    _msg=msg;
  }
  
  public boolean isSuccessful() {
    return _severity.isSuccessful();
  }

  public boolean isError() {
    return _severity.isError();
  }
  
  public boolean isFatal() {
    return _severity.isFatal();
  }
  
  
  public CASeverity getSeverity() {
    return _severity;
  }
  
  public String getMessage() {
    return _msg;
  }
  
  public String toString() {
    return getClass().getName() + "[" + getName() + "=" + getValue() + ","+_severity.getName()+"="+_severity.getValue()+"]="+getMessage();
  }
  
  
  static public CAStatus forName(String name) {
    return (CAStatus) _map.get(name);
  }
  
  /*  
  static public CAStatus forValue(int value) {
    CAStatus s;
    for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
      s= (CAStatus) it.next();
      if(s.getValue()==value) return s;
    }
    return null;
  }  
  */
  
  static final public CAStatus forValue(final int value ) {
      if (value >= 0 && value < _cachedTypesByValue.length)
          return _cachedTypesByValue[value];
      else
          return null;
    }
  
  /*  CA Status Code Definitions   */

  public final static int CA_M_MSG_NO     = 0x0000FFF8;
  public final static int CA_M_SEVERITY   = 0x00000007;
  public final static int CA_M_LEVEL      = 0x00000003;
  public final static int CA_M_SUCCESS    = 0x00000001;
  public final static int CA_M_ERROR      = 0x00000002;
  public final static int CA_M_SEVERE     = 0x00000004;

  public final static int CA_S_MSG_NO     = 0x0D;
  public final static int CA_S_SEVERITY   = 0x03;

  public final static int CA_V_MSG_NO     = 0x03;
  public final static int CA_V_SEVERITY   = 0x00;
  public final static int CA_V_SUCCESS    = 0x00;

  /**
   * Get EPICS status code.
   * @return EPICS status code.
   */
  public final int getStatusCode()
  {
  	return ((getValue() << CA_V_MSG_NO) & CA_M_MSG_NO) | 
		   ((getSeverity().getValue() << CA_V_SEVERITY) & CA_M_SEVERITY);
  }

  /**
   * Get status instance from EPICS status code.
   * @param value EPICS status code.
   * @return status instance from EPICS status code.
   */
  static final public CAStatus forStatusCode(final int value)
  {
  	return forValue((value & CA_M_MSG_NO) >> CA_V_MSG_NO);
  }

}
