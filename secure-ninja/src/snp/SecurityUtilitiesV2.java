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

public class SecurityUtilitiesV2 {

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
        KeyStore trustStore = null;
        try {
            trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(fis, password.toCharArray());
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: the algorithm for the keystore could not be loaded");
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            System.err.println("Error: could not process certificates");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("Error: encountered I/O exception while loading trust store");
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            System.err.println("Error: no implementation of keystore available");
            e.printStackTrace();
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
        SecureRandom randomGen = null;
        try {
            randomGen = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: no implementation of RNG algorithm available");
            e.printStackTrace();
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
        
        KeyStore keyStore = null;
        
        System.out.println("Loading KeyStore");
        // generate keystore
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(fis, password.toCharArray());
        } catch (KeyStoreException e) {
            System.err.println("Error: no provider implements this KeyStore type");
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: could not find KeyStore algorithm");
            e.printStackTrace();
            return null;
        } catch (CertificateException e) {
            System.err.println("Error: Certificate parsing encountered an error");
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("Error: encountered I/O exception whilst loading KeyStore");
            e.printStackTrace();
            return null;
        }
        
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
        SecureRandom randomGen = null;
        try {
            randomGen = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: no implementation of RNG algorithm available");
            e.printStackTrace();
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
