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
## Tests
To test that the service is working we can use our device connected to the proxy server to run the following tests

- To test http call open on a web browser `http://google.com`
- To test https call open on a web browser `https://google.com` and search something on google

If we want to use the same computer we can also make use of a tool like postman and configure it to use the proxy that we have running, we can read how to do it [here](https://learning.postman.com/docs/sending-requests/capturing-request-data/proxy/)

## Example Using Android to test the Proxy
> Proxy Setup

<div align="center">
<img src="https://user-images.githubusercontent.com/11185117/130718553-ae0fadd3-dfc7-496d-b840-f661fb6ec858.jpeg" alt="Proxy Setup" width="40%"/>
</div>

> Load Google Using Proxy

<div align="center">
<img src="https://user-images.githubusercontent.com/11185117/130718550-215f1099-9c2e-4e0a-a63d-0da7ff92b463.jpeg" alt="Google Loeaded" width="40%"/>
</div>

> Search on Google

<div align="center">
<img src="https://user-images.githubusercontent.com/11185117/130718555-caaab0d1-1448-4d8b-b1f0-79707f2cbc59.jpeg" alt="Search on Google" width="40%"/>
</div>


> Application Logs on Eclipse

<div align="center">
<img src="https://user-images.githubusercontent.com/11185117/130719656-23177942-ddca-4720-931b-55c66b76050e.png" alt="Eclipse Proxy Logs" width="100%"/>
</div>




## License
[MIT](https://choosealicense.com/licenses/mit/)
