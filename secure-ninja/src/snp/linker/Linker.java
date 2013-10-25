package snp.linker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLServerSocket;

import snp.Log;
import snp.NetworkUtilities;
import snp.SecurityUtilities;

/**
 * Our LinkBroker software agent.
 * It packages Java .class files into JAR when requested by developer.
 * @author Edwin Tay(20529864) & Wan Ying Goh(20784663)
 * @version Oct 2013
 */
public class Linker {

    /**
     * Provides SSLServerSockets for future usage --- should be initialised with a key store so that
     * this agent can prove their trustworthiness to clients.
     */
    private SSLServerSocketFactory sslServFact;

    /**
     * The server connection that this agent uses to communicate with Developers.
     */
    private SSLServerSocket serverConnection;

    /**
     * The SSLSocketFactory which provides SSLSockets for contacting SoftwareHouses.
     * Note that it should be initialised with a trust store so that "registered" software houses
     * can be contacted for linking.
     */
    private SSLSocketFactory sslFact;

    /**
     * Linker's constructor.
     * @param portNumber the port the linker server's ServerSocket listens on
     * @param keyFile the relative path to the keystore
     * @param keyStorePW the password to access the keystore specified by keyfile; note that every
     *  keypair in the keystore is expected to have the same password.
     * @param trustFile the relative path to the truststore
     * @param trustStorePW the password to access the truststore specified by trustfile
     * @throws UnknownHostException if this host canont be determined
     * @throws IOException if an I/O error occurs
     */
    public Linker(int portNumber, String keyFile, String keyStorePW, String trustFile, String trustStorePW)
            throws UnknownHostException, IOException {
        sslFact = (SSLSocketFactory) SecurityUtilities.getSSLSocketFactory(trustFile, trustStorePW);
        sslServFact = (SSLServerSocketFactory) SecurityUtilities.getSSLServerSocketFactory(
                keyFile, keyStorePW);
        serverConnection = (SSLServerSocket) sslServFact.createServerSocket(portNumber, 0,
                InetAddress.getLocalHost());
        Log.log("Created a new LinkBroker at %s:%d\n", InetAddress.getLocalHost()
                .getCanonicalHostName(), portNumber);
    }
    
    /**
     * Private method to process the request from developer.
     */
    private void processRequests() {
        while (true) {
            SSLSocket s = null;
            try {
                s = (SSLSocket) serverConnection.accept();
                Log.log("New connection from " + s.getInetAddress().getCanonicalHostName() + ":"
                        + s.getPort());
            } catch (IOException e) {
                Log.error("I/O error whilst accepting socket");
                e.printStackTrace();
            }

            if (s != null) {
                packageJarFile(s);
                try {
                    s.close();
                } catch (IOException e) {
                    Log.error("I/O error whilst closing" + " socket");
                    e.printStackTrace();
                }
            }
            
            // note that it is possible for the temporary JAR to be left behind --- we ought to
            // clean up the temporary file that is left behind
            File tempJar = new File("temp.jar");
            if (tempJar.exists()) {
                Log.log("Performing cleanup");
                tempJar.delete();
            }
        }
    }

    /**
     * Packages the JAR file as requested by the remote host of the SSLSocket.
     * This method will open up connections to contact Software Houses and ask for license
     * verification and library providing.
     * 
     * If the JAR cannot successfully be packaged, the Linker will attempt to provide as much
     * information as possible to the Developer to explain what went wrong.
     * @param connection socket connecting to developer
     */
    private void packageJarFile(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);

        if (inStream != null && outStream != null) {
            // JAR Creation: specify manifest
            JarOutputStream jarOut = null;
            Manifest manifest = null;
            try {
                manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, ".");

                String mainFile = inStream.readUTF();
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainFile);

                Log.log("main-point: " + mainFile);

                jarOut = new JarOutputStream(new FileOutputStream("temp.jar"), manifest);
            } catch (FileNotFoundException e1) {
                Log.error("Could not create temp.jar");
                e1.printStackTrace();
            } catch (IOException e1) {
                Log.error("I/O error whilst constructing JarOutputStream");
                e1.printStackTrace();
            }

            if (jarOut != null && manifest != null) {
                int nLicenses = 0;
                try {
                    Log.log("Reading number of licenses");
                    nLicenses = inStream.readInt();
                } catch (IOException e) {
                    Log.error("I/O error during license reading");
                    e.printStackTrace();
                    nLicenses = -1;
                }

                int count = 0;

                if (nLicenses > 0) {
                    // read and verify each license
                    for (int i = 0; i < nLicenses; i++) {
                        String swhIP = null, license = null;
                        int swhPort = -1;
                        // here, we ask the developer to tell us the SWH to contact
                        // this seems sensible, since a SWH might provide multiple libraries
                        // or a library MAY be provided by multiple software houses
                        //
                        // if the approach used DNS to resolve the IP to send to, perhaps this would
                        // be mitigated and seem more sensible?
                        // we also note that the port number would usually be a range of numbers
                        // that the SWH and Linker agree to use beforehand.
                        try {
                            swhIP = inStream.readUTF();
                            swhPort = inStream.readInt();
                            license = inStream.readUTF();
                        } catch (IOException e) {
                            Log.error("Could not read license information");
                            e.printStackTrace();
                            break;
                        }

                        if (swhIP != null && license != null && swhPort != -1) {
                            SSLSocket swhCon;
                            try {
                                Log.log("Establishing socket to " + swhIP + ":"
                                        + swhPort);
                                swhCon = (SSLSocket) sslFact.createSocket(swhIP, swhPort);
                                DataOutputStream swhOut = NetworkUtilities
                                        .getDataOutputStream(swhCon);
                                DataInputStream swhIn = NetworkUtilities.getDataInputStream(swhCon);

                                // tell SWH that request is for license verify
                                swhOut.writeUTF("VER");

                                swhOut.writeUTF(license);
                                swhOut.writeUTF(connection.getInetAddress().getCanonicalHostName());
                                
                                // writes success code to the Dev and SWH to inform them of the
                                // result and ACK the response resp.
                                int success = swhIn.readInt();
                                outStream.writeInt(success);
                                swhOut.writeInt(success);

                                if (success == 0 && NetworkUtilities.readFile(swhCon, jarOut, true)) {
                                    Log.log("Successfully read file");
                                    Log.log("Notifying SWH and Dev");
                                    count++;
                                } else {
                                    swhCon.close();
                                    jarOut.close();
                                    break;
                                }

                                swhCon.close();
                            } catch (UnknownHostException e) {
                                Log.error("Could not resolve SWH IP");
                                e.printStackTrace();
                            } catch (IOException e) {
                                Log.error("Encountered I/O issue getting library");
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                // could not read license information
                                Log.log("License information could not be read --- exiting");
                                outStream.writeBoolean(false);
                                count = -1;
                                break;
                            } catch (IOException e) {
                                Log.error("Could not tell developer that we could not read " +
                                		"licenses");
                                e.printStackTrace();
                                count = -1;
                                break;
                            }
                        }
                    }

                    if (count == nLicenses) {
                        Log.log("Successfully read all files into JAR");
                        try {
                            outStream.writeBoolean(true);
                        } catch (IOException e) {
                            Log.error("Could not notify developer of success");
                            e.printStackTrace();
                        }

                        Log.log("Reading class files from Developer");
                        int nFiles = -1;
                        try {
                            nFiles = inStream.readInt();
                            count = 0;

                            for (int i = 0; i < nFiles; i++) {
                                // reading a file and intending to treat it as a JAREntry
                                if (NetworkUtilities.readFile(connection, jarOut, true)) {
                                    count++;
                                } else {
                                    Log.log("Could not read files to JAR");
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            Log.error("IO Exception whilst reading class files");
                            e.printStackTrace();
                        }

                        try {
                            jarOut.close();
                        } catch (IOException e) {
                            Log.error("Could not properly close JarOutputStream");
                            e.printStackTrace();
                        }

                        if (nFiles > 0 && count == nFiles) {
                            File jarFile = new File("temp.jar");
                            if (NetworkUtilities.writeFile(connection, jarFile, "temp.jar")) {
                                Log.log("Sent JAR file successfully");
                            } else {
                                Log.log("Could not send JAR file");
                            }

                            Log.log("Cleanup: deleting JAR file");
                            if (jarFile.delete()) {
                                Log.log("Successfully deleted temp.jar");
                            } else {
                                Log.error("Could not delete temp.jar");
                            }
                        }
                    } else {
                        try {
                            Log.log("Linking fail, notifying developer");
                            outStream.writeBoolean(false);
                        } catch (IOException e) {
                            Log.error("Could not notify developer");
                            e.printStackTrace();
                        }
                    }
                } else {
                    Log.log("Could not read number of licenses");
                }
            }
            NetworkUtilities.closeSocketDataInputStream(inStream, connection);
            NetworkUtilities.closeSocketDataOutputStream(outStream, connection);

            Log.logEnd();
        }
    }

    /**
     * The main program for running Linker.
     * 5 arguments are expected and they should be given in this order:
     * [portNumber] [keyStore filepath] [keystore password] 
     * [truststore filepath] [truststore password]
     * @param args the arguments that are expected
     */
    public static void main(String[] args) {
        if (args.length != 5) {
            System.err.println("Usage: needs 5 arguments.");
            System.err.println("\tArgument 1 = port number");
            System.err.println("\tArgument 2 = keystore filepath");
            System.err.println("\tArgument 3 = keystore password");
            System.err.println("\tArgument 4 = truststore filepath");
            System.err.println("\tArgument 5 = truststore password");
            System.exit(1);
        }

        Scanner sc = new Scanner(System.in);
        int portNumber = Integer.parseInt(args[0]);
        String keyFile = args[1];
        String keyStorePW = args[2];
        String trustFile = args[3];
        String trustStorePW = args[4];
        Linker link = null;
        try {
            link = new Linker(portNumber, keyFile, keyStorePW, trustFile, trustStorePW);
        } catch (UnknownHostException e) {
            Log.error("Could not resolve hostname");
            e.printStackTrace();
        } catch (IOException e) {
            Log.error("I/O error whilst establishing new Linker");
            e.printStackTrace();
        }
        if (link != null) {
            link.processRequests();
        }
        sc.close();
    }
}
