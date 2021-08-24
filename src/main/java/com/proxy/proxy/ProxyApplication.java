package com.proxy.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProxyApplication implements CommandLineRunner{
	private static Logger log = LoggerFactory.getLogger(ProxyApplication.class);
	@Value("${proxy.port:8080}")
	int proxyPort;
	public static void main(String[] args) {
		SpringApplication.run(ProxyApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Proxy Service Started");
		ClientHandler clientHandler = new ClientHandler(proxyPort);
		clientHandler.start();
	}

}
