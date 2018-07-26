package org.irunika.websocket.tester;

import com.beust.jcommander.JCommander;
import org.irunika.websocket.tester.config.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class is contain the main class to run the test.
 *
 * @author irunika
 */
public class TestRunner {

    private static final Logger log = LoggerFactory.getLogger(TestRunner.class);

    public static void main(String[] argv) throws InterruptedException {

        // Use JCommander for argument parsing
        Args args = new Args();
        JCommander.newBuilder().addObject(args).build().parse(argv);

        String url = args.getUrl();
        int noOfConnections = args.getNoOfConnections();
        int noOfMessages = args.getNoOfMessages();
        int payloadInBytes = args.getPayloadInBytes();
        int testTimeInMinutes = args.getTestTimeInMinutes();

        CountDownLatch countDownLatch = new CountDownLatch(noOfConnections);
        List<WebSocketClientRunner> webSocketClientRunners = new LinkedList<>();
        ExecutorService executor = Executors.newFixedThreadPool(noOfConnections);
        log.info("Creating connections...");

        long testStartTime = 0L;
        try {
            for (int clientId = 0; clientId < noOfConnections; clientId++) {
                WebSocketClientRunner webSocketClientRunner = new WebSocketClientRunner(
                        clientId, url, testTimeInMinutes > 0 ? -1 : noOfMessages, payloadInBytes, countDownLatch);
                webSocketClientRunners.add(webSocketClientRunner);
                if (clientId == 0L) {
                    testStartTime = System.currentTimeMillis();
                }
                executor.execute(webSocketClientRunner);
            }

            if (testTimeInMinutes > 0) {
                Thread.sleep((long) testTimeInMinutes * 60 * 1000);
                webSocketClientRunners.forEach(
                        webSocketClientRunner -> webSocketClientRunner.setStopSendingMessages(true));
            }

            if (!countDownLatch.await(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                log.error("Latch countdown without completion");
            }

        } finally {
            long testEndTime = System.currentTimeMillis();
            executor.shutdown();
            long totalNoOfMessages = 0;
            double totalTPS = 0;
            int totalNoOfErrorMessages = 0;
            for (WebSocketClientRunner webSocketClientRunner : webSocketClientRunners) {
                totalNoOfMessages = totalNoOfMessages + webSocketClientRunner.getNoOfMessagesReceived();
                totalNoOfErrorMessages = totalNoOfErrorMessages + webSocketClientRunner.getNoOfErrorMessages();
                double tps = calculateTPS(noOfMessages, webSocketClientRunner);
                totalTPS = totalTPS + tps;
                log.info("Client {}: Test run TPS: {}", webSocketClientRunner.getClientId(), tps);
            }

            log.info("Average TPS per client: {}", (totalTPS / noOfConnections));
            log.info("Total time taken for the test: {} minutes",
                     testTimeInMinutes > 0 ? testTimeInMinutes : getTimeInSecs(testStartTime, testEndTime) / 60);
            log.info("Max no of concurrent connections: {}/{}", WebSocketClientRunner.getMaxNoOfActiveConnections(), noOfConnections);
            log.info("Total no of message round trips: {}", totalNoOfMessages);
            log.info("No of error messages:{} out of : {}", totalNoOfErrorMessages, totalNoOfMessages);
            log.info("Throughput: {}", getThroughput(testStartTime, testEndTime, totalNoOfMessages));
            log.info("Running is done!");
        }
    }

    private static double calculateTPS(int noOfMessages, WebSocketClientRunner webSocketClientRunner) {
        double timeInSecs = getTimeInSecs(webSocketClientRunner.getStartTime(), webSocketClientRunner.getEndTime());
        return (double) noOfMessages / timeInSecs;
    }

    private static double getThroughput(long testStartTime, long testEndTime, long totalNoOfMessages) {
        long totalTimeInMillis = testEndTime - testStartTime;
        double totalTimeInSecs = (double) totalTimeInMillis / 1000;
        return (double) totalNoOfMessages / totalTimeInSecs;
    }

    private static double getTimeInSecs(long startTime, long endTime) {
        return (double) (endTime - startTime) / 1000;
    }

}
