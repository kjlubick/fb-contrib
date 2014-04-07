import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;


public class HE_Sample {
	

	public static void main(String[] args) {
		CachedThreadPoolProblem p = new CachedThreadPoolProblem();
		p.test();
		
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

class CachedThreadPoolProblem {
	//tag, sort of a false positive because JVM will exit after 60 seconds
	private ExecutorService executor;

	public CachedThreadPoolProblem() {
		this.executor = Executors.newCachedThreadPool();
	}
	
	public void test() {
		executor.execute(new SampleExecutable());
		executor.execute(new SampleExecutable());
	}
	
}

class SingleThreadExecutorThreadFactoryGood {
	//tag, but kind of a false positive - Daemon threads will not prevent hang
	private ExecutorService executor;

	public SingleThreadExecutorThreadFactoryGood() {
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

