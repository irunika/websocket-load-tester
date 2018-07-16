package org.irunika.websocket.tester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLException;

public class TestRunner implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TestRunner.class);

    public static void main(String[] args) throws InterruptedException {
        int threadPoolSize = 1000;
        int noOfConnections = 1000;
        int noOfMessages = 1000;
        int payloadInBytes = 1000;
        String url = "ws://localhost:9090/echo";

        CountDownLatch countDownLatch = new CountDownLatch(noOfConnections);
        List<TestRunner> testRunners = new LinkedList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        log.info("Creating connections...");
        for (int i = 0; i < noOfConnections; i++) {
            TestRunner testRunner = new TestRunner(i, url, noOfMessages, payloadInBytes, countDownLatch);
            testRunners.add(testRunner);
            executor.execute(testRunner);
        }

        if (!countDownLatch.await(Long.MAX_VALUE, TimeUnit.SECONDS)) {
            log.error("Latch countdown without completion");
        }

        executor.shutdown();

        double totalTps = 0;
        for (TestRunner testRunner: testRunners) {
            double tps = (double) (1000 * noOfMessages) / (testRunner.getEndTime() - testRunner.getStartTime());
            totalTps = totalTps + tps;
            log.info("Client {}: Test run TPS: {}", testRunner.getClientId(), tps);
        }

        log.info("Average TPS: {}", (totalTps / noOfConnections));
        log.info("Max no of active connections: {}", TestRunner.getMaxNoOfActiveConnections());
        log.info("Running is done!");
    }

    private final int clientId;
    private final int noOfMessages;
    private final WebSocketClient webSocketClient;
    private long startTime;
    private String initialPayload;

    private static AtomicInteger noOfActiveConnections = new AtomicInteger();
    private static AtomicInteger maxNoOfActiveConnection = new AtomicInteger();

    public TestRunner(int clientId, String url, int noOfMessages, int paylaodSize, CountDownLatch countDownLatch) {
        this.clientId = clientId;
        this.noOfMessages = noOfMessages;
        this.webSocketClient = new WebSocketClient(clientId, url, noOfMessages, countDownLatch);
        this.initialPayload = createPayload(paylaodSize);
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
            for (int i = 0; i < noOfMessages; i++) {
                webSocketClient.sendText(String.format("%s%d", initialPayload, i));
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
