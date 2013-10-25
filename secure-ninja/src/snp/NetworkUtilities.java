package snp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Utilities class for networking.
 * @author Edwin Tay(20529864) && Wan Ying Goh(20784663)
 * @version Oct 2013
 */
public class NetworkUtilities {

    /**
     * Map from number to hex characters.
     */
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * @param connection the connected socket
     * @return DataInputStream associated with the specified socket. Null if it is unsuccessful.
     */
    public static DataInputStream getDataInputStream(SSLSocket connection) {
        DataInputStream inStream = null;
        try {
            Log.log("Opening input stream from: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            inStream = new DataInputStream(connection.getInputStream());
        } catch (IOException e) {
            Log.error("Could not open I/O socket stream");
            e.printStackTrace();
        }
        return inStream;
    }

    /**
     * @param connection the connected socket
     * @return DataOutputStream associated with the specified socket. Null if it is unsuccessful.
     */
    public static DataOutputStream getDataOutputStream(SSLSocket connection) {
        DataOutputStream outStream = null;
        try {
            Log.log("Opening output stream to: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            outStream = new DataOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            Log.error("Could not open I/O socket stream");
            e.printStackTrace();
        }
        return outStream;
    }

    /**
     * Close the DateInputStream associated with the specified socket.
     * @param inStream the datatInputStream to be closed
     * @param connection the connected socket, for debugging/logging purposes
     */
    public static void closeSocketDataInputStream(DataInputStream inStream, SSLSocket connection) {
        try {
            Log.log("Closing input stream from: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            inStream.close();
        } catch (IOException e) {
            Log.error("Could not close I/O socket stream");
            e.printStackTrace();
            return;
        }
    }

    /**
     * Close the DateOutputStream associated with the specified socket.
     * @param outStream the datatOutputStream to be closed.
     * @param connection the connected socket, for debugging/logging purposes
     */
    public static void closeSocketDataOutputStream(DataOutputStream outStream, SSLSocket connection) {
        try {
            Log.log("Closing output stream to: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            outStream.close();
        } catch (IOException e) {
            Log.error("Could not close I/O socket stream");
            e.printStackTrace();
            return;
        }
    }

    /**
     * Reading a file from the connection and write it the targeted OutputStream.
     * @param connection the connected socket
     * @param target outputStream to write to
     * @param isJAREntry indication whether the file is a JarEntry
     * @return true if reading is successful. False otherwise.
     */
    public static boolean readFile(SSLSocket connection, OutputStream target, boolean isJAREntry) {
        DataInputStream inStream = getDataInputStream(connection);
        boolean success = true;

        if (inStream != null) {
            try {
                Log.log("Reading file from network");
                long fileLength = inStream.readLong();
                Log.log("Length: " + fileLength);
                String filePath = inStream.readUTF();
                Log.log("File path: " + filePath);
                // NOTE: filepath is always written, regardless of usage, and though this is a
                // little wasteful it doesn't reveal any new information

                if (isJAREntry) {
                    // Note: filePath separators may need to change
                    // e.g. ("/" -> "\")
                    Log.log("Constructing a JAR file");
                    JarOutputStream jarTarget = (JarOutputStream) target;
                    jarTarget.putNextEntry(new JarEntry(filePath));
                }

                for (long l = 0; l < fileLength; l++) {
                    target.write(inStream.read());
                }
            } catch (IOException e) {
                Log.error("Could not read file from network");
                e.printStackTrace();
                success = false;
            }

            return success;
        }
        return false;
    }

    /**
     * Writing a file to the connection's outputStream.
     * @param connection the connected socket
     * @param f file to be written
     * @param name the full qualified classname of the file.
     * @return true if writing is successful. False otherwise.
     */
    public static boolean writeFile(SSLSocket connection, File f, String name) {
        DataOutputStream outStream = getDataOutputStream(connection);
        boolean success = true;

        if (outStream != null) {
            try {
                Log.log("Writing file to network");
                long fileSize = f.length();
                outStream.writeLong(fileSize);
                Log.log("Length: " + fileSize);
                // writes the fully qualified filename as a classname
                // most of the time, we are writing a Java class file into a JAREntry and it is
                // necessay to note what the directory structure of the JAR is
                String path = name.replace('.', '/') + ".class";
                outStream.writeUTF(path);
                Log.log("File path: " + path);

                FileInputStream fileInStream = new FileInputStream(f);
                for (long l = 0; l < fileSize; l++) {
                    outStream.write(fileInStream.read());
                }
                fileInStream.close();
            } catch (IOException e) {
                Log.error("Could not write file to network");
                e.printStackTrace();
                success = false;
            }

            return success;
        }
        return false;
    }

    /**
     * Converts bytes to hexString.
     * This code is copied from StackOverflow.
     * Note: the code on StackOverflow is licensed under CreativeCommons and is used here for
     * educational purposes.
     * @param bytes bytes to be converted.
     * @return hexString representation of the bytes
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Converts hexString to bytes.
     * This code is copied from StackOverflow.
     * Note: the code on StackOverflow is licensed under CreativeCommons and is used here for
     * educational purposes.
     * @param s hexString to be converted.
     * @return the bytes representation
     */
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}