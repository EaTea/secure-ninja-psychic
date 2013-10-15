/**
 * 
 */
package edu.uwa.secure_ninja.developer;

import java.net.InetAddress;

/**
 * @author wying1211
 *
 */
public class License {
    /**
     * 
     */
    private String licenseString;
    /**
     * 
     */
    private InetAddress developerIP;
    /**
     * 
     */
    private InetAddress softwareHouseIP;
    /**
     * 
     */
    private String libraryName;

    /**
     * 
     * @param license
     * @param swhIP
     * @param name
     * @param developerIP
     */
    public License(String license, InetAddress swhIP, String name,
            InetAddress developerIP) {
        this.licenseString = license;
        this.softwareHouseIP = swhIP;
        this.libraryName = name;
        this.developerIP = developerIP;
    }
    /**
     * 
     * @return
     */
    protected String getLicenseString() {
        return licenseString;
    }
    
    /**
     * 
     * @param licenseString
     */
    private void setLicenseString(String licenseString) {
        this.licenseString = licenseString;
    }
    
    /**
     * 
     * @return
     */
    protected InetAddress getSoftwareHouseIP() {
        return softwareHouseIP;
    }
    
    /**
     * 
     * @param softwareHouseIP
     */
    private void setSoftwareHouseIP(InetAddress softwareHouseIP) {
        this.softwareHouseIP = softwareHouseIP;
    }
    
    /**
     * 
     * @return
     */
    protected InetAddress getDeveloperIP() {
        return developerIP;
    }
    
    /**
     * 
     * @param softwareHouseIP
     */
    private void setDeveloperIP(InetAddress developerIP) {
        this.developerIP = developerIP;
    }
    
    /**
     * 
     * @return
     */
    protected String getLibraryName() {
        return libraryName;
    }
    
    /**
     * 
     * @param libraryName
     */
    private void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

}
