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
 * $Id: JNI.java,v 1.9 2008-10-27 09:06:49 msekoranja Exp $
 *
 * Modification Log:
 * 01. 05/07/2003  erb  initial development
 *
 */

package gov.aps.jca.jni;

import gov.aps.jca.*;
import gov.aps.jca.dbr.*;

import java.security.*;
import java.io.*;

class JNI {
  
  static private void loadLibrary(String path, String lib) {
    if(path.equals("")) {
      //      System.out.println("Trying : "+lib);
      System.loadLibrary(lib);
    } else {
      try {
        String libname= System.mapLibraryName(lib);

	final String targetArch = JNITargetArch.getTargetArch();
	if ( targetArch.matches("darwin-(ppc|x86)") ) libname = "lib" + lib + ".dylib";
	else if(libname.endsWith(".jnilib")) libname= lib.replaceFirst(".jnilib$", ".so");
        
        libname= (new File(path,libname)).getAbsolutePath();
        //        System.out.println("trying: "+libname);
        
        System.load(libname);
        
      } catch(UnsatisfiedLinkError linkError) {
        //        System.out.println(linkError);
        //        System.out.println("trying: "+lib);
        System.loadLibrary(lib);
      }
    }
  }
  
  static private boolean _initialized=false;
  
  static public void init() throws CAException {
    if(_initialized) return;
    
    PrivilegedAction action= new PrivilegedAction() {
      public Object run() {
        // privileged code goes here:
        
        try {
          try {
            
            String targetArch=JNITargetArch.getTargetArch();
            //            System.out.println("TargetArch: "+targetArch);
            JCALibrary jca=JCALibrary.getInstance();
            
            
            String libPath=jca.getProperty( "gov.aps.jca.jni.epics."+targetArch+
            ".library.path", "" );
            loadLibrary( libPath, "Com" );
            loadLibrary( libPath, "ca" );
            
            File caRepeaterPath=new File( jca.getProperty(
            "gov.aps.jca.jni.epics."+targetArch+".caRepeater.path", "" ) );
            try {
              String caRepeater="caRepeater";
              if( caRepeaterPath.exists() ) {
                caRepeater= ( new File( caRepeaterPath, "caRepeater" ) ).
                getAbsolutePath();
                
              }
              Runtime.getRuntime().exec( caRepeater );
            } catch( java.io.IOException ex ) {
              Runtime.getRuntime().exec( "caRepeater" );
            }
          } catch( Throwable ex2 ) {
            //            System.out.println(ex2);
          }
          //          System.out.println("Loading jca2");
          System.loadLibrary( "jca" );
          
          return null; // nothing to return
        } catch( Exception ex1 ) {
          //          System.out.println(ex1);
          return ex1;
        }
      }
    };
    
    Object res= AccessController.doPrivileged(action);
    if(res!=null) throw new CAException("Unable to initialize gov.aps.jca.jni.JNI", (Exception)res);
    _initialized=true;
  }
  
  
  static private Object _lock=new Object();
  
  static public Object lock() {
    return _lock;
  }
  
  static public void setenv( String name, String value ) {
    synchronized( _lock ) {
      _setenv( name, value );
    }
  }
  
  static public long ctxt_contextCreate( boolean preemptive_callback ) throws JNIException {
    synchronized( _lock ) {
      return _ctxt_contextCreate( preemptive_callback );
    }
  }
  
  static public void ctxt_contextDestroy() throws JNIException {
//    synchronized( _lock ) {
      _ctxt_contextDestroy();
//    }
  }
  
  static public void ctxt_setMessageCallback( JNIContextMessageCallback
  callback ) throws JNIException {
//    synchronized( _lock ) {
      _ctxt_setMessageCallback( callback );
//    }
  }
  
  static public void ctxt_setExceptionCallback( JNIContextExceptionCallback
  callback ) throws JNIException {
//    synchronized( _lock ) {
      _ctxt_setExceptionCallback( callback );
//    }
  }
  
  static public void ctxt_pendIO( double timeout ) throws JNIException {
//    synchronized( _lock ) {
      _ctxt_pendIO( timeout );
//    }
  }
  
  static public boolean ctxt_testIO() throws JNIException {
//    synchronized( _lock ) {
      return _ctxt_testIO();
//    }
  }
  
  static public void ctxt_pendEvent( double time ) throws JNIException {
//    synchronized( _lock ) {
      try {
        _ctxt_pendEvent( time );
      } catch(JNIException jex) {
        if(jex.getStatus()!=CAStatus.TIMEOUT) throw jex;
      }
//    }
  }
  
  static public void ctxt_poll() throws JNIException {
//    synchronized( _lock ) {
    try {
      _ctxt_poll();
    } catch(JNIException jex) {
      if(jex.getStatus()!=CAStatus.TIMEOUT) throw jex;
    }
//    }
  }
  
  static public void ctxt_flushIO() throws JNIException {
//    synchronized( _lock ) {
      _ctxt_flushIO();
//    }
  }
  
  static public void ctxt_attachThread( long ctxtID ) throws JNIException {
//    synchronized( _lock ) {
      _ctxt_attachThread( ctxtID );
//    }
  }
  
  static public long ch_channelCreate( String name,
  JNIConnectionCallback callback, short priority ) throws
  JNIException {
    synchronized( _lock ) {
      return _ch_channelCreate( name, callback, priority );
    }
  }
  
  static public void ch_channelDestroy( long channelID ) throws JNIException {
//    synchronized( _lock ) {
      _ch_channelDestroy( channelID );
//    }
  }
  
  static public void ch_setConnectionCallback( long channelID,
  JNIConnectionCallback callback ) throws
  JNIException {
//    synchronized( _lock ) {
      _ch_setConnectionCallback( channelID, callback );
//    }
  }
  
  static public void ch_setAccessRightsCallback( long channelID,
  JNIAccessRightsCallback
  callback ) throws JNIException {
//    synchronized( _lock ) {
      _ch_setAccessRightsCallback( channelID, callback );
//    }
  }
  
  static public int ch_getFieldType( long channelID ) {
//    synchronized( _lock ) {
      return _ch_getFieldType( channelID );
//    }
  }
  
  static public int ch_getElementCount( long channelID ) {
//    synchronized( _lock ) {
      return _ch_getElementCount( channelID );
//    }
  }
  
  static public int ch_getState( long channelID ) {
//    synchronized( _lock ) {
      return _ch_getState( channelID );
//    }
  }
  
  static public String ch_getHostName( long channelID ) {
//    synchronized( _lock ) {
      return _ch_getHostName( channelID );
//    }
  }
  
  static public boolean ch_getReadAccess( long channelID ) {
//    synchronized( _lock ) {
      return _ch_getReadAccess( channelID );
//    }
  }
  
  static public boolean ch_getWriteAccess( long channelID ) {
//    synchronized( _lock ) {
      return _ch_getWriteAccess( channelID );
//    }
  }
  
  static public void ch_arrayPut( int type, int count, long channelID, long dbrID ) throws
  JNIException {
//    synchronized( _lock ) {
      _ch_arrayPut( type, count, channelID, dbrID );
//    }
  }
  
  static public void ch_arrayPutCallback( int type, int count, long channelID,
  long dbrID, JNIPutCallback callback ) throws
  JNIException {
//    synchronized( _lock ) {
      _ch_arrayPutCallback( type, count, channelID, dbrID, callback );
//    }
  }
  
  static public void ch_arrayGet( int type, int count, long channelID, long dbrID ) throws
  JNIException {
//    synchronized( _lock ) {
      _ch_arrayGet( type, count, channelID, dbrID );
//    }
  }
  
  static public void ch_arrayGetCallback( int type, int count, long channelID,
  JNIGetCallback callback ) throws
  JNIException {
//    synchronized( _lock ) {
      _ch_arrayGetCallback( type, count, channelID, callback );
//    }
  }
  
  static public long ch_addMonitor( int type, int count, long channelID,
  JNIMonitorCallback callback, int mask ) throws
  JNIException {
//    synchronized( _lock ) {
      return _ch_addMonitor( type, count, channelID, callback, mask );
//    }
  }
  
  static public void ch_clearMonitor( long monitorID ) throws JNIException {
//    synchronized( _lock ) {
      _ch_clearMonitor( monitorID );
//    }
  }
  
  static public long dbr_create( int type, int count ) {
//    synchronized( _lock ) {
      return _dbr_create( type, count );
//    }
  }
  
  static public void dbr_destroy( long dbrid ) {
//    synchronized( _lock ) {
      _dbr_destroy( dbrid );
//    }
  }
  
  static public void dbr_setValue( long dbrid, int type, int count, Object value ) {
//    synchronized( _lock ) {
      _dbr_setValue( dbrid, type, count, value );
//    }
  }
  
  static public Object dbr_getValue( long dbrid, int type, int count,
  Object value ) {
//    synchronized( _lock ) {
      return _dbr_getValue( dbrid, type, count, value );
//    }
  }
  
  static public short dbr_getStatus( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getStatus( dbrid, type );
//    }
  }
  
  static public short dbr_getSeverity( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getSeverity( dbrid, type );
//    }
  }
  
  static public Number dbr_getUDL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getUDL( dbrid, type );
//    }
  }
  
  static public Number dbr_getLDL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getLDL( dbrid, type );
//    }
  }
  
  static public Number dbr_getUAL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getUAL( dbrid, type );
//    }
  }
  
  static public Number dbr_getUWL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getUWL( dbrid, type );
//    }
  }
  
  static public Number dbr_getLWL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getLWL( dbrid, type );
//    }
  }
  
  static public Number dbr_getLAL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getLAL( dbrid, type );
//    }
  }
  
  static public Number dbr_getUCL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getUCL( dbrid, type );
//    }
  }
  
  static public Number dbr_getLCL( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getLCL( dbrid, type );
//    }
  }
  
  static public short dbr_getPrecision( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getPrecision( dbrid, type );
//    }
  }
  
  static public TimeStamp dbr_getTimeStamp( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getTimeStamp( dbrid, type );
//    }
  }
  
  static public String[] dbr_getLabels( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getLabels( dbrid, type );
//    }
  }
  
  static public String dbr_getUnits( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getUnits( dbrid, type );
//    }
  }
  
  static public int dbr_getAckT( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getAckT( dbrid, type );
//    }
  }
  
  static public int dbr_getAckS( long dbrid, int type ) {
//    synchronized( _lock ) {
      return _dbr_getAckS( dbrid, type );
//    }
  }
  
  static public void dbr_update( DBR dbr, long dbrid ) {
    
//    synchronized( _lock ) {
      int type=dbr.getType().getValue();
      int count=dbr.getCount();
      
      _dbr_getValue( dbrid, type, count, dbr.getValue() );
      
      if( dbr instanceof STS ) {
        ( ( STS )dbr ).setStatus( _dbr_getStatus( dbrid, type ) );
        ( ( STS )dbr ).setSeverity( _dbr_getSeverity( dbrid, type ) );
      }
      if( dbr instanceof TIME && !dbr.isGR()) {
        ( ( TIME )dbr ).setTimeStamp( _dbr_getTimeStamp( dbrid, type ) );
      }
      if( dbr instanceof GR ) {
        ( ( GR )dbr ).setUnits( _dbr_getUnits( dbrid, type ) );
        ( ( GR )dbr ).setUpperDispLimit( _dbr_getUDL( dbrid, type ) );
        ( ( GR )dbr ).setLowerDispLimit( _dbr_getLDL( dbrid, type ) );
        ( ( GR )dbr ).setUpperAlarmLimit( _dbr_getUAL( dbrid, type ) );
        ( ( GR )dbr ).setUpperWarningLimit( _dbr_getUWL( dbrid, type ) );
        ( ( GR )dbr ).setLowerWarningLimit( _dbr_getLWL( dbrid, type ) );
        ( ( GR )dbr ).setLowerAlarmLimit( _dbr_getLAL( dbrid, type ) );
      }
      if( dbr instanceof CTRL ) {
        ( ( CTRL )dbr ).setUpperCtrlLimit( _dbr_getUCL( dbrid, type ) );
        ( ( CTRL )dbr ).setLowerCtrlLimit( _dbr_getLCL( dbrid, type ) );
      }
      if( dbr instanceof PRECISION ) {
        ( ( PRECISION )dbr ).setPrecision( _dbr_getPrecision( dbrid, type ) );
      }
      if( dbr instanceof LABELS ) {
        ( ( LABELS )dbr ).setLabels( _dbr_getLabels( dbrid, type ) );
      }
      if( dbr instanceof ACK ) {
        ( ( ACK )dbr ).setAckT( _dbr_getAckT( dbrid, type ) != 0 );
        ( ( ACK )dbr ).setAckS( Severity.forValue(_dbr_getAckS( dbrid, type )) );
      }
//    }
  }
  
  static native private void _setenv( String name, String value );
  
  static native private int _ca_getVersion();
  
  static native private int _ca_getRevision();
  
  static native private int _ca_getModification();
  
  static native private String _ca_getVersionString();
  
  static native private String _ca_getReleaseString();
  
  static native private long _ctxt_contextCreate( boolean preemptive_callback ) throws
  JNIException;
  
  static native private void _ctxt_contextDestroy() throws JNIException;
  
  static native private void _ctxt_setMessageCallback(
  JNIContextMessageCallback callback ) throws JNIException;
  
  static native private void _ctxt_setExceptionCallback(
  JNIContextExceptionCallback callback ) throws JNIException;
  
  static native private void _ctxt_pendIO( double timeout ) throws JNIException;
  
  static native private boolean _ctxt_testIO() throws JNIException;
  
  static native private void _ctxt_pendEvent( double time ) throws JNIException;
  
  static native private void _ctxt_poll() throws JNIException;
  
  static native private void _ctxt_flushIO() throws JNIException;
  
  static native private void _ctxt_attachThread( long ctxtID ) throws
  JNIException;
  
  static native private long _ch_channelCreate( String name,
  JNIConnectionCallback callback, short priority ) throws
  JNIException;
  
  static native private void _ch_channelDestroy( long channelID ) throws
  JNIException;
  
  static native private void _ch_setConnectionCallback( long channelID,
  JNIConnectionCallback callback ) throws JNIException;
  
  static native private void _ch_setAccessRightsCallback( long channelID,
  JNIAccessRightsCallback callback ) throws JNIException;
  
  static native private int _ch_getFieldType( long channelID );
  
  static native private int _ch_getElementCount( long channelID );
  
  static native private int _ch_getState( long channelID );
  
  static native private String _ch_getHostName( long channelID );
  
  static native private boolean _ch_getReadAccess( long channelID );
  
  static native private boolean _ch_getWriteAccess( long channelID );
  
  static native private void _ch_arrayPut( int type, int count, long channelID,
		  long dbrID ) throws JNIException;
  
  static native private void _ch_arrayPutCallback( int type, int count,
		  long channelID, long dbrID, JNIPutCallback callback ) throws JNIException;
  
  static native private void _ch_arrayGet( int type, int count, long channelID,
		  long dbrID ) throws JNIException;
  
  static native private void _ch_arrayGetCallback( int type, int count,
		  long channelID, JNIGetCallback callback ) throws JNIException;
  
  static native private long _ch_addMonitor( int type, int count, long channelID,
  JNIMonitorCallback callback,
  int mask ) throws JNIException;
  
  static native private void _ch_clearMonitor( long monitorID ) throws
  JNIException;
  
  static native private long _dbr_create( int type, int count );
  
  static native private void _dbr_destroy( long dbrid );
  
  static native private void _dbr_setValue( long dbrid, int type, int count,
  Object value );
  
  static native private Object _dbr_getValue( long dbrid, int type, int count,
  Object value );
  
  static native private short _dbr_getStatus( long dbrid, int type );
  
  static native private short _dbr_getSeverity( long dbrid, int type );
  
  static native private Number _dbr_getUDL( long dbrid, int type );
  
  static native private Number _dbr_getLDL( long dbrid, int type );
  
  static native private Number _dbr_getUAL( long dbrid, int type );
  
  static native private Number _dbr_getUWL( long dbrid, int type );
  
  static native private Number _dbr_getLWL( long dbrid, int type );
  
  static native private Number _dbr_getLAL( long dbrid, int type );
  
  static native private Number _dbr_getUCL( long dbrid, int type );
  
  static native private Number _dbr_getLCL( long dbrid, int type );
  
  static native private short _dbr_getPrecision( long dbrid, int type );
  
  static native private TimeStamp _dbr_getTimeStamp( long dbrid, int type );
  
  static native private String[] _dbr_getLabels( long dbrid, int type );
  
  static native private String _dbr_getUnits( long dbrid, int type );
  
  static native private int _dbr_getAckT( long dbrid, int type );
  
  static native private int _dbr_getAckS( long dbrid, int type );
  
}
