/*
 * Severity.java
 *
 * Created on 15 décembre 2003, 09:45
 */

package gov.aps.jca.dbr;

/**
 *
 * @author  rix
 */
public class Severity extends gov.aps.jca.ValuedEnum {
  static private java.util.Map _severity= new java.util.HashMap();
  
  static public final Severity NO_ALARM= new Severity("NO_ALARM", 0);
  static public final Severity MINOR_ALARM= new Severity("MINOR_ALARM", 1);
  static public final Severity MAJOR_ALARM= new Severity("MAJOR_ALARM", 2);
  static public final Severity INVALID_ALARM= new Severity("INVALID_ALARM", 3);
  
  // not-so-nice optimization (using hashmap would be nicer, but not very fast) 
  static Severity[] _cachedTypesByValue;
  
  static {
      // cache all types by value
      // values are all are all unsigned short
      int maxValue = 0;
      for(java.util.Iterator it= _severity.values().iterator(); it.hasNext(); ) {
          Severity t= (Severity) it.next();
          if (t.getValue() > maxValue)
              maxValue = t.getValue(); 
        }
      
      _cachedTypesByValue = new Severity[maxValue + 1];
      for(java.util.Iterator it= _severity.values().iterator(); it.hasNext(); ) {
          Severity t= (Severity) it.next();
          if (t.getValue() >= 0)
              _cachedTypesByValue[t.getValue()] = t;
        }
  }
  
  static public Severity forName(String name) {
    return (Severity) _severity.get(name);
  }
  /*  
  static public Severity forValue(int value) {
    Severity s;
    for(java.util.Iterator it= _severity.values().iterator(); it.hasNext(); ) {
      s= (Severity) it.next();
      if(s.getValue()==value) return s;
    }
    return null;
  }
  */
  static final public Severity forValue(final int value ) {
      if (value >= 0 && value < _cachedTypesByValue.length)
          return _cachedTypesByValue[value];
      else
          return null;
    }
  
  /** Creates a new instance of Severity */
  protected Severity(String name, int value) {
    super(name,value,_severity);
  }
  
}
