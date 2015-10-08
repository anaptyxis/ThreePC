package ut.distcomp.framework;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;

/**
 * ThreePhaseCommit Controller
 * @author Bradley Beth
 *
 */
public class ThreePhaseCommit {	

	private int numProcs;
	private ArrayList<Process> procList;
	private ArrayList<BufferedWriter> procOutList;
	private int coordinatorID;
	private int lastKilledID;
	private ArrayList<String> script;
	
	
	public static void main(String[] args) {
	
		ThreePhaseCommit tpc = new ThreePhaseCommit();
		
		if (args != null)
			tpc.readScript(args[0]);	//read in controller script
		else {
			System.err.println("ABORT: Please specify a script as the CLI argument.");
			System.exit(1);
		}
		
		tpc.processScript();
				
	}
	
	public ThreePhaseCommit() {
		numProcs = 0;
		procList = new ArrayList<Process>();	
		procOutList = new ArrayList<BufferedWriter>();	
		script = new ArrayList<String>();
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
		for (String s: script) {
			System.out.println(">> "+s); 
			String[] strArr = (s.trim()).split(" ");
			if (strArr[0].equals("createProcesses")) {
				int arg = Integer.parseInt(strArr[1]);
				createProcesses(arg);			
			}
			if (strArr[0].equals("kill")) {
				int arg = Integer.parseInt(strArr[1]);
				kill(arg);			
			}
			if (strArr[0].equals("killAll")) {
				killAll();				
			}
			if (strArr[0].equals("killLeader")) {
				killLeader();				
			}
			if (strArr[0].equals("revive")) {
				int arg = Integer.parseInt(strArr[1]);
				revive(arg);							
			}
			if (strArr[0].equals("reviveLast")) {
				reviveLast();				
			}
			if (strArr[0].equals("reviveAll")) {
				reviveAll();				
			}
			if (strArr[0].equals("partialMessage")) {
				int arg1 = Integer.parseInt(strArr[1]);
				int arg2 = Integer.parseInt(strArr[2]);
				partialMessage(arg1, arg2);											
			}
			if (strArr[0].equals("resumeMessages")) {
				int arg = Integer.parseInt(strArr[1]);
				resumeMessages(arg);										
			}
			if (strArr[0].equals("allClear")) {
				allClear();
			}
			if (strArr[0].equals("rejectNextChange")) {
				int arg = Integer.parseInt(strArr[1]);
				rejectNextChange(arg);												
			}
			if (strArr[0].equals("change")) {
				int arg = Integer.parseInt(strArr[1]);
																
			}
			if (strArr[0].equals("add")) {
				int arg = Integer.parseInt(strArr[1]);
																
			}
			if (strArr[0].equals("wait")) {
				int arg = Integer.parseInt(strArr[1]);
				try {
					Thread.sleep(arg*1000);		//wait arg seconds
				} catch (InterruptedException e) {
					System.err.println("Error putting thread to sleep.");
					e.printStackTrace();
				}											
			}		
		}
		System.out.println("======");
	}
	
	/* recommended interface */
	public void createProcesses(int p) {
		numProcs = p;
		String classpath = System.getProperty("java.class.path");
		for (int i = 0; i < numProcs; i++) {
			System.out.println("3PC Controller: Starting process p"+i);
			ProcessBuilder pb = new ProcessBuilder("java","-cp",classpath,"ut.distcomp.framework.Node","config"+i+".txt","DTLog"+i+".txt", "new");
			pb.redirectOutput(Redirect.INHERIT);
			pb.redirectError(Redirect.INHERIT);		
			try {
				Process proc = pb.start();
				procOutList.add(new BufferedWriter(new OutputStreamWriter(proc.getOutputStream())));
				procList.add(proc);
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
		System.out.println();
	}

	public void killLeader() {
		
		System.out.print("3PC Controller: Killing LEADER process------>");
		this.kill(coordinatorID);
		
	}

	public void revive(int procID) {
		
		String classpath = System.getProperty("java.class.path");
		System.out.println("3PC Controller: Reviving process p"+procID);
		ProcessBuilder pb = new ProcessBuilder("java","-cp",classpath,"ut.distcomp.framework.Node","config"+procID+".txt","DTLog"+procID+".txt", "revive");
		//TODO: pb.redirectInput();
		pb.redirectOutput(Redirect.INHERIT);
		pb.redirectError(Redirect.INHERIT);
		try {
			procList.set(procID,pb.start());
		} catch (IOException e) {
			System.err.println("Trouble reviving process p"+procID);
			e.printStackTrace();
		}	
	}

	public void reviveLast() {

		System.out.print("3PC Controller: Reviving LAST killed process p------>");
		this.revive(lastKilledID); 
		
	}

	public void reviveAll() {

		System.out.println("3PC Controller: Reviving ALL dead processes: ");
		for (int i = 0; i < procList.size(); i++) {
		  if (procList.get(i) == null) {
			  this.revive(i);
		  }
		}
		System.out.println();
	}

	public void partialMessage(int procID, int numMsgs) {
		
		try {
			System.out.println("3PC: Sending partialMessage "+numMsgs+" to p"+procID);
			procOutList.get(procID).write("partialMessage "+numMsgs);
			procOutList.get(procID).flush();
		} catch (IOException e) {
			System.err.println("3PC: Error writing 'partialMessage' "+numMsgs+" to process p"+procID);
			e.printStackTrace();
		}
		
	}
	
	public void resumeMessages(int procID) {
		
		try {
			System.out.println("3PC: Sending resumeMessages to p"+procID);
			procOutList.get(procID).write("resumeMessages");
			procOutList.get(procID).flush();
		} catch (IOException e) {
			System.err.println("3PC: Error writing 'resumeMessages' to process p"+procID);
			e.printStackTrace();
		}
		
	}
	
	public void allClear() {
		
		for (int i = 0; i < procList.size(); i++) {
			if (procList.get(i) != null)
				try {
					System.out.println("3PC: Sending allClear to p"+i);
					procOutList.get(i).write("allClear");
					procOutList.get(i).flush();
				} catch (IOException e) {
					System.err.println("3PC: Error writing 'allClear' to process p"+i);
					e.printStackTrace();
				}
		}
		
	}

	public void rejectNextChange(int procID) {
		 
		try {
			System.out.println("3PC: Sending rejectNextChanse to p"+procID);
			procOutList.get(procID).write("rejectNextChange");
			procOutList.get(procID).flush();
		} catch (IOException e) {
			System.err.println("3PC: Error writing 'rejectNextChange' to process p"+procID);
			e.printStackTrace();
		}

	}

	
}
