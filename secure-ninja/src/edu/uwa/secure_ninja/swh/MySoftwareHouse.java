/**
 * 
 */
package edu.uwa.secure_ninja.swh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import edu.uwa.secure_ninja.developer.License;

/**
 * @author wying1211
 *
 */
public class MySoftwareHouse implements ISoftwareHouse {
    /**
     *
     */
    private HashMap<License, Integer> clients;
    /**
     * 
     */
    private ServerSocket server;

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        MySoftwareHouse mswh = new MySoftwareHouse();
        try {
            mswh.server = new ServerSocket(1234);
            mswh.generateLicenses(mswh.server.accept());
            mswh.server.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

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
            int numLicenses = in.readInt();
            String developerID =
                    connection.getInetAddress().getHostName();
            long time = System.currentTimeMillis();
            String licence = library + numLicenses + developerID
                    + time;
            out.writeUTF(licence);
            out.writeInt(numLicenses);
//            System.out.println(licence);
            License temp = new License(licence, server.getInetAddress(),
                    library, connection.getInetAddress());
            clients.put(temp, numLicenses);
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
    public boolean verifyLicense(String license, String library,
            String developerID) {
        // TODO Auto-generated method stub
        return false;
    }

    /** 
     */
    public void acceptLicenses(Socket connection) {
        // TODO Auto-generated method stub

    }

    /** 
     */
    public void sendLibraryFile(Socket connection, String libraryName) {
        // TODO Auto-generated method stub

    }

}
