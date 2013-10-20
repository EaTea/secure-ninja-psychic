package snp.linkbroker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class UpgradedLinkBrokerV1 {
    private SSLServerSocketFactory sslSrvFact;

    private SSLSocketFactory sslFact;

    private int portNumber;

    private SSLServerSocket server;

    public void setPortNumber(int portNum) {
        this.portNumber = portNum;
    }

    public int getPortNumber() {
        return this.portNumber;
    }

    public UpgradedLinkBrokerV1(int portNum) {
        this.setPortNumber(portNum);
        sslSrvFact =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }


    /**
     * 
     * @param connection
     *            connection with developer
     * @return
     */
    public void getFiles(SSLSocket connection) {
        // TODO Auto-generated method stub
        try {
            DataInputStream in = new DataInputStream(
                    connection.getInputStream());
            DataOutputStream out = new DataOutputStream(
                    connection.getOutputStream());
            int JARNameLen = in.readInt();
            String JARName = in.readUTF();
            System.err.println(JARName);
            FileOutputStream stream = new FileOutputStream(JARName + ".jar");
            JarOutputStream jarOut =
                    new JarOutputStream(stream, new Manifest());
            int requests = in.readInt();
            System.err.println(requests);
            int count = 0;
            SSLSocket[] swhConnections = new SSLSocket[requests];
            DataInputStream[] swhIns = new DataInputStream[requests];
            DataOutputStream[] swhOuts = new DataOutputStream[requests];
            for (int i = 0; i < requests; i++) {
                int swhIPLen = in.readInt();
                String swhIP = in.readUTF();
                System.err.println(swhIPLen + " " + swhIP);
                swhConnections[i] =
                        (SSLSocket) sslFact.createSocket
                        (swhIP, 1778 /*(testing purpose)portNumber*/);
                System.err.println(swhConnections[i].getPort());
                swhIns[i] = new DataInputStream(
                        swhConnections[i].getInputStream());
                swhOuts[i] = new DataOutputStream(
                        swhConnections[i].getOutputStream());
                swhOuts[i].writeInt(in.readInt());
                String license = in.readUTF();
                System.err.println("<" + license + ">");
                swhOuts[i].writeUTF(license);
                long length = swhIns[i].readLong();
                System.err.println(length);
                if (length > -1) {
                    count++;
                    int len = swhIns[i].readInt();
                    JarEntry jar = new JarEntry(swhIns[i].readUTF());
                    jarOut.putNextEntry(jar);
                }
                for (long j = 0; j < length; j++) {
                    jarOut.write(swhIns[i].read());
                }
            }
            System.err.println(count);
            if (count == requests) {
                for (int i = 0; i < requests; i++) {
                    swhOuts[i].writeBoolean(true); // success
                    swhOuts[i].close();
                    swhConnections[i].close();
                }
                out.writeInt(0);
                // read class file from developer
                int classes = in.readInt();
                for (int i = 0; i < classes; i++) {
                    int pathLen = in.readInt();
                    jarOut.putNextEntry(new JarEntry(in.readUTF()));
                    long size = in.readLong();
                    int a;
                    boolean success = true;
                    for (long j = 0; j < size; j++) {
                        a = in.read();
                        if (a == -1) {
                            success = false;
                            break;
                        } else {
                            jarOut.write(a);
                        }
                    }
                    // out.writeBoolean(success);
                    if (!success) {
                        return; // linking fail
                    }
                    // TODO jarOut.closeEntry();
                }
                jarOut.close();
                File jar = new File(JARName + ".jar");
                FileInputStream jarIn = new FileInputStream(jar);
                out.writeLong(jar.length());
                System.err.println(jar.length());
                int a;
                while ((a = jarIn.read()) != -1) {
                    out.write(a);
                }
                jarIn.close();
            } else {
                out.writeInt(-1); // failed
            }
            out.close();
            in.close();
            jarOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        UpgradedLinkBrokerV1 bork = new UpgradedLinkBrokerV1(port);
        System.err.println(bork.getPortNumber());
        try {
            bork.server = (SSLServerSocket) bork.sslSrvFact.createServerSocket(
                    bork.getPortNumber(), 0 /* Java Implementation Specific*/ ,
                                InetAddress.getLocalHost());
            System.out.println("Link Broker "
                    + bork.server.getInetAddress().getCanonicalHostName() + " "
                    + bork.server.getLocalPort());
            bork.getFiles((SSLSocket) bork.server.accept());
            bork.server.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
