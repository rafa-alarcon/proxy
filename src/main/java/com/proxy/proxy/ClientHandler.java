package com.proxy.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends Thread {
	
	private static Logger log = LoggerFactory.getLogger(ClientHandler.class);
	
	private int port = 9999;
	
	
	
	public ClientHandler(int port) {
		this();
		this.port = port;
	}

	public ClientHandler() {
		super("Proxy Client Handler");
	}


	@Override
    public void run() {
		log.debug("Starting proxy using port {}",port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
        	log.debug("Started proxy server");
            Socket socket;
        	//it will wait for a connection on the local port
            while ((socket = serverSocket.accept()) != null) {
            	log.debug("Socket connection accepted: {}", socket.getRemoteSocketAddress());
            	// Create new Thread and pass it Runnable RequestHandler
				Thread thread = new Thread(new RequestHandler(socket),"RequestHandler");
				
				thread.start();	
            }
           
        } catch (IOException e) {
        	log.error("Error ", e);
        }
    }
}
