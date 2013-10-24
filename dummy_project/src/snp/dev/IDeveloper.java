/**
 * 
 */
package snp.dev;

import java.io.File;
import java.net.InetAddress;
import java.util.List;

/**
 * 
 * @author Wan Ying GOH, Edwin TAY
 * 
 */
public interface IDeveloper {

    /**
	 *
	 * @param numLicenses
	 * @param softwareHouseIP
	 */
	void requestLicenses(int numLicenses, String libraryName,
	InetAddress softwareHouseIP, int portNumber);

	/**
	 * 
	 * @param linkBrokerIP
	 * @param classFiles
	 * @param licenses
	 * @param JARName
	 * @return
	 */
	boolean linkFiles(InetAddress linkBrokerIP, List<File> classFiles,
			List<String> licenses, String JARName, int portNumber);
}
