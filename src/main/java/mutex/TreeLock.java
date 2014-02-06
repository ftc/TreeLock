package mutex;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TreeLock {
	private final List<LockTree> bottomRow;
	
    final private ThreadLocal<Integer> THREAD_ID = new ThreadLocal<Integer>(){
        final private AtomicInteger id = new AtomicInteger(0);
        protected Integer initialValue(){
            return id.getAndIncrement();
        }
    };
	
	private abstract class LockTree{
		public void lock(int side){}
		public void unlock(int side){}
	}
	private class Node extends LockTree{
		final private AtomicBoolean[] flag = new AtomicBoolean[2];
		
		//peterson lock for each node
		private volatile int victim;
		private final LockTree parent;
		
		private final int myside; //used to determine if 0 or 1 in peterson lock
		
		public Node(LockTree parent, int divider) {
			this.parent = parent;
			this.myside = divider;
			for(int i=0; i<flag.length; ++i)
				flag[i] = new AtomicBoolean();
		}
		
		//Really the interface should be better here but if you aren't a node just call with 0
		@Override
		public void lock(int side) {
			int i = side; //side used instead of thread number
			int j = 1-i;
			flag[i].set(true); //I am interested
			victim = i; //you go first
			while(flag[j].get() && victim == i) {}; //wait
			parent.lock(myside); //lock next level up
		}
		//Again if not a node just call with 0
		@Override
		public void unlock(int side) {
			parent.unlock(myside); //Unlock parent before unlocking self (does this order actually matter?)
			int i = side; //use side from child to know what to unlock
			flag[i].set(false);
		}
	}
	private class Top extends LockTree{
		public void lock(int side) {
			//Does nothing
		}
		public void unlock(int side) {
			//Also does nothing
		}
	}
	public TreeLock(int n) {
		this.bottomRow = generateTree(n);
		System.out.println("TreeLock created");
	}
	public void lock(){
		int tid = THREAD_ID.get();
		bottomRow.get(tid).lock(0);
	}
	public void unlock() {
		int tid = THREAD_ID.get();
		bottomRow.get(tid).unlock(0);
	}
	
	private List<LockTree> generateTree(int n) {
		if( (n&(n-1)) != 0) //check if n is a power of 2
			throw new IllegalArgumentException("N must be a power of 2");
		if(n <= 0)
			throw new IllegalArgumentException("At least one thread is required");
		
		int i = 1;
		LockTree root = new Top();
		List<LockTree> bottomRow = new ArrayList<LockTree>();
		bottomRow.add(new Node(root, 0)); //top node doesn't have side
		while(i<n) {
			i = i*2;
			List<LockTree> nextRow = new ArrayList<LockTree>();
			for(int j = 0; j<i; ++j){
				nextRow.add(new Node(bottomRow.get(j/2), j%2)); //TODO: figure out how to generate divider
			}
			bottomRow = nextRow;
		}
		return bottomRow;
		
	}
}
