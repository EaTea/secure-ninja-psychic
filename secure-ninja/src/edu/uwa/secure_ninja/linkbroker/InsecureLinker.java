package edu.uwa.secure_ninja.linkbroker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import edu.uwa.secure_ninja.NetworkUtilities;

public class InsecureLinker {

    private ServerSocket serverConnection;
    
    public InsecureLinker(int portNumber) throws UnknownHostException, IOException {
        System.out.printf("Creating new LinkBroker at %s:%d\n",
                InetAddress.getLocalHost().getCanonicalHostName(),
                portNumber);
        serverConnection = new ServerSocket(portNumber, 0,
                InetAddress.getLocalHost());
    }
    
    private void processRequests() {
        while (true) {
            Socket s = null;
            try {
                s = serverConnection.accept();
                System.out.println("New connection from "
                        + s.getInetAddress().getCanonicalHostName()
                        + ":" + s.getPort());
            } catch (IOException e) {
                System.err.println("Error: I/O error whilst accepting socket");
                e.printStackTrace();
            }
            
            if (s != null) {
                packageJarFile(s);
                try {
                    s.close();
                } catch(IOException e) {
                    System.err.println("Error: I/O error whilst closing" +
                    		" socket");
                    e.printStackTrace();
                }
            }
        }
    }
     
    private void packageJarFile(Socket connection) {
        DataInputStream inStream =
            NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream =
            NetworkUtilities.getDataOutputStream(connection);
        
        if (inStream != null && outStream != null) {
            JarOutputStream jarOut = null;
            try {
                 jarOut = new JarOutputStream(
                        new FileOutputStream("temp.jar"), new Manifest());
            } catch (FileNotFoundException e1) {
                System.err.println("Error: could not create temp.jar");
                e1.printStackTrace();
            } catch (IOException e1) {
                System.err.println("Error: I/O error whilst constructing "
                        + "JarOutputStream");
                e1.printStackTrace();
            }
            if (jarOut != null) {
                int nLicenses = 0;
                try {
                    System.out.println("Reading number of licenses");
                    nLicenses = inStream.readInt();
                } catch(IOException e) {
                    System.err.println("Error: I/O error during license reading");
                    e.printStackTrace();
                    nLicenses = -1;
                }
                
                int count = 0;
                
                if (nLicenses != -1) {
                    for (int i = 0; i < nLicenses; i++) {
                        String swhIP= null, license = null;
                        int swhPort = -1;
                        try {
                            swhIP = inStream.readUTF();
                            swhPort = inStream.readInt();
                            license = inStream.readUTF();
                        } catch(IOException e) {
                            System.err.println("Error: could not read license information");
                            e.printStackTrace();
                            break;
                        }

                        if (swhIP != null && license != null && swhPort != -1) {
                            Socket swhCon;
                            try {
                                System.out.println("Establishing socket to "
                                        + swhIP + ":" + swhPort);
                                swhCon = new Socket(swhIP, swhPort);
                                DataOutputStream swhOut =
                                    NetworkUtilities.getDataOutputStream(swhCon);
                                
                                // tell SWH that request is for license verify
                                swhOut.writeUTF("VER");
                                
                                swhOut.writeUTF(license);
                                swhOut.writeUTF(
                                        connection.getInetAddress().getCanonicalHostName());
                                
                                if (NetworkUtilities.readFile(swhCon, jarOut, true)) {
                                    System.out.println("Successfully read file");
                                    System.out.println("Notifying SWH and Dev");
                                    outStream.writeBoolean(true);
                                    swhOut.writeBoolean(true);
                                    count++;
                                } else {
                                    outStream.writeBoolean(false);
                                    swhOut.writeBoolean(false);
                                    swhCon.close();
                                    jarOut.close();
                                    break;
                                }
                                
                                jarOut.close();
                                swhCon.close();
                            } catch (UnknownHostException e) {
                                System.err.println("Error: could not resolve"
                                        + "SWH IP");
                                e.printStackTrace();
                            } catch (IOException e) {
                                System.err.println("Error: encountered I/O"
                                        + " issue getting library");
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                // could not read license information
                                System.out.println("License information could"
                                        + " not be read --- exiting");
                                outStream.writeBoolean(false);
                                count = -1;
                                break;
                            } catch (IOException e) {
                                
                            }
                        }
                    }
                    
                    if (count == nLicenses) {
                        System.out.println("Successfully read all files into JAR");
                        
                        System.out.println("Reading class files from Developer");
                        int nFiles = -1;
                        try {
                            nFiles = inStream.readInt();
                            count = 0;
                            
                            for (int i = 0; i < nFiles; i++) {
                                if (NetworkUtilities.readFile(connection, jarOut, true)) {
                                    count++;
                                } else {
                                    System.out.println("Could not read files to JAR");
                                    break;
                                }
                            }
                        } catch(IOException e) {
                            System.err.println("Error: IO Exception whilst reading class files");
                            e.printStackTrace();
                        }
                        
                        if (nFiles != -1 && count == nFiles) {
                            File jarFile = new File("temp.jar");
                            if (NetworkUtilities.writeFile(connection, jarFile)) {
                                System.out.println("Sent JAR file successfully");
                            } else {
                                System.out.println("Could not send JAR File");
                            }
                        }
                    }
                } else {
                    System.out.println("Could not read number of licenses");
                }
                
                try {
                    jarOut.close();
                } catch (IOException e) {
                    System.err.println("Error: could not properly close JarOutputStream");
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
        if (args.length < 1) {
            System.out.println("Usage: requires one integer parameter for port");
            return;
        }
        int portNumber = Integer.parseInt(args[0]);
        InsecureLinker link = null;
        try {
            link = new InsecureLinker(portNumber);
        } catch (UnknownHostException e) {
            System.err.println("Error: could not resolve hostname");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error: I/O error whilst establishing"
                    + " new Linker");
            e.printStackTrace();
        }
        if (link != null) {
            link.processRequests();
        }
    }
}
