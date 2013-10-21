/**
 * 
 */
package snp.linker;

import java.io.File;
import java.net.Socket;
import java.util.List;
import java.util.jar.JarFile;

/**
 * @author Wan Ying GOH, Edwin TAY
 *
 */
public interface ILinkBroker {

	/**
	 * 
	 * @param connection
	 * @return
	 */
	List<File> getFiles(Socket connection);
	
	/**
	 * 
	 * @param classFiles
	 * @return
	 */
	JarFile packageIntoJar(List<File> classFiles);
	
	/**
	 * 
	 * @param connection
	 */
	void sendJARFile(Socket connection);
	
}
