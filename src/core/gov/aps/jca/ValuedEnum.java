package gov.aps.jca;

public abstract class ValuedEnum extends Enum {
  private int _value;
  
  protected ValuedEnum(String name, int value) {
    this(name,value,null);
  }
  
  protected ValuedEnum(String name, int value, java.util.Map map) {
    super(name,map);
    _value= value;
  }
  
  
    /**
     * Get value of enum item.
     *
     * @return the enum item's value.
     */
    public final int getValue()
    {
        return _value;
    }

    /**
     * Test if enum item is equal in value to other enum.
     *
     * @param other the other enum
     * @return true if equal
     */
    public final boolean isEqualTo( final ValuedEnum other )
    {
        return _value == other._value;
    }

    /**
     * Test if enum item is greater than in value to other enum.
     *
     * @param other the other enum
     * @return true if greater than
     */
    public final boolean isGreaterThan( final ValuedEnum other )
    {
        return _value > other._value;
    }

    /**
     * Test if enum item is greater than or equal in value to other enum.
     *
     * @param other the other enum
     * @return true if greater than or equal
     */
    public final boolean isGreaterThanOrEqual( final ValuedEnum other )
    {
        return _value >= other._value;
    }

    /**
     * Test if enum item is less than in value to other enum.
     *
     * @param other the other enum
     * @return true if less than
     */
    public final boolean isLessThan( final ValuedEnum other )
    {
        return _value < other._value;
    }

    /**
     * Test if enum item is less than or equal in value to other enum.
     *
     * @param other the other enum
     * @return true if less than or equal
     */
    public final boolean isLessThanOrEqual( final ValuedEnum other )
    {
        return _value <= other._value;
    }

    /**
     * Override toString method to produce human readable description.
     *
     * @return String in the form <code>type[name=value]</code>, eg.:
     * <code>JavaVersion[Java 1.0=100]</code>.
     */
    public String toString()
    {
        return getClass().getName() + "[" + getName() + "=" + _value + "]";
    }
}
  



