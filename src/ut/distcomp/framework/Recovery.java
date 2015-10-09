package ut.distcomp.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class Recovery {
		private String filenameString;
		private StateAC lastState;
		private HashSet<Integer> upSet;
		Hashtable<String, String> playList;
		private boolean askOthers ;
		private String old_decision = new String() ;
		private ArrayList<MessageParser> decisionList;
		private boolean pending ;
		private MessageParser pendingDecision;
		ArrayList<StateAC> stateList ;
		String whatIdo = new String();
		public Recovery(String filename) {
			// TODO Auto-generated constructor stub
			filenameString = filename;
			upSet = new HashSet<Integer>();
			playList = new Hashtable<String, String>();
		    lastState = null;
		    askOthers = false;
		    old_decision = null;
		    pending = false;
		    stateList = new ArrayList<StateAC>();
		    whatIdo = null;
		    decisionList = new ArrayList<MessageParser>();
		    pendingDecision = new MessageParser();
		}
		
		/*
		 * 
		 * Parse the log file
		 * 
		 */
		public void parseLogFile(){
			try {
				BufferedReader actionListReader = new BufferedReader(new FileReader(new File(filenameString)));
				String line = null;
				do {
					line = actionListReader.readLine();  
					//System.out.println("The line is "+line);
					if(line != null){
					String[] split_input = line.split(";");
					int size = split_input.length;
					//get the most recent UP set
					if(line.contains("UPset")){
						setHashSet(split_input[size-1]);
						//stateList.add(StateAC.IDLE);
					}
					// there is a decision
				    if(line.contains("decision")){
						 pending = true;
						 if(line.contains("yes")){
							 old_decision = "yes";
						 }
						 else if (line.contains("no")){
							 old_decision = "no";
						 }
						 pending = true;
						 MessageParser tmp = new MessageParser();
						 tmp.setAction(split_input[3]);
						 tmp.setSong(split_input[4]);
						 tmp.setURl(split_input[5]);
						 
						 pendingDecision = tmp;
					} 
					// there is uncertain
					else if(line.contains("uncertain")){
						 stateList.add(StateAC.UNCERTAIN);
						 pending = true;
					}
					
					// there is committable infomation
					else if (line.contains("commitable")) {
						stateList.add(StateAC.COMMITABLE);
						pending = true;
					}
					
					// Get the action
					else if(line.contains("commit") && !line.contains("commitable")){
						  MessageParser tmp = new MessageParser();
						  tmp.setAction(split_input[3]);
						  tmp.setSong(split_input[4]);
						  tmp.setURl(split_input[5]);
						  playListFollowAction(tmp);
						  decisionList.add(tmp);
						  
						  // clear all the pending data
						  pending = false;
						  stateList.clear();
						  old_decision = null;
					}
					
					
					// Get the last state
					lastState = stringToState(split_input[0]);
					}
				}while(line != null && !line.isEmpty());
				
				actionListReader.close();
				
			} catch (FileNotFoundException e) {
				System.err.println("Error reading script file "+filenameString+".");
				System.exit(2);
			} catch (IOException e) {
				System.err.println("Error closing script file "+filenameString+".");
				System.exit(3);
			}
		}
		
		/* 
		 *  make decision when recovery
		 */
		
		public void makeDecision(){
			System.out.println("Last state is "+ lastState.toString());
			System.out.println("old decision is "+ old_decision);
			
			if(old_decision != null){
					// Fails before sending Yes
					if(old_decision.equals("yes") && lastState == StateAC.DECIDE_YES){
							askOthers = false;
							whatIdo = "abort";
					}
					//Fails before sending No
					else if(old_decision.equals("no") && lastState == StateAC.DECIDE_NO){
							askOthers = false;
							whatIdo = "abort";
					}
					//Fails after sending yes
					else if(old_decision.equals("yes") && lastState == StateAC.UNCERTAIN){
						   askOthers = true;
						   whatIdo = "decide_yes";
					}
					else if(lastState == StateAC.COMMITABLE){
							askOthers = true;
							whatIdo = "decide_yes";
					}
			}
			
			//There is no pending decision
			else{
				
			}
		}
		
		/*
		 * 
		 */
		
		public MessageParser getPendingDecision() {
			 return pendingDecision;
		}
		
		/*
		 *   Return the info whether there is pending decision
		 */
		
		public boolean getPendingInfo() {
			return pending;
		}
		
		/*
		 *  Return Decision List
		 */
		
		public ArrayList<MessageParser> getDecisionList() {
				return decisionList;
		}
		
		/*
		 *   Return boolean whether I should ask others to determine
		 */
		
		public boolean DoAskOthers() {
			return askOthers;
		}
		
		/*
		 * Set hash set from log file 
		*/
		
		public void setHashSet(String input) {
		        input = input.replace("[", "") ;
		        input = input.replace("]", "") ;
		        input = input.replaceAll(" ", "");
		        String[] tmp = input.split(":");
		        if (tmp.length==2){
		        	String[] items=tmp[1].split(",");
		        	upSet = new HashSet<Integer>();

		        	for(String item: items) {
		        		upSet.add(Integer.valueOf(Integer.parseInt(item)));
		        	}
		        }
		    }
		
		/*
		 *  Get What I should Do 
		 * 
		 */
		public String getWhatIdo() {
			return whatIdo;
		}
		
		
		/*
		 *  Get the playlist , return the hashtable 
		 */
		public Hashtable<String , String> getPlayList(){
			return playList;
		}
		
		/*
		 *  Return the last state 
		 */
		public StateAC getLastState(){
			
			return lastState;
		}
		
		/*
		 *  get the Up Set from DT log;
		 * 
		 */
		public HashSet<Integer> getUpSet(){
			return upSet;
		}
		/*
		 * add (songName, URL) to the local playList.
		 * 
		 * FALSE if playList already contains songName.
		 */
		public boolean add(String songName, String URL) {
			
			if (playList.contains(songName))
				return false;
			else
				playList.put(songName, URL);
			return true;
			
		}
		
		/*
		 * remove songName from the local playList.
		 * 
		 * FALSE if playList does not contain songName.
		 */
		public boolean remove(String songName) {
			
			return (playList.remove(songName) != null);
			
		}
		
		/*
		 * edit (songName, URL) to the be (newSongName, newSongURL)
		 * Assume that in a URL-only edit, songName == newSongName.
		 * 
		 * FALSE if playList does not contain songName.
		 * FALSE if playList already contains newSongName.
		 */
		public boolean edit(String songName, String newSongName,  String newSongURL) {
			
			if ((playList.contains(songName)) || (playList.contains(newSongName)))
				return false;
			else {
				playList.remove(songName);
				playList.put(newSongName, newSongURL);
				if (!songName.equals(newSongName))
					playList.put(newSongName, (playList.remove(songName)));
				return true;
			}
		}
		  /*
	     * 
	     *  Playlist following action ,add ,edit or delete
	     * 
	     */
	    
	    private void playListFollowAction(MessageParser parser){
	    	 // edit commit
	        if (parser.getInstruction().equalsIgnoreCase("edit")){
	               String newsong = parser.getSong();
	               String oldsong = parser.getOldSong();
	               String url = parser.getUrl();
	               edit(oldsong,newsong,url);
	              
	        }
	        //commit for add
	        else if (parser.getInstruction().equalsIgnoreCase("add")){
	                String song = parser.getSong();
	                String url = parser.getUrl();
	                add(song, url);
	              
	        }
	        // commit for delete
	        else if (parser.getInstruction().equalsIgnoreCase("del")){
	                String song = parser.getSong();
	                remove(song);
	              
	        }
	        // wrong vote request
	        else{

	        }
	    }
	    
	    /*
	     *  Convert string to State 
	     * 
	     */
	    
	    private StateAC stringToState(String stateinfo){
	    	
	    	if(stateinfo.equals(StateAC.IDLE.toString() ))
	    			return StateAC.IDLE;
	    	if(stateinfo.equals(StateAC.START_3PC.toString() ))
	    			return StateAC.START_3PC;
	    	if(stateinfo.equals(StateAC.WAIT_FOR_VOTE_REQ.toString() ))
	    			return StateAC.WAIT_FOR_VOTE_REQ;
	    	if(stateinfo.equals(StateAC.WAIT_FOR_VOTE_DEC.toString() ))
	    			return StateAC.WAIT_FOR_VOTE_DEC;
	    	if(stateinfo.equals(StateAC.WAIT_FOR_ACKS.toString() ))
	    			return StateAC.WAIT_FOR_ACKS;
	    	if(stateinfo.equals(StateAC.WAIT_FOR_STATE_RES.toString() ))
	    			return StateAC.WAIT_FOR_STATE_RES;
	    	if(stateinfo.equals(StateAC.UNCERTAIN.toString() ))
	    			return StateAC.UNCERTAIN;
	    	if(stateinfo.equals(StateAC.COMMITABLE.toString() ))
	    			return StateAC.COMMITABLE;
	    	if(stateinfo.equals(StateAC.COMMIT.toString() ))
	    			return StateAC.COMMIT;
	    	if(stateinfo.equals(StateAC.ABORT.toString() ))
	    		return StateAC.ABORT;
	    				
	        return null;
	    	
	    }
		
		
}
