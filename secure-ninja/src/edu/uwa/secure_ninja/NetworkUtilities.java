package edu.uwa.secure_ninja;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class NetworkUtilities {

    public static DataInputStream getDataInputStream(Socket connection) {
        DataInputStream inStream = null;
        try {
            System.out.println("Opening input stream from: "
                    + connection.getInetAddress().getHostName() + ":"
                    + connection.getPort());
            inStream = new DataInputStream(connection.getInputStream());
        } catch (IOException e) {
            System.err.println("Error: could not open I/O socket stream");
            e.printStackTrace();
        }
        return inStream;
    }

    public static DataOutputStream getDataOutputStream(Socket connection) {
        DataOutputStream outStream = null;
        try {
            System.out.println("Opening output stream to: "
                    + connection.getInetAddress().getHostName() + ":"
                    + connection.getPort());
            outStream = new DataOutputStream(connection.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error: could not open I/O socket stream");
            e.printStackTrace();
        }
        return outStream;
    }

    public static void closeSocketDataInputStream(DataInputStream inStream,
            Socket connection) {
        try {
            System.out.println("Closing input stream from: "
                    + connection.getInetAddress().getHostName() + ":"
                    + connection.getPort());
            inStream.close();
        } catch (IOException e) {
            System.err.println("Error: could not close I/O socket stream");
            e.printStackTrace();
            return;
        }
    }

    public static void closeSocketDataOutputStream(DataOutputStream outStream,
            Socket connection) {
        try {
            System.out.println("Closing output stream to: "
                    + connection.getInetAddress().getHostName() + ":"
                    + connection.getPort());
            outStream.close();
        } catch (IOException e) {
            System.err.println("Error: could not close I/O socket stream");
            e.printStackTrace();
            return;
        }
    }

    public static boolean readFile(Socket connection, OutputStream target,
            boolean isWritingJAR) {
        DataInputStream inStream = getDataInputStream(connection);
        boolean success = true;

        if (inStream != null) {
            try {
                System.out.println("Reading file from network");
                long fileLength = inStream.readLong();
                System.out.println("Length: " + fileLength);
                String filePath = inStream.readUTF();
                System.out.println("File path: " + filePath);

                if (isWritingJAR) {
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

            closeSocketDataInputStream(inStream, connection);
            return success;
        }
        return false;
    }

    public static boolean writeFile(Socket connection, File f) {
        DataOutputStream outStream = getDataOutputStream(connection);
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

            closeSocketDataOutputStream(outStream, connection);
            return success;
        }
        return false;
    }
}