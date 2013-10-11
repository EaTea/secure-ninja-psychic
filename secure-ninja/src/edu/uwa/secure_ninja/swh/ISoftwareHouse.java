/**
 * 
 */
package edu.uwa.secure_ninja.swh;

import java.net.Socket;

/**
 * 
 * @author Wan Ying GOH, Edwin TAY
 *
 */
public interface ISoftwareHouse {

	/**
	 * 
	 * @param numLicenses
	 * @param developerID
	 * @param connection
	 */
	void generateLicenses(Socket connection);
	
	/**
	 * 
	 * @param license
	 * @param library
	 * @param developerID
	 * @return
	 */
	boolean verifyLicense(String license, String library, String developerID);
	
	/**
	 * 
	 * @param connection
	 */
	void acceptLicenses(Socket connection);
	
	/**
	 * 
	 * @param connection
	 * @param libraryNames
	 */
	void sendLibraryFile(Socket connection, String libraryName);
}
