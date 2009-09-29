

package gov.aps.jca;



public abstract class Enum {
  
  private final String _name;
  
  protected Enum(final String name) {
    this(name,null);
  }
  
  protected Enum(final String name, final java.util.Map map) {
    _name=name;
    if(map!=null) map.put(_name,this);
  }
  
    public final boolean equals( final Object other )  {
        if( null == other )  {
            return false;
        }  else {
            return other == this || 
                ( other.getClass().getName().equals( this.getClass().getName() ) &&
                _name.equals( ( (Enum)other )._name ) );
        }
    }

    public int hashCode()  {
      return _name.hashCode();
    }

    public final String getName()  {
        return _name;
    }

    public String toString() {
        return getClass().getName() + "[" + _name + "]";
    }

    
  
}

