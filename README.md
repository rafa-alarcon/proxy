# Java Proxy

This application uses java Sockets to work as a proxy server

## Build

To build this project you can use the maven wrapper included

```bash
./mvnw.cmd clean install
```

## Run
Once you build the project you can just use java to run this application using the next command

```bash
java -jar proxy-0.0.1-SNAPSHOT.jar
```
## How this works
This spring boot application that implements [CommandLineRunner](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/CommandLineRunner.html) interface to start a ClientHandler that will be watching for client calls to the proxy port.

### ClientHandler 
The client handler will create a ServerSocket to listen for client calls on the configured port and it will create a new thread for every call to be able to keep receiving requests. The thread that will process the client request will be using a RequestHandler

## Configuration
The only configurable value now for this application is the port used to listen for client calls and the logging levels we can change it using [externalized configuration](https://docs.spring.io/spring-boot/docs/1.0.0.RC5/reference/html/boot-features-external-config.html). Below we will see default values for confoiguration
```bash
logging.level.root=WARN
logging.level.com.proxy=INFO
proxy.port=8081
```


## License
[MIT](https://choosealicense.com/licenses/mit/)