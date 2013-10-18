/**
 * 
 */
package edu.uwa.secure_ninja.swh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import edu.uwa.secure_ninja.developer.License;

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
     * @param args
     */
    public static void main(String[] args) {
        MySoftwareHouse mswh = new MySoftwareHouse();
        try {
            mswh.server = new ServerSocket(1777);
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
                   library, connection.getInetAddress().getHostName(), numLicenses);
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
        try {
            DataInputStream in = new DataInputStream(connection.getInputStream());
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
            out.writeInt(path.length());
            out.writeUTF(path);
            out.writeLong(file.length());
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
}
