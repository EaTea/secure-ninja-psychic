package snp.swh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import snp.swh.UpgradedLicenseV1;

public class UpgradedSoftWareHouseV1 {
    /**
    *
    */
    private HashMap<String, UpgradedLicenseV1> clientLicenses = new HashMap<String, UpgradedLicenseV1>();
    /**
    * 
    */
    private HashMap<String, File> libraries = new HashMap<String, File>();
    /**
    * 
    */
    private SSLServerSocketFactory sslSrvFact;

    private SSLServerSocket server;

    private int port;

    public UpgradedSoftWareHouseV1(int port) {
        sslSrvFact = (SSLServerSocketFactory) SSLServerSocketFactory
                .getDefault();
        setPort(port);
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        UpgradedSoftWareHouseV1 mswh = new UpgradedSoftWareHouseV1(port);
        for (int i = 1; i < args.length - 1; i += 2) {
            mswh.addLibraryFile(args[i], args[i + 1]);
        }
        try {
            mswh.server = (SSLServerSocket) mswh.sslSrvFact.createServerSocket(
                    port, 0 /* Java Implementation Specific */,
                    InetAddress.getLocalHost());
            System.out.println("Software House: "
                    + mswh.server.getInetAddress().getCanonicalHostName() + " "
                    + mswh.server.getLocalPort());
            for (String libName : mswh.libraries.keySet()) {
                System.out.println("\t" + libName + " "
                        + mswh.libraries.get(libName).getAbsolutePath());
            }
            mswh.generateLicenses((SSLSocket) mswh.server.accept());
            mswh.acceptLicenses((SSLSocket) mswh.server.accept());
            mswh.server.close(); // TODO: get rid of this?
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
    *
    */
    public void generateLicenses(SSLSocket connection) {
        try {
            DataInputStream in = new DataInputStream(
                    connection.getInputStream());
            DataOutputStream out = new DataOutputStream(
                    connection.getOutputStream());
            String library = in.readUTF();
            if (libraries.containsKey(library)) {
                int numLicenses = in.readInt();
                String developerID = connection.getInetAddress().getHostName();
                long time = System.currentTimeMillis();
                String licence = library + numLicenses + developerID + time;
                out.writeInt(licence.length());
                out.writeUTF(licence);
                // out.writeInt(numLicenses);
                // System.out.println(licence);
                UpgradedLicenseV1 temp = new UpgradedLicenseV1(licence,
                        server.getInetAddress(), library, connection
                                .getInetAddress().getHostName(), numLicenses);
                clientLicenses.put(licence, temp);
            } else {
                out.writeInt(0); // 0 means no license
            }
            in.close();
            out.close();
            connection.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
    */
    public boolean verifyLicense(String license) {
        UpgradedLicenseV1 temp = clientLicenses.get(license);
        System.err.println("[" + license + "]");
        if (temp != null) {
            if (temp.getNumberLicenses() > 0) {
                return true;
            } else {
                clientLicenses.remove(temp);
            }
        }
        return false;
    }

    /** 
    */
    public void acceptLicenses(SSLSocket connection) {
        // TODO Auto-generated method stub
        System.err.println("ME PORT NUMBER: " + connection.getLocalPort());
        try {
            DataInputStream in = new DataInputStream(
                    connection.getInputStream());
            int length = in.readInt();
            String license = in.readUTF();
            // accept it and send files;
            if (verifyLicense(license)) {
                UpgradedLicenseV1 temp = clientLicenses.get(license);
                String libraryName = temp.getLibraryName();
                sendLibraryFile(connection, libraryName);
                if (in.readBoolean()) { // everything got linked up
                    temp.decrementNumberLicenses();
                }
            } else {
                // send -1 if declined
                DataOutputStream out = new DataOutputStream(
                        connection.getOutputStream());
                out.writeLong(-1);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param connection
     * @param libraryName
     */
    public void sendLibraryFile(SSLSocket connection, String libraryName) {
        try {
            DataOutputStream out = new DataOutputStream(
                    connection.getOutputStream());
            File file = libraries.get(libraryName);
            FileInputStream fileIn = new FileInputStream(file);
            String path = file.getPath();
            out.writeLong(file.length());
            out.writeInt(path.length());
            out.writeUTF(path);
            int a;
            while ((a = fileIn.read()) != -1) {
                out.write(a);
            }
            fileIn.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @param libraryName
     * @param fileName
     */
    public void addLibraryFile(String libraryName, String fileName) {
        libraries.put(libraryName, new File(fileName));
    }

    public int getPort() {
        return port;
    }

    private void setPort(int port) {
        this.port = port;
    }

}
