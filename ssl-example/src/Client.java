import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class Client {

	private SSLSocket connection;
	private SSLSocketFactory sslFact;

	public Client(String addr, int port) throws UnknownHostException,
	        IOException, NoSuchAlgorithmException, UnrecoverableKeyException,
	        KeyManagementException, KeyStoreException, CertificateException {
		sslFact = (SSLSocketFactory) getSSLSocketFactory();
		connection = (SSLSocket) sslFact.createSocket(addr, port);
	}

	private SSLSocketFactory getSSLSocketFactory()
	        throws NoSuchAlgorithmException, KeyStoreException,
	        CertificateException, FileNotFoundException, IOException,
	        UnrecoverableKeyException, KeyManagementException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(new FileInputStream("./client-keystore.jks"),
		        "cits3231".toCharArray());

		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(new FileInputStream("./client-truststore.jks"),
		        "cits3231".toCharArray());

		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory
		        .getDefaultAlgorithm());
		kmf.init(keyStore, "cits3231".toCharArray());

		TrustManagerFactory tmf = TrustManagerFactory
		        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(trustStore);

		SSLContext ctx = SSLContext.getInstance("SSL");
		ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
		return ctx.getSocketFactory();
	}

	public static void main(String[] args) throws IOException,
	        NoSuchAlgorithmException, UnrecoverableKeyException,
	        KeyManagementException, KeyStoreException, CertificateException {
		Client c = new Client(args[0], 8080);
		DataInputStream dis = new DataInputStream(c.connection.getInputStream());
		DataOutputStream dos = new DataOutputStream(
		        c.connection.getOutputStream());
		dos.writeUTF("Hello server!\n");
		System.out.println(dis.readUTF());
		dos.close();
		dis.close();
		c.connection.close();
	}
}
