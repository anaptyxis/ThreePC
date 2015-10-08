package ut.distcomp.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


/**
 * Node
 * @author Bradley Beth
 *
 */
public class Node {

	private Config config;
	private NetController nc;
	private Hashtable<String,String> playList;
	private int viewNumber = 5;
	private int coordinator = 0;
	private DTLog dtLog;
	private boolean running;   //only altered if process shuts down gracefully
	private boolean revival;
	private int myID;
    private StateAC myState = StateAC.IDLE;
    private ArrayList<Integer> DecisionList = new ArrayList<Integer>(); //-1 NO, 0 NO DECISION, 1 YES
    private ArrayList<Integer> ACKList = new ArrayList<Integer>();
    private HashSet<Integer> upSet = new HashSet<Integer>();
    private ArrayList<MessageParser> ActionList = new ArrayList<MessageParser>();
	private MessageParser currentAction = null;
	
	public Node(String configName, String dtL, boolean revival) {
		//if dtLog is not empty, then failure has occurred and this is 
		//a revival. We need to put a handler in to bring the process 
		//back up. Otherwise, the process/Node is constructed from scratch.
		// TODO: validate that DTLog is not destructively opened on
		//		 revival.
		
		try {
			config = new Config(configName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		nc = new NetController(config);
		playList = new Hashtable<String,String>();
		dtLog = new DTLog(dtL, revival);	//revival == Node recovering?
/*		String logEntry = dtLog.readEntry();
		while (logEntry != null) {
			logEntry = dtLog.readEntry();	
		}
*/		running = true;
		this.revival = revival;
        viewNumber = config.numProcesses;
        currentAction = new MessageParser();
		myID = getID();
		for(int i = 0 ; i < viewNumber; i++){
			if(i!=myID){
				DecisionList.add(0);
				ACKList.add(0);
			}else{
				DecisionList.add(1);
				ACKList.add(1);
			}
		}
		
		for(int j = 0 ; j < viewNumber; j++){
			upSet.add(j);
		}
		
		dtLog.writeEntry(myState, "UPset: "+ upSet);
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
	
	public void readActions(String actionsFName) {
		try {
			BufferedReader actionListReader = new BufferedReader(new FileReader(new File(actionsFName)));
			String line = null;
			do {
				line = actionListReader.readLine();  
				if (line != null) {
					String[] strArr = line.split(" ");
					System.out.println(strArr.length);
					if (strArr[0].equals("add"))
						ActionList.add(new MessageParser( Integer.toString(myID) + ";" + strArr[0] + ";" + strArr[1] + ";" + strArr[2] + ";"+ StateAC.IDLE.toString()+";"+TransitionMsg.CHANGE_REQ.toString()));
					if (strArr[0].equals("edit"))
						ActionList.add(new MessageParser( Integer.toString(myID) + ";" + strArr[0] + ";" + strArr[1] + "," + strArr[2] + ";" + strArr[3] + ";" + StateAC.IDLE.toString()+";"+TransitionMsg.CHANGE_REQ.toString()));
					if (strArr[0].equals("remove"))
						ActionList.add(new MessageParser( Integer.toString(myID) + ";" + strArr[0] + ";" + strArr[1] + ";" + StateAC.IDLE.toString()+";"+TransitionMsg.CHANGE_REQ.toString()));
					System.out.println(line);
				}
			} while (line != null); 						
			actionListReader.close();
		} catch (FileNotFoundException e) {
			System.err.println("Error reading script file "+actionsFName+".");
			System.exit(2);
		} catch (IOException e) {
			System.err.println("Error closing script file "+actionsFName+".");
			System.exit(3);
		}
		
	}
	
	

    /*
    * Process received message as coordinator
    */
    private void processReceivedMsgAscoordinator(String message){
        MessageParser parser = new MessageParser(message);
        // start 3 PC
        if (parser.getMessageHeader().toString().equals(TransitionMsg.CHANGE_REQ.toString())){
        	myState = StateAC.START_3PC;
        	dtLog.writeEntry(myState, parser.getTransaction());
        	parser.setMessageHeader(TransitionMsg.VOTE_REQ.toString());
        		for(int i = 0 ; i < viewNumber ; i++){
        			if(i!=coordinator){
                       // System.out.println("send message to " + Integer.toString(i));
        				sendMsg(i, parser.composeMessage() );
        			}
        		}
        		myState = StateAC.WAIT_FOR_VOTE_DEC;
        		dtLog.writeEntry(myState, parser.getTransaction());
        }
        // receive VOTE_DEC YES
        else if (parser.getMessageHeader().toString().equals(TransitionMsg.YES.toString()) ){
        	  int source = Integer.parseInt(parser.getSource());
        	  DecisionList.set(source, 1);
        }
        // receive Vote_NO
        else if( parser.getMessageHeader().toString().equals(TransitionMsg.NO.toString())){
        	  int source = Integer.parseInt(parser.getSource());
        	  DecisionList.set(source, -1);
        }
        // receive ACK
        else if (parser.getMessageHeader().toString().equals(TransitionMsg.ACK.toString())){
        	 int source = Integer.parseInt(parser.getSource());
        	 ACKList.set(source, 1);
        }
        
        //wrong message
        else{

        }
        
        // Decide whether to precommit or abort;
        Boolean ready = false;
        if(!DecisionList.contains(0)) ready  = true;
        
        //System.out.println(DecisionList);
        
        if(ready){
        	//abort
        	if(DecisionList.contains(-1)){
        		parser.setMessageHeader(TransitionMsg.ABORT.toString());
        		for(int j = 0 ; j < viewNumber ; j++){
        			if(j != coordinator && DecisionList.get(j)==1){
        				sendMsg(j, parser.composeMessage());
        			}
        		}
        		//log
        		 
                 myState = StateAC.ABORT;
                 dtLog.writeEntry(myState, parser.getTransaction());
        	}
        	//commit
        	else{
        		parser.setMessageHeader(TransitionMsg.PRECOMMIT.toString());
        		for(int j = 0 ; j < viewNumber ; j++){
        			if(j != coordinator ){
        				sendMsg(j, parser.composeMessage());
        			}
        		}
        		//log
        
                 myState = StateAC.WAIT_FOR_ACKS;
                 dtLog.writeEntry(myState, parser.getTransaction());
        	}
        	
        	//clear the history
        	for(int j = 0 ; j < viewNumber ; j++){
        		if(j!=myID)
    			DecisionList.set(j, 0);
    		}
        }
        
        
        // Decide whether to send final commit message
        Boolean readyCommit = true;
        for(int j : upSet){
        	
        	if(j == coordinator)
        		continue;
        	if(ACKList.get(j) != 0)
        		continue;
        	else
        		readyCommit  = false;
        }
       System.out.println("The Up set is "+ upSet);
        if(readyCommit){
        	parser.setMessageHeader(TransitionMsg.COMMIT.toString());
    		for(int j : upSet){
    			if(j != coordinator ){
    				sendMsg(j, parser.composeMessage());
    			}
    		}
    		//log
    		
            myState = StateAC.COMMIT;
            dtLog.writeEntry(myState, parser.getTransaction());
            for(int j = 0 ; j < viewNumber ; j++){
        		if(j!=myID)
    			ACKList.set(j, 0);
    		}
        }
    }

    /*
     * Playlist Vote action 
     * Input action is add edit, and delete
     * output will be Vote Yes and vote No
    */
    
    private void playlistVoteAction(MessageParser parser){
    	 // edit request
        if (parser.getInstruction().equalsIgnoreCase("edit")){
                String song = parser.getOldSong();
                //vote yes
                if(playList.containsKey(song)){
                    parser.setMessageHeader(TransitionMsg.YES.toString());
                    parser.setSource(Integer.toString(myID));
                    sendMsg(coordinator,parser.composeMessage());
                    myState = StateAC.UNCERTAIN;
                    dtLog.writeEntry(myState, parser.getTransaction());
                }
                //vote no
                else{
                    parser.setMessageHeader(TransitionMsg.NO.toString());
                    parser.setSource(Integer.toString(myID));
                    sendMsg(coordinator,parser.composeMessage());
                    myState = StateAC.ABORT;
                    dtLog.writeEntry(myState, parser.getTransaction());

                }
        }
        //request for add
        else if (parser.getInstruction().equalsIgnoreCase("add")){
               String song = parser.getSong();
               //vote yes
               if(!playList.containsKey(song)){
                   parser.setMessageHeader(TransitionMsg.YES.toString());
                   parser.setSource(Integer.toString(myID));
                   sendMsg(coordinator,parser.composeMessage());
                   myState = StateAC.UNCERTAIN;
                   dtLog.writeEntry(myState, parser.getTransaction());
               }
               //vote no
               else{
                   parser.setMessageHeader(TransitionMsg.NO.toString());
                   parser.setSource(Integer.toString(myID));
                   sendMsg(coordinator,parser.composeMessage());
                   myState = StateAC.ABORT;
                   dtLog.writeEntry(myState, parser.getTransaction());

               }
        }
        // request for delete
        else if (parser.getInstruction().equalsIgnoreCase("del")){
               //vote yes
               String song = parser.getSong();
               if(playList.containsKey(song)){
                   parser.setMessageHeader(TransitionMsg.YES.toString());
                   parser.setSource(Integer.toString(myID));
                   sendMsg(coordinator,parser.composeMessage());
                   myState =StateAC.UNCERTAIN;
                   dtLog.writeEntry(myState, parser.getTransaction());
               }
               //vote no
               else{
                   parser.setMessageHeader(TransitionMsg.NO.toString());
                   parser.setSource(Integer.toString(myID));
                   sendMsg(coordinator,parser.composeMessage());
                   myState = StateAC.ABORT;
                   dtLog.writeEntry(myState, parser.getTransaction());
               }
        }
        // wrong vote request
        else {

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
               myState = StateAC.COMMIT;
               dtLog.writeEntry(myState, parser.getTransaction());

        }
        //commit for add
        else if (parser.getInstruction().equalsIgnoreCase("add")){
                String song = parser.getSong();
                String url = parser.getUrl();
                add(song, url);
                myState = StateAC.COMMIT;
                dtLog.writeEntry(myState, parser.getTransaction());
        }
        // commit for delete
        else if (parser.getInstruction().equalsIgnoreCase("del")){
                String song = parser.getSong();
                remove(song);
                myState = StateAC.COMMIT;
                dtLog.writeEntry(myState, parser.getTransaction());
        }
        // wrong vote request
        else{

        }
    }
    
    /*
    * Process received message as participant
    */
    private void processReceivedMsgAsParticipant(String message) throws InterruptedException{
        MessageParser parser = new MessageParser(message);

        // Receive vote request message

        if(parser.getMessageHeader().toString().equals(TransitionMsg.VOTE_REQ.toString())){
               
              	playlistVoteAction(parser);
     
        }

        //  Receive pre-commit message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.PRECOMMIT.toString())){
                // log the message

                // send the message
                parser.setMessageHeader(TransitionMsg.ACK.toString());
                parser.setSource(Integer.toString(myID));
                sendMsg(coordinator,parser.composeMessage());
                myState = StateAC.COMMITABLE;
                dtLog.writeEntry(myState, parser.getTransaction());
        }

        //Receive commit message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.COMMIT.toString())){
        		
             	playListFollowAction(parser);
        }

        //  Receive Abort message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.ABORT.toString())){
                // log the message
        		myState = StateAC.ABORT;
        		dtLog.writeEntry(myState, parser.getTransaction());
        }

        //Receive state request

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.STATE_REQ.toString())){
        	   System.out.println("Receive State Request"); 
        	   //set the new UP set
        	   upSet = parser.getUpSet();
        	   // send participant state
        	   sendParticipantState(myState, parser);
        }
        
        //Receive recover request

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.RECOVER_REQ.toString())){
        	
        }
        
        //Receive UR_ELECTED message, I am new coordinator now

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.UR_ELECTED.toString())){
        	    System.out.println("Hahaha I am "+ Integer.toString(myID) + "new leader");
        	    //I am new Coordinator
        	    if(myID != coordinator) 
        	    	System.out.println("Seems something wrong happens, sinces I am not the coordinator");
        	    // update the UP set
        	    upSet = parser.getUpSet();
        	    System.out.println("my new UP set is " + upSet);
        	    //Send out the State_REQ
        	    sendStateReq(parser);
        	    
        	    // change my state and DT log that
        	    
        	    myState = StateAC.WAIT_FOR_STATE_RES;
        	    dtLog.writeEntry(myState, parser.getTransaction());
        	    //And now I am new Coordinator
        	    getMessageAsCoordinator();
        }
        
        
        // Receive Wrong Message
        else{

        }
    }




	public int getID() {
		return nc.getConfig().procNum;
	}
	
	public int getNumProcesses() {
		return nc.getConfig().numProcesses;
	}
	
	public int getTimeOut() {
		return nc.getConfig().timeOut;
	}
	
	/*
	 *   Get the state of Participant
	 */
	private StateAC getParticipantState(){
		if(myID != coordinator) 
			return myState;
		else 
			return null;

	}
	
	/*
	 *   Get the state of Coordinator
	 */
	private StateAC getCoordinatorState(){
		if(myID == coordinator)
			return myState;
		else {
			return null;
		}
	}
	
	
	/*
	 * Get message using polling when there is no failure
	 */
	
	private void getMessageAsCoordinator() throws InterruptedException{
		 while(true){
             List<String> messages = null;
             ArrayList<MessageParser> stateReqList = new ArrayList<MessageParser>();
             long startTime = (System.currentTimeMillis()+getTimeOut());
             long smallTimeout = getTimeOut()/10;
             Boolean atLeastOneBoolean= false;
             // receive message until getTimeOut() and there is no failure
             while(System.currentTimeMillis() < startTime) {
                 Thread.sleep(smallTimeout);
                 messages = (nc.getReceivedMsgs());
                
                 for(String m:messages) {
                	currentAction = new MessageParser(m);
                	if(currentAction.getMessageHeader().equals(TransitionMsg.STATE_RES.toString()))
                		stateReqList.add(currentAction);
                	else {
                		processReceivedMsgAscoordinator(m);
					}
                    
               	    //System.out.println("I am Here");
                    
                    atLeastOneBoolean = true;
                 }
             }
             // if there is message comming, and the message is about the state response
             if(atLeastOneBoolean && myState==StateAC.WAIT_FOR_STATE_RES){
            	 TransitionMsg header = terminationRule(myState, stateReqList);
           	  	 System.out.println("the decision made on collection is " + header.toString());
           	  	 MessageParser actionMessageParser = new MessageParser();
           	  	 actionMessageParser =stateReqList.get(1);
           	  		 	
           	  	actionMessageParser.setMessageHeader(header.toString());
           	  	 
           	  	 // if decision is Abort
           	  	 if(header == TransitionMsg.ABORT){
           	  		
           	  		 for(MessageParser tmp : stateReqList){
           	  			 int j = Integer.parseInt(tmp.getSource());
           	  			 	sendMsg(j, currentAction.composeMessage());
           	  		 }
           	  		 
           	  		 myState  = StateAC.ABORT;
           	  		 dtLog.writeEntry(myState, actionMessageParser.getTransaction());
           	  	 }   
           	  	
           	  	 // if the decision is Commit 
           	    if(header == TransitionMsg.COMMIT){
        	  		 
        	  		 for(MessageParser tmp : stateReqList){
         	  			 int j = Integer.parseInt(tmp.getSource());
         	  				 sendMsg(j, actionMessageParser.composeMessage());
         	  		 }
        	  		 
        	  		myState  = StateAC.COMMIT;
        	  		dtLog.writeEntry(myState, actionMessageParser.getTransaction());
        	  	 }   
           	    
           	    // if the decision is Precommit
           	   if(header == TransitionMsg.PRECOMMIT){
           		   boolean isUncertain = false;
           		   
           		   // Sent every uncertain case a Precommit info
           		   for(MessageParser tmp : stateReqList){
      	  			 if(tmp.getStateInfo() == StateAC.UNCERTAIN ){
      	  				 int j = Integer.parseInt(tmp.getSource());
      	  				 sendMsg(j, actionMessageParser.composeMessage());
      	  				 System.out.println("Re issue precommit"+ actionMessageParser.composeMessage());
      	  				 isUncertain = true;
      	  			 }
           		   }
      	  		 
      	  		  // Corner case, if everyone is commitable, send commit
      	  		  if(!isUncertain){
      	  			 actionMessageParser.setMessageHeader(TransitionMsg.COMMIT.toString());
      	  			 for(MessageParser tmp : stateReqList){
      	  				 if(tmp.getStateInfo() == StateAC.COMMITABLE ){
      	  				 	int j = Integer.parseInt(tmp.getSource());
      	  				 	sendMsg(j, actionMessageParser.composeMessage());      	  		
      	  				 }
      	  			 }
      	  			 myState  = StateAC.COMMIT;
    	  			 dtLog.writeEntry(myState, actionMessageParser.getTransaction());

      	  		  }else{
      	  			 myState  = StateAC.WAIT_FOR_ACKS;
      	  			 dtLog.writeEntry(myState, actionMessageParser.getTransaction());
      	  			 System.out.println("Send out precommit");
      	  		 }
      	  	   }   
           	    
           	    
           	  stateReqList.clear();
           	  	
           	  continue;
           	 }
            
             
             // There is no vote decision coming
             if(!atLeastOneBoolean && myState==StateAC.WAIT_FOR_VOTE_DEC){
            	  System.out.println("Time out action for coordinator wait for vote Decision");
            	  currentAction.setMessageHeader(TransitionMsg.ABORT.toString());
          		  for(int j = 0 ; j < viewNumber ; j++){
          		 	if(j != coordinator ){
          				sendMsg(j, currentAction.composeMessage());
          			}
          		  }
            	  myState = StateAC.ABORT;
            	  dtLog.writeEntry(myState, currentAction.getTransaction());
             }
             // if there is no ACK come back
             else if (!atLeastOneBoolean && myState == StateAC.WAIT_FOR_ACKS){
            	 System.out.println("Time out action for coordinator ACK");
            	 currentAction.setMessageHeader(TransitionMsg.COMMIT.toString());
         		 for(int j = 0 ; j < viewNumber ; j++){
         		 	if(j != coordinator ){
         				sendMsg(j, currentAction.composeMessage());
         			}
         		 }
         		 //log
         		
                myState = StateAC.COMMIT;
                dtLog.writeEntry(myState, currentAction.getTransaction());
             }
            
             
		 }
	}
	
	/*
	 * 
	 * Time out action when there is a failure
	 * 
	 */
	 private void getMessagesAsParticipant() throws InterruptedException {
		 
          while(true){
              List<String> messages ;
              long startTime = (System.currentTimeMillis()+getTimeOut());
              long smallTimeout = getTimeOut()/10;
              boolean atleastone = false;
              int num_of_election_message = 0;
              boolean changeRole = false;
              while(System.currentTimeMillis() < startTime) {
                  Thread.sleep(smallTimeout);
                  messages = (nc.getReceivedMsgs());
                  //System.out.println("Receive message :  "+ messages);
                  for(String m :messages) {
                	   
                	   currentAction = new MessageParser(m);
                	  
                	   if(currentAction.getMessageHeader().toString().equals( TransitionMsg.UR_ELECTED.toString())){
                		   //System.out.println("I am "+ myID+ " and I receive election message");
                		   // What you receive is UR_ELECTED message
                		   // You should not process more than once
                		   if(num_of_election_message==0){
                			   num_of_election_message++;
                			   processReceivedMsgAsParticipant(m);
                			   changeRole = true;
                		   }
                	   }
                	   else{
                		   System.out.println("Receive message :  "+ messages);
                		   processReceivedMsgAsParticipant(m);
                		  
                	   }
                	   atleastone = true;
                     
                  }
              }
              
              if(changeRole) break;
              
              num_of_election_message = 0;
              
              
              // wait for VOTE request from coordinator
              if(!atleastone && myState == StateAC.IDLE){
            	  	  System.out.println("Participant wait for Coordinator's Vote Request");
            	      myState = StateAC.ABORT;
            	      dtLog.writeEntry(myState, "wait for vote request");
              }
              
              // wait for Precommit message from coordinator
              if(!atleastone && myState==StateAC.UNCERTAIN){
            	      System.out.println("Participant wait for Coordinator's Precommit");
            	      //remove coordinator in UP set
            	      upSet.remove(coordinator);
            	      //run election protocol  and send state request
            	      int newCoor = electionProtocol(currentAction);
            	      
            	      //System.out.println("I am " + myID + " new coordinator is " + Integer.toString(newCoor));
            	    
              }
              
              // wait for commit message from coordinator
              if(!atleastone && myState==StateAC.COMMITABLE){
            	  	  System.out.println("Participant wait for Coordinator's Commit");
            	  	  upSet.remove(coordinator);
            	  	  //run election protocol  and send state request
            	  	  int newCoor = electionProtocol(currentAction);
              }
             
           }
      }
	
	
	/*
	 * 
	 *Termination Protocal 
	 *Depends on my state and received message
	 * 
	 */
	
	 private TransitionMsg terminationRule(StateAC myState, List<MessageParser> list ) {
			
			System.out.println("start termination rule");
			//System.out.println("What I receive is " + list);
	        TransitionMsg termination_decision = null;
	        if(list.isEmpty()) {
	           System.out.println("there is a empty set for termination rule");
	        } else {
	            int count_commitable = 0;
	            int count_uncertain = 0;
	            boolean commit_flag = false;
	            boolean abort_flag = false;

	            for(MessageParser item:list) {
	            	
	            	//System.out.println("The state is ");
	            	// if some decide to abort
	                if(item.getStateInfo()==StateAC.ABORT) {
	                    abort_flag = true;
	                    break;
	                } 
	                // if some decide to commit
	                else if(item.getStateInfo()==StateAC.COMMIT) {
	                    commit_flag = true;
	                    break;
	                } 
	                // record the number of uncertain
	                else if(item.getStateInfo()==StateAC.UNCERTAIN) {
	                    count_uncertain ++;
	                    continue;
	                } 
	                // record the number of commitable
	                else if(item.getStateInfo() == StateAC.COMMITABLE) {
	                    count_commitable ++;
	                    continue;
	                }
	            }
	            
	            switch(myState) {
	                case ABORT: abort_flag = true; break;
	                case COMMIT: commit_flag = true; break;
	                case UNCERTAIN: count_uncertain ++; break;
	                case COMMITABLE: count_commitable++; break;
	            }

	            if(abort_flag) {
	                termination_decision = TransitionMsg.ABORT;
	            } else if(commit_flag) {
	                termination_decision = TransitionMsg.COMMIT;
	            } else if(count_uncertain == list.size() ) {
	                termination_decision = TransitionMsg.ABORT;
	            } else if(count_commitable > 0) {
	                termination_decision = TransitionMsg.PRECOMMIT;
	            }
	           
	        }


	        return termination_decision;
	    }
	/*
	 * 
	 * Election Protocal
	 * 
	 */
	private int electionProtocol(MessageParser parser){
		parser.setSource(Integer.toString(myID));
		parser.initHashSet(upSet.toString());
		int newC = Integer.MAX_VALUE;
		for(int m : upSet){
			newC = Math.min(newC, m);
		}
		// if I am the new coordinator
		coordinator=newC;
		if(myID != newC)
		     sendURElectedMsg(myState, parser, newC);
		return coordinator;
	}
	
	/*
	 * 
	 * Send you are elected message
	 * 
	 */
	
	 private void sendURElectedMsg(StateAC participantState, MessageParser pmRequest, int destProcNum ){
         System.out.println("I am "+ myID+ " send message to new leader " + Integer.toString(destProcNum));
		 pmRequest.setMessageHeader(TransitionMsg.UR_ELECTED.toString());
         String stRequest = pmRequest.composeWithUpset();
         sendMsg(destProcNum, stRequest);
     }
	
	/*
	 * 
	 *  As participant, sent out the state
	 * 
	 */
	 private void sendParticipantState(StateAC state, MessageParser stResponse){

	        String senderProcNum = stResponse.getSource();
	        stResponse.setSource(String.valueOf(config.procNum));
	        stResponse.setMessageHeader(TransitionMsg.STATE_RES.toString());
	        stResponse.setStateInfo(myState);
	        String stateResponse = stResponse.composeMessage();

	        sendMsg(Integer.valueOf(senderProcNum), stateResponse);
	        
	        return;
	    }
	
	
	/*
	 * 
	 * Sent out the state request as a new coordinate
	 * 
	 */
	
	  private void sendStateReq(MessageParser pmrequest) {
        pmrequest.setMessageHeader(TransitionMsg.STATE_REQ.toString());
        pmrequest.setSource(String.valueOf(config.procNum));
        String request = pmrequest.composeWithUpset();
        
        //TODO Change if liveSet of processes in implemented
        for(Integer i: upSet) {
            if(config.procNum == i.intValue())
                continue;
            System.out.println("I am send state request " + Integer.toString(i) );
            sendMsg(i.intValue(), request);
        }
 }
	 
	 /*
	  * 
	  * Basic send and receive and shutdown function
	  * 
	  */

	  public void sendMsg(int procID, String msg) {
			this.nc.sendMsg(procID, msg);
	  }
		
	  public List<String> retrieveMsgs() {
			return this.nc.getReceivedMsgs();
	  }
		
	  public void shutdown() {
			this.dtLog.close();
			this.nc.shutdown();
			running = false;
	  }
	 
	 /*
	  * 
	  * 
	  *  Main Function
	  * 
	  * 
	  */
	public static void main(String[] args) throws InterruptedException {
		
		boolean revival = (args[2].trim().equals("revive"));
		Node n = new Node(args[0],args[1], revival);
		//MessageParser parser= new MessageParser( Integer.toString(n.myID) + ";" + "add" + ";" +"test" + ";"+"http://www.google.com" + ";"+ TransitionMsg.CHANGE_REQ.toString());

		Thread.sleep(1000);
		if(n.myID==1){
			n.readActions("ActionList.txt");
			for (MessageParser parser : n.ActionList)
				n.sendMsg(0,parser.composeMessage());
			System.out.println("I want to start 3PC");
		}
        if(n.myID == n.coordinator)
           n.getMessageAsCoordinator();
        else
           n.getMessagesAsParticipant();
		
	}
	
}