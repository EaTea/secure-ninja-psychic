package snp.dev;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
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

import snp.LicenseV2;
import snp.NetworkUtilitiesV2;
import snp.SecurityUtilitiesV2;

public class DeveloperV2 {

    private SSLSocketFactory sslfact;

    private Map<String, Queue<LicenseV2>> licenseMap;

    public DeveloperV2(String trustFile, String password) throws UnknownHostException {
        System.out.println("Developer created at "
                + InetAddress.getLocalHost().getCanonicalHostName());
        licenseMap = new HashMap<String, Queue<LicenseV2>>();
        sslfact = (SSLSocketFactory) SecurityUtilitiesV2.getSSLSocketFactory(trustFile, password);
    }

    protected File linkFiles(List<File> classFiles, List<LicenseV2> requestedLicenses, final String jarName,
            SSLSocket connection) {

        ObjectInputStream inStream = NetworkUtilitiesV2.getObjectInputStream(connection);
        ObjectOutputStream outStream = NetworkUtilitiesV2.getObjectOutputStream(connection);
        File jarFile = null;

        if (inStream != null && outStream != null) {
            System.out.println("Sending licenses");

            int count = 0;

            try {
                // send the main entry point across
                outStream.writeUTF(classFiles.get(0).getPath());
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
                for (LicenseV2 lic : requestedLicenses) {
                    try {
                        System.out.println("Writing license to network");
                        outStream.writeUTF(lic.getSoftwareHouseIP().getCanonicalHostName());
                        outStream.writeInt(lic.getPort());
                        outStream.writeUTF(lic.getEncryptedLicense());
                        //TODO remove
                        System.out.println("Encrypted " + lic.getEncryptedLicense());
                        System.out.println("Unencrypted " + lic.getLicenseString());

                        if (inStream.readBoolean()) {
                            decrementLicense(lic.getLibraryName(), lic);
                            count++;
                            System.out.println("License used successfully, removing license");
                            // FIXME: assumption about which licenses should be
                            // removed could be dangerous
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
                    try {
                        outStream.writeInt(classFiles.size());
                        for (File f : classFiles) {
                            System.out.println("Sending " + f.getName() + " across network");

                            if (NetworkUtilitiesV2.writeFile(connection, f)) {
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

                    if (count == classFiles.size()) {
                        System.out.println("Receiving JAR file from" + " LinkBroker");
                        try {
                            FileOutputStream target = new FileOutputStream(jarName + ".jar");
                            if (NetworkUtilitiesV2.readFile(connection, target, false)) {
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

            NetworkUtilitiesV2.closeSocketObjectInputStream(inStream, connection);
            NetworkUtilitiesV2.closeSocketObjectOutputStream(outStream, connection);

            System.out.println("<-----End Communication----->");
            System.out.println();
        }
        return jarFile;
    }

    private LicenseV2 getLicense(String library) {
        if (licenseMap.containsKey(library) && !licenseMap.get(library).isEmpty()) {
            return licenseMap.get(library).peek();
        }
        return null;
    }

    private List<LicenseV2> getLicenses(List<String> libNames) {
        System.out.println("Checking licenses");
        List<LicenseV2> requestedLicenses = new ArrayList<LicenseV2>();

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

    protected void requestLicense(int numLicense, String libraryName, SSLSocket connection) {
        ObjectInputStream inStream = NetworkUtilitiesV2.getObjectInputStream(connection);
        ObjectOutputStream outStream = NetworkUtilitiesV2.getObjectOutputStream(connection);

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
                PublicKey pubKey = null;
                try {
                    pubKey = (PublicKey) inStream.readObject();
                    outStream.writeBoolean(true);
                } catch (ClassNotFoundException e) {
                    outStream.writeBoolean(false);
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                int nLicReturned = inStream.readInt();
                System.out.printf("%s returning %d licenses\n", connection.getInetAddress()
                        .getCanonicalHostName() + ":" + connection.getPort(), nLicReturned);
                for (int i = 0; i < nLicReturned; i++) {
                    String license = inStream.readUTF();
                    String encrypted = wrapLicense(license, pubKey);
                    if(encrypted != null) {
                        addLicense(libraryName, new LicenseV2(license, connection.getInetAddress(),
                                libraryName, InetAddress.getLocalHost().getCanonicalHostName(), 1,
                                connection.getPort(), encrypted));
                      //inform SWH that we successfully added the license
                        outStream.writeBoolean(true);
                    } else {
                        outStream.writeBoolean(false); //telling SWH we cant encrypt their license
                        System.err.println("Failed to encrypt license with SWH's public key, exiting"); 
                        NetworkUtilitiesV2.closeSocketObjectInputStream(inStream, connection);
                        NetworkUtilitiesV2.closeSocketObjectOutputStream(outStream, connection);

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
                NetworkUtilitiesV2.closeSocketObjectInputStream(inStream, connection);
                NetworkUtilitiesV2.closeSocketObjectOutputStream(outStream, connection);

                System.out.println("<-----End Communication----->");
                System.out.println();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addLicense(String library, LicenseV2 l) {
        if (!licenseMap.containsKey(library)) {
            licenseMap.put(library, new LinkedList<LicenseV2>());
        }
        licenseMap.get(library).add(l);
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
    
    private void decrementLicense(String library, LicenseV2 lic) {
        if (library != null && lic != null
                && licenseMap.containsKey(library)
                && !licenseMap.get(library).isEmpty()) {
            licenseMap.get(library).remove(lic);
        }
    }

    private boolean hasLicense(String library) {
        return getLicense(library) != null;
    }

    private void processCommands(Scanner sc) {
//        Scanner sc = new Scanner(System.in);
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
                    System.out.println(connection.getSession().getCipherSuite());
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
                
                List<LicenseV2> requestedLicenses = getLicenses(libNames);
                if (requestedLicenses == null) {
                    System.out.println("Sorry, you are missing a license for one or more " +
                    		"requested libraries and cannot link");
                    continue;
                }

                System.out.println("How many files to link?");
                int nFiles = sc.nextInt();
                if (nFiles == 0) {
                    System.out.println("Sorry, you need at least 1 file to provide for linking");
                    continue;
                }
                System.out.printf("Please input %d class file paths\n", nFiles);
                System.out.println("Note: 1st class file treated as main");
                List<File> classFiles = new ArrayList<File>();
                for (int i = 0; i < nFiles; i++) {
                    classFiles.add(new File(sc.next()));
                }

                try {
                    SSLSocket connection = (SSLSocket) sslfact.createSocket(remoteHost, remotePort);
                    linkFiles(classFiles, requestedLicenses, jarFileName, connection);
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

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: needs two arguments.");
            System.err.println("\tArgument 1 = truststore filepath");
            System.err.println("\tArgument 2 = truststore password");
            System.exit(1);
        }
        
        DeveloperV2 dev = null;
        //System.out.println("Please Enter:\n" + "\t <keyFilePath> <trustFilePath> <Password>");
//        System.out.println("Please Enter:\n" + "\t <trustFilePath> <password>");
        Scanner sc = null;
        try {
            sc = new Scanner(System.in);
//            String keyFile = sc.next();
            String trustFile = args[0];
            String password = args[1];
//            String trustFile = "../keystores/dev-truststore.jks";
//            String password = "cits3231";
            dev = new DeveloperV2(trustFile, password);
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