package io.proxy2;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.util.Objects;

import static io.netty.channel.ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK;
import static io.netty.channel.ChannelOption.WRITE_BUFFER_WATER_MARK;
import static io.proxy2.Constants.hosts;


/**
 * @author: Halo
 * @date: 2020/3/26 16:41
 * @Description:
 */
public class Init implements Runnable{

    int n = 1;
    UpStreamChannelPool pool;
    public static final int DEFAULT_HIGH_WATER_MARK = 10*64*1024;
    public static final int DEFAULT_LOW_WATER_MARK = 32*1024;
    public static final WriteBufferWaterMark DEFAULT =
            new WriteBufferWaterMark(DEFAULT_LOW_WATER_MARK, DEFAULT_HIGH_WATER_MARK);
    public Init(int n,UpStreamChannelPool pool){
        this.n = Math.max(n, io.proxy2.Constants.DEFAULT_UPSTREAM_LINK);
        this.pool = pool;
    }

    @Override
    public void run() {
        for (int i = 0; i < n; i++) {
            // TODO
            while (Objects.isNull(HelloWebServer.evg)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Bootstrap b = new Bootstrap();
            b.group(HelloWebServer.evg)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new LoggingHandler(LogLevel.INFO),
                                    new HttpResponseDecoder(),
                                    new HexDumpProxyBackendHandler()
                            );

                        }
                    })
                    .option(ChannelOption.AUTO_READ, false);
            if (Epoll.isAvailable()) {
                b.option(EpollChannelOption.SO_REUSEPORT, true);
                b.channel(EpollSocketChannel.class);
            }else if (KQueue.isAvailable()) {
                b.channel(KQueueSocketChannel.class);
            }else{
                b.channel(NioSocketChannel.class);
            }
            b.option(ChannelOption.SO_REUSEADDR, true);
//            b.option("sendBufferSize", 1048576);
//            b.option("receiveBufferSize", 1048576);
            b.option(WRITE_BUFFER_WATER_MARK, DEFAULT);
            b.option(ChannelOption.TCP_NODELAY, true);
            ChannelFuture connect = b.connect(new InetSocketAddress(hosts, 18891));
            connect.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    pool.add(channelFuture.channel());
                }
            });
        }
    }
}
