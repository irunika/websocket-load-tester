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

        CountDownLatch countDownLatch = new CountDownLatch(noOfConnections);
        List<WebSocketClientRunner> webSocketClientRunners = new LinkedList<>();
        ExecutorService executor = Executors.newFixedThreadPool(noOfConnections);
        log.info("Creating connections...");

        long testStartTime = 0L;
        try {

            for (int i = 0; i < noOfConnections; i++) {
                WebSocketClientRunner webSocketClientRunner = new WebSocketClientRunner(i, url, noOfMessages,
                                                                                        payloadInBytes, countDownLatch);
                webSocketClientRunners.add(webSocketClientRunner);
                if (i == 0L) {
                    testStartTime = System.currentTimeMillis();
                }
                executor.execute(webSocketClientRunner);
            }

            if (!countDownLatch.await(Long.MAX_VALUE, TimeUnit.SECONDS)) {
                log.error("Latch countdown without completion");
            }

        } finally {
            long testEndTime = System.currentTimeMillis();
            executor.shutdown();
            long totalNoOfMsgs = noOfConnections * noOfMessages;
            double totalTps = 0;
            int totalNoOfErrorMessages = 0;
            for (WebSocketClientRunner webSocketClientRunner : webSocketClientRunners) {
                double tps = (double) (1000 * noOfMessages) / (webSocketClientRunner.getEndTime() - webSocketClientRunner
                        .getStartTime());
                totalTps = totalTps + tps;
                totalNoOfErrorMessages = totalNoOfErrorMessages + webSocketClientRunner.getNoOfErrorMessages();
                log.info("Client {}: Test run TPS: {}", webSocketClientRunner.getClientId(), tps);
            }

            log.info("Max no of active connections: {}", WebSocketClientRunner.getMaxNoOfActiveConnections());
            log.info("Average TPS per client: {}", (totalTps / noOfConnections));
            log.info("No of error messages:{} out of : {}", totalNoOfErrorMessages, totalNoOfMsgs);
            log.info("Throughput: {}", getThroughput(testStartTime, testEndTime, totalNoOfMsgs));
            log.info("Running is done!");
        }
    }

    public static double getThroughput(long testStartTime, long testEndTime, long totalNoOfMsgs) {
        long totalTimeInMillis = testEndTime - testStartTime;
        double totalTimeInSecs = (double) totalTimeInMillis / 1000;
        return (double) totalNoOfMsgs / totalTimeInSecs;
    }

    public static double getErrorPercentage(int noOfErrorMsgs, long totalNoOfMsgs) {
        return ((double) noOfErrorMsgs * 100) / totalNoOfMsgs;
    }
}
