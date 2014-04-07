import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HE_Sample {
	

	public static void main(String[] args) {
		SingleThreadExecutorProblem p = new SingleThreadExecutorProblem();
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
