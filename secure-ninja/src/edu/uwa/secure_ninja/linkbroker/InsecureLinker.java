package edu.uwa.secure_ninja.linkbroker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;

public class InsecureLinker {

    private ServerSocket serverConnection;
    
    public InsecureLinker(int portNumber) throws UnknownHostException, IOException {
        serverConnection = new ServerSocket(portNumber, 0,
                InetAddress.getLocalHost());
        System.out.printf("Created new LinkBroker at %s:%d\n",
                serverConnection.getInetAddress().getCanonicalHostName(),
                serverConnection.getLocalPort());
    }
}
