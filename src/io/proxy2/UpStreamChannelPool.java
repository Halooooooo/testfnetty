package io.proxy2;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

import static io.proxy2.Constants.DEFAULT_UPSTREAM_LINK;

/**
 * @author: Halo
 * @date: 2020/3/26 16:40
 * @Description:
 */
public class UpStreamChannelPool<E extends Channel> extends ConcurrentLinkedDeque<E>{

    private AtomicInteger acquiredChannelCount = new AtomicInteger(0);

    static final float DEFAULT_LOAD_FACTOR = 0.25f;
    static final int DEFAULT_EXTEND_FACTOR = 2;
    static final int DEFAULT_HALF_SIZE = DEFAULT_UPSTREAM_LINK/2;
    public E getChannel(){
        acquiredChannelCount.decrementAndGet();
        return removeFirst();
    }

    public E getActiveChannel(){
        acquiredChannelCount.incrementAndGet();
        E e = removeFirst();
        checkPoolSize();
        while(!e.isActive()){
            if(!e.isOpen()) forceClose(e);
            addLast(e);
            e = removeFirst();
        }
        return e;
    }

    private float loadFactor(){
        return DEFAULT_LOAD_FACTOR;
    }
    private int getAcquiredNum(){
        return acquiredChannelCount.get();
    }

    public void checkPoolSize(){
        if((size()+getAcquiredNum())<DEFAULT_HALF_SIZE||(size()+getAcquiredNum())*loadFactor()>=size()){
            poolReSize();
        }
    }

    //todo
    private void poolReSize(){
        System.out.println("poolReSize");
        int n = Math.max(size()*2+1,getAcquiredNum());
        Constants.executor.execute(new Init(n,this));
    }
    /**
     * @author: Halo
     * @date: 2020/3/26
     * @Description: 正常关闭
     * @param:
     * @return
     */
    public void release(E e){
        acquiredChannelCount.incrementAndGet();
        addLast(e);
    }

    /**
     * @author: Halo
     * @date: 2020/3/26
     * @Description: 异常关闭
     * @param:
     * @return
     */
    public void forceClose(E e){
        checkPoolSize();
        if (e.isActive()) {
            acquiredChannelCount.decrementAndGet();
            e.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
