package snp.swh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import snp.CompileUtility;
import snp.License;
import snp.Log;
import snp.NetworkUtilities;
import snp.SecurityUtilities;
/**
 * Our implementation of the Software House (SWH).
 * SWH provides functionality for requesting licenses and verifying licenses + sending libraries.
 * @author Edwin Tay(20529864) & Wan Ying Goh(20784663)
 *
 */
public class SWH {

    /**
     * The path to the top level of all the Java source code this SWH is responsible for.
     */
    private String srcPath;

    /**
     * A map from license strings to licenses.
     */
    private Map<String, License> clientLicenses;

    /**
     * A map of libraries to source files.
     */
    private Map<String, File> libraries;

    /**
     * Provides SSLServerSockets for future usage --- should be initialised with a key store so that
     * this agent can prove their trustworthiness to Linkers and Devs.
     */
    private SSLServerSocketFactory sslservfact;


    /**
     * The server connection that this agent uses to communicate with Developers.
     * In future, it might have been nice to use SocketChannels to open multiple, asynchronous
     * communication channels and select on the appropriate one.
     */
    private SSLServerSocket serverConnection;

    /**
     * KeyPair used for SWH asymmetric encryption and decryption of licenses.
     * Note: En/decrypting didn't really need to be asymmetric if it was all done with a secret that
     * only the software house knew.
     * In light of this, it might have been a weakness/inefficiency to choose asymmetric encryption.
     */
    private KeyPair myKey;

    /**
     * key size for asymmetric encryption keys.
     */
    private static final int keySize = 2048;

    /**
     * algorithm name for encryption.
     */
    private static final String algo = "RSA";

    public SWH(String srcPath, int serverPort, String keyFile, String password)
            throws UnknownHostException, IOException, NoSuchAlgorithmException {
        clientLicenses = new HashMap<String, License>();
        libraries = new HashMap<String, File>();

        sslservfact = (SSLServerSocketFactory) SecurityUtilities.getSSLServerSocketFactory(keyFile,
                password);
        serverConnection = (SSLServerSocket) sslservfact.createServerSocket(serverPort, 0,
                InetAddress.getLocalHost());

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algo);
        keyGen.initialize(keySize);
        myKey = keyGen.genKeyPair();

        this.srcPath = srcPath;
        if (srcPath.endsWith("/")) {
            srcPath = srcPath.substring(0, srcPath.length() - 2);
        }
        Log.log("Created a new SoftwareHouse at "
                + serverConnection.getInetAddress().getCanonicalHostName() + ":"
                + serverConnection.getLocalPort());
    }

    /**
     * Listens for, and calls the appropriate commands, depending upon the requests that are given
     * to the SWH.
     * @throws IOException
     */
    private void listenForCommands() throws IOException {
        SSLSocket connection = null;
        do {
            try {
                connection = (SSLSocket) serverConnection.accept();
            } catch (IOException e) {
                Log.error("IO error whilst accepting connection");
                e.printStackTrace();
            }

            if (connection != null) {
                Log.log("Accepting connection from "
                        + connection.getInetAddress().getCanonicalHostName() + ":"
                        + connection.getPort());
                DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
                if (inStream != null) {
                    String command = null;
                    try {
                        command = inStream.readUTF();
                    } catch (IOException e) {
                        Log.error("Could read command " + "from stream");
                        e.printStackTrace();
                    }

                    if (command != null) {
                        if (command.equalsIgnoreCase("REQ")) {
                            generateLicenses(connection);
                        } else if (command.equalsIgnoreCase("VER")) {
                            acceptLicenses(connection);
                        }
                    }
                }
            }

            try {
                Log.log("Closing connection to "
                        + connection.getInetAddress().getCanonicalHostName() + ":"
                        + connection.getPort());
                connection.close();
            } catch (IOException e) {
                Log.error("IO error whilst" + " closing connection");
                e.printStackTrace();
            }
        } while (true);
    }

    /**
     * @param libName
     * @param srcFile the source file for the associated library
     */
    private void addLibraryFile(String libName, File srcFile) {
        libraries.put(libName, srcFile);
    }

    /**
     * generates licenses for the remote host of connection
     * @param connection
     */
    private void generateLicenses(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);

        if (inStream != null && outStream != null) {
            Log.log("Reading license request from "
                    + connection.getInetAddress().getCanonicalHostName() + ":"
                    + connection.getPort());

            int numLicenses = -1;
            String libName = null;
            try {
                libName = inStream.readUTF();
                numLicenses = inStream.readInt();

                Log.log(connection.getInetAddress().getCanonicalHostName() + ":"
                        + connection.getPort() + " requested " + numLicenses + " licenses for "
                        + libName);
            } catch (IOException e) {
                Log.error("Could not read library name and numLicenses");
                e.printStackTrace();
            }

            if (numLicenses > 0 && libName != null && libraries.containsKey(libName)) {
                try {
                    Log.log("Generating licenses for "
                            + connection.getInetAddress().getCanonicalHostName() + ":"
                            + connection.getPort());
                    outStream.writeInt(numLicenses);

                    for (int i = 0; i < numLicenses; i++) {
                        // construct a license based on some attributes, plus a salt from
                        // Math.random()
                        String s = libName + i + System.currentTimeMillis() + Math.random();
                        MessageDigest md = MessageDigest.getInstance("MD5");

                        // Note that s.getBytes() is not platform independent.
                        // Better approach would be to use character encodings.
                        String license = NetworkUtilities.bytesToHex(md.digest(s.getBytes()));
                        outStream.writeUTF(license);

                        String unencrypted = wrapLicense(license, myKey.getPublic());
                        outStream.writeUTF(unencrypted);

                        addLicense(license, new License(license, InetAddress.getLocalHost(),
                                libName, connection.getLocalPort(), unencrypted));
                    }
                } catch (IOException e) {
                    Log.error("encountered I/O error whilst " + "generating licenses");
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    Log.error("could not construct MD5 message" + "digest");
                    e.printStackTrace();
                }
            } else {
                try {
                    Log.log("Refusing developer license request");
                    Log.log("Found values:\n\tnumLicenses: %d\n"
                            + "\tlibName: %s\n\thasLibrary? %s\n", numLicenses, libName,
                            libraries.containsKey(libName));
                    outStream.writeInt(-1);
                } catch (IOException e) {
                    Log.error("could not say no to Developer");
                    e.printStackTrace();
                }
            }

            NetworkUtilities.closeSocketDataInputStream(inStream, connection);
            NetworkUtilities.closeSocketDataOutputStream(outStream, connection);

            Log.logEnd();
        }
    }

    /**
     * Adds a license to client licenses
     * @param licenseString
     * @param l
     */
    private void addLicense(String licenseString, License l) {
        clientLicenses.put(licenseString, l);
    }

    /**
     * 
     * @param license
     * @return the licnese or null
     */
    private License getLicense(String license) {
        if (clientLicenses.containsKey(license)) {
            return clientLicenses.get(license);
        }
        return null;
    }

    /**
     * Verifies that the license provided by acceptLicenses is okay.
     * @param connection
     */
    private void acceptLicenses(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);
        
        if (inStream != null) {
            Log.log("Checking if license is legitimate");
            String license = null, developerID = null;
            try {
                license = inStream.readUTF();
                developerID = inStream.readUTF();
                Log.log("Read in license %s\n", license);
                license = unwrapLicense(license);
            } catch (IOException e) {
                Log.error("I/O error whilst reading licenses");
                e.printStackTrace();
            }

            if (license != null && verifyLicense(license) && developerID != null) {
                License temp = clientLicenses.get(license);
                String libraryName = temp.getLibraryName();
                Log.log("License corresponds to library %s\n", libraryName);

                Log.log("Compiling class file");

                
                if (!CompileUtility
                        .compileSWHFile(libraries.get(libraryName), libraryName, license)) {
                    Log.error("Could not compile " + libraryName);

                    Log.log("Informing linker that our services are down");
                    
                    try {
                        outStream.writeInt(-2);
                    } catch (IOException e) {
                        Log.error("encountered I/O error whilst sending rejection to Linker");
                        e.printStackTrace();
                    }
                    
                    NetworkUtilities.closeSocketDataOutputStream(outStream, connection);
                }

                // libraryName is a fully qualified classname, e.g. goo.buzz.Buzz
                // therefore, classFilePath is the unqualified className (Buzz) and the ".class"
                // extension.
                String classFilePath = libraryName.substring(libraryName.lastIndexOf('.') + 1)
                + ".class";
                File toWrite = new File(classFilePath);
                try {
                    outStream.writeInt(0);
                } catch(IOException e) {
                    Log.error("Error: encountered I/O error during confirmation of license verification");
                    e.printStackTrace();
                }
                
                if (toWrite.exists() && NetworkUtilities.writeFile(connection, toWrite, libraryName)) {
                    try {
                        if (inStream.readInt() == 0) {
                            Log.log("File sent successfully, removing license");
                            decrementLicense(license);
                        } else {
                            Log.log("Something went wrong on the linker's end");
                            // N.B.: it would be nice to have some kind of resend protocol here
                            // however, we were constrained on time and decided  to focus on other
                            // aspects of the project
                        }
                    } catch (IOException e) {
                        Log.error("Error: encountered I/O error during file transfer");
                        e.printStackTrace();
                    }
                    
                    Log.log("Deleting generated class file");
                    toWrite.delete();
                }
            } else {
                Log.log("Could not verify license, sending rejection to Linker");
               
                try {
                    outStream.writeInt(-1);
                } catch (IOException e) {
                    Log.error("encountered I/O error whilst " + "sending rejection to Linker");
                    e.printStackTrace();
                }
                NetworkUtilities.closeSocketDataOutputStream(outStream, connection);
            }

            NetworkUtilities.closeSocketDataInputStream(inStream, connection);
        }

        Log.logEnd();
    }

    /**
     * 
     * @param license
     * @return the unwrapped (decrypted) license or null
     */
    private String unwrapLicense(String license) {
        // Decrypt license string using our own private key
        PrivateKey privKey = myKey.getPrivate();
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            byte[] licenseBytes = NetworkUtilities.hexStringToByteArray(license);
            byte[] decrypted = cipher.doFinal(licenseBytes);

            String decryptedLicense = NetworkUtilities.bytesToHex(decrypted);
            return decryptedLicense;
        } catch (NoSuchAlgorithmException e) {
            Log.error("Could not find Cipher instance for RSA");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.error("Could not find generate padding");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.error("Provided public key was invalid");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            Log.error("Data block provided for encryption was too big");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            Log.error("Could not generate padding");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param license
     * @return true if the license can be found in clientLicenses, and false otherwise
     */
    private boolean verifyLicense(String license) {
        License temp = getLicense(license);
        if (temp != null) {
            clientLicenses.remove(temp);
            return true;
        }
        return false;
    }

    private void decrementLicense(String license) {
        clientLicenses.remove(license);
    }

    /**
     * @param license
     * @param pubKey
     * @return the wrapped (encrypted) license using the public key or null
     */
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
            Log.error("Could not find Cipher instance for RSA");
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            Log.error("Could not find generate padding");
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            Log.error("Provided public key was invalid");
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            Log.error("Data block provided for encryption was too big");
            e.printStackTrace();
        } catch (BadPaddingException e) {
            Log.error("Could not generate padding");
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Usage: needs 4 arguments.");
            System.err.println("\tArgument 1 = port number");
            System.err.println("\tArgument 2 = keystore filepath");
            System.err.println("\tArgument 3 = keystore password");
            System.err.println("\tArgument 4 = classpath");
            System.exit(1);
        }

        SWH swh = null;
        int portNumber = Integer.parseInt(args[0]);
        String keyFile = args[1];
        // String trustFile = sc.next();
        String password = args[2];
        String classpath = args[3];
        try {
            swh = new SWH(classpath, portNumber, keyFile, password);
        } catch (UnknownHostException e) {
            Log.error("Host name could not be resolved; exiting");
            e.printStackTrace();
        } catch (IOException e) {
            Log.error("An IO error occurred during ServerSocket initialisation; " + "exiting");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            Log.error("Could not get algorithm during RSA construction");
            e.printStackTrace();
        }
        if (swh != null) {
            Scanner sc = new Scanner(System.in);
            System.out.println("How many files is this SoftwareHouse responsible for?");
            int nFiles = sc.nextInt();

            System.out
                    .printf("Enter %d source files in format:\n" + "\t<fully qualified pathname>\n"
                            + "Example:\n" + "\tgoo.buzz.Buzz\n", nFiles);
            for (int i = 0; i < nFiles; i++) {
                String libName = sc.next();
                String libPath = libName.replace('.', '/') + ".java";
                File f = new File(swh.srcPath + "/" + libPath);
                swh.addLibraryFile(libName, f);
            }
            sc.close();
            swh.listenForCommands();
        }

    }

}
