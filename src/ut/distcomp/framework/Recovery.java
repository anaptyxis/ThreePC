package ut.distcomp.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
			try {
				BufferedReader actionListReader = new BufferedReader(new FileReader(new File(filenameString)));
				String line = null;
				do {
					line = actionListReader.readLine();  
					String[] split_input = line.split(";");
					//get the most recent UP set
					if(line.contains("UPset")){
						
					}
					
					// Get the action
					if(line.contains("commit") && !line.contains("commitable")){
						  MessageParser tmp = new MessageParser();
						  playListFollowAction(tmp);
					}
					
					
					// Get the last state
					lastState = stringToState(split_input[0]);
					
				}while(line != null);
			} catch (FileNotFoundException e) {
				System.err.println("Error reading script file "+filenameString+".");
				System.exit(2);
			} catch (IOException e) {
				System.err.println("Error closing script file "+filenameString+".");
				System.exit(3);
			}
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
