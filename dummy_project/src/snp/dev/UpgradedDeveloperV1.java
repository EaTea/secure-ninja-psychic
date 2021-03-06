/**
 * 
 */
package snp.dev;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import snp.swh.UpgradedLicenseV1;

/**
 * @author wying1211
 * 
 */
public class UpgradedDeveloperV1 {
    /**
    *
    */
    private HashMap<String, UpgradedLicenseV1> licenses = new HashMap<String, UpgradedLicenseV1>();

    private SSLSocketFactory sslFact;

    public UpgradedDeveloperV1() {
        sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    /**
     * 
     * @param args
     */
    public static void main(String[] args) {
        UpgradedDeveloperV1 mdev = new UpgradedDeveloperV1();
        try {
            InetAddress addr = InetAddress.getLocalHost();
            mdev.requestLicenses(10, "blah", addr, 1778);
            System.out.println("How many class files?");
            List<File> files = new ArrayList<File>();
            // Scanner sc = new Scanner(System.in);
            int nFiles = 1;// sc.nextInt();
            System.out.println("Filenames?");
            for (int i = 0; i < nFiles; i++) {
                String fileName = "./edu/uwa/secure_ninja/developer/MyDeveloper.class";
                // sc.next();
                File f = new File(fileName);
                files.add(f);
            }
            System.out.println("How many libraries?");
            List<String> libNames = new ArrayList<String>();
            int nLibs = 1;// sc.nextInt();
            for (int i = 0; i < nLibs; i++) {
                String libName = "blah";// sc.next();
                libNames.add(libName);
            }
            mdev.linkFiles(InetAddress.getByName(args[0]), files, libNames,
                    "BLAH", 1888);
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
            System.err.println("Hello!");
            SSLSocket client = (SSLSocket) sslFact.createSocket(
                    softwareHouseIP, portNumber);
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(
                    client.getOutputStream());
            out.writeUTF(libraryName);
            out.writeInt(numLicenses);
            System.err.println("My name is Elder Price!");
            int size = in.readInt();
            if (size > 0) { // getting license
                String license = in.readUTF();
                UpgradedLicenseV1 temp = new UpgradedLicenseV1(license,
                        softwareHouseIP, libraryName, client.getInetAddress()
                                .getHostName(), numLicenses);
                licenses.put(libraryName, temp);
            } else {
                System.out.printf("Software house %s\n doesn't have the %s",
                        softwareHouseIP.getHostName(), libraryName);
            }
            System.err
                    .println("I would like to share with you the most amazing book!");
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
            List<String> libraryNames, String JARName, int portNumber) {
        // TODO Auto-generated method stub
        boolean linkSuccess = false;
        try {
            System.err.println("Hello again!");
            SSLSocket client = (SSLSocket) sslFact.createSocket(linkBrokerIP,
                    portNumber);
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(
                    client.getOutputStream());
            Iterator<String> it = libraryNames.iterator();
            ArrayList<UpgradedLicenseV1> requests = new ArrayList<UpgradedLicenseV1>();
            System.err.println("My name is Elder Green!");
            while (it.hasNext()) {
                String lic = it.next();
                UpgradedLicenseV1 temp = licenses.get(lic);
                if (temp != null) {
                    if (temp.getNumberLicenses() > 0) {
                        requests.add(temp);
                    } else {
                        it.remove();
                        System.out.printf("We don't have a license for %s\n",
                                lic);
                        client.close();
                        in.close();
                        out.close();
                        return false;
                    }
                } else {
                    System.out.printf("We don't have a license for %s\n", lic);
                    client.close();
                    in.close();
                    out.close();
                    return false;
                }
            }
            System.err.println("It's a book about America!");
            out.writeInt(JARName.length());
            out.writeUTF(JARName);
            out.writeInt(requests.size());
            for (UpgradedLicenseV1 s : requests) {
                String swhIP = s.getSoftwareHouseIP().getHostName();
                out.writeInt(swhIP.length());
                out.writeUTF(swhIP);
                String lic = s.getLicenseString();
                out.writeInt(lic.length());
                out.writeUTF(lic);
            }
            int success = in.readInt();
            System.err.println("A long long time ago! " + success);
            if (success == 0) {
                out.writeInt(classFiles.size());
                for (File file : classFiles) {
                    FileInputStream fileIn = new FileInputStream(file);
                    String path = file.getAbsolutePath();
                    out.writeInt(path.length());
                    out.writeUTF(path);
                    out.writeLong(file.length());
                    int a;
                    while ((a = fileIn.read()) != -1) {
                        out.write(a);
                    }
                    fileIn.close();
                }
                // read in bytes from Linkbroker
                FileOutputStream stream = new FileOutputStream(JARName + ".jar");
                long size = in.readLong();
                System.err.println(size);
                int a;
                for (long s = 0; s < size; s++) {
                    a = in.read();
                    if (a == -1) {
                        System.err
                                .println("Error while receiving from linkbroker");
                        break;
                    }
                    stream.write(a);
                }
                stream.close();
            } else {
                System.out.println("Linking failed");
                linkSuccess = false;
            }
            client.close();
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return linkSuccess;
    }

}
