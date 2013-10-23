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

import snp.LicenseV2;
import snp.NetworkUtilitiesV2;
import snp.SecurityUtilitiesV2;

public class SWHV2 {

    private Map<String, LicenseV2> clientLicenses;

    private Map<String, File> libraries;

    private SSLServerSocketFactory sslservfact;

    private SSLServerSocket serverConnection;

    private KeyPair myKey;
    
    private static final int keySize = 2048;
    
    private static final String algo = "RSA";
    
    public SWHV2(int serverPort, String keyFile, String password) throws UnknownHostException,
            IOException, NoSuchAlgorithmException {
        clientLicenses = new HashMap<String, LicenseV2>();
        libraries = new HashMap<String, File>();
        sslservfact = (SSLServerSocketFactory) SecurityUtilitiesV2.getSSLServerSocketFactory(
                keyFile, password);
        serverConnection = (SSLServerSocket) sslservfact.createServerSocket(serverPort, 0,
                InetAddress.getLocalHost());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algo);
        keyGen.initialize(keySize);
        myKey = keyGen.genKeyPair();
        System.out.println("Created a new SoftwareHouse at "
                + serverConnection.getInetAddress().getCanonicalHostName() + ":"
                + serverConnection.getLocalPort());
    }

    private void addLicense(String licenseString, LicenseV2 l) {
        clientLicenses.put(licenseString, l);
    }

    private LicenseV2 getLicense(String license) {
        if (clientLicenses.containsKey(license)) {
            return clientLicenses.get(license);
        }
        return null;
    }

    private void decrementLicense(String license) {
        clientLicenses.remove(license);
    }

    public boolean verifyLicense(String license) {
        LicenseV2 temp = getLicense(license);
        if (temp != null) {
            if (temp.getNumberLicenses() > 0) {
                return true;
            } else {
                clientLicenses.remove(temp);
            }
        }
        return false;
    }

    private void addLibraryFile(String libName, File f) {
        libraries.put(libName, f);
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
                DataInputStream inStream = NetworkUtilitiesV2.getDataInputStream(connection);
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

    private void acceptLicenses(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilitiesV2.getDataInputStream(connection);
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
                LicenseV2 temp = clientLicenses.get(license);
                String libraryName = temp.getLibraryName();
                System.out.printf("License corresponds to library %s\n", libraryName);

                if (NetworkUtilitiesV2.writeFile(connection, libraries.get(libraryName))) {
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
                DataOutputStream outStream = NetworkUtilitiesV2.getDataOutputStream(connection);

                try {
                    outStream.writeLong(-1);
                } catch (IOException e) {
                    System.err.println("Error: encountered I/O error whilst "
                            + "sending rejection to Linker");
                    e.printStackTrace();
                }
                NetworkUtilitiesV2.closeSocketDataOutputStream(outStream, connection);
            }

            NetworkUtilitiesV2.closeSocketDataInputStream(inStream, connection);
        }

        System.out.println("<-----End Communication----->");
        System.out.println();
    }
    
    private String wrapLicense(String license, PublicKey pubKey) {
        if (pubKey == null) {
            System.out.println("Null PublicKey received");
            return null;
        }
        // TODO: encrypt a license with a SWH public key using asymmetric key encryption
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encrypted = cipher.doFinal(license.getBytes());
            String encryptedLicense = NetworkUtilitiesV2.bytesToHex(encrypted);
            System.out.println("Unencrypted : " + license);
            System.out.println("Encrypted : " + encryptedLicense);
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

    private String unwrapLicense(String license) {
        // No-op in the insecure case, will decrypt in the secure case
        // TODO: decrypt license string using our own private key
        PrivateKey privKey = myKey.getPrivate();
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            byte[] decrypted = cipher.doFinal(license.getBytes());
            String decryptedLicense = NetworkUtilitiesV2.bytesToHex(decrypted);
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

    private void generateLicenses(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilitiesV2.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilitiesV2.getDataOutputStream(connection);


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
                    System.out.println((NetworkUtilitiesV2.bytesToHex(keyBytes)));
                    if(!inStream.readBoolean()) {
                        System.out.println("Receiving my public key by developer failed");
                        NetworkUtilitiesV2.closeSocketDataInputStream(inStream, connection);
                        NetworkUtilitiesV2.closeSocketDataOutputStream(outStream, connection);

                        System.out.println("<-----End Communication----->");
                        System.out.println();
                        return;
                    }

                    System.out.println("Generating licenses for "
                            + connection.getInetAddress().getCanonicalHostName() + ":"
                            + connection.getPort());
                    outStream.writeInt(numLicenses);

                    for (int i = 0; i < numLicenses; i++) {
                        String s = libName + i + System.currentTimeMillis() + Math.random();
                        MessageDigest md = MessageDigest.getInstance("MD5");

                        // Note that s.getBytes() is not platform independent.
                        // Better approach would be to use character encodings.
                        String license = NetworkUtilitiesV2.bytesToHex(md.digest(s.getBytes()));
                        outStream.writeUTF(license);
                        wrapLicense(license, myKey.getPublic());
                        //Developer told us that they cant encrypt our license with our public key
                        if(!inStream.readBoolean()) {
                            System.out.println("Developer unable to "
                             + "encrypt our license with our key, exiting");
                            NetworkUtilitiesV2.closeSocketDataInputStream(inStream, connection);
                            NetworkUtilitiesV2.closeSocketDataOutputStream(outStream, connection);

                            System.out.println("<-----End Communication----->");
                            System.out.println();
                            return;
                        }
                        addLicense(license, new LicenseV2(license, InetAddress.getLocalHost(),
                                libName, connection.getInetAddress().getCanonicalHostName(), 1,
                                connection.getLocalPort(), null
                                /*SWH doesnt need to store encrypted license*/));
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

            NetworkUtilitiesV2.closeSocketDataInputStream(inStream, connection);
            NetworkUtilitiesV2.closeSocketDataOutputStream(outStream, connection);

            System.out.println("<-----End Communication----->");
            System.out.println();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: needs 3 arguments.");
            System.err.println("\tArgument 1 = port number");
            System.err.println("\tArgument 2 = keystore filepath");
            System.err.println("\tArgument 3 = keystore password");
            System.exit(1);
        }
        
        SWHV2 swh = null;
//        System.out.println("Please Enter:\n" + "\t<Port> <keyFilePath> <Password>");
        /*
         * if (args.length < 4) { System.out.println("Usage:\n" +
         * "requires one integer parameter for port\n"); return; }
         */
        int portNumber = Integer.parseInt(args[0]);
        String keyFile = args[1];
        // String trustFile = sc.next();
        String password = args[2];
        try {
            // TODO
            swh = new SWHV2(portNumber, keyFile, password);
        } catch (UnknownHostException e) {
            System.err.println("Error: host name could" + " not be resolved; exiting");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: an IO error occurred during"
                    + " ServerSocket initialisation; exiting");
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (swh != null) {
            Scanner sc = new Scanner(System.in);
            System.out.println("How many files is this SoftwareHouse" + "responsible for?");
            int nFiles = sc.nextInt();

            System.out.printf("Enter %d files in format:\n" + "\t<LibraryName> <Path>\n"
                    + "Example:\n" + "\tsample.google.buzz ./sample/google/buzz\n", nFiles);
            for (int i = 0; i < nFiles; i++) {
                String libName, libPath;
                libName = sc.next();
                libPath = sc.next();
                File f = new File(libPath);
                swh.addLibraryFile(libName, f);
            }
            sc.close();
            swh.listenForCommands();
        }

    }

}
