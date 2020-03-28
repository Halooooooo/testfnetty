package io.proxy2;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import java.util.concurrent.ScheduledExecutorService;

public class HelloServerInitializer extends ChannelInitializer<SocketChannel> {

    private ScheduledExecutorService service;

    public HelloServerInitializer(ScheduledExecutorService service) {
        this.service = service;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
//                .addLast(new LoggingHandler(LogLevel.INFO))
                .addLast("encoder", new HttpResponseEncoder())
                .addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false))
//                .addLast("encoder",new HttpResponseEncoder())
                .addLast("handler", new HelloServerHandler(service))
                ;
    }
}