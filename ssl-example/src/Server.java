import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class Server {
	private SSLServerSocket server;
	private SSLServerSocketFactory sslServFact;

	public Server(int port) throws IOException, UnrecoverableKeyException,
	        KeyManagementException, NoSuchAlgorithmException,
	        KeyStoreException, CertificateException {
		sslServFact = (SSLServerSocketFactory) getSSLServerSocketFactory();
		server = (SSLServerSocket) sslServFact.createServerSocket(port);
	}

	private SSLServerSocketFactory getSSLServerSocketFactory()
	        throws NoSuchAlgorithmException, KeyStoreException,
	        CertificateException, FileNotFoundException, IOException,
	        UnrecoverableKeyException, KeyManagementException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(new FileInputStream("./server-keystore.jks"),
		        "cits3231".toCharArray());
		
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(new FileInputStream("./server-truststore.jks"),
		        "cits3231".toCharArray());

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
		        .getDefaultAlgorithm());
		kmf.init(keyStore, "cits3231".toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory
		        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);

		SSLContext ctx = SSLContext.getInstance("SSL");
		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return ctx.getServerSocketFactory();
	}

	public static void main(String[] args) throws IOException,
	        UnrecoverableKeyException, KeyManagementException,
	        NoSuchAlgorithmException, KeyStoreException, CertificateException {
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
