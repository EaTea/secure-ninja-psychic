package edu.uwa.secure_ninja.developer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;
import java.util.Stack;

import edu.uwa.secure_ninja.NetworkUtilities;

public class InsecureDeveloper {
   
    /**
     *
     */
    private HashMap<String, Queue<License> > licenseMap;
    
    private String hostName;
    
    public String getHostName() {
        return hostName;
    }

    private void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    private void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    private int portNumber;
    
    public InsecureDeveloper(String hostName, int portNumber) {
        licenseMap = new HashMap<String, Queue<License> >();
        setHostName(hostName);
        setPortNumber(portNumber);
    }
    
    public InsecureDeveloper(int portNumber) throws UnknownHostException {
        this(InetAddress.getLocalHost().getCanonicalHostName(), portNumber);
    }

    protected File linkFiles(List<File> classFiles, List<String> libNames,
            String jarName, Socket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);
        
        if (inStream != null && outStream != null) {
            System.out.println("Sending licenses");
            
            for (String lib : libNames) {
                if (!hasLicense(lib)) {
                    System.out.println("Missing license for " + lib);
                    return null;
                }
                License lic = getLicense(lib);
                try {
                    System.out.println("Writing license to network");
                    outStream.writeUTF(
                            lic.getSoftwareHouseIP().getCanonicalHostName());
                    outStream.writeUTF(lic.getLicenseString());
                    outStream.writeUTF(lic.getDeveloperID());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
    protected void requestLicense(int numLicense, String libraryName,
            Socket connection) {
        DataInputStream inStream =
            NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream =
            NetworkUtilities.getDataOutputStream(connection);
        
        if (inStream != null && outStream != null) {
            // later would need to send client credentials
            try {
                System.out.printf("Getting %d licenses for %s from %s",
                        numLicense, libraryName,
                        connection.getInetAddress().getCanonicalHostName()
                        + ":" + connection.getPort());
                outStream.writeUTF(libraryName);
                outStream.writeInt(numLicense);

                int nLicReturned = inStream.readInt();
                System.out.printf("%s returning %d licenses",
                        connection.getInetAddress().getCanonicalHostName()
                        + ":" + connection.getPort(), nLicReturned);
                for (int i = 0; i < nLicReturned; i++) {
                    addLicense(libraryName,
                            new License(inStream.readUTF(),
                            connection.getInetAddress(), libraryName,
                            InetAddress.getLocalHost().getCanonicalHostName(),
                            1 /* how many uses a license has */));
                }

                if (nLicReturned <= 0) {
                    System.out.printf("%s declined to send licenses",
                            connection.getInetAddress().getCanonicalHostName());
                } else {
                    System.out.printf("Received %d licenses from %s",
                            nLicReturned,
                            connection.getInetAddress().getCanonicalHostName());
                }
                NetworkUtilities.closeSocketDataInputStream(inStream, connection);
                NetworkUtilities.closeSocketDataOutputStream(outStream, connection);
            } catch(IOException e) {
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

    public static void main(String args[]) {
        int portNumber = Integer.parseInt(args[0]);
        InsecureDeveloper dev = null;
        try {
            dev = new InsecureDeveloper(portNumber);
        } catch (UnknownHostException e) {
            System.err.println("Error: could not resolve hostname");
            e.printStackTrace();
        }
        
        if (dev != null) {
            Scanner sc = new Scanner(System.in);
            do {
                System.out.println("Commands:\n"
                        + "\tRequest <Hostname> <Port> <LibraryName>"
                        + " <NumberLicenses>" + "\n\tOR\n"
                        + "\tLink <Hostname> <Port> <JARFileName>"
                        + "\n\tOR\n"
                        + "\tQuit");
                String command = sc.next();
                if (command.equalsIgnoreCase("Request")) {
                    String remoteHost = sc.next();
                    int remotePort = sc.nextInt();
                    String libName = sc.next();
                    int numLicenses = sc.nextInt();
                    try {
                        Socket connection = new Socket(remoteHost, remotePort);
                        dev.requestLicense(numLicenses, libName, connection);
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
                    System.out.printf("Please input %d libraries", nLibs);
                    List<String> libNames = new ArrayList<String>();
                    for (int i = 0; i < nLibs; i++) {
                        libNames.add(sc.next());
                    }

                    System.out.println("How many files to link?");
                    int nFiles = sc.nextInt();
                    System.out.printf("Please input %d class file paths",
                            nFiles);
                    List<File> classFiles = new ArrayList<File>();
                    for (int i = 0; i < nFiles; i++) {
                        classFiles.add(new File(sc.next()));
                    }
                    
                    try {
                        Socket connection = new Socket(remoteHost, remotePort);
                        dev.linkFiles(classFiles, libNames, jarFileName,
                                connection);
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
                }
            } while(sc.hasNext());
        }
    }
    
}