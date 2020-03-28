package io.proxy2;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.FastThreadLocal;
import io.test.Message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HelloServerHandler extends ChannelInboundHandlerAdapter {

    private static final FastThreadLocal<DateFormat> FORMAT = new FastThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
        }
    };

    private static Message newMsg() {
        return new Message("Hello, World!");
    }

    private static byte[] serializeMsg(Message obj) {
        return ("{\"teset\":\""+obj.getMessage()+"\"}").getBytes();
    }

    private static int jsonLen() {
        return serializeMsg(newMsg()).length;
    }

    private static final byte[] STATIC_PLAINTEXT = "Hello, World!".getBytes(CharsetUtil.UTF_8);
    private static final int STATIC_PLAINTEXT_LEN = STATIC_PLAINTEXT.length;

    private static final CharSequence PLAINTEXT_CLHEADER_VALUE = AsciiString.cached(String.valueOf(STATIC_PLAINTEXT_LEN));
    private static final int JSON_LEN = jsonLen();
    private static final CharSequence JSON_CLHEADER_VALUE = AsciiString.cached(String.valueOf(JSON_LEN));
    private static final CharSequence SERVER_NAME = AsciiString.cached("Netty");

    private volatile CharSequence date = new AsciiString(FORMAT.get().format(new Date()));
    private Channel upStreamChannel;

    HelloServerHandler(ScheduledExecutorService service) {
        service.scheduleWithFixedDelay(new Runnable() {
            private final DateFormat format = FORMAT.get();

            @Override
            public void run() {
                date = new AsciiString(format.format(new Date()));
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            try {
                HttpRequest request = (HttpRequest) msg;
                RemoteMessage rmsg = process(ctx, request);
                UpStreamChannelPool<Channel> otherSCChannel = Constants.pool.get("otherSCChannel");
                upStreamChannel = otherSCChannel.getActiveChannel();
                upStreamChannel.pipeline().writeAndFlush(rmsg.getBody());
                upStreamChannel.pipeline().read();
                upStreamChannel.pipeline().fireUserEventTriggered(rmsg);

            } finally {
//                ReferenceCountUtil.release(msg);
            }
        }else if(msg instanceof HttpContent){

        }
    }

    private RemoteMessage process(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
//        for (int i = 0; i < 100000; i++) {
//
//        }
        RemoteMessage remoteMessage = new RemoteMessage(ctx.channel());
        remoteMessage.setHosts("10.124.151.16");
        remoteMessage.setPort(18891);
        String a = request.toString();
        remoteMessage.setBody(Unpooled.wrappedBuffer((a.substring(a.indexOf("\n")+1)+"\r\n"+"\r\n").getBytes()));
        return remoteMessage;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (upStreamChannel != null) {
            closeOnFlush(upStreamChannel);
        }
    }

    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
