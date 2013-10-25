package snp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Utilities class for setting up SSL.
 * @author Edwin Tay(20529864) && Wan Ying Goh(210784663)
 * @version Oct 2013
 */
public class SecurityUtilities {
    /**
     *  The algorithm we used to generate secureRandomGenerator.
     */
    private static final String numberAlgo = "SHA1PRNG";

    /**
     * Private method to generate KeyStore.
     * @param fis the file input stream
     * @param password password for the keyStore
     * @return a KeyStore if successful. Otherwise, null is returned.
     */
    private static KeyStore genKeyStore(FileInputStream fis, String password) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(fis, password.toCharArray());
            fis.close();
            return keyStore;
        } catch (NoSuchAlgorithmException e) {
            Log.error("The algorithm for the keystore could not be loaded");
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            Log.error("Could not process certificates");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            Log.error("Encountered I/O exception while loading key store");
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            Log.error("No implementation of keystore available");
            e.printStackTrace();
            return null;
        }
        
    }

    /**
     * Private method to generate SecureRandomNumberGenerator.
     * @return a SecureRandomNumberGenerator if successful. Otherwise, null is returned.
     */
    private static SecureRandom genSecureRandomNumberGenerator() {
        Log.log("Generating a SHA1PRNG secure random number generator");
        SecureRandom randomGen = null;
        try {
            randomGen = SecureRandom.getInstance(numberAlgo);
            return randomGen;
        } catch (NoSuchAlgorithmException e) {
            Log.error("No implementation of RNG algorithm available");
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Method to generate SSLSocketFactory.
     * @param trustFile the filePath of the trustFile
     * @param password the password for the keyStore
     * @return SSLSocketFactory if successful. Otherwise, null is returned.
     */
    public static SSLSocketFactory getSSLSocketFactory(String trustFile, String password) {
        Log.log("Generating SSLSocketFactory with:");
        Log.log("\ttrustFile: " + trustFile);
        TrustManagerFactory tmf = null;
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(trustFile);
        } catch(FileNotFoundException e) {
            Log.error("Could not find trust store");
            e.printStackTrace();
            return null;
        }
        
        //Generate TrustStore
        Log.log("Loading trust store");
        KeyStore trustStore = genKeyStore(fis, password);
        if(trustStore == null) {
            return null;
        }
        
        //Generate TrustManagerFactory
        Log.log("Generating default trust manager factory");
        try {
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory
                    .getDefaultAlgorithm());
            tmf.init(trustStore);
        } catch(KeyStoreException e) {
            Log.error("No implementation of trust manager algorithm available");
            e.printStackTrace();
            return null;
        } catch(NoSuchAlgorithmException e) {
            Log.error("No implementation of trust manager algorithm available");
            e.printStackTrace();
            return null;
        }
        
        //Generate SecureRandomNumberGenerator
        SecureRandom randomGen = genSecureRandomNumberGenerator();
        if(randomGen == null) {
            return null;
        }
        
        //Generate SSLcontext
        Log.log("Generating a SSL context");
        try {
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, tmf.getTrustManagers(), randomGen);

            return ctx.getSocketFactory();
        } catch(KeyManagementException e) {
            Log.error("trust manager may have failed whilst setting up SSLContext");
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.error("no implementation of trust manager algorithm exists");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Method to generate SSLServerSocketFactory.
     * @param keyFile the filePath of the keyFile
     * @param password the password for the keyStore
     * @return a SSLServerSocketFactory if successful. Otherwise, null is returned.
     */
    public static SSLServerSocketFactory getSSLServerSocketFactory(String keyFile, String password) {
        Log.log("Generating SSLServerSocketFactory with:");
        Log.log("\tkeyFile: " + keyFile);
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(keyFile);
        } catch (FileNotFoundException e) {
            Log.error("could not find keystore file");
            e.printStackTrace();
            return null;
        }
        
        //Generate KeyStore
        Log.log("Loading KeyStore");
        KeyStore keyStore = genKeyStore(fis, password);
        if (keyStore == null) {
            return null;
        }

        //Generate KeyManagerFactory
        Log.log("Loading KeyManagerFactory");
        KeyManagerFactory kmf = null;
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keyStore, password.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            Log.error("No provider implements this KeyManagerFactory type");
            e.printStackTrace();
            return null;
        } catch (UnrecoverableKeyException e) {
            Log.error("The password could not be used to open the KeyStore");
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            Log.error("KeyManagerFactory encountered a problem");
            e.printStackTrace();
            return null;
        }

        //Generate a SecureRandomNumberGenerator
        SecureRandom randomGen = genSecureRandomNumberGenerator();
        if(randomGen == null) {
            return null;
        }

        //Generate SSLContext
        Log.log("Generating SSLContext and ServerSocketFactory");
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(kmf.getKeyManagers(), null, randomGen);
            return ctx.getServerSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            Log.error("No provider implements the requested SSL algorithm");
            e.printStackTrace();
            return null;
        } catch (KeyManagementException e) {
            Log.error("KeyManagerFactory generation encountered some error");
            e.printStackTrace();
            return null;
        }
    }
}
