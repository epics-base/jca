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
 * $Id: DBR.java,v 1.6 2006-08-30 19:02:39 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.dbr;

import gov.aps.jca.CAStatusException;

import java.io.PrintStream;

public abstract class DBR {
  
  static public final DBRType TYPE= new DBRType("UNKNOWN", -1, null);

  protected int _count;
  protected Object _value;

  protected DBR(Object value) {
	  setValue(value);
  }

  protected void setValue(Object value)
  {
    _value=value;
    _count=java.lang.reflect.Array.getLength( _value );
  }
  
  public DBRType getType() {
    return DBR.TYPE;
  }

  public int getCount() {
    return _count;
  }

  public Object getValue() {
    return _value;
  }

  public abstract DBR convert(DBRType convertType) throws CAStatusException;
  
  public DBR convert(DBRType convertType, Object params) throws CAStatusException {
	  // normally conversion does not need external parameters, so make non-params method default
	  return convert(convertType);
  }

  public void printInfo() {
    printInfo( System.out );
  }

  public void printInfo( PrintStream out ) {
//    out.println( "CLASS: "+getClass().getName() );
    out.println( "TYPE: "+getType().getName() );
    out.println( "COUNT: "+getCount() );
    out.print( "VALUE: " );
    Object value=getValue();
    if( value instanceof byte[] ) {
      printValue( ( byte[] )value, out );
    } else if( value instanceof short[] ) {
      printValue( ( short[] )value, out );
    } else if( value instanceof int[] ) {
      printValue( ( int[] )value, out );
    } else if( value instanceof float[] ) {
      printValue( ( float[] )value, out );
    } else if( value instanceof double[] ) {
      printValue( ( double[] )value, out );
    } else if( value instanceof String[] ) {
      printValue( ( String[] )value, out );
    }
    out.println();
  }

  public boolean isBYTE() {
    return isBYTE( this );
  }

  public boolean isSHORT() {
    return isSHORT( this );
  }

  public boolean isINT() {
    return isINT( this );
  }

  public boolean isFLOAT() {
    return isFLOAT( this );
  }

  public boolean isDOUBLE() {
    return isDOUBLE( this );
  }

  public boolean isSTRING() {
    return isSTRING( this );
  }

  public boolean isENUM() {
    return isENUM( this );
  }

  public boolean isSTS() {
    return isSTS( this );
  }

  public boolean isGR() {
    return isGR( this );
  }

  public boolean isCTRL() {
    return isCTRL( this );
  }

  public boolean isLABELS() {
    return isLABELS( this );
  }

  public boolean isTIME() {
    return isTIME( this );
  }

  public boolean isPRECSION() {
    return isPRECISION( this );
  }

  static public boolean isBYTE( DBR dbr ) {
    return dbr instanceof BYTE;
  }

  static public boolean isSHORT( DBR dbr ) {
    return dbr instanceof SHORT;
  }

  static public boolean isINT( DBR dbr ) {
    return dbr instanceof INT;
  }

  static public boolean isFLOAT( DBR dbr ) {
    return dbr instanceof FLOAT;
  }

  static public boolean isDOUBLE( DBR dbr ) {
    return dbr instanceof DOUBLE;
  }

  static public boolean isSTRING( DBR dbr ) {
    return dbr instanceof STRING;
  }

  static public boolean isENUM( DBR dbr ) {
    return dbr instanceof ENUM;
  }

  static public boolean isSTS( DBR dbr ) {
    return dbr instanceof STS;
  }

  static public boolean isGR( DBR dbr ) {
    return dbr instanceof GR;
  }

  static public boolean isCTRL( DBR dbr ) {
    return dbr instanceof CTRL;
  }

  static public boolean isLABELS( DBR dbr ) {
    return dbr instanceof LABELS;
  }

  static public boolean isTIME( DBR dbr ) {
    return dbr instanceof TIME;
  }

  static public boolean isPRECISION( DBR dbr ) {
    return dbr instanceof PRECISION;
  }

  
  /**
   * Helper method to print an array of byte as a comma separated list to the standard output stream.
   *
   * @param value the array to print.
   */
  static public void printValue( byte[] value ) {
    printValue( value, System.out );
  }

  /**
   * Helper method to print an array of byte as a comma separated list to the specified output stream.
   *
   * @param value the array to print.
   * @param out the output stream.
   */
  static public void printValue( byte[] value, PrintStream out ) {
    for( int t=0; t<value.length; ++t ) {
      if( t>0 ) {
        out.print( "," );
      }
      out.print( value[t] );
    }
  }

  /**
   * Helper method to print an array of short as a comma separated list to the standard output stream.
   *
   * @param value the array to print.
   */
  static public void printValue( short[] value ) {
    printValue( value, System.out );
  }

  /**
   * Helper method to print an array of short as a comma separated list to the specified output stream.
   *
   * @param value the array to print.
   * @param out the output stream.
   */
  static public void printValue( short[] value, PrintStream out ) {
    for( int t=0; t<value.length; ++t ) {
      if( t>0 ) {
        out.print( "," );
      }
      out.print( value[t] );
    }
  }

  /**
   * Helper method to print an array of int as a comma separated list to the standard output stream.
   *
   * @param value the array to print.
   */
  static public void printValue( int[] value ) {
    printValue( value, System.out );
  }

  /**
   * Helper method to print an array of int as a comma separated list to the specified output stream.
   *
   * @param value the array to print.
   * @param out the output stream.
   */
  static public void printValue( int[] value, PrintStream out ) {
    for( int t=0; t<value.length; ++t ) {
      if( t>0 ) {
        out.print( "," );
      }
      out.print( value[t] );
    }
  }

  /**
   * Helper method to print an array of long as a comma separated list to the standard output stream.
   *
   * @param value the array to print.
   */
  static public void printValue( long[] value ) {
    printValue( value, System.out );
  }

  /**
   * Helper method to print an array of long as a comma separated list to the specified output stream.
   *
   * @param value the array to print.
   * @param out the output stream.
   */
  static public void printValue( long[] value, PrintStream out ) {
    for( int t=0; t<value.length; ++t ) {
      if( t>0 ) {
        out.print( "," );
      }
      out.print( value[t] );
    }
  }

  /**
   * Helper method to print an array of float as a comma separated list to the standard output stream.
   *
   * @param value the array to print.
   */
  static public void printValue( float[] value ) {
    printValue( value, System.out );
  }

  /**
   * Helper method to print an array of float as a comma separated list to the specified output stream.
   *
   * @param value the array to print.
   * @param out the output stream.
   */
  static public void printValue( float[] value, PrintStream out ) {
    for( int t=0; t<value.length; ++t ) {
      if( t>0 ) {
        out.print( "," );
      }
      out.print( value[t] );
    }
  }

  /**
   * Helper method to print an array of double as a comma separated list to the standard output stream.
   *
   * @param value the array to print.
   */
  static public void printValue( double[] value ) {
    printValue( value, System.out );
  }

  /**
   * Helper method to print an array of double as a comma separated list to the specified output stream.
   *
   * @param value the array to print.
   * @param out the output stream.
   */
  static public void printValue( double[] value, PrintStream out ) {
    for( int t=0; t<value.length; ++t ) {
      if( t>0 ) {
        out.print( "," );
      }
      out.print( value[t] );
    }
  }

  /**
   * Helper method to print an array of String as a comma separated list to the standard output stream.
   *
   * @param value the array to print.
   */
  static public void printValue( String[] value ) {
    printValue( value, System.out );
  }

  /**
   * Helper method to print an array of String as a comma separated list to the specified output stream.
   *
   * @param value the array to print.
   * @param out the output stream.
   */
  static public void printValue( String[] value, PrintStream out ) {
    for( int t=0; t<value.length; ++t ) {
      if( t>0 ) {
        out.print( "," );
      }
      out.print( value[t] );
    }
  }

}
