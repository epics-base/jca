/**********************************************************************
 *
 *      Original Author: Matej Sekoranja
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
 * $Id: Version.java,v 1.2 2004-11-06 15:26:34 msekoranja Exp $
 *
 */

package gov.aps.jca;

/**
 * Administrative class to keep track of the version number of the context
 * implementations.
 */
public class Version {

    /**
     * @see Version#getProductName()
     */
    private String productName;
    
    /**
     * @see Version#getImplementationLanguage()
     */
    private String implementationLanguage;
    
    /**
     * @see Version#getMajorVersion()
     */
    private int majorVersion;
    
    /**
     * @see Version#getMinorVersion()
     */
    private int minorVersion;
    
    /**
     * @see Version#getMaintenanceVersion()
     */
    private int maintenanceVersion;
    
    /**
     * @see Version#getDevelopmentVersion()
     */
    private int developmentVersion;
    
    /**
     * Default consturctor.
     * @param productName	product name.
     * @param implementationLangugage	impementation language.
     * @param majorVersion	major version.
     * @param minorVersion	minor version.
     * @param maintenanceVersion	maintenance version.
     * @param developmentVersion	development version.
     */
    public Version(String productName, String implementationLangugage,
            	   int majorVersion, int minorVersion, int maintenanceVersion,
            	   int developmentVersion)
    {
        //assert (productName != null);
        //assert (implementationLangugage != null);
        
        this.productName = productName;
        this.implementationLanguage = implementationLangugage;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.maintenanceVersion = maintenanceVersion;
        this.developmentVersion = developmentVersion;
    }
    
    /**
     * Get the long version string. Version String formatted like <BR/><CODE>
     * "<B>ProductName </B> \[<B>ImplementationLanguage</B>\] 'v'v.r[.dd|<B>D</B>nn]"
     * </CODE> <BR/>e.g. <BR/><CODE>"<B>CAJ </B> [<B>Java</B>] v1.0.1"</CODE>
     * <BR/>
     * 
     * @return String denoting current version
     */
    public String getLongVersionString()
    {
        return getProductName()
                + " ["
                + getImplementationLanguage()
                + "] v"
                + getMajorVersion()
                + "."
                + getMinorVersion()
                + "."
                + ((getDevelopmentVersion() > 0) ? ("D" + getDevelopmentVersion())
                        : ("" + getMaintenanceVersion()));
    }

    /**
     * Get the basic version string. Version String formatted like <BR/><CODE>
     * "<B>ProductName </B> 'v'v.r[.dd|<B>D</B>nn]"
     * </CODE> <BR/>e.g. <BR/><CODE>"<B>CAJ </B> v1.0.1"</CODE>
     * <BR/>
     * 
     * @return String denoting current version
     */
    public String getVersionString()
    {
        return getProductName()
                + " v"
                + getMajorVersion()
                + "."
                + getMinorVersion()
                + "."
                + ((getDevelopmentVersion() > 0) ? ("D" + getDevelopmentVersion())
                        : ("" + getMaintenanceVersion()));
    }
    
    /**
     * Name of product: Xalan.
     */
    public String getProductName()
    {
        return productName;
    }

    /**
     * Implementation Language: Java.
     */
    public String getImplementationLanguage()
    {
        return implementationLanguage;
    }

    /**
     * Major version number. This changes only when there is a
     * significant, externally apparent enhancement from the previous release.
     * 'n' represents the n'th version.
     * 
     * Clients should carefully consider the implications of new versions as
     * external interfaces and behaviour may have changed.
     */
    public int getMajorVersion()
    {
        return majorVersion;

    }

    /**
     * Minor vesion number. This changes when:
     * <ul>
     * <li>a new set of functionality is to be added</li>
     * <li>API or behaviour change</li>
     * <li>its designated as a reference release</li>
     * </ul>
     */
    public int getMinorVersion()
    {
        return minorVersion;
    }

    /**
     * Maintenance version number. Optional identifier used to designate
     * maintenance drop applied to a specific release and contains fixes for
     * defects reported. It maintains compatibility with the release and
     * contains no API changes. When missing, it designates the final and
     * complete development drop for a release.
     */
    public int getMaintenanceVersion()
    {
        return maintenanceVersion;
    }

    /**
     * Development drop number. Optional identifier designates development drop
     * of a specific release. D01 is the first development drop of a new
     * release.
     * 
     * Development drops are works in progress towards a compeleted, final
     * release. A specific development drop may not completely implement all
     * aspects of a new feature, which may take several development drops to
     * complete. At the point of the final drop for the release, the D suffix
     * will be omitted.
     * 
     * Each 'D' drops can contain functional enhancements as well as defect
     * fixes. 'D' drops may not be as stable as the final releases.
     */
    public int getDevelopmentVersion()
    {
        return developmentVersion;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getLongVersionString();
    }
}