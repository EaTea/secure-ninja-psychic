package snp.swh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import snp.NetworkUtilities;
import snp.SecurityUtilitiesV2;

public class SWHV2 {

    private Map<String, LicenseV2> clientLicenses;

    private Map<String, File> libraries;

    private SSLServerSocketFactory sslservfact;

    private SSLServerSocket serverConnection;

    public SWHV2(int serverPort, String keyFile, String password) throws UnknownHostException,
            IOException {
        clientLicenses = new HashMap<String, LicenseV2>();
        libraries = new HashMap<String, File>();
        sslservfact = (SSLServerSocketFactory) SecurityUtilitiesV2.getSSLServerSocketFactory(
                keyFile, password);
        serverConnection = (SSLServerSocket) sslservfact.createServerSocket(serverPort, 0 /*
                                                                                           * impl
                                                                                           * .
                                                                                           * specific
                                                                                           */,
                InetAddress.getLocalHost());
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

    private void listenForCommands() {
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

    private void acceptLicenses(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);

        if (inStream != null) {
            System.out.println("Checking if license is legitimate");
            String license = null, developerID = null;
            try {
                license = inStream.readUTF();
                developerID = inStream.readUTF();
                System.out.printf("Read in license %s\n", license);
                license = processLicense(license, developerID);
            } catch (IOException e) {
                System.err.println("Error: I/O error whilst reading licenses");
                e.printStackTrace();
            }

            if (verifyLicense(license) && developerID != null) {
                LicenseV2 temp = clientLicenses.get(license);
                String libraryName = temp.getLibraryName();
                System.out.printf("License corresponds to library %s\n", libraryName);

                if (NetworkUtilities.writeFile(connection, libraries.get(libraryName))) {
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

    private String processLicense(String license, String developerID) {
        // No-op in the insecure case, will decrypt in the secure case
        return license;
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

            if (numLicenses != -1 && libName != null && libraries.containsKey(libName)) {
                try {
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

                        addLicense(license, new LicenseV2(license, InetAddress.getLocalHost(),
                                libName, connection.getInetAddress().getCanonicalHostName(), 1 /*
                                                                                                * number
                                                                                                * of
                                                                                                * uses
                                                                                                */,
                                connection.getLocalPort()));
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

    public static void main(String[] args) {
        SWHV2 swh = null;
        System.out.println("Please Enter:\n" + "\t<Port> <keyFilePath> <Password>");
        /*
         * if (args.length < 4) { System.out.println("Usage:\n" +
         * "requires one integer parameter for port\n"); return; }
         */
        Scanner sc = new Scanner(System.in);
        int portNumber = sc.nextInt();
        String keyFile = sc.next();
        // String trustFile = sc.next();
        String password = sc.next();
        try {
            // TODO
            swh = new SWHV2(portNumber, keyFile, password);
            sc.close();
        } catch (UnknownHostException e) {
            System.err.println("Error: host name could" + " not be resolved; exiting");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: an IO error occurred during"
                    + " ServerSocket initialisation; exiting");
            e.printStackTrace();
        }
        if (swh != null) {
            // Scanner sc = new Scanner(System.in);
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

            swh.listenForCommands();
        }
        sc.close();
    }

}
