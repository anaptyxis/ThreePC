package ut.distcomp.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * ThreePhaseCommit Controller
 * @author Bradley Beth
 *
 */
public class ThreePhaseCommit {	

	private int numProcs;
	private ArrayList<Process> procList;
	private int coordinatorID;
	private int viewNumber;
	private ArrayList<String> script;
	
	public static void main(String[] args) {
	
		ThreePhaseCommit tpc = new ThreePhaseCommit();
		
		if (args != null)
			tpc.readScript(args[0]);
		else {
			System.err.println("ABORT: Please specify a script as the CLI argument.");
			System.exit(1);
		}
		
		tpc.processScript();
		
		tpc.createProcesses(5);	
		
		//Give a little time for testing
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		tpc.kill(2);
		tpc.killAll();
		
	}
	
	public ThreePhaseCommit() {
		numProcs = 0;
		procList = new ArrayList<Process>();		
		script = new ArrayList<String>();
		viewNumber = 0;
		coordinatorID = 0;
		
	}
	
	public void readScript(String scriptFName) {
		try {
			BufferedReader scriptReader = new BufferedReader(new FileReader(new File(scriptFName)));
			String line = null;
			do {
				line = scriptReader.readLine();  
				if (line != null)
					script.add(line);
			} while (line != null); 						
			scriptReader.close();
		} catch (FileNotFoundException e) {
			System.err.println("Error reading script file "+scriptFName+".");
			System.exit(2);
		} catch (IOException e) {
			System.err.println("Error closing script file "+scriptFName+".");
			System.exit(3);
		}
		
	}
	
	/*
	 * Parse and process each string in the script.
	 */
	public void processScript() {

		System.out.println("SCRIPT\n======");
		for (String s: script)
			System.out.println("   "+s); //TODO: for now just print
		System.out.println("======");
	}
	
	/* recommended interface */
	public void createProcesses(int p) {
		numProcs = p;
		String classpath = System.getProperty("java.class.path");
		for (int i = 0; i < numProcs; i++) {
			System.out.println("3PC Controller: Starting process p"+i);
			ProcessBuilder pb = new ProcessBuilder("java","-cp",classpath,"ut.distcomp.framework.Node","config"+i+".txt","DTLog"+i+".txt");
			pb.inheritIO();
			try {
				procList.add(pb.start());
			} catch (IOException e) {
				System.err.println("Trouble starting process p"+i);
				e.printStackTrace();
			}
		}			
	}

	public void kill(int procID) {
	
		if (procList.get(procID) != null) {
			System.out.println("3PC Controller: Killing process p"+procID);
			procList.get(procID).destroy();
			procList.set(procID, null);
		}
		
	}

	public void killAll() {
		
		System.out.print("3PC Controller: Killing ALL running processes: ");
		for (int i = 0; i < procList.size(); i++) {
		  if (procList.get(i) != null) {
			  System.out.print("p"+i+"\t");
			  procList.get(i).destroy();
			  procList.set(i, null);
		  }
		}
		
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
