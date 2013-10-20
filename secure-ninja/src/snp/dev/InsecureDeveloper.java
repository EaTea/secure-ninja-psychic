package snp.dev;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

import snp.NetworkUtilities;

public class InsecureDeveloper {

    private Map<String, Queue<License>> licenseMap;

    public InsecureDeveloper() throws UnknownHostException {
        System.out.println("Developer created at "
                + InetAddress.getLocalHost().getCanonicalHostName());
        licenseMap = new HashMap<String, Queue<License>>();
    }

    protected File linkFiles(List<File> classFiles, List<String> libNames,
            final String jarName, Socket connection) {

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

        DataInputStream inStream = NetworkUtilities
                .getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities
                .getDataOutputStream(connection);
        File jarFile = null;

        if (inStream != null && outStream != null) {
            System.out.println("Sending licenses");

            int count = 0;
            
            try {
                System.out.println("Sending number of licenses");
                outStream.writeInt(requestedLicenses.size());
            } catch(IOException e) {
                System.err.println("Error: encountered I/O error whilst writing"
                        + " number of licenses to network");
                e.printStackTrace();
                count = -1;
            }

            if (count != -1) {
                for (License lic : requestedLicenses) {
                    try {
                        System.out.println("Writing license to network");
                        outStream.writeUTF(lic.getSoftwareHouseIP()
                                .getCanonicalHostName());
                        outStream.writeInt(lic.getPort());
                        outStream.writeUTF(lic.getLicenseString());
                        
                        if (inStream.readBoolean()) {
                            decrementLicense(lic.getLibraryName());
                            count++;
                            System.out.println(
                                "Licsense used successfully, removing license");
                            // FIXME: assumption about which licenses should be
                            // removed could be dangerous
                        } else {
                            System.out.println("Something went wrong on the"
                                    + " linker's end");
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
                    System.err.println("Error: could not read return code"
                            + " from LinkBroker");
                    e.printStackTrace();
                }

                if (success) {
                    System.out.println("LinkBroker returned successful"
                            + " license check");

                    System.out.println("Sending class files to LinkBroker");
                    count = 0;
                    try {
                        outStream.writeInt(classFiles.size());
                        for (File f : classFiles) {
                            System.out.println("Sending " + f.getName()
                                    + " across network");
                            if (NetworkUtilities.writeFile(connection, f)) {
                                count++;
                            } else {
                                System.err.println("Error occurred sending "
                                        + f.getAbsolutePath());
                                break;
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error: Sending class files failed");
                        e.printStackTrace();
                    }

                    if (count == classFiles.size()) {
                        System.out.println("Receiving JAR file from"
                                + " LinkBroker");
                        try {
                            FileOutputStream target =
                                new FileOutputStream(jarName + ".jar");
                            if (NetworkUtilities.readFile(connection, target,
                                    false /* not reading classFiles to JAR */))
                            {
                                System.out.println("Successfully received"
                                        + " JAR file");
                                jarFile = new File(jarName + ".jar");
                            } else {
                                System.out.println("Error occurred"
                                        + " receiving " + jarName + ".jar");                            }
                        } catch (IOException e) {
                            System.err.println("Error: Receiving JAR file"
                                    + " failed");
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("LinkBroker did not "
                            + "return success code");
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

    protected void requestLicense(int numLicense, String libraryName,
            Socket connection) {
        DataInputStream inStream = NetworkUtilities
                .getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities
                .getDataOutputStream(connection);

        if (inStream != null && outStream != null) {
            // later would need to send client credentials
            try {
                outStream.writeUTF("REQ");

                System.out.printf("Getting %d licenses for %s from %s\n",
                        numLicense, libraryName,
                        connection.getInetAddress().getCanonicalHostName()
                                + ":" + connection.getPort());
                outStream.writeUTF(libraryName);
                outStream.writeInt(numLicense);
                // Might need to send developer ID

                int nLicReturned = inStream.readInt();
                System.out.printf("%s returning %d licenses\n", connection
                        .getInetAddress().getCanonicalHostName()
                        + ":"
                        + connection.getPort(), nLicReturned);
                for (int i = 0; i < nLicReturned; i++) {
                    String license = inStream.readUTF();
                    addLicense(libraryName, new License(license,
                            connection.getInetAddress(), libraryName,
                            InetAddress.getLocalHost().getCanonicalHostName(),
                            1 /* how many uses a license has */,
                            connection.getPort()));
                }

                if (nLicReturned <= 0) {
                    System.out.printf("%s declined to send licenses\n",
                            connection.getInetAddress().getCanonicalHostName());
                } else {
                    System.out.printf("Received %d licenses from %s\n",
                            nLicReturned, connection.getInetAddress()
                                    .getCanonicalHostName());
                }
                NetworkUtilities.closeSocketDataInputStream(inStream,
                        connection);
                NetworkUtilities.closeSocketDataOutputStream(outStream,
                        connection);

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

    private License getLicense(String library) {
        if (licenseMap.containsKey(library)
                && !licenseMap.get(library).isEmpty()) {
            return licenseMap.get(library).peek();
        }
        return null;
    }

    private void decrementLicense(String library) {
        if (licenseMap.containsKey(library)
                && !licenseMap.get(library).isEmpty()) {
            licenseMap.get(library).poll();
        }
    }

    private boolean hasLicense(String library) {
        return getLicense(library) != null;
    }

    private void processCommands() {
        Scanner sc = new Scanner(System.in);
        do {
            System.out.println("Commands:\n"
                    + "\tRequest <Hostname> <Port> <LibraryName>"
                    + " <NumberLicenses>" + "\n\tOR\n"
                    + "\tLink <Hostname> <Port> <JARFileName>" + "\n\tOR\n"
                    + "\tQuit");
            String command = sc.next();
            if (command.equalsIgnoreCase("Request")) {

                String remoteHost = sc.next();
                int remotePort = sc.nextInt();
                String libName = sc.next();
                int numLicenses = sc.nextInt();

                try {
                    Socket connection = new Socket(remoteHost, remotePort);
                    requestLicense(numLicenses, libName, connection);
                    connection.close();
                } catch (UnknownHostException e) {
                    System.err.println("Error: host name could"
                            + " not be resolved");
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

                System.out.println("How many files to link?");
                int nFiles = sc.nextInt();
                System.out.printf("Please input %d class file paths\n", nFiles);
                List<File> classFiles = new ArrayList<File>();
                for (int i = 0; i < nFiles; i++) {
                    classFiles.add(new File(sc.next()));
                }

                try {
                    Socket connection = new Socket(remoteHost, remotePort);
                    linkFiles(classFiles, libNames, jarFileName, connection);
                    connection.close();
                } catch (UnknownHostException e) {
                    System.err.println("Error: host name could"
                            + " not be resolved");
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
        sc.close();
    }

    public static void main(String[] args) {
        InsecureDeveloper dev = null;
        try {
            dev = new InsecureDeveloper();
        } catch (UnknownHostException e) {
            System.err.println("Error: host name could"
                    + " not be resolved");
            e.printStackTrace();
        }
        if (dev != null) {
            dev.processCommands();
        }
    }

}