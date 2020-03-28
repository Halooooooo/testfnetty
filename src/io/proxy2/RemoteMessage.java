package io.proxy2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

/**
 * @author: Halo
 * @date: 2020/3/25 11:32
 * @Description:
 */
public class RemoteMessage {
    private String hosts;
    private int port;
    private ByteBuf body;
    private final Channel inboundChannel ;

    public RemoteMessage(Channel inboundChannel) {
        this.inboundChannel = inboundChannel;
    }

    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ByteBuf getBody() {
        return body;
    }

    public void setBody(ByteBuf body) {
        this.body = body;
    }

    public Channel getInboundChannel() {
        return inboundChannel;
    }
}
