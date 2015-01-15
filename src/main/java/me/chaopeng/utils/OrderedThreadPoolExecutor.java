package me.chaopeng.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;
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
public final class OrderedThreadPoolExecutor extends ForkJoinPool {

	private static Logger logger = LoggerFactory.getLogger(OrderedThreadPoolExecutor.class);

	private final static int DEFAULT_NUM_EXECUTOR = 1024;
	private final static int DEFAULT_BATCH_LIMIT = 5;

	/**
	 * executors
	 */
	private final ChildExecutor[] childExecutors;

	/**
	 * the limit of batch process tasks
	 */
	private final int batchLimit;

	/**
	 * 类似于 Executors.newFiexedThreadPool() 永远保持一定的线程池大小
	 *
	 * @param corePoolSize 线程池大小
	 * @return OrderedThreadPoolExecutor
	 */
	public static OrderedThreadPoolExecutor newFixesOrderedThreadPool(int corePoolSize) {
		return newFixesOrderedThreadPool(corePoolSize, DEFAULT_NUM_EXECUTOR, DEFAULT_BATCH_LIMIT);
	}

	/**
	 * 类似于 Executors.newFiexedThreadPool() 永远保持一定的线程池大小
	 *
	 * @param corePoolSize  线程池大小
	 * @param numOfExecutor executor的数量
	 * @param batchLimit    批量执行任务，保证executor的公平性
	 * @return OrderedThreadPoolExecutor
	 */
	public static OrderedThreadPoolExecutor newFixesOrderedThreadPool(int corePoolSize, int numOfExecutor, int batchLimit) {
		logger.info("!!! init " + corePoolSize + " core OrderedThreadPoolExecutor");
		return new OrderedThreadPoolExecutor(
				corePoolSize, numOfExecutor, batchLimit
		);
	}

	/**
	 * Creates a new instance.
	 *
	 * @param numOfExecutor the number of executor
	 * @param batchLimit    the limit of batch process tasks
	 */
	private OrderedThreadPoolExecutor(int corePoolSize, int numOfExecutor, int batchLimit) {

		super(corePoolSize, ForkJoinPool.defaultForkJoinWorkerThreadFactory, new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				logger.error(e.getMessage(), e);
			}
		}, true);

		this.childExecutors = new ChildExecutor[numOfExecutor];
		for (int i = 0; i < this.childExecutors.length; ++i) {
			this.childExecutors[i] = new ChildExecutor(i);
		}

		this.batchLimit = batchLimit;
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
		return childExecutors[(int) (key % childExecutors.length)];
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

		@Override
		public String toString() {
			return "OrderedRunable{" +
					"key=" + key +
					'}';
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

			// add to schedule if executor is not waiting to process
			if (isRunning.compareAndSet(false, true)) {
				doUnorderedExecute(this);
			}
		}

		public void run() {
			for (int i = 0; i < batchLimit; ++i) {
				final Runnable task = tasks.poll();
				// if the task is null we should exit the loop
				if (task == null) {
					break;
				}

//				logger.debug("execute cmd " + task + " in ChildExecutor-" + executorId);
				boolean ran = false;
				try {
					task.run();
					ran = true;
				} catch (RuntimeException e) {
					if (!ran) {
						logger.error("execute cmd " + task + " error:" + e.getMessage(), e);
					}
//					throw e;
				}
			}

//			logger.debug("ChildExecutor-" + executorId + " exit");
			
			// re-add to thread pool if tasks is not empty
			isRunning.set(false);
			
			if (!tasks.isEmpty()) {
				doUnorderedExecute(this);
				isRunning.set(true);
			}

		}
	}
}
