package org.irunika.websocket.tester;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import javax.net.ssl.SSLException;

/**
 * WebSocket client for testing.
 * This client validate the round trip of a given message.
 */
public class WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketClient.class);

    private final int clientId;
    private final String url;
    private final int expectedNoOfMsgs;
    private final Queue<String> messageQueue;
    private final CountDownLatch countDownLatch;
    private Channel channel;
    private WebSocketClientHandler handler;

    public WebSocketClient(int clientId, String url, int expectedNoOfMsgs, CountDownLatch countDownLatch) {
        this.clientId = clientId;
        this.url = url;
        this.expectedNoOfMsgs = expectedNoOfMsgs;
        this.countDownLatch = countDownLatch;
        this.messageQueue = new ConcurrentLinkedQueue<>();
    }

    public void init() throws URISyntaxException, SSLException, InterruptedException {
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null? "ws" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            log.error("Only WS(S) is supported.");
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        handler = new WebSocketClientHandler(clientId,
                WebSocketClientHandshakerFactory.newHandshaker(uri,WebSocketVersion.V13, null, true, new DefaultHttpHeaders()),
                messageQueue, expectedNoOfMsgs, group, countDownLatch);

        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline p = ch.pipeline();
                if (sslCtx != null) {
                    p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                }
                p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192),
                          WebSocketClientCompressionHandler.INSTANCE, handler);
            }
        });

        channel = b.connect(uri.getHost(), port).sync().channel();
        handler.handshakeFuture().sync();
        WebSocketClientRunner.addConnection();
    }

    public void sendText(String text) throws InterruptedException {
        messageQueue.add(text);
        channel.writeAndFlush(new TextWebSocketFrame(text)).sync();
    }

    public long getEndTime() {
        return handler.getEndTime();
    }
}
