package org.irunika.websocket.test.bench;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client handler for WebSocket frames.
 *
 * @author irunika
 */
public class WebSocketClientHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClientHandler.class);

    private final int clientId;
    private final WebSocketClientHandshaker handshaker;
    private final Queue<String> messageQueue;
    private final EventLoopGroup eventLoopGroup;
    private final CountDownLatch countDownLatch;
    private ChannelPromise handshakeFuture;
    private final AtomicInteger noOfMessagesReceived;
    private final AtomicInteger noOfErrorMessagesAtomicInteger;
    private long endTime;
    private int expectedNoOfMessages;
    private ChannelHandlerContext ctx;

    public WebSocketClientHandler(int clientId, int expectedNoOfMessages, WebSocketClientHandshaker handshaker,
                                  Queue<String> messageQueue, EventLoopGroup eventLoopGroup, CountDownLatch countDownLatch) {
        this.clientId = clientId;
        this.expectedNoOfMessages = expectedNoOfMessages;
        this.handshaker = handshaker;
        this.messageQueue = messageQueue;
        this.eventLoopGroup = eventLoopGroup;
        this.countDownLatch = countDownLatch;
        this.noOfMessagesReceived = new AtomicInteger();
        this.noOfErrorMessagesAtomicInteger = new AtomicInteger();
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        endTime = System.currentTimeMillis();
        logMessage("WebSocket Client disconnected!");
        eventLoopGroup.shutdownGracefully().addListener(future -> {
            WebSocketClientRunner.removeConnection();
            countDownLatch.countDown();
        });
    }

    public long getEndTime() {
        return endTime;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            logMessage("WebSocket Client connected!");
            handshakeFuture.setSuccess();
            ((FullHttpResponse) msg).release();
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        try {
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
                String expected = messageQueue.remove();
                String actual = textFrame.text();
                if (!expected.equals(actual)) {
                    noOfErrorMessagesAtomicInteger.incrementAndGet();
                    logMessage(String.format("Error receiving message expected: %s, actual: %s", expected, actual));
                }

                if (expectedNoOfMessages == noOfMessagesReceived.incrementAndGet()) {
                    ctx.writeAndFlush(new CloseWebSocketFrame(1000, "Going away")).addListener(
                            ChannelFutureListener.CLOSE);
                }
            } else if (frame instanceof PongWebSocketFrame) {
                logMessage("WebSocket Client received pong");
            } else if (frame instanceof CloseWebSocketFrame) {
                logMessage("WebSocket Client received closing " + ((CloseWebSocketFrame) frame).statusCode());
                ch.close();
            }
        } finally {
            frame.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error", cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        log.info("exception caught: {}", cause.getMessage());
        ctx.close();
    }

    private void logMessage(String msg) {
        log.info("Client {}: {}", clientId, msg);
    }

    public int getNoOfErrorMessages() {
        return noOfErrorMessagesAtomicInteger.get();
    }

    public int getNoOfMessagesReceived() {
        return noOfMessagesReceived.get();
    }

    public void setStopReceivingMessages() {
        if (expectedNoOfMessages == -1) {
            ctx.writeAndFlush(new CloseWebSocketFrame(1000, "Going away")).addListener(
                    ChannelFutureListener.CLOSE);
        }
    }
}
