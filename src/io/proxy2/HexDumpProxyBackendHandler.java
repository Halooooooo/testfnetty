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
package io.proxy2;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

import java.util.Objects;

public class HexDumpProxyBackendHandler extends ChannelInboundHandlerAdapter {

    private RemoteMessage rmsg;

    private boolean isForceClose = false;
    public HexDumpProxyBackendHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {

        rmsg.getInboundChannel().writeAndFlush(msg).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    ctx.channel().read();
                } else {
                    future.channel().close();
                }
            }
        });
        if(msg instanceof HttpResponse){
            try {
                HttpResponse request = (HttpResponse) msg;
                if(Objects.nonNull(request.headers().get(HttpHeaderNames.CONNECTION))
                        &&HttpHeaderValues.CLOSE.equals(AsciiString.cached(request.headers().get(HttpHeaderNames.CONNECTION)))){
                    UpStreamChannelPool<Channel> otherSCChannel = Constants.pool.get("otherSCChannel");
                    otherSCChannel.forceClose(ctx.channel());
                    isForceClose = true;
                } else {
                    UpStreamChannelPool<Channel> otherSCChannel = Constants.pool.get("otherSCChannel");
                    otherSCChannel.release(ctx.channel());
                }
            } finally {
//                ReferenceCountUtil.release(msg);
            }
        }

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        HelloServerHandler.closeOnFlush(rmsg.getInboundChannel());
        if(!isForceClose) {
            UpStreamChannelPool<Channel> otherSCChannel = Constants.pool.get("otherSCChannel");
            otherSCChannel.forceClose(ctx.channel());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if(!isForceClose) {
            UpStreamChannelPool<Channel> otherSCChannel = Constants.pool.get("otherSCChannel");
            otherSCChannel.forceClose(ctx.channel());
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);
        if(evt instanceof RemoteMessage){
            rmsg = (RemoteMessage) evt;
        }

    }
}
