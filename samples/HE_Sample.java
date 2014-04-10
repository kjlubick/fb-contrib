import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class HE_Sample {
	

	public static void main(String[] args) {
		LocalExecutorOkay p = new LocalExecutorOkay();
		p.task();
		
		System.out.println("Should end");
	}
}

class SampleExecutable implements Runnable {
	@Override
	public void run() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("Hello");
	}

}

class SingleThreadExecutorProblem {
	//tag
	private ExecutorService executor;

	public SingleThreadExecutorProblem() {
		this.executor = Executors.newSingleThreadExecutor();
	}
	
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
}

class SingleThreadExecutorGood {
	//no tag
	private ExecutorService executor;

	public SingleThreadExecutorGood() {
		this.executor = Executors.newSingleThreadExecutor();
	}
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	public void shutDown() {
		executor.shutdown();
	}
}
class SingleThreadExecutorGood1 {
	//no tag
	private ExecutorService executor;

	public SingleThreadExecutorGood1() {
		this.executor = Executors.newSingleThreadExecutor();
	}
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	public void shutDown() {
		executor.shutdownNow();
	}
}
class SingleThreadExecutorGood2 {
	//no tag
	private ExecutorService executor;

	public SingleThreadExecutorGood2() {
		this.executor = Executors.newSingleThreadExecutor();
	}
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	public void shutDown() {
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		executor.shutdown();
	}
}

class SingleThreadExecutorTryProblem {
	//this won't get tagged
	private ExecutorService executor;

	public SingleThreadExecutorTryProblem() {
		this.executor = Executors.newSingleThreadExecutor();
	}
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	public void shutDown() {
		try {
			executor.awaitTermination(30, TimeUnit.SECONDS);
			executor.shutdown();		//this doesn't count as shutdown, so it should be tagged.
										//probably with a different bug
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
}

class FixedThreadPoolProblem {
	//tag
	private ExecutorService executor;

	public FixedThreadPoolProblem() {
		this.executor = Executors.newFixedThreadPool(3);
	}
	
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
}

class CachedThreadPoolMehProblem {
	//tag - this is bad practice, even though JVM will exit after 60 seconds
	private ExecutorService executor;

	public CachedThreadPoolMehProblem() {
		this.executor = Executors.newCachedThreadPool();
	}
	
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
}

class SingleThreadExecutorThreadFactoryMehProblem {
	//tag - this is bad practice, even though the threads will terminate
	private ExecutorService executor;

	public SingleThreadExecutorThreadFactoryMehProblem() {
		this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable arg0) {
				Thread t = new Thread(arg0);
				t.setDaemon(true);
				return t;
			}
		});
	}
	
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
}

class ScheduledThreadPoolProblem {
	//tag
	private ExecutorService executor;

	public ScheduledThreadPoolProblem() {
		this.executor = Executors.newScheduledThreadPool(1);
	}
	
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
}

class ReplacementExecutorProblem {
	private ExecutorService executor;

	public ReplacementExecutorProblem() {
		this.executor = Executors.newScheduledThreadPool(1);
		executor.execute(new SampleExecutable());
	}
	
	public void reset() {
		//tag (the old executor won't get picked up for garbage collection)
		this.executor = Executors.newScheduledThreadPool(1);
		executor.execute(new SampleExecutable());
		try {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
	public void shutDown() {
		executor.shutdownNow();
	}
	
}

class ReplacementExecutorGood {
	private ExecutorService executor;

	public ReplacementExecutorGood() {
		this.executor = Executors.newScheduledThreadPool(1);
		executor.execute(new SampleExecutable());
	}
	
	public void reset() {
		//no tag
		this.executor.shutdown();
		
		this.executor = Executors.newScheduledThreadPool(1);
		executor.execute(new SampleExecutable());
		try {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		executor.shutdown();
	}
	
}

class ReplacementExecutorBad2 {
	private ExecutorService executor;

	public ReplacementExecutorBad2() {
		this.executor = Executors.newScheduledThreadPool(1);
		executor.execute(new SampleExecutable());
	}
	
	public void reset() {
		//tag, because shutdown in other method isn't forced to be called
		this.executor = Executors.newScheduledThreadPool(1);

	}
	
	public void shutDown() {
		this.executor.shutdown();
	}
	
	public void task() {
		executor.execute(new SampleExecutable());
		try {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}

class ReplacementExecutorGood2 {
	private ExecutorService executor;

	public ReplacementExecutorGood2() {
		this.executor = Executors.newScheduledThreadPool(1);
		executor.execute(new SampleExecutable());
	}
	
	public void reset() {
		if (executor == null) {
			//no tag, this indicates some thought that another threadpool won't get left behind
			this.executor = Executors.newScheduledThreadPool(1);
		}
	}
	
	public void shutDown() {
		this.executor.shutdown();
		this.executor = null;
	}
	
	public void task() {
		executor.execute(new SampleExecutable());
		try {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}


class LocalExecutorProblem {
	
	public void task() {
		//tag - the GC doesn't kill the internal threadpool
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
}

class LocalExecutorOkay {
	
	public void task() {
		//no tag, the local pool will be shutdown
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
		executor.shutdown();
	}
	
}

class LocalExecutorOkay2 {
	
	public ExecutorService makeExecutor() {
		//no tag, it is the responsibility of the super to shut this down
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());

		return executor;
	}
	
}

class LocalExecutorProblem2 {
	
	public void task() {
		LocalExecutorOkay2 leo2 = new LocalExecutorOkay2();
		
		//tag, won't get shutdown
		ExecutorService executor = leo2.makeExecutor();
		executor.execute(new SampleExecutable());

	}
	
}


