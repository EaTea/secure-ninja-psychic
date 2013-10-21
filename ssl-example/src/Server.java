import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class Server {
    private SSLServerSocket server;
    private SSLServerSocketFactory sslServFact;
    
    public Server(int port) throws IOException {
        sslServFact = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        server = (SSLServerSocket) sslServFact.createServerSocket(port);
    }
    
    public static void main(String[] args) throws IOException {
        Server s = new Server(8080);
        Socket sto = s.server.accept();
        DataInputStream dis = new DataInputStream(sto.getInputStream());
        DataOutputStream dos = new DataOutputStream(sto.getOutputStream());
        String kk = dis.readUTF();
        System.out.println("Client sent: " + kk);
        String mk = "Hello client!";
        dos.writeUTF(mk);
        dis.close();
        dos.close();
        sto.close();
    }
}
