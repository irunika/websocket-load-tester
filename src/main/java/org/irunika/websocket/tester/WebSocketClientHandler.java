/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
//The MIT License
//
//Copyright (c) 2009 Carl Bystršm
//
//Permission is hereby granted, free of charge, to any person obtaining a copy
//of this software and associated documentation files (the "Software"), to deal
//in the Software without restriction, including without limitation the rights
//to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//copies of the Software, and to permit persons to whom the Software is
//furnished to do so, subject to the following conditions:
//
//The above copyright notice and this permission notice shall be included in
//all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//THE SOFTWARE.

package org.irunika.websocket.tester;

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
    private final int expectedNoOfMsgs;
    private final EventLoopGroup eventLoopGroup;
    private final CountDownLatch countDownLatch;
    private ChannelPromise handshakeFuture;
    private final AtomicInteger noOfMessagesAtomicInteger = new AtomicInteger();
    private final AtomicInteger noOfErrorMessagesAtomicInteger = new AtomicInteger();
    private long endTime;

    public WebSocketClientHandler(int clientId, WebSocketClientHandshaker handshaker, Queue<String> messageQueue,
                                  int expectedNoOfMsgs, EventLoopGroup eventLoopGroup, CountDownLatch countDownLatch) {
        this.clientId = clientId;
        this.handshaker = handshaker;
        this.messageQueue = messageQueue;
        this.expectedNoOfMsgs = expectedNoOfMsgs;
        this.eventLoopGroup = eventLoopGroup;
        this.countDownLatch = countDownLatch;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
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

                noOfMessagesAtomicInteger.incrementAndGet();
                if (noOfMessagesAtomicInteger.get() == expectedNoOfMsgs) {
                    endTime = System.currentTimeMillis();
                    ctx.writeAndFlush(new CloseWebSocketFrame(1000, "Going away")).addListener(ChannelFutureListener.CLOSE);
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
}
