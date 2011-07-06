/**
 * 
 */
package org.morganm.logores;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.bukkit.block.BlockState;

/** Object which manages the queue for the logger.
 * 
 * @author morganm
 *
 */
public class LogQueue {
	private static final int QUEUE_CAPACITY = 1000;
	
	private BlockingQueue<BlockState> logQueue;
	
	public LogQueue() {
		logQueue = new ArrayBlockingQueue<BlockState>(QUEUE_CAPACITY);
	}
	
	public void push(BlockState bs) {
		logQueue.add(bs);
	}
	
	/** This WILL block the caller.  Do not call unless you are in a thread it is OK to block.
	 *
	 * If you don't want to block, use isEmpty() check first and be sure you are only accessing
	 * the queue from a single thread (so another thread doesn't steal your data and leave you
	 * blocked waiting for more).
	 * 
	 * @return
	 * @throws InterruptedException
	 */
	public BlockState pop() throws InterruptedException {
		return logQueue.take();
	}
	
	public boolean isEmpty() {
		return logQueue.isEmpty();
	}
}
