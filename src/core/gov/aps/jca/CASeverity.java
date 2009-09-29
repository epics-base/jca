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
 * $Id: CASeverity.java,v 1.2 2004-12-07 15:36:11 msekoranja Exp $
 *
 */

package gov.aps.jca;

/**
 * Enumeration class representing Channel Access severity codes.
 *
 * @author  Eric Boucher
 */
public class CASeverity extends gov.aps.jca.ValuedEnum {
  static private final java.util.Map _map= new java.util.HashMap();
  
  
  /** Unsuccessful. */  
  static public final CASeverity WARNING= new CASeverity("WARNING", 0x00000000);
  /** Successful. */  
  static public final CASeverity SUCCESS= new CASeverity("SUCCESS", 0x00000001);
  /** Failed-continue. */  
  static public final CASeverity ERROR= new CASeverity("ERROR", 0x00000002);
  /** Successful. */  
  static public final CASeverity INFO= new CASeverity("INFO", 0x00000003);
  /** failed-quit. */  
  static public final CASeverity SEVERE= new CASeverity("SEVERE", 0x00000004);
  /** fatal. */  
  static public final CASeverity FATAL= new CASeverity("FATAL", 0x00000006);
  
  // not-so-nice optimization (using hashmap would be nicer, but not very fast) 
  static CASeverity[] _cachedTypesByValue;
  
  static {
      // cache all types by value
      // values are all are all unsigned short
      int maxValue = 0;
      for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
          CASeverity t= (CASeverity) it.next();
          if (t.getValue() > maxValue)
              maxValue = t.getValue(); 
        }
      
      _cachedTypesByValue = new CASeverity[maxValue + 1];
      for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
          CASeverity t= (CASeverity) it.next();
          if (t.getValue() >= 0)
              _cachedTypesByValue[t.getValue()] = t;
        }
  }
  
  /** Creates a new instance of CASeverity
   * @param name The name of the Severity code.
   * @param value the numerical value of the severity.
   */
  protected CASeverity(String name, int value) {
    super(name,value,_map);
  }
  
  /** tests whether this CASeverity represent a successful code.
   * @return true if this severity value represent a successful operation.
   */  
  public boolean isSuccessful() {
    return (getValue() & 0x00000001)!=0;
  }

  /** Tests whether this CASeverity represent an error code.
   * @return true if this severity value represent an error.
   */  
  public boolean isError() {
    return (getValue() & 0x00000002)!=0;
  }
  
  /** Tests whether this CASeverity represents a fatal code.
   * @return true if this severity value represent an fatal error.
   */  
  public boolean isFatal() {
    return (getValue() & 0x00000004)!=0;
  }
  
  /**
   * Return the CASeverity corresponding to specific name.
   * @param name the name of the CASeverity to look for.
   * @return the corresponding CASeverity if it exists, null otherwise.
   */  
  static public CASeverity forName(String name) {
    return (CASeverity) _map.get(name);
  }
  
  /**
   * Return the CASeverity corresponding to specific numerical value.
   * @param value the value of the CASeverity to look for.
   * @return the corresponding CASeverity if it exists, null otherwise.
   */
  static final public CASeverity forValue(final int value ) {
      if (value >= 0 && value < _cachedTypesByValue.length)
          return _cachedTypesByValue[value];
      else
          return null;
    }
  /*
  static public CASeverity forValue(int value) {
    CASeverity s;
    for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
      s= (CASeverity) it.next();
      if(s.getValue()==value) return s;
    }
    return null;
  }
 */
  
}
