package snp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import javax.net.ssl.SSLSocket;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class NetworkUtilitiesV2 {

    private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static ObjectInputStream getObjectInputStream(SSLSocket connection) {
        ObjectInputStream inStream = null;
        try {
            System.out.println("Opening input stream from: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            inStream = new ObjectInputStream(connection.getInputStream());
        } catch (IOException e) {
            System.err.println("Error: could not open I/O socket stream");
            e.printStackTrace();
        }
        return inStream;
    }

    public static ObjectOutputStream getObjectOutputStream(SSLSocket connection) {
        ObjectOutputStream outStream = null;
        try {
            System.out.println("Opening output stream to: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            outStream = new ObjectOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error: could not open I/O socket stream");
            e.printStackTrace();
        }
        return outStream;
    }

    public static void closeSocketObjectInputStream(ObjectInputStream inStream, SSLSocket connection) {
        try {
            System.out.println("Closing input stream from: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            inStream.close();
        } catch (IOException e) {
            System.err.println("Error: could not close I/O socket stream");
            e.printStackTrace();
            return;
        }
    }

    public static void closeSocketObjectOutputStream(ObjectOutputStream outStream, SSLSocket connection) {
        try {
            System.out.println("Closing output stream to: "
                    + connection.getInetAddress().getHostName() + ":" + connection.getPort());
            outStream.close();
        } catch (IOException e) {
            System.err.println("Error: could not close I/O socket stream");
            e.printStackTrace();
            return;
        }
    }

    public static boolean readFile(SSLSocket connection, OutputStream target, boolean isWritingJAR) {
        ObjectInputStream inStream = getObjectInputStream(connection);
        boolean success = true;

        if (inStream != null) {
            try {
                System.out.println("Reading file from network");
                long fileLength = inStream.readLong();
                System.out.println("Length: " + fileLength);
                String filePath = inStream.readUTF();
                System.out.println("File path: " + filePath);

                if (isWritingJAR) {
                    // FIXME: filePath separators may need to change
                    // e.g. ("/" -> "\")
                    System.out.println("Constructing a JAR file");
                    JarOutputStream jarTarget = (JarOutputStream) target;
                    jarTarget.putNextEntry(new JarEntry(filePath));
                }

                for (long l = 0; l < fileLength; l++) {
                    target.write(inStream.read());
                }
            } catch (IOException e) {
                System.err.println("Error: could not read file from network");
                e.printStackTrace();
                success = false;
            }

            return success;
        }
        return false;
    }

    public static boolean writeFile(SSLSocket connection, File f) {
        ObjectOutputStream outStream = getObjectOutputStream(connection);
        boolean success = true;

        if (outStream != null) {
            try {
                System.out.println("Writing file to network");
                long fileSize = f.length();
                outStream.writeLong(fileSize);
                System.out.println("Length: " + fileSize);
                outStream.writeUTF(f.getPath());
                System.out.println("File path: " + f.getPath());

                FileInputStream fileInStream = new FileInputStream(f);
                for (long l = 0; l < fileSize; l++) {
                    outStream.write(fileInStream.read());
                }
                fileInStream.close();
            } catch (IOException e) {
                System.err.println("Error: could not write file to network");
                e.printStackTrace();
                success = false;
            }

            return success;
        }
        return false;
    }

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
}