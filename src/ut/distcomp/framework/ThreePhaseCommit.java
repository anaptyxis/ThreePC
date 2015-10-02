package ut.distcomp.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * ThreePhaseCommit
 * @author Bradley Beth
 *
 */
public class ThreePhaseCommit {	
	/// main ///
	int numNodes;
	ArrayList<Node> nodes;
	
	public static void main(String[] args) {
	
		ThreePhaseCommit tpc = new ThreePhaseCommit();
		
		tpc.createProcesses(5);
		
		(tpc.nodes.get(0)).sendMsg(1,"zero to one");
		(tpc.nodes.get(3)).sendMsg(1,"three to one");
		(tpc.nodes.get(4)).sendMsg(1,"four to one");
			
		
		
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println((tpc.nodes.get(1)).retrieveMsgs());
		
		
		for (Node n : tpc.nodes)
			n.shutdown();
		
	}
	
	/* recommended interface */
	public void createProcesses(int n) {
		numNodes = n;
		nodes = new ArrayList<Node>();
		for (int i = 0; i < numNodes; i++)
			nodes.add(new Node("config"+i+".txt", 
							   "DTLog"+i+".txt"));
	}

	public void kill(int procID) {
	
	
	}

	public void killAll() {
		
		
	}

	public void killLeader() {
		
		
	}

	public void revive(int procID) {
		
		
	}

	public void reviveLast() {
		
		
	}

	public void reviveAll() {
		
		
	}

	public void partialMessages(int procID, int numMsgs) {
		
		
	}
	
	public void resumeMessages(int procID) {
		
		
	}
	
	public void allClear() {
		
		
	}

	public void rejectNextChange(int procID) {
		
		
	}

	
}
