package ut.distcomp.framework;

import java.util.HashSet;
import java.util.Hashtable;

public class Recovery {
		private String filenameString;
		private StateAC lastState;
		private HashSet<Integer> upSet;
		Hashtable<String, String> playList;
		
		
		public Recovery(String filename) {
			// TODO Auto-generated constructor stub
			filenameString = filename;
			upSet = new HashSet<Integer>();
			playList = new Hashtable<String, String>();
		    lastState = null;
		}
		
		/*
		 * 
		 * Parse the log file
		 * 
		 */
		private void parseLogFile(){
			
		}
		
		public Hashtable<String , String> getPlayList(){
			return playList;
		}
		
		public StateAC getLastState(){
			
			return lastState;
		}
		
		public HashSet<Integer> getUpSet(){
			return upSet;
		}
		
		
		
		
}
