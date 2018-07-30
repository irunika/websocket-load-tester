package org.irunika.websocket.test.bench;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;

/**
 * Runner for WebSocket client.
 *
 * @author irunika
 */
public class WebSocketClientRunner implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientRunner.class);

    private final int clientId;
    private final int noOfMessages;
    private final long messageDelay;
    private final WebSocketClient webSocketClient;
    private long startTime;
    private String initialPayload;
    private boolean stopSendingMessages = false;

    private static AtomicInteger noOfActiveConnections = new AtomicInteger();
    private static AtomicInteger maxNoOfActiveConnection = new AtomicInteger();

    public WebSocketClientRunner(int clientId, String url, int noOfMessages, int payloadSize, long messageDelay,
                                 CountDownLatch countDownLatch) {
        this.clientId = clientId;
        this.noOfMessages = noOfMessages;
        this.messageDelay = messageDelay >= 0 ? messageDelay : 0;
        this.webSocketClient = new WebSocketClient(clientId, noOfMessages, url, countDownLatch);
        this.initialPayload = createPayload(payloadSize);
    }

    public static synchronized void addConnection() {
        int currentNoOfConnections = noOfActiveConnections.incrementAndGet();
        if (currentNoOfConnections > maxNoOfActiveConnection.get()) {
            maxNoOfActiveConnection.set(currentNoOfConnections);
        }
    }

    public static void removeConnection() {
        noOfActiveConnections.decrementAndGet();
    }

    public static int getMaxNoOfActiveConnections() {
        return maxNoOfActiveConnection.get();
    }

    @Override
    public void run() {
        try {
            webSocketClient.init();
            startTime = System.currentTimeMillis();
            log.info("Client {}: Sending messages...", clientId);

            int messageId = 0;
            while (true) {
                webSocketClient.sendText(String.format("%s%d", initialPayload, messageId));
                if (stopSendingMessages || messageId == noOfMessages - 1) {
                    webSocketClient.stop();
                    break;
                }
                messageId++;
                Thread.sleep(messageDelay);
            }

        } catch (URISyntaxException | SSLException | InterruptedException e) {
            log.error("Error : ", e);
            Thread.currentThread().interrupt();
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return webSocketClient.getEndTime();
    }

    public int getNoOfErrorMessages() {
        return webSocketClient.getNoOfErrorMessages();
    }

    public void setStopSendingMessages(boolean stopSendingMessages) {
        this.stopSendingMessages = stopSendingMessages;
    }

    public int getNoOfMessagesReceived() {
        return webSocketClient.getNoOfMessagesReceived();
    }

    public int getClientId() {
        return clientId;
    }

    public static String createPayload(int payloadSize) {
        StringBuilder payloadBuilder = new StringBuilder();
        for (int i = 0; i < payloadSize; i++) {
            payloadBuilder.append('#');
        }
        return payloadBuilder.toString();
    }
}
