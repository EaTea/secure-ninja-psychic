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

public class NetworkUtilities {

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

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

                if (isJAREntry) {
                    // FIXME: filePath separators may need to change
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

    public static boolean writeFile(SSLSocket connection, File f, String name) {
        DataOutputStream outStream = getDataOutputStream(connection);
        boolean success = true;

        if (outStream != null) {
            try {
                Log.log("Writing file to network");
                long fileSize = f.length();
                outStream.writeLong(fileSize);
                Log.log("Length: " + fileSize);
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

    // TODO: Add ACKs to other authors
    
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