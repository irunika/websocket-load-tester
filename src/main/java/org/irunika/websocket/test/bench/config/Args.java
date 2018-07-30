package org.irunika.websocket.test.bench.config;

import com.beust.jcommander.Parameter;

public class Args {

    @Parameter(names = {"-u", "--url"}, description = "URL of the endpoint")
    private String url = "";

    @Parameter(names = {"-n", "--connections"}, description = "No of connections")
    private int noOfConnections = 1;

    @Parameter(names = {"-m", "--messages"}, description = "No of messages per connection")
    private int noOfMessages = 100;

    @Parameter(names = {"-p", "--payload"}, description = "Payload size in bytes")
    private int payloadInBytes = 100;

    @Parameter(names = {"-t", "--time"}, description = "Time for test in minutes")
    private int testTimeInMinutes = 0;

    @Parameter(names = {"-d, --delay"}, description = "Delay between two consecutive messages")
    private long messageDelay = 0;

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

    public int getTestTimeInMinutes() {
        return testTimeInMinutes;
    }

    public long getMessageDelay() {
        return messageDelay;
    }
}
