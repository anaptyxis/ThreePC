package ut.distcomp.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
	private int numNodes;
	private ArrayList<Node> nodes;
	private int coordinatorID;
	private int viewNumber;
	
	private ArrayList<String> script;
	
	public static void main(String[] args) {
	
		ThreePhaseCommit tpc = new ThreePhaseCommit();
		
		if (args[0] != null)
			tpc.readScript(args[0]);
		else {
			System.err.println("ABORT: Please specify a script as the CLI argument.");
			System.exit(1);
		}
		
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
	
	public ThreePhaseCommit() {
		numNodes = 0;
		nodes = new ArrayList<Node>();
		script = new ArrayList<String>();
		viewNumber = 0;
		coordinatorID = 0;
		
	}
	
	public void readScript(String scriptFName) {
		try {
			BufferedReader scriptReader = new BufferedReader(new FileReader(new File(scriptFName)));
			
			// add read
			
				
			scriptReader.close();
		} catch (FileNotFoundException e) {
			System.err.println("Error reading script file "+scriptFName+".");
			System.exit(2);
		} catch (IOException e) {
			System.err.println("Error closing script file "+scriptFName+".");
			System.exit(3);
		}
		
	}
	
	/* recommended interface */
	public void createProcesses(int n) {
		numNodes = n;
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
