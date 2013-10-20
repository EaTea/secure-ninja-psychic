/**
 * 
 */
package snp.developer;

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
    private String developerID;
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
     */
    private int numberLicenses;
    
    private int port;
    
    private void setPort(int port) {
        this.port = port;
    }
    
    public int getPort() {
        return port;
    }
    /**
     * 
     * @param license
     * @param swhIP
     * @param name
     * @param developerIP
     */
    public License(String license, InetAddress swhIP, String name,
            String developerID, int licenses, int port) {
        this.licenseString = license;
        this.softwareHouseIP = swhIP;
        this.libraryName = name;
        this.setDeveloperID(developerID);
        this.numberLicenses = licenses;
        this.port = port;
    }
    /**
     * 
     * @return
     */
    public String getLicenseString() {
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
    public InetAddress getSoftwareHouseIP() {
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
    public String getLibraryName() {
        return libraryName;
    }
    
    /**
     * 
     * @param libraryName
     */
    private void setLibraryName(String libraryName) {
        this.libraryName = libraryName;
    }

    public String getDeveloperID() {
        return developerID;
    }

    private void setDeveloperID(String developerID) {
        this.developerID = developerID;
    }

    public int getNumberLicenses() {
        return numberLicenses;
    }
    
    public void setNumberLicenses(int numLicense) {
        this.numberLicenses = numLicense;
    }
    
    public void decrementNumberLicenses() {
        numberLicenses--;
    }
}
