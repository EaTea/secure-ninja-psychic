/**
 * 
 */
package snp.swh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import snp.dev.License;

/**
 * @author wying1211
 *
 */
public class MySoftwareHouse {
    /**
     *
     */
    private HashMap<String, License> clientLicenses =
            new HashMap<String, License>();
    /**
     * 
     */
    private HashMap<String, File> libraries = new HashMap<String, File>();
    /**
     * 
     */
    private ServerSocket server;

    /**
     *
     */
    public void generateLicenses(Socket connection) {
        try {
            DataInputStream in = new DataInputStream(
                    connection.getInputStream());
            DataOutputStream out = new DataOutputStream(
                    connection.getOutputStream());
            String library = in.readUTF();
            if(libraries.containsKey(library)) {
                int numLicenses = in.readInt();
                String developerID =
                        connection.getInetAddress().getHostName();
                long time = System.currentTimeMillis();
                String licence = library + numLicenses + developerID
                        + time;
                out.writeInt(licence.length());
                out.writeUTF(licence);
 //               out.writeInt(numLicenses);
    //            System.out.println(licence);
                License temp = new License(licence, server.getInetAddress(),
                   library, connection.getInetAddress().getHostName(), numLicenses,
                   connection.getPort());
                clientLicenses.put(licence, temp);
            } else {
                out.writeInt(0); //0 means no license
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
        License temp = clientLicenses.get(license);
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
    public void acceptLicenses(Socket connection) {
        // TODO Auto-generated method stub
        System.err.println("ME PORT NUMBER: " + connection.getLocalPort());
        try {
            DataInputStream in = new DataInputStream(connection.getInputStream());
            int length = in.readInt();
            String license = in.readUTF();
            //accept it and send files;
            if (verifyLicense(license)) {
                License temp = clientLicenses.get(license);
                String libraryName = temp.getLibraryName();
                sendLibraryFile(connection, libraryName);
                if (in.readBoolean()) { //everything got linked up
                    temp.decrementNumberLicenses();
                }
            } else {
                //send -1 if declined
                DataOutputStream out = new DataOutputStream(connection.getOutputStream());
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
    public void sendLibraryFile(Socket connection, String libraryName) {
        try {
            DataOutputStream out =
                    new DataOutputStream(connection.getOutputStream());
            File file = libraries.get(libraryName);
            FileInputStream fileIn =
                    new FileInputStream(file);
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

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        MySoftwareHouse mswh = new MySoftwareHouse();
        int port = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length - 1; i += 2) {
            mswh.addLibraryFile(args[i], args[i + 1]);
        }
        try {
            mswh.server = new ServerSocket(port,
                    0 /* Java Implementation Specific */,
                    InetAddress.getLocalHost());
            System.out.println("Software House: " +
                    mswh.server.getInetAddress().getCanonicalHostName() + " " +
                    mswh.server.getLocalPort());
            for (String libName : mswh.libraries.keySet()) {
                System.out.println("\t" + libName + " " +
                        mswh.libraries.get(libName).getAbsolutePath());
            }
            mswh.generateLicenses(mswh.server.accept());
            mswh.acceptLicenses(mswh.server.accept());
            mswh.server.close(); //TODO: get rid of this?
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
