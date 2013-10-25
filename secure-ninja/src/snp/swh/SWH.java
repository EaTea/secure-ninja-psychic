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
import snp.NetworkUtilities;
import snp.SecurityUtilities;

public class SWH {

    private String classpath;
    
    private Map<String, License> clientLicenses;

    private Map<String, File> libraries;

    private SSLServerSocketFactory sslservfact;

    private SSLServerSocket serverConnection;

    private KeyPair myKey;
    
    private static final int keySize = 2048;
    
    private static final String algo = "RSA";
    
    public SWH(String classpath, int serverPort, String keyFile, String password) throws UnknownHostException,
            IOException, NoSuchAlgorithmException {
        clientLicenses = new HashMap<String, License>();
        libraries = new HashMap<String, File>();
        sslservfact = (SSLServerSocketFactory) SecurityUtilities.getSSLServerSocketFactory(
                keyFile, password);
        serverConnection = (SSLServerSocket) sslservfact.createServerSocket(serverPort, 0,
                InetAddress.getLocalHost());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algo);
        keyGen.initialize(keySize);
        myKey = keyGen.genKeyPair();
        this.classpath = classpath;
        if (classpath.endsWith("/")) {
            classpath = classpath.substring(0, classpath.length()-2);
        }
        System.out.println("Created a new SoftwareHouse at "
                + serverConnection.getInetAddress().getCanonicalHostName() + ":"
                + serverConnection.getLocalPort());
    }

    private void listenForCommands() throws IOException {
        SSLSocket connection = null;
        System.out.println();
        do {
            try {
                connection = (SSLSocket) serverConnection.accept();
            } catch (IOException e) {
                System.err.println("Error: IO error whilst" + " accepting connection");
                e.printStackTrace();
            }
    
            if (connection != null) {
                System.out.println("Accepting connection from "
                        + connection.getInetAddress().getCanonicalHostName() + ":"
                        + connection.getPort());
                DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
                if (inStream != null) {
                    String command = null;
                    try {
                        command = inStream.readUTF();
                    } catch (IOException e) {
                        System.err.println("Error: could read command " + "from stream");
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
                System.out.println("Closing connection to "
                        + connection.getInetAddress().getCanonicalHostName() + ":"
                        + connection.getPort());
                connection.close();
            } catch (IOException e) {
                System.err.println("Error: IO error whilst" + " closing connection");
                e.printStackTrace();
            }
        } while (true);
    }

    private void addLibraryFile(String libName, File f) {
        libraries.put(libName, f);
    }

    private void generateLicenses(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);
    
    
        if (inStream != null && outStream != null) {
            System.out.println("Reading license request from "
                    + connection.getInetAddress().getCanonicalHostName() + ":"
                    + connection.getPort());
    
            int numLicenses = -1;
            String libName = null;
            try {
                libName = inStream.readUTF();
                numLicenses = inStream.readInt();
    
                System.out.println(connection.getInetAddress().getCanonicalHostName() + ":"
                        + connection.getPort() + " requested " + numLicenses + " licenses for "
                        + libName);
            } catch (IOException e) {
                System.err.println("Error: could not " + "read library name and numLicenses");
                e.printStackTrace();
            }
    
            if (numLicenses > 0 && libName != null && libraries.containsKey(libName)) {
                try {
                    System.out.println("Sending my public key to developer");
                    byte[] keyBytes = myKey.getPublic().getEncoded();
                    outStream.writeUTF(algo);
                    outStream.writeInt(keyBytes.length);
                    outStream.write(keyBytes);
    
                    System.out.println("Generating licenses for "
                            + connection.getInetAddress().getCanonicalHostName() + ":"
                            + connection.getPort());
                    outStream.writeInt(numLicenses);
    
                    for (int i = 0; i < numLicenses; i++) {
                        String s = libName + i + System.currentTimeMillis() + Math.random();
                        MessageDigest md = MessageDigest.getInstance("MD5");
    
                        // Note that s.getBytes() is not platform independent.
                        // Better approach would be to use character encodings.
                        String license = NetworkUtilities.bytesToHex(md.digest(s.getBytes()));
                        outStream.writeUTF(license);
                        //Developer told us that they cant encrypt our license with our public key
                        if(!inStream.readBoolean()) {
                            System.out.println("Developer unable to "
                             + "encrypt our license with our key, exiting");
                            NetworkUtilities.closeSocketDataInputStream(inStream, connection);
                            NetworkUtilities.closeSocketDataOutputStream(outStream, connection);
    
                            System.out.println("<-----End Communication----->");
                            System.out.println();
                            return;
                        }
                        addLicense(license, new License(license, InetAddress.getLocalHost(),
                                libName, connection.getLocalPort(), null,null 
                                /*SWH to encrypt the licenses*/));
                    }
                } catch (IOException e) {
                    System.err.println("Error: encountered I/O error whilst "
                            + "generating licenses");
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("Error: could not construct MD5 message" + "digest");
                    e.printStackTrace();
                }
            } else {
                try {
                    System.out.println("Refusing developer license request");
                    System.out.printf("Found values:\n\tnumLicenses: %d\n"
                            + "\tlibName: %s\n\thasLibrary? %s\n", numLicenses, libName,
                            libraries.containsKey(libName));
                    outStream.writeInt(-1);
                } catch (IOException e) {
                    System.err.println("Error: could not say no to Developer");
                    e.printStackTrace();
                }
            }
    
            NetworkUtilities.closeSocketDataInputStream(inStream, connection);
            NetworkUtilities.closeSocketDataOutputStream(outStream, connection);
    
            System.out.println("<-----End Communication----->");
            System.out.println();
        }
    }

    private void addLicense(String licenseString, License l) {
        clientLicenses.put(licenseString, l);
    }

    private License getLicense(String license) {
        if (clientLicenses.containsKey(license)) {
            return clientLicenses.get(license);
        }
        return null;
    }

    private void acceptLicenses(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        if (inStream != null) {
            System.out.println("Checking if license is legitimate");
            String license = null, developerID = null;
            try {
                license = inStream.readUTF();
                developerID = inStream.readUTF();
                System.out.printf("Read in license %s\n", license);
                license = unwrapLicense(license);
            } catch (IOException e) {
                System.err.println("Error: I/O error whilst reading licenses");
                e.printStackTrace();
            }
    
            if (license != null && verifyLicense(license) && developerID != null) {
                License temp = clientLicenses.get(license);
                String libraryName = temp.getLibraryName();
                System.out.printf("License corresponds to library %s\n", libraryName);
                
                System.out.println("Compiling class file");
                
                CompileUtility.compileSWHFile(libraries.get(libraryName), libraryName, license);
                
                String classFilePath = libraryName.substring(libraryName.lastIndexOf('.')+1)
                    + ".class";
                File toWrite = new File(classFilePath);
    
                if (NetworkUtilities.writeFile(connection, toWrite, libraryName)) {
                    try {
                        if (inStream.readBoolean()) {
                            System.out.println("File sent successfully, removing license");
                            decrementLicense(license);
                        } else {
                            System.err.println("Something went wrong on the" + " linker's end");
                        }
                    } catch (IOException e) {
                        System.err
                                .println("Error: encountered I/O error during" + " file transfer");
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("Could not verify license, sending" + " rejection to Linker");
                DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);
    
                try {
                    outStream.writeLong(-1);
                } catch (IOException e) {
                    System.err.println("Error: encountered I/O error whilst "
                            + "sending rejection to Linker");
                    e.printStackTrace();
                }
                NetworkUtilities.closeSocketDataOutputStream(outStream, connection);
            }
    
            NetworkUtilities.closeSocketDataInputStream(inStream, connection);
        }
    
        System.out.println("<-----End Communication----->");
        System.out.println();
    }

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

    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.err.println("Usage: needs 3 arguments.");
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
            // TODO
            swh = new SWH(classpath, portNumber, keyFile, password);
        } catch (UnknownHostException e) {
            System.err.println("Error: host name could not be resolved; exiting");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: an IO error occurred during ServerSocket initialisation; " +
            		"exiting");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (swh != null) {
            Scanner sc = new Scanner(System.in);
            System.out.println("How many files is this SoftwareHouse responsible for?");
            int nFiles = sc.nextInt();

            System.out.printf("Enter %d source files in format:\n"
                    + "\t<fully qualified pathname>\n"
                    + "Example:\n" + "\tgoo.buzz.Buzz\n", nFiles);
            for (int i = 0; i < nFiles; i++) {
                String libName = sc.next();
                String libPath = libName.replace('.', '/') + ".java";
                File f = new File(swh.classpath + "/" + libPath);
                swh.addLibraryFile(libName, f);
            }
            sc.close();
            swh.listenForCommands();
        }

    }

}
