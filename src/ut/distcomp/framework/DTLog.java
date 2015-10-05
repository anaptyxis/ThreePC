package ut.distcomp.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * DTLog
 * 
 * A DTLog entry is a String containing:
 * 
 *   "timestamp" "transition" "{add,remove,edit}" "1–3 parameters" "source" "destination"
 * 
 * @author Bradley Beth
 *
 */
public class DTLog {

	String logFileName;
	File logFile;
	FileWriter writer;
	BufferedReader reader; 
	
	public DTLog(String name) {
		logFileName = name;
		logFile = new File(logFileName);
		try {
			writer = new FileWriter(logFile);
			reader = new BufferedReader(new FileReader(logFile));
		} catch (IOException e) {
			System.err.println("Error opening LOG FILE "+logFileName+".");
			e.printStackTrace();
		}
	}
	
	public void writeEntry(TransitionMsg transition,
						   String command, 
						   String[] args, 
						   int src, 
						   int dest) {
		
		String entry = "";
		Timestamp ts = new Timestamp(new Date().getTime());
		entry += transition +" ";
		entry += ts.toString() + " ";
		entry += command + " ";
		//for (String arg : args)
		//	entry += arg + " ";
		entry += String.valueOf(src) + " ";
		entry += String.valueOf(dest) + " ";
        entry += "\n";
		writeEntry(entry);
		
	}
	
	private void writeEntry(String entry) {
		try {
			writer.write(entry);
			writer.flush();
		} catch (IOException e) {
			System.err.println("Error writing to LOG FILE "+logFileName+".");
			e.printStackTrace();
		} 	
	}
	
	public String readEntry() {
		String entry = null;
		try {
			entry = reader.readLine();
		} catch (IOException e) {
			System.err.println("Error reading from LOG FILE "+logFileName+".");
			e.printStackTrace();
		}
		return entry;
	}
	
	public void close() {
		try {
			writer.close();
			reader.close();
		} catch (IOException e) {
			System.err.println("Error closing DTLog file "+logFileName+".");
		}		
	}

}
