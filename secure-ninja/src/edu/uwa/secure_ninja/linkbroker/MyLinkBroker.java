package edu.uwa.secure_ninja.linkbroker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * @author Edwin Tay & Wan Ying Goh
 *
 */
public class MyLinkBroker {
    private int portNumber;
    /**
     * 
     * @param connection connection with developer
     * @return
     */
    public void getFiles(Socket connection) {
        // TODO Auto-generated method stub
        try {
            DataInputStream in =
                    new DataInputStream(connection.getInputStream());
            DataOutputStream out =
                    new DataOutputStream(connection.getOutputStream());
            int JARNameLen = in.readInt();
            String JARName = in.readUTF();
            FileOutputStream stream = new FileOutputStream(JARName+".jar");
            JarOutputStream jarOut =
                    new JarOutputStream(stream, new Manifest());
            int requests = in.readInt();
            int count = 0;
            Socket[] swhConnections = new Socket[requests];
            DataInputStream[] swhIns = new DataInputStream[requests];
            DataOutputStream[] swhOuts = new DataOutputStream[requests];
            for (int i = 0; i < requests; i++) {
                int swhIPLen = in.readInt();
                String swhIP = in.readUTF();
                swhConnections[i] = new Socket(swhIP, portNumber);
                swhIns[i] =
                        new DataInputStream(swhConnections[i].getInputStream());
                swhOuts[i] =
                      new DataOutputStream(swhConnections[i].getOutputStream());
                swhOuts[i].writeInt(in.readInt());
                swhOuts[i].writeUTF(in.readUTF());
                long length = swhIns[i].readLong();
                if (length > -1) {
                    count++;
                    int len = swhIns[i].readInt();
                    JarEntry jar = new JarEntry(swhIns[i].readUTF());
                    jarOut.putNextEntry(jar);
                }
                for (long j = 0; j < length; j++) {
                    jarOut.write(swhIns[i].read());
                }
            }
            if (count == requests) {
                for (int i = 0; i < requests; i++) {
                    swhOuts[i].writeBoolean(true); //success
                    swhOuts[i].close();
                    swhConnections[i].close();
                }
                out.writeInt(0);
                //read class file from developer
                int classes = in.readInt();
                for (int i = 0; i < classes; i++) {
                    int pathLen = in.readInt();
                    jarOut.putNextEntry(new JarEntry(in.readUTF()));
                    long size = in.readLong();
                    int a;
                    boolean success = true;
                    for (long j = 0; j < size; j++) {
                        a = in.read();
                        if (a == -1) {
                            success = false; break;
                        } else {
                            jarOut.write(a);
                        }
                    }
                    out.writeBoolean(success);
                    if(!success) {
                        return; //linking fail
                    }
//          TODO      jarOut.closeEntry();
                }
                File jar = new File(JARName+".jar");
                FileInputStream jarIn = new FileInputStream(jar);
                out.writeLong(jar.length());
                int a;
                while ((a = jarIn.read()) != -1) {
                    out.write(a);
                }
                jarIn.close();
            } else {
                out.writeInt(-1); //failed
            }
            out.close();
            in.close();
            jarOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JarFile packageIntoJar(List<File> classFiles) {
        // TODO Auto-generated method stub
        return null;
    }

    public void sendJARFile(Socket connection) {
        // TODO Auto-generated method stub

    }

}
