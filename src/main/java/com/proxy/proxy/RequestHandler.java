package com.proxy.proxy;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandler implements Runnable{
	
	private static Logger log = LoggerFactory.getLogger(RequestHandler.class);

	/**
	 * Socket connected to client
	 */
	Socket clientSocket;

	/**
	 * Read data client sends to proxy
	 */
	BufferedReader proxyToClientReader;

	/**
	 * Send data from proxy to client
	 */
	BufferedWriter proxyToClientWriter;
	
	/**
	 * Thread that is used to transmit data read from client to server when using HTTPS
	 * Reference to this is required so it can be closed once completed.
	 */
	private Thread httpsClientToServer;

	public RequestHandler(Socket clientSocket) {
		super();
		this.clientSocket = clientSocket;
		try{
			this.clientSocket.setSoTimeout(10000);
			proxyToClientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			proxyToClientWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		} 
		catch (IOException e) {
			log.error("Error trying to create reader or writer: ",e);
		}
	}

	@Override
	public void run() {
		// Get Request from client
		String requestString;
		try{
			requestString = proxyToClientReader.readLine();
		} catch (IOException e) {
			log.error("Error reading request from client",e);
			return;
		}
		
		// Parse out URL

		log.debug("Reuest Received {}", requestString);
		// Get the Request type
		String request = requestString.substring(0,requestString.indexOf(' '));

		// remove request type and space
		String urlString = requestString.substring(requestString.indexOf(' ')+1);

		// Remove everything past next space
		urlString = urlString.substring(0, urlString.indexOf(' '));

		// Prepend http:// if necessary to create correct URL
		if(!urlString.substring(0,4).equals("http")){
			String temp = "http://";
			urlString = temp + urlString;
		}
		
		// Check request type
		if(request.equals("CONNECT")){
			log.debug("HTTPS Request for : {}\n", urlString);
			
			handleHTTPSRequest(urlString);
		} 

		else{
			// Check if we have a cached copy
			log.debug("HTTP GET for : {} \n", urlString);
			sendToClient(urlString);
		}
		
		try {
			proxyToClientWriter.close();
			proxyToClientReader.close();
			clientSocket.close();
			log.info("request completed");
		} catch (IOException e) {
			log.error("Error trying to close connection with client");
		}
		
		
		
	}
	
	private void readHeaders() {
		// Only first line of HTTPS request has been read at this point (CONNECT *)
		// Read (and throw away) the rest of the initial data on the stream
		try {
			for(String header;(header = proxyToClientReader.readLine())!=null;){
				if (header.isEmpty()) break; // Stop when headers are completed. We're not interested in all the HTML.
				log.info("HTTP header line {}",header);
			}
		} catch (IOException e) {
			log.error("Error reading HTTPS headers",e);
		}
	}
	
	/**
	 * Sends the contents of the file specified by the urlString to the client
	 * @param urlString URL ofthe file requested
	 */
	private void sendToClient(String urlString){

		try{
			// Compute a logical file name as per schema
			// This allows the files on stored on disk to resemble that of the URL it was taken from
			int fileExtensionIndex = urlString.lastIndexOf(".");
			String fileExtension;

			// Get the type of file
			fileExtension = urlString.substring(fileExtensionIndex, urlString.length());

			// Get the initial file name
			String fileName = urlString.substring(0,fileExtensionIndex);

			// Trim off http://www. as no need for it in file name
			fileName = fileName.substring(fileName.indexOf('.')+1);

			// Remove any illegal characters from file name
			fileName = fileName.replace("/", "__");
			fileName = fileName.replace('.','_');
			
			// Trailing / result in index.html of that directory being fetched
			if(fileExtension.contains("/")){
				fileExtension = fileExtension.replace("/", "__");
				fileExtension = fileExtension.replace('.','_');
				fileExtension += ".html";
			}
		
			fileName = fileName + fileExtension;

			// Check if file is an image
			if((fileExtension.contains(".png")) || fileExtension.contains(".jpg") ||
					fileExtension.contains(".jpeg") || fileExtension.contains(".gif")){
				log.debug("Request is an image");
				// Create the URL
				URL remoteURL = new URL(urlString);
				BufferedImage image = ImageIO.read(remoteURL);

				if(image != null) {
					// Send response code to client
					String line = "HTTP/1.0 200 OK\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					writeToClient(line);

					// Send them the image data
					ImageIO.write(image, fileExtension.substring(1), clientSocket.getOutputStream());

				// No image received from remote server
				} else {
					log.debug("Sending 404 to client as image wasn't received from server {}", fileName);
					String error = "HTTP/1.0 404 NOT FOUND\n" +
							"Proxy-agent: ProxyServer/1.0\n" +
							"\r\n";
					writeToClient(error);
					return;
				}
			} 

			// File is a text file
			else {
				log.debug("Request is a text file");
				// Create the URL
				URL remoteURL = new URL(urlString);
				// Create a connection to remote server
				HttpURLConnection proxyToServerCon = (HttpURLConnection)remoteURL.openConnection();
				proxyToServerCon.setRequestProperty("Content-Type", 
						"application/x-www-form-urlencoded");
				proxyToServerCon.setRequestProperty("Content-Language", "en-US");  
				proxyToServerCon.setUseCaches(false);
				proxyToServerCon.setDoOutput(true);
			
				// Create Buffered Reader from remote Server
				BufferedReader proxyToServerReader = new BufferedReader(new InputStreamReader(proxyToServerCon.getInputStream()));
				

				// Send success code to client
				String line = "HTTP/1.0 200 OK\n" +
						"Proxy-agent: ProxyServer/1.0\n" +
						"\r\n";
				writeToClient(line);
				
				
				// Read from input stream between proxy and remote server
				while((line = proxyToServerReader.readLine()) != null){
					// Send on data to client
					proxyToClientWriter.write(line);
				}
				
				// Ensure all data is sent by this point
				proxyToClientWriter.flush();

				// Close Down Resources
				if(proxyToServerReader != null){
					proxyToServerReader.close();
				}
			}

			if(proxyToClientWriter != null){
				proxyToClientWriter.close();
			}
		} 

		catch (Exception e){
			log.error("Error ocurred trying to get resource ", e);
		}
	}
	
	
	/**
	 * Handles HTTPS requests between client and remote server
	 * @param urlString desired file to be transmitted over https
	 */
	private void handleHTTPSRequest(String urlString){
		// Extract the URL and port of remote 
		String url = urlString.substring(7);
		String[] pieces = url.split(":");
		url = pieces[0];
		int port  = Integer.parseInt(pieces[1]);
		readHeaders();
		try{

			// Get actual IP associated with this URL through DNS
			InetAddress address = InetAddress.getByName(url);
			
			// Open a socket to the remote server 
			Socket proxyToServerSocket = new Socket(address, port);
			proxyToServerSocket.setSoTimeout(10000);

			// Send Connection established to the client
			String line = "HTTP/1.0 200 Connection established\r\n" +
					"Proxy-Agent: ProxyServer/1.0\r\n" +
					"\r\n";
			writeToClient(line);
			
			// Client and Remote will both start sending data to proxy at this point
			// Proxy needs to asynchronously read data from each party and send it to the other party

			// Create a new thread to listen to client and transmit to server
			ClientToServerHttpsTransmit clientToServerHttps = 
					new ClientToServerHttpsTransmit(clientSocket.getInputStream(), proxyToServerSocket.getOutputStream());
			
			httpsClientToServer = new Thread(clientToServerHttps);
			httpsClientToServer.start();
			log.debug("started client to server transmit");
			
			// Listen to remote server and relay to client
			byte[] buffer = new byte[4096];
			int read;
			do {
				read = proxyToServerSocket.getInputStream().read(buffer);
				if (read > 0) {
					clientSocket.getOutputStream().write(buffer, 0, read);
					if (proxyToServerSocket.getInputStream().available() < 1) {
						clientSocket.getOutputStream().flush();
					}
				}
			} while (read >= 0);

			// Close Down Resources
			if(proxyToServerSocket != null){
				proxyToServerSocket.close();
			}

			if(proxyToClientWriter != null){
				proxyToClientWriter.close();
			}
			
			
		} catch (SocketTimeoutException e) {
			log.error("Request timeout : "+ urlString , e);
			String line = "HTTP/1.0 504 Timeout Occured after 10s\n" +
					"User-Agent: ProxyServer/1.0\n" +
					"\r\n";
			writeToClient(line);
		} 
		catch (Exception e){
			log.error("Error on HTTPS : "+ urlString , e);
		}
	}
	
	private void writeToClient(String message) {
		try{
			proxyToClientWriter.write(message);
			proxyToClientWriter.flush();
		} catch (IOException ioe) {
			log.error("Error sending timout error : " , ioe);
		}
	}
	
	
	
	/**
	 * Listen to data from client and transmits it to server.
	 * This is done on a separate thread as must be done 
	 * asynchronously to reading data from server and transmitting 
	 * that data to the client. 
	 */
	class ClientToServerHttpsTransmit implements Runnable{
		
		InputStream proxyToClientIS;
		OutputStream proxyToServerOS;
		
		/**
		 * Creates Object to Listen to Client and Transmit that data to the server
		 * @param proxyToClientIS Stream that proxy uses to receive data from client
		 * @param proxyToServerOS Stream that proxy uses to transmit data to remote server
		 */
		public ClientToServerHttpsTransmit(InputStream proxyToClientIS, OutputStream proxyToServerOS) {
			this.proxyToClientIS = proxyToClientIS;
			this.proxyToServerOS = proxyToServerOS;
		}

		@Override
		public void run(){
			try {
				// Read byte by byte from client and send directly to server
				byte[] buffer = new byte[4096];
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				int read;
				do {
					read = proxyToClientIS.read(buffer);
					if (read > 0) {
						proxyToServerOS.write(buffer, 0, read);
						outputStream.write(buffer, 0, read);
						if (proxyToClientIS.available() < 1) {
							proxyToServerOS.flush();
						}
					}
				} while (read >= 0);
			}
			catch (IOException e) {
				log.error("Proxy to client HTTPS read timed out : ", e);
			}
		}
	}

	
	
}
