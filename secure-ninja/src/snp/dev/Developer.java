package snp.dev;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;

import snp.CompileUtility;
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
            classpath = classpath.substring(0, classpath.length() - 2);
        }
    }

    // TODO Catch InputMismatchException ?
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
                System.out.printf("Please input %d fully qualified class names(s)\n", nFiles);
                System.out.println("Note: 1st file treated as main, last file treated as auth");
                Map<String, File> srcFiles = new HashMap<String, File>();
                
                String mainName = "";
                
                for (int i = 0; i < nFiles; i++) {
                    String name = sc.next();
                    String path = name.replace('.', '/') + ".java";
                    srcFiles.put(name, new File(this.classpath + "/" + path));
                    
                    if (i == 0) {
                        mainName = name;
                    }
                }

                try {
                    SSLSocket connection = (SSLSocket) sslfact.createSocket(remoteHost, remotePort);
                    linkFiles(mainName, srcFiles, requestedLicenses, jarFileName, connection);
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

                // reading in the number of licenses we received from the SWH
                int nLicReturned = inStream.readInt();
                System.out.printf("%s returning %d licenses\n", connection.getInetAddress()
                        .getCanonicalHostName() + ":" + connection.getPort(), nLicReturned);
                for (int i = 0; i < nLicReturned; i++) {
                    String license = inStream.readUTF();
                    String unencrypted = inStream.readUTF();
                    addLicense(libraryName, new License(license, connection.getInetAddress(),
                                libraryName, connection.getPort(), unencrypted));
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

    private void addLicense(String library, License l) {
        if (!licenseMap.containsKey(library)) {
            licenseMap.put(library, new LinkedList<License>());
        }
        licenseMap.get(library).add(l);
    }

    // TODO: make a new class for modules
    private File linkFiles(String mainFile, Map<String, File> srcFiles, List<License> requestedLicenses,
            final String jarName, SSLSocket connection) {

        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);
        File jarFile = null;

        if (inStream != null && outStream != null) {
            System.out.println("Sending licenses");

            int count = 0;
            Map<String, String> licenseMap = new HashMap<String, String>();
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            String password = "";

            try {
                // send the main entry point across
                outStream.writeUTF(mainFile);
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
                            licenseMap.put(lic.getLibraryName(), lic.getLicenseString());
                            md.update(lic.getLicenseString().getBytes());
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

                    password = NetworkUtilities.bytesToHex(md.digest());
                    
                    System.out.println("The final JAR file password will be: " + password);                    
                    
                    System.out.println("Sending class files to LinkBroker");
                    count = 0;

                    //List<File> classFiles = new ArrayList<File>();
                    Map<String, File> classFiles = new HashMap<String, File>();
                    
                    Set<String> srcFileNames = srcFiles.keySet();
                    for (String name : srcFileNames) {
                        File f = srcFiles.get(name);
                        
                        CompileUtility.compileDevFile(f, name, licenseMap, password);
                        
                        String classFilePath = name.substring(name.lastIndexOf('.')+1)
                            + ".class";
                        System.out.println("File: " + name + ", classFilePath: " + classFilePath);
                        classFiles.put(name, new File(classFilePath));
                    }

                    try {
                        outStream.writeInt(classFiles.size());
                        Set<String> classFileNames = classFiles.keySet();
                        for (String name : classFileNames) {
                            File f = classFiles.get(name);
                            System.out.println("Sending " + name + " across network");

                            if (NetworkUtilities.writeFile(connection, f, name)) {
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
            dev = new Developer(classpath, trustFile, password);
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