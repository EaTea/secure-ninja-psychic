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
 * 
 * @author Edwin Tay(20529864) & Wan Ying Goh(20784663)
 *
 */
public class Linker {

    /**
     * 
     */
    private SSLServerSocketFactory sslServFact;

    /**
     * 
     */
    private SSLServerSocket serverConnection;

    /**
     * 
     */
    private SSLSocketFactory sslFact;

    /**
     * Linker's constructor.
     * @param portNumber the port the linker server used
     * @param keyFile
     * @param keyStorePW
     * @param trustFile
     * @param trustStorePW
     * @throws UnknownHostException
     * @throws IOException
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
        }
    }

    /**
     * 
     * @param connection socket connecting to developer
     */
    private void packageJarFile(SSLSocket connection) {
        DataInputStream inStream = NetworkUtilities.getDataInputStream(connection);
        DataOutputStream outStream = NetworkUtilities.getDataOutputStream(connection);

        if (inStream != null && outStream != null) {
            JarOutputStream jarOut = null;
            Manifest manifest = null;
            try {
                manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, ".");

                String mainFile = inStream.readUTF();
//                mainFile = mainFile.replaceAll("/", ".").substring(0,
//                        mainFile.lastIndexOf(".java"));
                manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainFile);

                Log.log("main-point: " + mainFile);

                jarOut = new JarOutputStream(new FileOutputStream("temp.jar"), manifest);
            } catch (FileNotFoundException e1) {
                Log.error("could not create temp.jar");
                e1.printStackTrace();
            } catch (IOException e1) {
                Log.error("I/O error whilst constructing " + "JarOutputStream");
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
                    for (int i = 0; i < nLicenses; i++) {
                        String swhIP = null, license = null;
                        int swhPort = -1;
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
                                Log.log("Something went wrong,"
                                        + " could not delete temp.jar");
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
            File tempJar = new File("temp.jar");
            if (tempJar.exists()) {
                tempJar.delete();
            }
        }
        sc.close();
    }
}
