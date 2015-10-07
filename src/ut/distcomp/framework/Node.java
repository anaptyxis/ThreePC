package ut.distcomp.framework;

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
	private static int coordinator = 0;
	private DTLog dtLog;
	private boolean running;   //only altered if process shuts down gracefully
	private int myID;
    private StateAC myState = StateAC.IDLE;
    private ArrayList<Integer> DecisionList = new ArrayList<Integer>();
    private ArrayList<Integer> ACKList = new ArrayList<Integer>();
    private HashSet<Integer> upSet = new HashSet<Integer>();
	private MessageParser currentAction = null;
	public Node(String configName, String dtL) {
		try {
			config = new Config(configName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		nc = new NetController(config);
		playList = new Hashtable<String,String>();
		dtLog = new DTLog(dtL);
		running = true;
        viewNumber = config.numProcesses;
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
        
        // receive STATE response
        else if (parser.getMessageHeader().toString().equals(TransitionMsg.STATE_RES.toString())){
        	 
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
        Boolean readyCommit = false;
        if(!ACKList.contains(0)) readyCommit  = true;
        if(readyCommit){
        	parser.setMessageHeader(TransitionMsg.COMMIT.toString());
    		for(int j = 0 ; j < viewNumber ; j++){
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
    private void processReceivedMsgAsParticipant(String message){
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
        	sendParticipantState(myState, parser);
        }
        
        //Receive recover request

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.RECOVER_REQ.toString())){
        	
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
	  
	private StateAC getParticipantState(){
		if(myID != coordinator) 
			return myState;
		else 
			return null;

	}
	
	
	/*
	 * Get message using polling when there is no failure
	 */
	
	private void getMessageAsCoordinator() throws InterruptedException{
		 while(true){
             List<String> messages = null;
             long startTime = (System.currentTimeMillis()+getTimeOut());
             long smallTimeout = getTimeOut()/10;
             Boolean atLeastOneBoolean= false;
             // receive message until getTimeOut() and there is no failure
             while(System.currentTimeMillis() < startTime) {
                 Thread.sleep(smallTimeout);
                 messages = (nc.getReceivedMsgs());
                
                 for(String m:messages) {
                    processReceivedMsgAscoordinator(m);
               	    //System.out.println("I am Here");
                    currentAction = new MessageParser(m);
                    atLeastOneBoolean = true;
                 }
             }
             
             if(!atLeastOneBoolean)
            	 System.out.println("Time out action for coordinator");
             
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
	 private List<MessageParser> getMessagesAsParticipant() throws InterruptedException {

          

          while(true){
              List<String> messages;
              List<MessageParser> parsers = null;
              MessageParser current = new MessageParser();
              long startTime = (System.currentTimeMillis()+getTimeOut());
              long smallTimeout = getTimeOut()/10;
              boolean atleastone = false;
              while(System.currentTimeMillis() < startTime) {
                  Thread.sleep(smallTimeout);
                  messages = (nc.getReceivedMsgs());
                 
                  for(String m :messages) {
                	   
                       processReceivedMsgAsParticipant(m);
                	   atleastone = true;
                     
                  }
              }
              
              // wait for VOTE request from coordinator
              if(!atleastone && myState == StateAC.IDLE){
            	  	  System.out.println("Participant wait for Coordinator's Vote Request");
            	      myState = StateAC.ABORT;
            	      dtLog.writeEntry(myState, "wait for vote request");
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
        
        TransitionMsg termination_decision = null;
        if(list.isEmpty()) {
           
        } else {
            int count_commitable = 0;
            int count_uncertain = 0;
            boolean commit_flag = false;
            boolean abort_flag = false;

            for(MessageParser item:list) {
            	
            	// if some decide to abort
                if(item.getMessageHeader().equalsIgnoreCase(StateAC.ABORT.toString())) {
                    abort_flag = true;
                    break;
                } 
                // if some decide to commit
                else if(item.getMessageHeader().equalsIgnoreCase(StateAC.COMMIT.toString())) {
                    commit_flag = true;
                    break;
                } 
                // record the number of uncertain
                else if(item.getMessageHeader().equalsIgnoreCase(StateAC.UNCERTAIN.toString())) {
                    count_uncertain ++;
                    continue;
                } 
                // record the number of commitable
                else if(item.getMessageHeader().equalsIgnoreCase(StateAC.COMMITABLE.toString())) {
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
            } else if(count_uncertain == list.size() + 1) {
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
		int newC = Integer.MAX_VALUE;
		for(int m : upSet){
			newC = Math.min(newC, m);
		}
		// if I am the new coordinator
		if(newC == myID) coordinator=myID;
		else {
		     sendURElectedMsg(myState, parser, newC);
		}
		return coordinator;
	}
	
	/*
	 * 
	 * Send you are elected message
	 * 
	 */
	
	 private void sendURElectedMsg(StateAC participantState, MessageParser pmRequest, int destProcNum ){
         
		 pmRequest.setMessageHeader(TransitionMsg.UR_ELECTED.toString());
         String stRequest = pmRequest.composeMessage();
         nc.sendMsg(destProcNum, stRequest);
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
        String stateResponse = stResponse.composeMessage();

        nc.sendMsg(Integer.valueOf(senderProcNum), stateResponse);
        
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
	        String request = pmrequest.composeMessage();

	        //TODO Change if liveSet of processes in implemented
	        for(Integer i: upSet) {
	            if(config.procNum == i.intValue())
	                continue;

	            nc.sendMsg(i.intValue(), request);
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
		
		Node n = new Node(args[0],args[1]);
		MessageParser parser= new MessageParser( Integer.toString(n.myID) + ";" + "add" + ";" +"test" + ";"+"http://www.google.com" + ";"+ TransitionMsg.CHANGE_REQ.toString());
		
		Thread.sleep(1000);
		if(n.myID==1){
			n.sendMsg(0,parser.composeMessage());
			System.out.println("I want to start 3PC");
		}
        if(n.myID == coordinator)
           n.getMessageAsCoordinator();
        else
           n.getMessagesAsParticipant();
		
	}
	
}