/**
 * 
 */
package edu.uwa.secure_ninja.developer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.HashMap;

/**
 * @author Edwin Tay() & Wan Ying Goh(20784663)
 *
 */
public class MyDeveloper implements IDeveloper {
    /**
     *
     */
    private HashMap<String, License> licenses =
            new HashMap<String, License>();
   /**
    *
    * @param args
    */
    public static void main(String[] args) {
        MyDeveloper mdev = new MyDeveloper();
        try {
            InetAddress addr = InetAddress.getByName("180.216.16.55");
            mdev.requestLicenses(10, "AAA", addr, 1777);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param numLicenses
     * @param libraryName
     * @param softwareHouseIP
     * @param portNumber
     */
    public final void requestLicenses(int numLicenses, String libraryName,
                InetAddress softwareHouseIP, int portNumber) {
        try {
            Socket client = new Socket(softwareHouseIP, portNumber);
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(
                    client.getOutputStream());
            out.writeUTF(libraryName);
            out.writeInt(numLicenses);
            String license = in.readUTF();
            int number = in.readInt();
            License temp = new License(license, softwareHouseIP, libraryName,
                    client.getInetAddress().getHostName(), number);
            System.out.println(client.getInetAddress());
            System.out.println(softwareHouseIP);
            licenses.put(license, temp);
            //System.out.println("Dev: " + license);
            in.close();
            out.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public boolean linkFiles(InetAddress linkBrokerIP, List<File> classFiles,
            List<License> licenses, String JARName, int portNumber) {
        // TODO Auto-generated method stub
        try {
            Socket client = new Socket(linkBrokerIP, portNumber);
            InputStream in = client.getInputStream();
            OutputStream out = client.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
