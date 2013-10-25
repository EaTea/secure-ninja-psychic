/**
 * 
 */
package snp;

import java.net.InetAddress;
import java.security.PublicKey;


/**
 * 
 * @author Edwin Tay(20529864) && Wan Ying Goh(210784663)
 * @version Oct 2013
 */
public class License {
    /**
     * Storing the licenseKey in unencrypted form.
     */
    private String unencryptedLicense;
    /**
     * Storing the licenseKey in encrypted form.
     */
    private String encryptedLicense;
    /**
     * The IP of softwareHouse that's readable by Java.
     */
    private InetAddress softwareHouseIP;
    /**
     * The libraryName associated with the license.
     */
    private String libraryName;
    /**
     * The port we contacted the softwareHouse on.
     */
    private int port;

    /**
     * License's constructor.
     * @param license the licenseKey of the 
     * @param swhIP
     * @param name
     * @param port
     * @param unencrypted
     */
    public License(String license, InetAddress swhIP, String name, int port, String unencrypted) {
        this.unencryptedLicense = license;
        this.softwareHouseIP = swhIP;
        this.libraryName = name;
        this.port = port;
        this.encryptedLicense = unencrypted;
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

    /**
     * 
     * @return
     */
    public String getEncryptedLicenseString() {
        return encryptedLicense;
    }
}
