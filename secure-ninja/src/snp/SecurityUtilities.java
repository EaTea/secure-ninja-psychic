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
 * 
 * @author Edwin Tay(20529864) && Wan Ying Goh(210784663)
 *
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
            System.err.println("Error: the algorithm for the keystore could not be loaded");
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            System.err.println("Error: could not process certificates");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("Error: encountered I/O exception while loading key store");
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            System.err.println("Error: no implementation of keystore available");
            e.printStackTrace();
            return null;
        }
        
    }

    /**
     * Private method to generate SecureRandomNumberGenerator.
     * @return a SecureRandomNumberGenerator if successful. Otherwise, null is returned.
     */
    private static SecureRandom genSecureRandomNumberGenerator() {
        System.out.println("Generating a SHA1PRNG secure random number generator");
        SecureRandom randomGen = null;
        try {
            randomGen = SecureRandom.getInstance(numberAlgo);
            return randomGen;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: no implementation of RNG algorithm available");
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
        System.out.println("Generating SSLSocketFactory with:");
        System.out.println("\ttrustFile: " + trustFile);
        TrustManagerFactory tmf = null;
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(trustFile);
        } catch(FileNotFoundException e) {
            System.err.println("Error: could not find trust store");
            e.printStackTrace();
            return null;
        }
        
        System.out.println("Loading trust store");
        KeyStore trustStore = genKeyStore(fis, password);
        if(trustStore == null) {
            return null;
        }
        
        System.out.println("Generating default trust manager factory");
        try {
            // generate trust manager
            tmf = TrustManagerFactory.getInstance(TrustManagerFactory
                    .getDefaultAlgorithm());
            tmf.init(trustStore);
        } catch(KeyStoreException e) {
            System.err.println("Error: no implementation of trust manager algorithm available");
            e.printStackTrace();
            return null;
        } catch(NoSuchAlgorithmException e) {
            System.err.println("Error: no implementation of trust manager algorithm available");
            e.printStackTrace();
            return null;
        }
        

        System.out.println("Generating a SHA1PRNG secure random number generator");
        SecureRandom randomGen = genSecureRandomNumberGenerator();
        if(randomGen == null) {
            return null;
        }
        
        System.out.println("Generating a SSL context");
        try {
            // generate sslcontext
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, tmf.getTrustManagers(), randomGen);

            return ctx.getSocketFactory();
        } catch(KeyManagementException e) {
            System.err.println("Error: trust manager may have failed whilst setting up SSLContext");
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: no implementation of trust manager algorithm exists");
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
        System.out.println("Generating SSLServerSocketFactory with:");
        System.out.println("\tkeyFile: " + keyFile);
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(keyFile);
        } catch (FileNotFoundException e) {
            System.err.println("Error: could not find keystore file");
            e.printStackTrace();
            return null;
        }
        
        // generate keystore
        System.out.println("Loading KeyStore");
        KeyStore keyStore = genKeyStore(fis, password);
        if (keyStore == null) {
            return null;
        }

        //Generate keyManagerFactory
        System.out.println("Loading KeyManagerFactory");
        KeyManagerFactory kmf = null;
        try {
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            kmf.init(keyStore, password.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: no provider implements this KeyManagerFactory type");
            e.printStackTrace();
            return null;
        } catch (UnrecoverableKeyException e) {
            System.err.println("Error: the password could not be used to open the KeyStore");
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            System.err.println("Error: KeyManagerFactory encountered a problem");
            e.printStackTrace();
            return null;
        }

        System.out.println("Generating a SHA1PRNG secure random number generator");
        SecureRandom randomGen = genSecureRandomNumberGenerator();
        if(randomGen == null) {
            return null;
        }
        
        System.out.println("Generating SSLContext and ServerSocketFactory");
        // generate sslcontext
        SSLContext ctx;
        try {
            ctx = SSLContext.getInstance("SSL");
            ctx.init(kmf.getKeyManagers(), null, randomGen);
            return ctx.getServerSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: no provider implements the requested SSL algorithm");
            e.printStackTrace();
            return null;
        } catch (KeyManagementException e) {
            System.err.println("Error: KeyManagerFactory generation encountered some error");
            e.printStackTrace();
            return null;
        }
    }
}
