import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class Client {

    private SSLSocket connection;
    private SSLSocketFactory sslFact;
    
    public Client(String addr, int port) throws UnknownHostException, IOException {
        sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
        connection = (SSLSocket) sslFact.createSocket(addr, port);
    }
    
    public static void main(String[] args) throws IOException {
        Client c = new Client(args[0], 8080);
        DataInputStream dis = new DataInputStream(c.connection.getInputStream());
        DataOutputStream dos = new DataOutputStream(c.connection.getOutputStream());
        dos.writeUTF("Hello server!\n");
        System.out.println(dis.readUTF());
        dos.close();
        dis.close();
        c.connection.close();
    }
}
