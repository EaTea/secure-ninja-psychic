/**
 * 
 */
package snp;

import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
     * @param developerIP
     */
    public License(String license, InetAddress swhIP, String name, int port, byte[] keyBytes, String algo) {
        this.unencryptedLicense = license;
        this.softwareHouseIP = swhIP;
        this.libraryName = name;
        this.port = port;
        if(keyBytes != null && algo != null) {
            PublicKey pubKey = genPublicKey(keyBytes, algo);
            this.encryptedLicense = wrapLicense(license, pubKey);
        } else {
            this.encryptedLicense = null;
        }
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

    private PublicKey genPublicKey(byte[] keyBytes, String algo) {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        try {
            KeyFactory keyFact = KeyFactory.getInstance(algo);
            PublicKey pubKey = keyFact.generatePublic(keySpec);
            return pubKey;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private String wrapLicense(String license, PublicKey pubKey) {
        if (pubKey == null) {
            System.out.println("Null PublicKey received");
            return null;
        }
        // Encrypt a license with a SWH public key using asymmetric key
        // encryption
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encrypted = cipher.doFinal(NetworkUtilities.hexStringToByteArray(license));
            String encryptedLicense = NetworkUtilities.bytesToHex(encrypted);
            return encryptedLicense;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
