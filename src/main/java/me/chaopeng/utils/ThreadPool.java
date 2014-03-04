package me.chaopeng.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 线程池
 * @author chao
 */
public class ThreadPool {
	private final static ExecutorService pool = Executors.newCachedThreadPool();
	private final static ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);

	public static ExecutorService getPool() {
		return pool;
	}

	public static ScheduledExecutorService getTimer() {
		return timer;
	}
}
