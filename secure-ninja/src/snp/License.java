package snp;

import java.net.InetAddress;

/**
 * Our implementation of license.
 * @author Edwin Tay(20529864) && Wan Ying Goh(20784663)
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
     * @param license the licenseKey
     * @param swhIP the IP of the issuing SWH
     * @param name libraryName associated with the license
     * @param port the port number used SWH's server
     * @param enrypted the encrypted license key
     */
    public License(String license, InetAddress swhIP, String name, int port, String enrypted) {
        this.unencryptedLicense = license;
        this.softwareHouseIP = swhIP;
        this.libraryName = name;
        this.port = port;
        this.encryptedLicense = enrypted;
    }

    /**
     * @return the port number used by SWH's server.
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the license key
     */
    public String getLicenseString() {
        return unencryptedLicense;
    }

    /**
     * @return the IP of the issuing SWH.
     */
    public InetAddress getSoftwareHouseIP() {
        return softwareHouseIP;
    }

    /**
     * @return the libraryName associated with the license
     */
    public String getLibraryName() {
        return libraryName;
    }

    /**
     * @return the encrypted license key
     */
    public String getEncryptedLicenseString() {
        return encryptedLicense;
    }
}
