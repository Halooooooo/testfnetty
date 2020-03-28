package io.proxy2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author: Halo
 * @date: 2020/3/25 17:10
 * @Description:
 */
public class Constants {
    public static ConcurrentHashMap<String, UpStreamChannelPool> pool = new ConcurrentHashMap<>();
    public static int DEFAULT_UPSTREAM_LINK = 1024;
    public static Executor executor = Executors.newSingleThreadExecutor();
    public static String hosts = "127.0.0.1";
}
