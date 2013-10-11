/**
 * 
 */
package edu.uwa.secure_ninja.swh;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author wying1211
 *
 */
public class MySoftwareHouse implements ISoftwareHouse {
	public static void main(String[] args) {
		MySoftwareHouse mswh = new MySoftwareHouse();
		try {
			ServerSocket server = new ServerSocket(1234);
			mswh.generateLicenses(server.accept());
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/** 
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
			System.out.println(licence);
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
