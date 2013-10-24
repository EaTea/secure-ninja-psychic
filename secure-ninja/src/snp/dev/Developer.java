package snp.dev;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;

import snp.License;
import snp.NetworkUtilities;
import snp.SecurityUtilities;

public class Developer {

    private SSLSocketFactory sslfact;

    private Map<String, Queue<License>> licenseMap;

    private String classpath;

    public Developer(String classpath, String trustFile, String password)
            throws UnknownHostException {
        System.out.println("Developer created at "
                + InetAddress.getLocalHost().getCanonicalHostName());
        licenseMap = new HashMap<String, Queue<License>>();
        sslfact = (SSLSocketFactory) SecurityUtilities.getSSLSocketFactory(trustFile, password);
        this.classpath = classpath;
        if (classpath.endsWith("/")) {
            classpath = classpath.substring(0, classpath.length()-2);
        }
    }

    private void processCommands(Scanner sc) {
        do {
            System.out.println("Commands:\n" + "\tRequest <Hostname> <Port> <LibraryName>"
                    + " <NumberLicenses>" + "\n\tOR\n" + "\tLink <Hostname> <Port> <JARFileName>"
                    + "\n\tOR\n" + "\tQuit");
            String command = sc.next();
            if (command.equalsIgnoreCase("Request")) {
    
                String remoteHost = sc.next();
                int remotePort = sc.nextInt();
                String libName = sc.next();
                int numLicenses = sc.nextInt();
    
                try {
                    SSLSocket connection = (SSLSocket) sslfact.createSocket(remoteHost, remotePort);
                    requestLicense(numLicenses, libName, connection);
                    connection.close();
                } catch (UnknownHostException e) {
                    System.err.println("Error: host name could" + " not be resolved");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("Error: I/O error occurred");
                    e.printStackTrace();
                }
            } else if (command.equalsIgnoreCase("Link")) {
    
                String remoteHost = sc.next();
                int remotePort = sc.nextInt();
                String jarFileName = sc.next();
    
                System.out.println("How many required libraries?");
                int nLibs = sc.nextInt();
                System.out.printf("Please input %d libraries\n", nLibs);
                List<String> libNames = new ArrayList<String>();
                for (int i = 0; i < nLibs; i++) {
                    libNames.add(sc.next());
                }
    
                List<License> requestedLicenses = getLicenses(libNames);
                if (requestedLicenses == null) {
                    System.out.println("Sorry, you are missing a license for one or more "
                            + "requested libraries and cannot link");
                    continue;
                }
    
                System.out.println("How many files to link?");
                int nFiles = sc.nextInt();
                if (nFiles == 0) {
                    System.out.println("Sorry, you need at least 1 file to provide for linking");
                    continue;
                }
                System.out.printf("Please input %d source file path(s), relative to %s\n", nFiles,
                        this.classpath);
                System.out.println("Note: 1st file treated as main, last file treated as auth");
                List<File> srcFiles = new ArrayList<File>();
                for (int i = 0; i < nFiles; i++) {
                    srcFiles.add(new File(this.classpath+"/"+sc.next()));
                }
    
                try {
                    SSLSocket connection = (SSLSocket) sslfact.createSocket(remoteHost, remotePort);
                    linkFiles(srcFiles, requestedLicenses, jarFileName, connection);
                    connection.close();
                } catch (UnknownHostException e) {
                    System.err.println("Error: host name could" + " not be resolved");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.err.println("Error: I/O error occurred");
                    e.printStackTrace();
                }
            } else if (command.equalsIgnoreCase("Quit")) {
                System.out.println("Bye bye!");
                break;
            } else {
                System.out.println("Sorry, that was not a recognised command.");
            }
        } while (true);
    }

    private void requestLicense(int numLicense, String libraryName, SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);

        if (inStream != null && outStream != null) {
            // later would need to send client credentials
            try {
                outStream.writeUTF("REQ");
                System.out.printf(
                        "Getting %d licenses for %s from %s\n",
                        numLicense,
                        libraryName,
                        connection.getInetAddress().getCanonicalHostName() + ":"
                                + connection.getPort());
                outStream.writeUTF(libraryName);
                outStream.writeInt(numLicense);

                String algo = inStream.readUTF();
                // getting SWH's public key
                int keySize = inStream.readInt();
                byte[] keyBytes = new byte[keySize];
                for (int i = 0; i < keySize; i++) {
                    keyBytes[i] = inStream.readByte();
                }
                System.out.print("KeyBytes received ");
                System.out.println((NetworkUtilities.bytesToHex(keyBytes)));

                PublicKey pubKey = genPublicKey(keyBytes, algo);
                if (pubKey == null) {
                    System.out.println("Unable to reconstruct SWH's public key");
                    outStream.writeBoolean(false); // tell SWH we can't get
                                                   // their key

                    NetworkUtilities.closeSocketDataInputStream(inStream, connection);
                    NetworkUtilities.closeSocketDataOutputStream(outStream, connection);

                    System.out.println("<-----End Communication----->");
                    System.out.println();
                    return;
                }
                System.out.print("KeyBytes generated ");
                System.out.println(NetworkUtilities.bytesToHex(pubKey.getEncoded()));

                // Telling SWH that we got their key
                outStream.writeBoolean(true);

                // reading in the number of licenses we received from the SWH
                int nLicReturned = inStream.readInt();
                System.out.printf("%s returning %d licenses\n", connection.getInetAddress()
                        .getCanonicalHostName() + ":" + connection.getPort(), nLicReturned);
                for (int i = 0; i < nLicReturned; i++) {
                    String license = inStream.readUTF();
                    String encrypted = wrapLicense(license, pubKey);
                    if (encrypted != null) {
                        addLicense(libraryName, new License(license, connection.getInetAddress(),
                                libraryName, connection.getPort(), encrypted));
                        // inform SWH that we successfully added the license
                        outStream.writeBoolean(true);
                    } else {
                        outStream.writeBoolean(false); // telling SWH we cant
                                                       // encrypt their license
                        System.err
                                .println("Failed to encrypt license with SWH's public key, exiting");
                        NetworkUtilities.closeSocketDataInputStream(inStream, connection);
                        NetworkUtilities.closeSocketDataOutputStream(outStream, connection);

                        System.out.println("<-----End Communication----->");
                        System.out.println();
                        return;
                    }
                }

                if (nLicReturned <= 0) {
                    System.out.printf("%s declined to send licenses\n", connection.getInetAddress()
                            .getCanonicalHostName());
                } else {
                    System.out.printf("Received %d licenses from %s\n", nLicReturned, connection
                            .getInetAddress().getCanonicalHostName());
                }
                NetworkUtilities.closeSocketDataInputStream(inStream, connection);
                NetworkUtilities.closeSocketDataOutputStream(outStream, connection);

                System.out.println("<-----End Communication----->");
                System.out.println();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

    private void addLicense(String library, License l) {
        if (!licenseMap.containsKey(library)) {
            licenseMap.put(library, new LinkedList<License>());
        }
        licenseMap.get(library).add(l);
    }

    private File linkFiles(List<File> srcFiles, List<License> requestedLicenses,
            final String jarName, SSLSocket connection) {

        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);
        File jarFile = null;

        if (inStream != null && outStream != null) {
            System.out.println("Sending licenses");

            int count = 0;

            try {
                // send the main entry point across
                outStream.writeUTF(srcFiles.get(0).getPath());
            } catch (IOException e) {
                System.err.println("Error: could not send across main file point");
                e.printStackTrace();
                count = -1;
            }

            try {
                System.out.println("Sending number of licenses");
                // write -1 for an error occurring previously
                outStream.writeInt(count == -1 ? -1 : requestedLicenses.size());
            } catch (IOException e) {
                System.err.println("Error: encountered I/O error whilst writing"
                        + " number of licenses to network");
                e.printStackTrace();
                count = -1;
            }

            if (count != -1) {
                for (License lic : requestedLicenses) {
                    try {
                        System.out.println("Writing license to network");
                        outStream.writeUTF(lic.getSoftwareHouseIP().getCanonicalHostName());
                        outStream.writeInt(lic.getPort());
                        outStream.writeUTF(lic.getEncryptedLicenseString());

                        if (inStream.readBoolean()) {
                            decrementLicense(lic.getLibraryName(), lic);
                            count++;
                            // add authenticator license
                            System.out.println("License used successfully, removing license");
                        } else {
                            System.out.println("Something went wrong on the" + " linker's end");
                            break;
                        }
                    } catch (IOException e) {
                        System.err.println("Error: I/O error sending license");
                        e.printStackTrace();
                        break;
                    }
                }
            }

            if (count == requestedLicenses.size()) {
                boolean success = false;
                try {
                    success = inStream.readBoolean();
                } catch (IOException e) {
                    System.err.println("Error: could not read return code" + " from LinkBroker");
                    e.printStackTrace();
                }

                if (success) {
                    System.out.println("LinkBroker returned successful" + " license check");

                    System.out.println("Sending class files to LinkBroker");
                    count = 0;
                    
                    List<File> classFiles = new ArrayList<File>();
                    for (File f : srcFiles) {
                        // compile srcFile into classFile
                        // new File ff, push into classFiles
                    }

                    try {
                        outStream.writeInt(classFiles.size());
                        for (File f : classFiles) {
                            System.out.println("Sending " + f.getName() + " across network");

                            if (NetworkUtilities.writeFile(connection, f)) {
                                count++;
                            } else {
                                System.err.println("Error occurred sending " + f.getAbsolutePath());
                                break;
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error: Sending class files failed");
                        e.printStackTrace();
                    }

                    if (count == srcFiles.size()) {
                        System.out.println("Receiving JAR file from" + " LinkBroker");
                        try {
                            FileOutputStream target = new FileOutputStream(jarName + ".jar");
                            if (NetworkUtilities.readFile(connection, target, false)) {
                                System.out.println("Successfully received" + " JAR file");
                                jarFile = new File(jarName + ".jar");
                            } else {
                                System.out.println("Error occurred" + " receiving " + jarName
                                        + ".jar");
                            }
                        } catch (IOException e) {
                            System.err.println("Error: Receiving JAR file" + " failed");
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("LinkBroker did not " + "return success code");
                    System.out.println("Unsuccessful return from linking");
                }
            }

            NetworkUtilities.closeSocketDataInputStream(inStream, connection);
            NetworkUtilities.closeSocketDataOutputStream(outStream, connection);

            System.out.println("<-----End Communication----->");
            System.out.println();
        }
        return jarFile;
    }

    private List<License> getLicenses(List<String> libNames) {
        System.out.println("Checking licenses");
        List<License> requestedLicenses = new ArrayList<License>();

        for (String lib : libNames) {
            if (!hasLicense(lib)) {
                System.out.println("Missing license for " + lib);
                System.out.println("Unsuccessful return from linking");
                return null;
            } else {
                System.out.println("Found a license for " + lib);
                requestedLicenses.add(getLicense(lib));
            }
        }
        return requestedLicenses;
    }

    private License getLicense(String library) {
        if (licenseMap.containsKey(library) && !licenseMap.get(library).isEmpty()) {
            return licenseMap.get(library).peek();
        }
        return null;
    }

    private boolean hasLicense(String library) {
        return getLicense(library) != null;
    }

    private void decrementLicense(String library, License lic) {
        if (library != null && lic != null && licenseMap.containsKey(library)
                && !licenseMap.get(library).isEmpty()) {
            licenseMap.get(library).remove(lic);
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: needs two arguments.");
            System.err.println("\tArgument 1 = truststore filepath");
            System.err.println("\tArgument 2 = truststore password");
            System.err.println("\tArgument 3 = classpath");
            System.exit(1);
        }

        Developer dev = null;
        Scanner sc = null;
        try {
            sc = new Scanner(System.in);
            String trustFile = args[0];
            String password = args[1];
            String classpath = args[2];
            dev = new Developer(trustFile, password, classpath);
        } catch (UnknownHostException e) {
            System.err.println("Error: host name could" + " not be resolved");
            e.printStackTrace();
        }
        if (dev != null && sc != null) {
            dev.processCommands(sc);
        }
        sc.close();
    }

}