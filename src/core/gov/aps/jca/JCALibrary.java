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
 * $Id: JCALibrary.java,v 1.17 2009-09-14 10:29:56 msekoranja Exp $
 *
 */

package gov.aps.jca;

import gov.aps.jca.cas.Server;
import gov.aps.jca.cas.ServerContext;
import gov.aps.jca.configuration.*;
import gov.aps.jca.dbr.DBRType;

import java.io.*;
import java.util.*;

/**
 * The JCALibrary class is the entry point to all JCA enabled application.
 * There is only one instance of this class which can be accessed by the static method {@link #getInstance()}.
 * This object can be used to retreive all JCA configuration and version and to create new contexts.<br>
 * The JCALibrary can be configured with properties. See <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/Properties.html">java.util.Properties</a> for more details.
 * <pre>
 * <B>Global      :</B> The file &lt;jre home&gt;/lib/JCALibrary.properties defines global properties
 * <B>User        :</B> The file &lt;user home&gt;/.JCALibrary/JCALibrary.properties defines per user's properties
 * <B>Command Line:</B> Properties can also be passed as command line argument using the -D switch
 * </pre>
 * The command line properties take precedence over the user's properties which
 * take precedence over the global properties.<br><br>
 *
 * The JCALibrary class itself doesn't require any configuration however most classes implementation will refer to
 * the properties defined in this files.<br>
 *
 * See the specification of the object's implementation you're planning on using.
 * <br>The core JCA package comes with two Context's implementation:
 * <pre>
 * <B>{@link #JNI_THREAD_SAFE}</B>
 * <B>{@link #JNI_SINGLE_THREADED}</B>
 * <B>{@link #CHANNEL_ACCESS_JAVA}</B>
 * </pre>
 * This two implementations are using the Java Native Interface (JNI) to communicate with channel access servers.
 * They are both accessing a JNI bridge which require a set of two special properties:
 *<pre>
 *  gov.aps.jca.jni.epics.&lt;your arch&gt;.library.path: the path of you're epics distribution shared libraries.
 *  gov.aps.jca.jni.epics.&lt;your arch&gt;.caRepeater.path: the path of you're epics caRepeater executable.
 *</pre>
 *
 * <b>Note:</b> &lt;your arch&gt; represent you're epics host architecture, ie win32-x86, linux-x86, solaris-sparc, etc...<br>
 * <b>Note:</b> The properties files can contain several sets of these properties with different host architectures.<br>
 *
 * @author <a href="mailto:boucher@aps.anl.gov">Eric Boucher</a>
 */
public final class JCALibrary {
  
  static private final int VERSION=2;
  static private final int REVISION=3;
  // TODO version to be incremented - do not forget
  static private final int MODIFICATION=6;
  static private final String VERSION_STRING=""+VERSION+"."+REVISION+"."+MODIFICATION;
  
  static private JCALibrary _instance=null;
  
  /** Getter method to the only instance of JCALibrary.
   * @return the singleton instance of JCALibrary.
   */
  static synchronized public JCALibrary getInstance() {
    if( _instance==null ) {
      _instance=new JCALibrary();
    }
    return _instance;
  }
  
  /**Construct and configure the JCALibrary.
   */
  protected JCALibrary() {

	// initialize DBR types  
	DBRType.initialize();
    
    String fileSep=System.getProperty( "file.separator" );
    String path=null;
    
    // Read Configuration
    try {
        InputStream is = JCALibrary.class.getResourceAsStream( "JCALibrary.properties" );
        if (is == null)
            throw new RuntimeException("resource not found.");
        _builtinProperties.load( is );
    } catch( Throwable ressourceEx ) {
      System.out.println( "Unable to load default configuration located in 'JCALibrary.properties' resource: " + ressourceEx.getMessage() );
    }
    
    try {
      // system's properties
      path=System.getProperty( "java.home" )+fileSep+"lib"+fileSep+
      "JCALibrary.properties";
      _defaultProperties.load( new FileInputStream( path ) );
    } catch( Throwable systemEx ) {
    }
    
    try {
      // properties
      path=System.getProperty( "gov.aps.jca.JCALibrary.properties", null );
      if( path==null ) {
        path=System.getProperty( "user.home" )+fileSep+".JCALibrary"+fileSep+
        "JCALibrary.properties";
      }
      _properties.load( new FileInputStream( path ) );
    } catch( Throwable userEx ) {
    }
    
  }
  
  /**Getter method for the version number.
   * @return the JCALibrary version number.
   */
  public int getVersion() {
    return VERSION;
  }
  
  /**Getter method for the revision number.
   * @return the JCALibrary revision number.
   */
  public int getRevision() {
    return REVISION;
  }
  
  /**Getter method for the modification number.
   * @return the JCALIbrary modification number.
   */
  public int getModification() {
    return MODIFICATION;
  }
  
  /**Getter method for the full version string.
   * @return the JCALibrary version string.
   */
  public String getVersionString() {
    return VERSION_STRING;
  }
  
  /** Print some basic info about the JCALibrary to the standard output stream.
   */
  public void printInfo() {
    printInfo( System.out );
  }
  
  /** Print some basic info about the JCALibrary to the specified output stream.
   * @param out the output stream to send info to.
   */
  public void printInfo( PrintStream out ) {
    out.println( toString() );
  }
  
  public String toString() {
    return JCALibrary.class.getName()+"["+getVersionString()+"]";
  }
  
  // PROPERTIES
  Properties _builtinProperties=new Properties();
  Properties _defaultProperties=new Properties( _builtinProperties );
  Properties _properties=new Properties( _defaultProperties );
  
  /** Retreive a JCALibrary property.
   * @param name the name of the property to search for.
   * @return the string value of the property if it exists, null otherwise.
   */
  public String getProperty( String name ) {
    return System.getProperty( name, _properties.getProperty( name ) );
  }
  
  /** Retreive a JCALibrary property with a default value.
   * @param name the name of the property to search for.
   * @param defaultValue the default value to use if the property doesn't exist.
   * @return the string value of the property if it exists, the defaultValue otherwise.
   */
  public String getProperty( String name, String defaultValue ) {
    return System.getProperty( name,
    _properties.getProperty( name, defaultValue ) );
  }
  
  /** Retreive a JCALibrary property as a float with a default value.
   * @param name the name of the property to search for.
   * @param defaultValue the default value to use if the property doesn't exist.
   * @return the value of the property converted as a float if it exists, the defaultValue otherwise.
   */
  public float getPropertyAsFloat( String name, float defaultValue ) {
    return Float.parseFloat( getProperty( name, String.valueOf( defaultValue ) ) );
  }
  
  /** Retreive a JCALibrary property as an int with a default value.
   * @param name the name of the property to search for.
   * @param defaultValue the default value to use if the property doesn't exist.
   * @return the value of the property converted as a int if it exists, the defaultValue otherwise.
   */
  public int getPropertyAsInt( String name, int defaultValue ) {
    return Integer.parseInt( getProperty( name, String.valueOf( defaultValue ) ) );
  }
  
  /** Retreive a JCALibrary property as a boolean with a default value.
   * @param name the name of the property to search for.
   * @param defaultValue the default value to use if the property doesn't exist.
   * @return the value of the property converted as a boolean if it exists, the defaultValue otherwise.
   */
  public boolean getPropertyAsBoolean( String name, boolean defaultValue ) {
    return Boolean.valueOf( getProperty( name, String.valueOf( defaultValue ) ) ).
    booleanValue();
  }
  
  /** Retreive a JCALibrary property as a double with a default value.
   * @param name the name of the property to search for.
   * @param defaultValue the default value to use if the property doesn't exist.
   * @return the value of the property converted as a double if it exists, the defaultValue otherwise.
   */
  public double getPropertyAsDouble( String name, double defaultValue ) {
    return Double.parseDouble( getProperty( name, String.valueOf( defaultValue ) ) );
  }
  
  /** Retreive a JCALibrary property as a long with a default value.
   * @param name the name of the property to search for.
   * @param defaultValue the default value to use if the property doesn't exist.
   * @return the value of the property converted as a long if it exists, the defaultValue otherwise.
   */
  public long getPropertyAsLong( String name, long defaultValue ) {
    return Long.parseLong( getProperty( name, String.valueOf( defaultValue ) ) );
  }
  
  /**Print all JCALibrary properties to the standard output stream.
   */
  public void listProperties() {
    listProperties( System.out );
  }
  
  /**Print all JCALibrary properties to a specified output stream.
   * @param out the output stream to print the properties to.
   */
  public void listProperties( PrintStream out ) {
    List sys=Collections.list( System.getProperties().propertyNames() );
    List props=Collections.list( _properties.propertyNames() );
    
    props.removeAll( sys );
    props.addAll( sys );
    Collections.sort( props );
    
    String name;
    for( Iterator it=props.iterator(); it.hasNext(); ) {
      name= ( String )it.next();
      out.println( name+"= "+getProperty( name ) );
    }
  }
  
  /**
   * Constant string representing the fully qualified class name of a thread safe Context implementation.
   * This type of context is usefull if you don't want to worry (too much) about thread safety in your application.
   * However you should be award that this feature comes with a price in term of complexity.<br>
   * The internal implementation of this type of context is indeed quite complex and might
   * affect the overall performance of your application.<br><br>
   *
   *
   * You can configure this type of contexts either by using <a href="#Properties">JCALibrary properties</a> or at runtime by using
   * a <a href="#Configuration">Configuration object</a>.<br><br>
   *
   *<table width="100%" border="0" cellspacing="0" cellpadding="0">
   * <tr>
   * <td bgcolor="#99CCFF"><a name="Properties"><b>Using JCALibrary Properties</b></a></td>
   * </tr>
   * </table>
   *<br>
   *
   * <table width="100%" border="1" cellspacing="0" cellpadding="0">
   * <tr>
   * <td width="36%">
   * <div align="center"><b>Property name</b></div>
   * </td>
   * <td width="13%">
   * <div align="center"><b>Range</b></div>
   * </td>
   * <td width="8%">
   * <div align="center"><b>Default value</b></div>
   * </td>
   * <td width="43%">
   * <div align="center"><b>Description</b></div>
   * </td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>preemptive_callback</b></td>
   * <td width="13%">
   * <div align="center">true/false</div>
   * </td>
   * <td width="8%">
   * <div align="center">true</div>
   * </td>
   * <td width="43%">
   * <p>Define whether the context should use independant threads to send request
   * callback notifications (events). <br>
   * If set to <b>no</b>, your application should periodically call pendEvent
   * to process pending events.
   * </td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>addr_list</b></td>
   * <td width="13%">
   * <div align="center">N.N.N.N N.N.N.N:P ...</div>
   * </td>
   * <td width="8%">
   * <div align="center">empty string</div>
   * </td>
   * <td width="43%">A space-separated list of broadcast address for process variable
   * name resolution. Each address must be of the form: ip.number:port or host.name:port</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>auto_addr_list</b></td>
   * <td width="13%">
   * <div align="center">true/false</div>
   * </td>
   * <td width="8%">
   * <div align="center">true</div>
   * </td>
   * <td width="43%">Define whether or not the network interfaces should be discovered
   * at runtime. </td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>connection_timeout</b></td>
   * <td width="13%">
   * <div align="center">&gt;0.1</div>
   * </td>
   * <td width="8%">
   * <div align="center">30.0</div>
   * </td>
   * <td width="43%">If the context doesn't see a beacon from a server that it
   * is connected to for <b>connection_timeout</b> seconds then a state-of-health
   * message is sent to the server over TCP/IP. If this state-of-health message
   * isn't promptly replied to then the context will assume that the server is
   * no longer present on the network and disconnect.</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>beacon_period</b></td>
   * <td width="13%">
   * <div align="center">&gt;0.1</div>
   * </td>
   * <td width="8%">
   * <div align="center">15.0</div>
   * </td>
   * <td width="43%">Period in second between two beacon signals</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>repeater_port</b></td>
   * <td width="13%">
   * <div align="center">&gt;5000</div>
   * </td>
   * <td width="8%">
   * <div align="center">5065</div>
   * </td>
   * <td width="43%">Port number for the repeater to listen to</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>server_port</b></td>
   * <td width="13%">
   * <div align="center">&gt;5000</div>
   * </td>
   * <td width="8%">
   * <div align="center">5064</div>
   * </td>
   * <td width="43%">Port number for the server to listen to</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>max_array_bytes</b></td>
   * <td width="13%">
   * <div align="center">&gt;=16384</div>
   * </td>
   * <td width="8%">
   * <div align="center">16384</div>
   * </td>
   * <td width="43%">Length in bytes of the maximum array size that may pass through
   * Channel Access</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.ThreadSafeContext.<b>event_dispatcher_class</b></td>
   * <td width="13%">
   * <div align="center"></div>
   * </td>
   * <td width="8%">
   * <div align="center">gov.aps.jca.event.DirectEventDispatcher</div>
   * </td>
   * <td width="43%">The fully qualified class name of the event dispatcher used
   * to dispatch callback event. This class must have a default constructor with
   * no arguments. Check the documentation of the event dispatcher to see how
   * to configure it.</td>
   * </tr>
   * </table>
   *
   *<br>
   * <b>Note:</b> If you use the prefix <b>gov.aps.jca.Context</b> or <b>gov.aps.jca.jni.JNIContext</b> instead of <b>gov.aps.jca.jni.ThreadSafeContext</b>
   * then the property will affect both JNI_THREAD_SAFE and JNI_SINGLE_THREADED context configuration (specific configuration overrides default configurations).<br><br>
   *
   *
   *<table width="100%" border="0" cellspacing="0" cellpadding="0">
   * <tr>
   * <td bgcolor="#99CCFF"><a name="Configuration"><b>Using a Configuration object</b></a></td>
   * </tr>
   * </table>
   *<br>
   */
  static public final String JNI_THREAD_SAFE    = "gov.aps.jca.jni.ThreadSafeContext";
  
  /**
   * Constant string representing the fully qualified class name of a single-threaded Context implementation.
   *
   * <table width="100%" border="1" cellspacing="0" cellpadding="0">
   * <tr>
   * <td width="36%">
   * <div align="center"><b>Property name</b></div>
   * </td>
   * <td width="13%">
   * <div align="center"><b>Range</b></div>
   * </td>
   * <td width="8%">
   * <div align="center"><b>Default value</b></div>
   * </td>
   * <td width="43%">
   * <div align="center"><b>Description</b></div>
   * </td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>preemptive_callback</b></td>
   * <td width="13%">
   * <div align="center">true/false</div>
   * </td>
   * <td width="8%">
   * <div align="center">true</div>
   * </td>
   * <td width="43%">
   * <p>Define whether the context should use independant threads to send request
   * callback notifications (events). <br>
   * If set to <b>no</b>, your application should periodically call pendEvent
   * to process pending events.
   * </td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>addr_list</b></td>
   * <td width="13%">
   * <div align="center">N.N.N.N N.N.N.N:P ...</div>
   * </td>
   * <td width="8%">
   * <div align="center">empty string</div>
   * </td>
   * <td width="43%">A space-separated list of broadcast address for process variable
   * name resolution. Each address must be of the form: ip.number:port or host.name:port</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>auto_addr_list</b></td>
   * <td width="13%">
   * <div align="center">true/false</div>
   * </td>
   * <td width="8%">
   * <div align="center">true</div>
   * </td>
   * <td width="43%">Define whether or not the network interfaces should be discovered
   * at runtime. </td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>connection_timeout</b></td>
   * <td width="13%">
   * <div align="center">&gt;0.1</div>
   * </td>
   * <td width="8%">
   * <div align="center">30.0</div>
   * </td>
   * <td width="43%">If the context doesn't see a beacon from a server that it
   * is connected to for <b>connection_timeout</b> seconds then a state-of-health
   * message is sent to the server over TCP/IP. If this state-of-health message
   * isn't promptly replied to then the context will assume that the server is
   * no longer present on the network and disconnect.</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>beacon_period</b></td>
   * <td width="13%">
   * <div align="center">&gt;0.1</div>
   * </td>
   * <td width="8%">
   * <div align="center">15.0</div>
   * </td>
   * <td width="43%">Period in second between two beacon signals</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>repeater_port</b></td>
   * <td width="13%">
   * <div align="center">&gt;5000</div>
   * </td>
   * <td width="8%">
   * <div align="center">5065</div>
   * </td>
   * <td width="43%">Port number for the repeater to listen to</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>server_port</b></td>
   * <td width="13%">
   * <div align="center">&gt;5000</div>
   * </td>
   * <td width="8%">
   * <div align="center">5064</div>
   * </td>
   * <td width="43%">Port number for the server to listen to</td>
   * </tr>
   * <tr>
   * <td width="36%">gov.aps.jca.jni.SingleThreadedContext.<b>max_array_bytes</b></td>
   * <td width="13%">
   * <div align="center">&gt;=16384</div>
   * </td>
   * <td width="8%">
   * <div align="center">16384</div>
   * </td>
   * <td width="43%">Length in bytes of the maximum array size that may pass through
   * Channel Access</td>
   * </tr>
   * </table>
   * <br/>
   * <b>Note:</b> If you use the prefix <b>gov.aps.jca.Context</b> or <b>gov.aps.jca.jni.JNIContext</b> instead of <b>gov.aps.jca.jni.SingleThreadedContext</b>
   * then the property will affect both JNI_THREAD_SAFE and JNI_SINGLE_THREADED context configuration (specific configuration overrides default configurations).<br><br>
   */
  static public final String JNI_SINGLE_THREADED= "gov.aps.jca.jni.SingleThreadedContext";
  
  /** Constant string representing the fully qualified class name of a 100% pure java channel access Context implementation.
  *
  *
  * <table width="100%" border="1" cellspacing="0" cellpadding="0">
  * <tr>
  * <td width="36%">
  * <div align="center"><b>Property name</b></div>
  * </td>
  * <td width="13%">
  * <div align="center"><b>Range</b></div>
  * </td>
  * <td width="8%">
  * <div align="center"><b>Default value</b></div>
  * </td>
  * <td width="43%">
  * <div align="center"><b>Description</b></div>
  * </td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.CAJContext.<b>addr_list</b></td>
  * <td width="13%">
  * <div align="center">N.N.N.N N.N.N.N:P ...</div>
  * </td>
  * <td width="8%">
  * <div align="center">empty string</div>
  * </td>
  * <td width="43%">A space-separated list of broadcast address for process variable
  * name resolution. Each address must be of the form: ip.number:port or host.name:port</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.CAJContext.<b>auto_addr_list</b></td>
  * <td width="13%">
  * <div align="center">true/false</div>
  * </td>
  * <td width="8%">
  * <div align="center">true</div>
  * </td>
  * <td width="43%">Define whether or not the network interfaces should be discovered
  * at runtime. </td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.CAJContext.<b>connection_timeout</b></td>
  * <td width="13%">
  * <div align="center">&gt;0.1</div>
  * </td>
  * <td width="8%">
  * <div align="center">30.0</div>
  * </td>
  * <td width="43%">If the context doesn't see a beacon from a server that it
  * is connected to for <b>connection_timeout</b> seconds then a state-of-health
  * message is sent to the server over TCP/IP. If this state-of-health message
  * isn't promptly replied to then the context will assume that the server is
  * no longer present on the network and disconnect.</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.CAJContext.<b>beacon_period</b></td>
  * <td width="13%">
  * <div align="center">&gt;0.1</div>
  * </td>
  * <td width="8%">
  * <div align="center">15.0</div>
  * </td>
  * <td width="43%">Period in second between two beacon signals</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.CAJContext.<b>repeater_port</b></td>
  * <td width="13%">
  * <div align="center">&gt;5000</div>
  * </td>
  * <td width="8%">
  * <div align="center">5065</div>
  * </td>
  * <td width="43%">Port number for the repeater to listen to</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.CAJContext.<b>server_port</b></td>
  * <td width="13%">
  * <div align="center">&gt;5000</div>
  * </td>
  * <td width="8%">
  * <div align="center">5064</div>
  * </td>
  * <td width="43%">Port number for the server to listen to</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.CAJContext.<b>max_array_bytes</b></td>
  * <td width="13%">
  * <div align="center">&gt;=16384</div>
  * </td>
  * <td width="8%">
  * <div align="center">16384</div>
  * </td>
  * <td width="43%">Length in bytes of the maximum array size that may pass through
  * Channel Access</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersThreadPool.<b>thread_pool_size</b></td>
  * <td width="13%">
  * <div align="center">&gt;=2</div>
  * </td>
  * <td width="8%">
  * <div align="center">5</div>
  * </td>
  * <td width="43%">Number of threads to be used to process network events</td>
  * </tr>
  * </table>
  * <br/>
   * <b>Note:</b> prefix <b>gov.aps.jca.Context</b> can be used instead of <b>com.cosylab.epics.caj.CAJContext</b> 
   * to set global context configuration (specific configuration overrides default configurations).<br><br>
  */
 static public final String CHANNEL_ACCESS_JAVA = "com.cosylab.epics.caj.CAJContext";
 
  /** Create a new context instance using a fully qualified class name and using the Context's default configuration.
   * The context class should define a default constructor with no argument.
   * @param fqn the fully qualified class name of the context to create.
   * @return the new context.
   * @exception CAException is thrown if the context could not be instanciated.
   * @see #JNI_THREAD_SAFE
   * @see #JNI_SINGLE_THREADED
   * @see #createContext(gov.aps.jca.configuration.Configuration configuration)
   */
  public Context createContext(String fqn) throws CAException {
    DefaultConfiguration conf= new DefaultConfiguration("CONTEXT");
    conf.setAttribute("class",  fqn);
    return createContext(conf);
  }
  
  /** Create a new context instance using a Configuration object.
   * The Configuration object must define an attribute called <I>class</I>
   * with the fully qualified class name of the Context as a value.
   * All other attributes or values are specific to the Context to create.
   *
   * @param configuration the Configuration object containing the Context's class name and configuration.
   * @return the new context.
   * @exception CAException is thrown if the context could not be instanciated.
   * @see #JNI_THREAD_SAFE
   * @see #JNI_SINGLE_THREADED
   * @see gov.aps.jca.configuration.Configuration
   */
  public Context createContext(Configuration configuration) throws CAException {
    try {
      String fqn= configuration.getAttribute("class");
      
      Class contextClass= Class.forName(fqn);
      Context context= (Context) contextClass.newInstance();
      if(context instanceof Configurable) {
        ((Configurable)context).configure(configuration);
      }
      return context;
    } catch(Throwable th) {
      throw new CAException("Unable to create context", th);
    }
  }

  
  /**********************************************************************************************/
  
  /** Constant string representing the fully qualified class name of a 100% pure java channel access ServerContext implementation.
  *
  * The following properties to be supported:
  * <table width="100%" border="1" cellspacing="0" cellpadding="0">
  * <tr>
  * <td width="36%">
  * <div align="center"><b>Property name</b></div>
  * </td>
  * <td width="13%">
  * <div align="center"><b>Range</b></div>
  * </td>
  * <td width="8%">
  * <div align="center"><b>Default value</b></div>
  * </td>
  * <td width="43%">
  * <div align="center"><b>Description</b></div>
  * </td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.cas.CAJServerContext.<b>beacon_addr_list</b></td>
  * <td width="13%">
  * <div align="center">N.N.N.N N.N.N.N:P ...</div>
  * </td>
  * <td width="8%">
  * <div align="center">com.cosylab.epics.caj.CAJContext.addr_list (empty string)</div>
  * </td>
  * <td width="43%">A space-separated list of broadcast address which to send beacons.
  * Each address must be of the form: ip.number:port or host.name:port</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.cas.CAJServerContext.<b>auto_beacon_addr_list</b></td>
  * <td width="13%">
  * <div align="center">true/false</div>
  * </td>
  * <td width="8%">
  * <div align="center">com.cosylab.epics.caj.CAJContext.auto_addr_list (true)</div>
  * </td>
  * <td width="43%">Define whether or not the network interfaces should be discovered
  * at runtime. </td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.cas.CAJServerContext.<b>beacon_period</b></td>
  * <td width="13%">
  * <div align="center">&gt;0.1</div>
  * </td>
  * <td width="8%">
  * <div align="center">com.cosylab.epics.caj.CAJContext.beacon_period (15.0)</div>
  * </td>
  * <td width="43%">Period in second between two beacon signals</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.cas.CAJServerContext.<b>beacon_port</b></td>
  * <td width="13%">
  * <div align="center">&gt;5000</div>
  * </td>
  * <td width="8%">
  * <div align="center">com.cosylab.epics.caj.CAJContext.repeater_port (5065)</div>
  * </td>
  * <td width="43%">Port number which to sends beacons</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.cas.CAJServerContext.server_port</td>
  * <td width="13%">
  * <div align="center">&gt;5000</div>
  * </td>
  * <td width="8%">
  * <div align="center">com.cosylab.epics.caj.CAJContext.server_port (5064)</div>
  * </td>
  * <td width="43%">Port number for the server to listen to</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.cas.CAJServerContext.<b>ignore_addr_list</b></td>
  * <td width="13%">
  * <div align="center">N.N.N.N N.N.N.N:P ...</div>
  * </td>
  * <td width="8%">
  * <div align="center">empty string</div>
  * </td>
  * <td width="43%">A space-separated list of addresses which name resolution request to ignore from.
  * Each address must be of the form: ip.number:port or host.name:port</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.cas.CAJServerContext.<b>max_array_bytes</b></td>
  * <td width="13%">
  * <div align="center">&gt;=16384</div>
  * </td>
  * <td width="8%">
  * <div align="center">com.cosylab.epics.caj.CAJContext.max_array_bytes (16384)</div>
  * </td>
  * <td width="43%">Length in bytes of the maximum array size that may pass through
  * Channel Access</td>
  * </tr>
  * <tr>
  * <td width="36%">com.cosylab.epics.caj.impl.reactor.lf.LeaderFollowersThreadPool.<b>thread_pool_size</b></td>
  * <td width="13%">
  * <div align="center">&gt;=2</div>
  * </td>
  * <td width="8%">
  * <div align="center">5</div>
  * </td>
  * <td width="43%">Number of threads to be used to process network events</td>
  * </tr>
  * </table>
  * <br/>
   * <b>Note:</b> prefix <b>gov.aps.jca.Context</b> can be used instead of <b>com.cosylab.epics.caj.CAJServerContext</b> 
   * to set global context configuration (specific configuration overrides default configurations).<br><br>
  */
 static public final String CHANNEL_ACCESS_SERVER_JAVA = "com.cosylab.epics.caj.cas.CAJServerContext";
 
  /** Create a new context instance using a fully qualified class name and using the ServerContext's default configuration.
   * The context class should define a default constructor with no argument.
   * @param fqn the fully qualified class name of the context to create.
   * @param server	<code>Server</code> implementation providing <code>ProcessVariable</code> access (existance test and attach).
   * @return the new context.
   * @exception CAException is thrown if the context could not be instanciated.
   * @see #CHANNEL_ACCESS_SERVER_JAVA
   * @see #createServerContext(gov.aps.jca.configuration.Configuration configuration)
   */
  public ServerContext createServerContext(String fqn, Server server) throws CAException {
    DefaultConfiguration conf= new DefaultConfiguration("SERVER_CONTEXT");
    conf.setAttribute("class",  fqn);
    return createServerContext(conf, server);
  }
  
  /** Create a new server context instance using a Configuration object.
   * The Configuration object must define an attribute called <I>class</I>
   * with the fully qualified class name of the Context as a value.
   * All other attributes or values are specific to the Context to create.
   *
   * @param configuration the Configuration object containing the ServerContext's class name and configuration.
   * @param server	<code>Server</code> implementation providing <code>ProcessVariable</code> access (existance test and attach).
   * @return the new context.
   * @exception CAException is thrown if the context could not be instanciated.
   * @see #CHANNEL_ACCESS_SERVER_JAVA
   * @see gov.aps.jca.configuration.Configuration
   */
  public ServerContext createServerContext(Configuration configuration, Server server) throws CAException {
    try {
      String fqn= configuration.getAttribute("class");
      
      Class contextClass= Class.forName(fqn);
      ServerContext context= (ServerContext) contextClass.newInstance();
      context.initialize(server);
      if(context instanceof Configurable) {
        ((Configurable)context).configure(configuration);
      }
      return context;
    } catch(Throwable th) {
      throw new CAException("Unable to create server context", th);
    }
  }
  
  
}
