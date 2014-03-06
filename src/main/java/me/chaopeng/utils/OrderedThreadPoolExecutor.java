package me.chaopeng.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OrderedThreadPoolExecutor是参考netty3的OrderedMemoryAwareThreadPoolExecutor实现的有序线程池，<b>没有MemoryAware的实现</b>
 * <p/>
 * <ul>
 * <li>线程池会确保相同key的Runable按照execute的顺序执行</li>
 * <li>线程池不会会确保相同的key的Runable按照始终在同一个线程内执行</li>
 * <li>线程池不会会确保不同的key的Runable按照execute的顺序执行</li>
 * <li>如果某个相同key的Runable很多，会导致线程池内执行该key的线程长时间被占用</li>
 * <li>同时间过多的task可能导致内存泄露！！！此处可能会被利用做hash攻击！！！</li>
 * </ul>
 *
 * @author chao
 * @see OrderedRunable
 */
public final class OrderedThreadPoolExecutor extends ThreadPoolExecutor {

	private static Logger logger = LoggerFactory.getLogger(OrderedThreadPoolExecutor.class);

	private static final int NUM_EXECUTORS = 2;
	/**
	 * executors
	 */
	private final ChildExecutor[] childExecutors = new ChildExecutor[NUM_EXECUTORS];

	/**
	 * 类似于 Executors.newFiexedThreadPool() 永远保持一定的线程池大小
	 *
	 * @param corePoolSize 线程池大小
	 * @return OrderedThreadPoolExecutor
	 */
	public static OrderedThreadPoolExecutor newFixesOrderedThreadPool(int corePoolSize) {
		return new OrderedThreadPoolExecutor(
				corePoolSize, corePoolSize, 0L, TimeUnit.SECONDS, Executors.defaultThreadFactory()
		);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param minPoolSize   the minimum number of active threads
	 * @param maxPoolSize   the maximum number of active threads
	 * @param keepAliveTime the amount of time for an inactive thread to shut itself down
	 * @param unit          the {@link TimeUnit} of {@code keepAliveTime}
	 * @param threadFactory the {@link ThreadFactory} of this pool
	 */
	private OrderedThreadPoolExecutor(
			int minPoolSize, int maxPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {

		super(minPoolSize, maxPoolSize, keepAliveTime, unit,
				new LinkedBlockingQueue<Runnable>(), threadFactory);

		for (int i = 0; i < childExecutors.length; ++i) {
			childExecutors[i] = new ChildExecutor(i);
		}
	}

	@Override
	public void execute(Runnable task) {
		if (task instanceof OrderedRunable) {
			doExecute((OrderedRunable) task);
		} else {
			throw new RejectedExecutionException("task must be enclosed an OrderedRunable.");
		}
	}

	private void doExecute(OrderedRunable task) {
		getChildExecutor(task.key).execute(task);
	}

	private void doUnorderedExecute(ChildExecutor runnable) {
		super.execute(runnable);
	}

	private ChildExecutor getChildExecutor(Long key) {
		return childExecutors[(int) (key % NUM_EXECUTORS)];
	}

	/**
	 * Runable Task for OrderedThreadPoolExecutor
	 *
	 * @see OrderedThreadPoolExecutor
	 */
	public abstract static class OrderedRunable implements Runnable {
		protected Long key;

		public OrderedRunable(Long key) {
			this.key = key;
		}
	}

	/**
	 * 实际执行者
	 */
	protected final class ChildExecutor implements Executor, Runnable {
		private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
		private final AtomicBoolean isRunning = new AtomicBoolean();
		private final int executorId;

		public ChildExecutor(int executorId) {
			this.executorId = executorId;
		}

		public void execute(Runnable command) {
			// TODO: What todo if the add return false ?
			tasks.add(command);
//			logger.debug("add cmd " + command + " to ChildExecutor-" + executorId);

			if (!isRunning.get()) {
				doUnorderedExecute(this);
			}
		}

		public void run() {
			boolean acquired;

			// check if its already running by using CAS. If so just return here. So in the worst case the thread
			// is executed and do nothing
			if (isRunning.compareAndSet(false, true)) {
				acquired = true;
				try {
					Thread thread = Thread.currentThread();
					for (; ; ) {
						final Runnable task = tasks.poll();
						// if the task is null we should exit the loop
						if (task == null) {
//							logger.debug("ChildExecutor-" + executorId + " exit");
							break;
						}

//						logger.debug("execute cmd " + task + " in ChildExecutor-" + executorId);
						boolean ran = false;
						beforeExecute(thread, task);
						try {
							task.run();
							ran = true;
							afterExecute(task, null);
						} catch (RuntimeException e) {
							if (!ran) {
								afterExecute(task, e);
							}
							throw e;
						}
					}
				} finally {
					// set it back to not running
					isRunning.set(false);
				}

				if (acquired && !isRunning.get() && tasks.peek() != null) {
					doUnorderedExecute(this);
				}
			}
		}
	}

}
