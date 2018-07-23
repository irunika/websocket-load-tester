package org.irunika.websocket.tester.config;

import com.beust.jcommander.Parameter;

public class Args {

    @Parameter(names = {"-u", "--url"}, description = "URL of the endpoint")
    String url = "ws://localhost:9090/echo";

    @Parameter(names = {"-n", "--connections"}, description = "No of connections")
    int noOfConnections = 1;

    @Parameter(names = {"-m", "--messages"}, description = "No of messages per connection")
    int noOfMessages = 1;

    @Parameter(names = {"-p", "--payload"}, description = "Payload size")
    int payloadInBytes = 1;

    public String getUrl() {
        return url;
    }

    public int getNoOfConnections() {
        return noOfConnections;
    }

    public int getNoOfMessages() {
        return noOfMessages;
    }

    public int getPayloadInBytes() {
        return payloadInBytes;
    }
}
