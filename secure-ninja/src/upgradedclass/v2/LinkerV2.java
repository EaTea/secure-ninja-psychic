package upgradedclass.v2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;


public class LinkerV2 {

    private SSLServerSocketFactory sslServFact;
    private SSLServerSocket serverConnection;
    private SSLSocketFactory sslFact;

    public LinkerV2(int portNumber, String keyFile, String trustFile,
            String password) throws UnknownHostException, IOException {
        System.out.printf("Creating new LinkBroker at %s:%d\n",
                InetAddress.getLocalHost().getCanonicalHostName(),
                portNumber);
        sslFact = (SSLSocketFactory) SecurityUtilitiesV2
                .getSSLSocketFactory(keyFile, trustFile, password);
        sslServFact = (SSLServerSocketFactory) SecurityUtilitiesV2
                .getSSLServerSocketFactory(keyFile, trustFile, password);
        serverConnection = (SSLServerSocket) sslServFact.createServerSocket(
                portNumber, 0, InetAddress.getLocalHost());
    }


    private void processRequests() {
        while (true) {
            SSLSocket s = null;
            try {
                s = (SSLSocket) serverConnection.accept();
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
     
    private void packageJarFile(SSLSocket connection) {
        DataInputStream inStream =
            NetworkUtilitiesV2.getDataInputStream(connection);
        DataOutputStream outStream =
            NetworkUtilitiesV2.getDataOutputStream(connection);
        
        if (inStream != null && outStream != null) {
            JarOutputStream jarOut = null;
            Manifest manifest = null;
            try {
            	 manifest = new Manifest();
            	 manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            	 manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, ".");
            	 
        		 String mainFile = inStream.readUTF();
        		 mainFile = mainFile.replaceAll("/", ".").substring(0, mainFile.lastIndexOf(".class"));
                 manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainFile);
                 
                 System.err.println("DEBUG:main-point: " + mainFile);

                 jarOut = new JarOutputStream(
                        new FileOutputStream("temp.jar"), manifest);
            } catch (FileNotFoundException e1) {
                System.err.println("Error: could not create temp.jar");
                e1.printStackTrace();
            } catch (IOException e1) {
                System.err.println("Error: I/O error whilst constructing "
                        + "JarOutputStream");
                e1.printStackTrace();
            }
            
            if (jarOut != null && manifest != null) {
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
                            SSLSocket swhCon;
                            try {
                                System.out.println("Establishing socket to "
                                        + swhIP + ":" + swhPort);
                                swhCon = (SSLSocket) sslFact.createSocket(swhIP, swhPort);
                                DataOutputStream swhOut =
                                    NetworkUtilitiesV2.getDataOutputStream(swhCon);
                                
                                // tell SWH that request is for license verify
                                swhOut.writeUTF("VER");
                                
                                swhOut.writeUTF(license);
                                swhOut.writeUTF(
                                        connection.getInetAddress().getCanonicalHostName());
                                
                                if (NetworkUtilitiesV2.readFile(swhCon, jarOut, true)) {
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
                                System.err.println("Error: could not tell developer"
                                		+ " that we could not read licenses");
                                e.printStackTrace();
                                count = -1;
                                break;
                            }
                        }
                    }
                    
                    if (count == nLicenses) {
                        System.out.println("Successfully read all files into JAR");
                        try {
                            outStream.writeBoolean(true);
                        } catch(IOException e) {
                            System.err.println("Error: Could not notify developer of success");
                            e.printStackTrace();
                        }
                        
                        System.out.println("Reading class files from Developer");
                        int nFiles = -1;
                        try {
                            nFiles = inStream.readInt();
                            count = 0;
                            
                            for (int i = 0; i < nFiles; i++) {
                                if (NetworkUtilitiesV2.readFile(connection, jarOut, true)) {
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

                        try {
                            jarOut.close();
                        } catch (IOException e) {
                            System.err.println("Error: could not properly close JarOutputStream");
                            e.printStackTrace();
                        }
                        
                        if (nFiles != -1 && count == nFiles) {
                            File jarFile = new File("temp.jar");
                            if (NetworkUtilitiesV2.writeFile(connection, jarFile)) {
                                System.out.println("Sent JAR file successfully");
                            } else {
                                System.out.println("Could not send JAR file");
                            }
                            
                            System.out.println("Cleanup: deleting JAR file");
                            if (jarFile.delete()) {
                            	System.out.println("Successfully deleted"
                            			+ " temp.jar");
                            } else {
                            	System.out.println("Something went wrong,"
                            			+ " could not delete temp.jar");
                            }
                        }
                    } else {
                        try {
                            System.out.println("Linking fail, notifying developer");
                            outStream.writeBoolean(false);
                        } catch (IOException e) {
                            System.err.println("Error: could not notify developer");
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("Could not read number of licenses");
                }
            }
            NetworkUtilitiesV2.closeSocketDataInputStream(inStream, connection);
            NetworkUtilitiesV2.closeSocketDataOutputStream(outStream, connection);

            System.out.println("<-----End Communication----->");
            System.out.println();
        }
    }

    public static void main(String[] args) {
       /* if (args.length < 1) {
            System.out.println("Usage: requires one integer parameter for port");
            return;
        }*/
        System.out.println("Please Enter:\n"
                + "\t<Port> <keyFilePath> <trustFilePath> <Password>");
        Scanner sc = new Scanner(System.in);
        int portNumber = sc.nextInt();
        String keyFile = sc.next();
        String trustFile = sc.next();
        String password = sc.next();
        LinkerV2 link = null;
        try {
            link = new LinkerV2(portNumber, keyFile, trustFile, password);
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
        sc.close();
    }
}
