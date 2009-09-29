/*
 * Status.java
 *
 * Created on 15 décembre 2003, 09:32
 */

package gov.aps.jca.dbr;

/**
 *
 * @author  rix
 */
public class Status extends gov.aps.jca.ValuedEnum {
  static private final java.util.Map _map= new java.util.HashMap();

  
  static public final Status NO_ALARM= new Status("NO_ALARM", 0);
  static public final Status READ_ALARM= new Status("READ_ALARM", 1);
  static public final Status WRITE_ALARM= new Status("WRITE_ALARM", 2);
  static public final Status HIHI_ALARM= new Status("HIHI_ALARM", 3);
  static public final Status HIGH_ALARM= new Status("HIGH_ALARM", 4);
  static public final Status LOLO_ALARM= new Status("LOLO_ALARM", 5);
  static public final Status LOW_ALARM= new Status("LOW_ALARM", 6);
  static public final Status STATE_ALARM= new Status("STATE_ALARM", 7);
  static public final Status COS_ALARM= new Status("COS_ALARM", 8);
  static public final Status COMM_ALARM= new Status("COMM_ALARM", 9);
  static public final Status TIMEOUT_ALARM= new Status("TIMEOUT_ALARM", 10);
  static public final Status HW_LIMIT_ALARM= new Status("HW_LIMIT_ALARM", 11);
  static public final Status CALC_ALARM= new Status("CALC_ALARM", 12);
  static public final Status SCAN_ALARM= new Status("SCAN_ALARM", 13);
  static public final Status LINK_ALARM= new Status("LINK_ALARM", 14);
  static public final Status SOFT_ALARM= new Status("SOFT_ALARM", 15);
  static public final Status BAD_SUB_ALARM= new Status("BAD_SUB_ALARM", 16);
  static public final Status UDF_ALARM= new Status("UDF_ALARM", 17);
  static public final Status DISABLE_ALARM= new Status("DISABLE_ALARM", 18);
  static public final Status SIMM_ALARM= new Status("SIMM_ALARM", 19);
  static public final Status READ_ACCESS_ALARM= new Status("READ_ACCESS_ALARM", 20);
  static public final Status WRITE_ACCESS_ALARM= new Status("WRITE_ACCESS_ALARM", 21);
  
  // not-so-nice optimization (using hashmap would be nicer, but not very fast) 
  static Status[] _cachedTypesByValue;
  
  static {
      // cache all types by value
      // values are all are all unsigned short
      int maxValue = 0;
      for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
          Status t= (Status) it.next();
          if (t.getValue() > maxValue)
              maxValue = t.getValue(); 
        }
      
      _cachedTypesByValue = new Status[maxValue + 1];
      for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
          Status t= (Status) it.next();
          if (t.getValue() >= 0)
              _cachedTypesByValue[t.getValue()] = t;
        }
  }

  protected Status(String name, int value) {
    super(name,value, _map);
  }
  
  static public Status forName(String name) {
    return (Status) _map.get(name);
  }
  /*
  static public Status forValue(int value) {
    Status s;
    for(java.util.Iterator it= _map.values().iterator(); it.hasNext(); ) {
      s= (Status) it.next();
      if(s.getValue()==value) return s;
    }
    return null;
  }
  */
  static final public Status forValue(final int value ) {
      if (value >= 0 && value < _cachedTypesByValue.length)
          return _cachedTypesByValue[value];
      else
          return null;
    }
  
}
