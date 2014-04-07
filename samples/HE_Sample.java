import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class HE_Sample {
	

	public static void main(String[] args) {
		ReplacementExecutorProblem p = new ReplacementExecutorProblem();
		p.reset();
		
		System.out.println("Should end");
	}
}

class SampleExecutable implements Runnable {
	@Override
	public void run() {
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
	//tag, sort of a false positive because JVM will exit after 60 seconds
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
	//tag, but kind of a false positive - Daemon threads will not prevent hang
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


