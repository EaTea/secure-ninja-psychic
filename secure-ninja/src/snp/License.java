/**
 * 
 */
package snp;

import java.net.InetAddress;

/**
 * @author wying1211
 * 
 */
public class License {
    /**
     * 
     */
    private String unencryptedLicense;
    /**
     * 
     */
    private String encryptedLicense;
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
    private int port;

    /**
     * 
     * @param license
     * @param swhIP
     * @param name
     * @param developerIP
     */
    public License(String license, InetAddress swhIP, String name, int port, String encrypted) {
        this.unencryptedLicense = license;
        this.softwareHouseIP = swhIP;
        this.libraryName = name;
        this.port = port;
        this.encryptedLicense = encrypted;
    }

    public int getPort() {
        return port;
    }

    /**
     * 
     * @return
     */
    public String getLicenseString() {
        return unencryptedLicense;
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
     * @return
     */
    public String getLibraryName() {
        return libraryName;
    }

    public String getEncryptedLicenseString() {
        return encryptedLicense;
    }
}
