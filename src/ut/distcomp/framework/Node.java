package ut.distcomp.framework;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;


/**
 * Node
 * @author Bradley Beth
 *
 */
public class Node {
	private static final String EXTENSION = ".txt";
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
    private LinkedList<MessageParser> ActionList = new LinkedList<MessageParser>();
	private MessageParser currentAction = null;
	private int msgCount;
	private int msgBound;
	private BufferedReader inputFromController;
	
	private ArrayList<MessageParser> oldDecisionList = new ArrayList<MessageParser>();
	private Recovery recover = null;
	private boolean pending = false;
	private Queue<MessageParser> QueuedMessageList;
	private boolean coordinatorWorking = true;
	private boolean TotalFailure = false;
	private ArrayList<HashSet<Integer>> recoveryUpSets = null;
	private LinkedList<MessageParser> messageQueue;
	
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
*/	
		running = true;
		this.revival = revival;
		myID = getID();
		currentAction = new MessageParser();
		msgCount = 0;
		msgBound = Integer.MAX_VALUE;		//start with no bound on msgCount
		inputFromController = new BufferedReader(new InputStreamReader(System.in));	
		messageQueue = new LinkedList<MessageParser>();
		// not the revival case
		if(!revival){
			System.out.print("I am fresh process");
			viewNumber = config.numProcesses;
      
			for(int i = 0 ; i < viewNumber; i++){
				DecisionList.add(0);
				if(i!=myID){
					ACKList.add(0);
				}else{
					ACKList.add(1);
				}
			}
    
			for(int j = 0 ; j < config.numProcesses; j++){
				upSet.add(j);
			}
    
			dtLog.writeEntry(myState, "UPset: "+ upSet);
			oldDecisionList = new ArrayList<MessageParser>();
		}else{
			//System.out.print("I am recover process "+Integer.toString(myID)+"  "+ "DTLog"+Integer.toString(myID)+EXTENSION);
			recover = new Recovery("DTLog"+Integer.toString(myID)+EXTENSION);
			recover.parseLogFile();
			recover.makeDecision();
      
			oldDecisionList = recover.getDecisionList();
			playList = recover.getPlayList();
			upSet = recover.getUpSet();
			myState = recover.getLastState();
			pending = recover.getPendingInfo();
			recoveryUpSets = new ArrayList<HashSet<Integer>>();
			//====DEBUG=========
			System.out.println("last state is "+recover.getLastState().toString()+
          "  I am need to ask others is " + recover.DoAskOthers() +" and the upset is  "
          +upSet);
        
			if(recover.DoAskOthers()){
				// ask others to get infomation
				askOtherForHelp(recover.getPendingDecision());
			}else{
				if(recover.getWhatIdo().equals("abort")){
					myState = StateAC.ABORT;
					dtLog.writeEntry(myState, recover.getPendingDecision().getTransaction());
				}
			}
    }
  }

  /*
   *   From revive ask other for help
   * 
   */
  
  private void askOtherForHelp(MessageParser parser){
      parser.setMessageHeader(TransitionMsg.RECOVER_REQ.toString());
      parser.setSource(Integer.toString(myID));
      parser.setUpSet(upSet);
      // send everyone who is alive, just in my opnion
      for (int m : upSet){
        if(m!=myID){
          sendMsg(m, parser.composeWithUpset());
          //System.out.println("What I print is" + parser.composeMessage());
        }
      }
      myState = StateAC.WAIT_FOR_RECOVER_REP;
      dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
      //reconstruct the UP set
     
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
					if (strArr[0].equals("add"))
						ActionList.add(new MessageParser( Integer.toString(myID) + ";" + strArr[0] + ";" + strArr[1] + ";" + strArr[2] + ";"+ StateAC.IDLE.toString()+";"+TransitionMsg.CHANGE_REQ.toString()));
					if (strArr[0].equals("edit"))
						ActionList.add(new MessageParser( Integer.toString(myID) + ";" + strArr[0] + ";" + strArr[1] + "," + strArr[2] + ";" + strArr[3] + ";" + StateAC.IDLE.toString()+";"+TransitionMsg.CHANGE_REQ.toString()));
					if (strArr[0].equals("remove"))
						ActionList.add(new MessageParser( Integer.toString(myID) + ";" + strArr[0] + ";" + strArr[1] + ";" + strArr[2] + ";" + StateAC.IDLE.toString()+";"+TransitionMsg.CHANGE_REQ.toString()));
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
        	System.out.println("I receive change request");
        	myState = StateAC.START_3PC;
        	dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
        	parser.setMessageHeader(TransitionMsg.VOTE_REQ.toString());
        	parser.setUpSet(upSet);
        		for(int i : upSet){
        			if(i!=coordinator){
                        System.out.println("send vote request to " + parser.composeMessage());
        				sendMsg(i, parser.composeWithUpset() );
        			}
        		}
        		playlistVoteActionAsCoordinator(parser);
        		myState = StateAC.WAIT_FOR_VOTE_DEC;
        		dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
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
        //Receive recover request

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.RECOVER_REQ.toString())){
        	boolean ableToResponse = false; 
        	
        	upSet.add(Integer.parseInt(parser.getSource()));
        	for(MessageParser msgParser : oldDecisionList){
               if(isSameAction(parser, msgParser)){
            	 ableToResponse = true;
                 int j = Integer.parseInt(parser.getSource());
                 parser.setMessageHeader(TransitionMsg.RECOVER_REP.toString());
                 parser.setStateInfo(StateAC.COMMIT);
                 
                 parser.setUpSet(upSet);
                 sendMsg(j, parser.composeWithUpset());
                 System.out.println("I am helping others to revive"); 
               }
            }
        	
        	//dtLog.writeEntry(myState, parser);
        	
        	// Not able to response
        	if(!ableToResponse){
        		
        	}
        }
        //wrong message
        else{

        }
        
        // Decide whether to precommit or abort;
        
        Boolean ready = true;
        for(int j : upSet){
        	
        	if(j == coordinator)
        		continue;
        	if(DecisionList.get(j) != 0)
        		continue;
        	else
        		ready  = false;
        }
        //System.out.println(DecisionList);
        
        if(ready){
        	//abort
        	if(DecisionList.contains(-1)){
        		parser.setMessageHeader(TransitionMsg.ABORT.toString());
        		parser.setUpSet(upSet);
        		for(int j : upSet){
        			if(j != coordinator && DecisionList.get(j)==1){
        				sendMsg(j, parser.composeWithUpset());
        			}
        		}
        		//log
        		 
                 myState = StateAC.ABORT;
                 dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
        	}
        	//commit
        	else{
        		parser.setMessageHeader(TransitionMsg.PRECOMMIT.toString());
        		upSet = parser.getUpSet();
        		for(int j : upSet){
        			if(j != coordinator ){
        				sendMsg(j, parser.composeWithUpset());
        			}
        		}
        		//log
        
                 myState = StateAC.WAIT_FOR_ACKS;
                 dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
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
       //System.out.println("The Up set is "+ upSet);
        if(readyCommit){
        	parser.setMessageHeader(TransitionMsg.COMMIT.toString());
        	upSet = parser.getUpSet();
    		for(int j : upSet){
    			if(j != coordinator ){
    				sendMsg(j, parser.composeWithUpset());
    			}
    		}
    		//log
    		
            myState = StateAC.COMMIT;
            dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
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
                	upSet = parser.getUpSet();
                	myState = StateAC.DECIDE_YES;
                    dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                    
                    parser.setMessageHeader(TransitionMsg.YES.toString());
                    parser.setSource(Integer.toString(myID));
                    
                    sendMsg(coordinator,parser.composeWithUpset());
                    
                    myState = StateAC.UNCERTAIN;
                    dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                }
                //vote no
                else{
                	upSet = parser.getUpSet();
                	myState = StateAC.DECIDE_NO;
                    dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                    parser.setMessageHeader(TransitionMsg.NO.toString());
                    parser.setSource(Integer.toString(myID));
                    
                    sendMsg(coordinator,parser.composeWithUpset());
                    myState = StateAC.ABORT;
                    dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);

                }
        }
        //request for add
        else if (parser.getInstruction().equalsIgnoreCase("add")){
               String song = parser.getSong();
               //vote yes
               if(!playList.containsKey(song)){
            	   upSet = parser.getUpSet();
            	   myState = StateAC.DECIDE_YES;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                   parser.setMessageHeader(TransitionMsg.YES.toString());
                   parser.setSource(Integer.toString(myID));
                   
                   sendMsg(coordinator,parser.composeWithUpset());
                   myState = StateAC.UNCERTAIN;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
               }
               //vote no
               else{
            	   upSet = parser.getUpSet();
            	   myState = StateAC.DECIDE_NO;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                   parser.setMessageHeader(TransitionMsg.NO.toString());
                   parser.setSource(Integer.toString(myID));
                   
                   sendMsg(coordinator,parser.composeWithUpset());
                   myState = StateAC.ABORT;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);

               }
        }
        // request for delete
        else if (parser.getInstruction().equalsIgnoreCase("remove")){
               //vote yes
               String song = parser.getSong();
               if(playList.containsKey(song)){
            	   upSet = parser.getUpSet();
            	   myState = StateAC.DECIDE_YES;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                   parser.setMessageHeader(TransitionMsg.YES.toString());
                   parser.setSource(Integer.toString(myID));
                   
                   sendMsg(coordinator,parser.composeWithUpset());
                   myState =StateAC.UNCERTAIN;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
               }
               //vote no
               else{
            	   upSet = parser.getUpSet();
            	   myState = StateAC.DECIDE_NO;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                   parser.setMessageHeader(TransitionMsg.NO.toString());
                   parser.setSource(Integer.toString(myID));
                   
                   sendMsg(coordinator,parser.composeWithUpset());
                   myState = StateAC.ABORT;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
               }
        }
        // wrong vote request
        else {

        }
    }
    
    
    private void playlistVoteActionAsCoordinator(MessageParser parser){
   	 // edit request
       if (parser.getInstruction().equalsIgnoreCase("edit")){
               String song = parser.getOldSong();
               //vote yes
               if(playList.containsKey(song)){
               	
               	   myState = StateAC.DECIDE_YES;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                   DecisionList.set(coordinator, 1);
               }
               //vote no
               else{
               	   myState = StateAC.DECIDE_NO;
                   dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                   DecisionList.set(coordinator, -1);
               }
       }
       //request for add
       else if (parser.getInstruction().equalsIgnoreCase("add")){
              String song = parser.getSong();
              //vote yes
              if(!playList.containsKey(song)){
           	      myState = StateAC.DECIDE_YES;
                  dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                  DecisionList.set(coordinator, 1);
              }
              //vote no
              else{
           	      myState = StateAC.DECIDE_NO;
                  dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                  DecisionList.set(coordinator, -1);

              }
       }
       // request for delete
       else if (parser.getInstruction().equalsIgnoreCase("remove")){
              //vote yes
              String song = parser.getSong();
              if(playList.containsKey(song)){
           	      myState = StateAC.DECIDE_YES;
                  dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                  DecisionList.set(coordinator, 1);
              }
              //vote no
              else{
           	      myState = StateAC.DECIDE_NO;
                  dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
                  DecisionList.set(coordinator, -11);
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
        	   upSet = parser.getUpSet();
               String newsong = parser.getSong();
               String oldsong = parser.getOldSong();
               String url = parser.getUrl();
               edit(oldsong,newsong,url);
               myState = StateAC.COMMIT;
               dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);

        }
        //commit for add
        else if (parser.getInstruction().equalsIgnoreCase("add")){
        		upSet = parser.getUpSet();
                String song = parser.getSong();
                String url = parser.getUrl();
                add(song, url);
                myState = StateAC.COMMIT;
                dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
        }
        // commit for delete
        else if (parser.getInstruction().equalsIgnoreCase("remove")){
        		upSet = parser.getUpSet();
                String song = parser.getSong();
                remove(song);
                myState = StateAC.COMMIT;
                dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
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
                //System.out.println("receive vote request" + parser.composeMessage());
              	playlistVoteAction(parser);
     
        }

        //  Receive pre-commit message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.PRECOMMIT.toString())){
                // send the message
                parser.setMessageHeader(TransitionMsg.ACK.toString());
                parser.setSource(Integer.toString(myID));
                upSet = parser.getUpSet();
                
                sendMsg(coordinator,parser.composeWithUpset());
                myState = StateAC.COMMITABLE;
                dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
        }

        //Receive commit message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.COMMIT.toString())){
        		
             	playListFollowAction(parser);
                oldDecisionList.add(parser);
        }

        //  Receive Abort message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.ABORT.toString())){
                // log the message
        		myState = StateAC.ABORT;
        		upSet = parser.getUpSet();
        		dtLog.writeEntry(myState, parser.getTransaction()+";"+"UPset :"+upSet);
        }

        //Receive state request

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.STATE_REQ.toString())){
        	  //if coordinator is not working 
        	  if(!coordinatorWorking){
        	      System.out.println("Receive State Request"); 
        	      
        	      //set the new UP set
        	      upSet = parser.getUpSet();
        	      parser.setUpSet(upSet);
        	      // send participant state
        	      sendParticipantState(myState, parser);
        	      // sey coordinator working to true
        	   	  coordinatorWorking=true;
        	   }
        	   // the coordinator is still working
        	   else{
        		   coordinator = Integer.parseInt(parser.getSource());
        		   for(int j : upSet){
        			   if(j<coordinator)
        				   upSet.remove(j);
        		   }
        	   }
        }
        
        //Receive recover request

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.RECOVER_REQ.toString())){
        	boolean ableToResponse = false; 
        	
        	upSet.add(Integer.parseInt(parser.getSource()));
        	for(MessageParser msgParser : oldDecisionList){
               if(isSameAction(parser, msgParser)){
            	 ableToResponse = true;
                 int j = Integer.parseInt(parser.getSource());
                 parser.setMessageHeader(TransitionMsg.RECOVER_REP.toString());
                 parser.setStateInfo(StateAC.COMMIT);
                 
                 parser.setUpSet(upSet);
                 sendMsg(j, parser.composeWithUpset());
                 System.out.println("I am helping others to revive"); 
               }
            }
        	
        	//dtLog.writeEntry(myState, transaction)
        	
        	// Not able to response
        	if(!ableToResponse){
        		
        	}
        }

         //Receive recover response

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.RECOVER_REP.toString())){
          // get the decision only when there is pending info and no total failure happens
            
          //Before make decision, first to make sure I am the last process to fail
         boolean lastProces = false;
         HashSet<Integer> unionHashSet = new HashSet<Integer>();
         if(isSubset(parser.getUpSet(),upSet))
         lastProces = true;
         
         recoveryUpSets.add(parser.getUpSet());
         unionHashSet = parser.getUpSet();
         
         for (HashSet<Integer> m : recoveryUpSets){
        	  unionHashSet = unionOfHashSet(unionHashSet, m);
         }
         System.out.println("The union set is"+ unionHashSet);
         if(isSubset(unionHashSet, upSet))
        	 lastProces = true;
         
         // If I am the last process to fail
                 
         if(pending && lastProces){    
             if(parser.getStateInfo()==StateAC.COMMIT){
                 upSet = parser.getUpSet();
                 myState = StateAC.COMMIT;
                 dtLog.writeEntry(myState, parser.getTransaction()+"; UPset :"+ upSet);
                 oldDecisionList.add(parser);
                 pending = false;
                 System.out.println("My pending decision is resolved");
              } 
            recoveryUpSets.clear();
          }
          
        }
        
        //Receive UR_ELECTED message, I am new coordinator now

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.UR_ELECTED.toString())){
        	    System.out.println("Hahaha I am "+ Integer.toString(myID) + "new leader");
        	    coordinatorWorking = true;
        	    //I am new Coordinator
        	    if(myID != coordinator) 
        	    	System.out.println("Seems something wrong happens, sinces I am not the coordinator");
        	    // update the UP set
        	    upSet = parser.getUpSet();
        	    //System.out.println("my new UP set is " + upSet);
        	    //Send out the State_REQ
        	    parser.setUpSet(upSet);
        	    sendStateReq(parser);
        	    
        	    // change my state and DT log that
        	    
        	    myState = StateAC.WAIT_FOR_STATE_RES;
        	    dtLog.writeEntry(myState, parser.getTransaction()+";"+ "UPset :" + upSet);
        	    //And now I am new Coordinator
        	    getMessageAsCoordinator();
        }
        
        
        // Receive Wrong Message
        else{

        }
    }

    
    
    /*
     *  Check whether 2 hashset has belong relationship
     *   
     */
    
    private boolean isSubset (HashSet<Integer> a, HashSet<Integer> b) {
		  	boolean result = true;
		  	
		  	for(int tmp : a){
		  		if(! b.contains(tmp))
		  			result = false;
		  	}
		  	
		  	return result;
	}
    
    /*
     * 
     * Get the Union of 2 Hashset
     * 
     * */
    
    private HashSet<Integer> unionOfHashSet(HashSet<Integer> a, HashSet<Integer> b){
    	   HashSet<Integer> result = new HashSet<Integer>();
    	   for(int j : a){
    		    if(b.contains(j)){
    		    	result.add(j);
    		    }
    	   }
    	   return result;
    }
    
    
    
    /*
     *  Check Whether the 2 parser contain same action
     */
    
    private boolean isSameAction(MessageParser a, MessageParser b){
        boolean result = true;
        if(!a.getInstruction().equals(b.getInstruction()))
          result = false;
        
        if(!a.getSong().equals(b.getSong()))
          result = false;
        
        if(!a.getUrl().equals(b.getUrl()))
          result = false;
        
        return result;
      
    }

	/*
	 *   Get the ID
	 */
	public int getID() {
		return nc.getConfig().procNum;
	}
	
	/*
	 *   Get the State
	 */
	public StateAC getState() {
		return myState;
	}
	
	/*
	 *   Get the number of process
	 */
	public int getNumProcesses() {
		return nc.getConfig().numProcesses;
	}
	
	/*
	 *   Get the Time out
	 */
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
	 *   Check and handle any controller directives
	 */
	private void checkForControllerDirectives() {
		 
		 try {
			if (inputFromController.ready()) { 
				String incoming = inputFromController.readLine();
				String[] strArr = incoming.split(" ");
				if (strArr[0].equals("partialMessage"))
					setMsgBound(Integer.parseInt(strArr[1]));
				if (strArr[0].equals("resumeMessages"))
					setMsgBound(Integer.MAX_VALUE);
				//if (strArr[0].equals("allClear"))
					//stuff
				//if (strArr[0].equals("rejectNextChange"))
					//stuff
			 }
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*
	 * Get message using polling when there is no failure
	 */
	
	private void getMessageAsCoordinator() throws InterruptedException{
		System.out.println("Function call "+ Integer.toString(myID)); 
		while(true){
			 
			 //first check to see if any incoming messages from controller
			 checkForControllerDirectives();
			// System.out.println("I am here");
             List<String> messages = null;
             ArrayList<MessageParser> stateReqList = new ArrayList<MessageParser>();
             long startTime = (System.currentTimeMillis()+getTimeOut());
             long smallTimeout = getTimeOut()/10;
             Boolean atLeastOneBoolean= false;
             Boolean collectAllStateBoolean= false;
             HashSet<Integer> tmp2 = new HashSet<Integer>();
             tmp2.add(myID);
             // receive message until getTimeOut() and there is no failure
             while(System.currentTimeMillis() < startTime) {
                 Thread.sleep(smallTimeout);
                 messages = (nc.getReceivedMsgs());
                
                 for(String m:messages) {
                	currentAction = new MessageParser(m);
                	if(currentAction.getMessageHeader().equals(TransitionMsg.STATE_RES.toString())){
                		tmp2.add(Integer.parseInt(currentAction.getSource()));
                		stateReqList.add(currentAction);
                	}
                	else {
                		System.out.println("receive as coordinator" + m);
                		processReceivedMsgAscoordinator(m);
					}
                    
               	    //System.out.println("I am Here");
                	//System.out.println("tmp is " + tmp2);
                    if(isSubset(upSet, tmp2)){
                    	collectAllStateBoolean = true;
                    	tmp2.clear();
                    }
                    atLeastOneBoolean = true;
                 }
             }
             // if there is message comming, and the message is about the state response
             if(collectAllStateBoolean && myState==StateAC.WAIT_FOR_STATE_RES){
            	 TransitionMsg header = terminationRule(myState, stateReqList);
           	  	 System.out.println("the decision made on collection is " + header.toString());
           	  	 MessageParser actionMessageParser = new MessageParser();
           	  	 actionMessageParser =stateReqList.get(1);
           	  		 	
           	  	 actionMessageParser.setMessageHeader(header.toString());
           	  	 upSet = actionMessageParser.getUpSet();
           	  	 // if decision is Abort
           	  	 if(header == TransitionMsg.ABORT){
           	  		
           	  		 for(MessageParser tmp : stateReqList){
           	  			 int j = Integer.parseInt(tmp.getSource());
           	  			 	sendMsg(j, actionMessageParser.composeWithUpset());
           	  		 }
           	  		 
           	  		 myState  = StateAC.ABORT;
           	  		 dtLog.writeEntry(myState, actionMessageParser.getTransaction()+";"+"UPset :"+upSet);
           	  	 }   
           	  	
           	  	 // if the decision is Commit 
           	    if(header == TransitionMsg.COMMIT){
        	  		 upSet = actionMessageParser.getUpSet();
        	  		 for(MessageParser tmp : stateReqList){
         	  			 int j = Integer.parseInt(tmp.getSource());
         	  				 sendMsg(j, actionMessageParser.composeWithUpset());
         	  		 }
        	  		 
        	  		myState  = StateAC.COMMIT;
        	  		dtLog.writeEntry(myState, actionMessageParser.getTransaction()+";"+"UPset :"+upSet);
        	  	 }   
           	    
           	    // if the decision is Precommit
           	   if(header == TransitionMsg.PRECOMMIT){
           		   boolean isUncertain = false;
           		   actionMessageParser.setUpSet(upSet);
           		   // Sent every uncertain case a Precommit info
           		   for(MessageParser tmp : stateReqList){
      	  			 if(tmp.getStateInfo() == StateAC.UNCERTAIN ){
      	  				 int j = Integer.parseInt(tmp.getSource());
      	  				 sendMsg(j, actionMessageParser.composeWithUpset());
      	  				 System.out.println("Re issue precommit"+ actionMessageParser.composeMessage());
      	  				 isUncertain = true;
      	  			 }
           		   }
      	  		 
      	  		  // Corner case, if everyone is commitable, send commit
      	  		  if(!isUncertain){
      	  			 actionMessageParser.setMessageHeader(TransitionMsg.COMMIT.toString());
      	  			 actionMessageParser.setUpSet(upSet);
      	  			 for(MessageParser tmp : stateReqList){
      	  				 if(tmp.getStateInfo() == StateAC.COMMITABLE ){
      	  				 	int j = Integer.parseInt(tmp.getSource());
      	  				 	sendMsg(j, actionMessageParser.composeWithUpset());      	  		
      	  				 }
      	  			 }
      	  			 myState  = StateAC.COMMIT;
    	  			 dtLog.writeEntry(myState, actionMessageParser.getTransaction()+";"+"UPset :"+upSet);

      	  		  }else{
      	  			 myState  = StateAC.WAIT_FOR_ACKS;
      	  			 dtLog.writeEntry(myState, actionMessageParser.getTransaction()+";"+"UPset :"+upSet);
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
            	  
            	  //update the decision list
            	  Iterator<Integer> iterator = upSet.iterator();
            	  while (iterator.hasNext()) {
            	      Integer element = iterator.next();
            	      if (DecisionList.get(element) == 0) {
            	          iterator.remove();
            	      }
            	  }
            	  //clear the decision list
            	  for(int p = 0 ; p < config.numProcesses;p++){
            		   DecisionList.set(p, 0);
            	  }
            	  
            	  currentAction.setUpSet(upSet);
          		  for(int j :upSet){
          		 	if(j != coordinator ){
          				sendMsg(j, currentAction.composeWithUpset());
          			}
          		  }
            	  myState = StateAC.ABORT;
            	  dtLog.writeEntry(myState, currentAction.getTransaction()+";"+"UPset :" +upSet );
             }
             
             // if there is no ACK come back
             else if (!atLeastOneBoolean && myState == StateAC.WAIT_FOR_ACKS){
            	 System.out.println("Time out action for coordinator ACK");
            	 currentAction.setMessageHeader(TransitionMsg.COMMIT.toString());
            	 
            	  //update the decision list
            	 Iterator<Integer> iterator = upSet.iterator();
           	     while (iterator.hasNext()) {
           	       Integer element = iterator.next();
           	       if (ACKList.get(element) == 0) {
           	          iterator.remove();
           	       }
           	     }
           	  	//clear the decision list
           	  	for(int p = 0 ; p < config.numProcesses;p++){
           	  		ACKList.set(p, 0);
           	  	}
           	    // send the data
           	  	currentAction.setUpSet(upSet);
         		for(int j : upSet){
         		 	if(j != coordinator ){
         				sendMsg(j, currentAction.composeWithUpset());
         			}
         		 }
         		 //log
         		
                myState = StateAC.COMMIT;
                dtLog.writeEntry(myState, currentAction.getTransaction()+";"+"UPset :" +upSet);
             }
             else if (!atLeastOneBoolean){
            	 System.out.println("I am Here "+ Integer.toString(myID));
            	 myState = StateAC.IDLE;
            	 break;
             }
             
		 }
	}
	
	/*
	 * 
	 * Time out action when there is a failure
	 * 
	 */
	 private void getMessagesAsParticipant() throws InterruptedException {
		  System.out.println("Function call "+ Integer.toString(myID));
          while(true){

  			 //first check to see if any incoming messages from controller
  			 checkForControllerDirectives();
        	  
        	  List<String> messages ;
              long startTime = (System.currentTimeMillis()+getTimeOut()*11/10);
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
                		  // System.out.println("Receive message :  "+ messages);
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
            	  	  coordinatorWorking = false;
            	  	  upSet.remove(coordinator);
            	      myState = StateAC.ABORT;
            	      // update UP set
            	      currentAction.setUpSet(upSet);
            	      dtLog.writeEntry(myState, "UPset :"+upSet);
              }
              
              // wait for Precommit message from coordinator
              else if(!atleastone && myState==StateAC.UNCERTAIN){
            	      System.out.println("Participant wait for Coordinator's Precommit");
            	      coordinatorWorking=false;
            	      //remove coordinator in UP set
            	      upSet.remove(coordinator);
            	      //run election protocol  and send state request
            	      currentAction.setUpSet(upSet);
            	      int newCoor = electionProtocol(currentAction);
            	      
            	      //System.out.println("I am " + myID + " new coordinator is " + Integer.toString(newCoor));
            	    
              }
              
              // wait for commit message from coordinator
              else if(!atleastone && myState==StateAC.COMMITABLE){
            	  	  System.out.println("Participant wait for Coordinator's Commit");
            	  	  coordinatorWorking=false;
            	  	  upSet.remove(coordinator);
            	  	  currentAction.setUpSet(upSet);
            	  	  //run election protocol  and send state request
            	  	  int newCoor = electionProtocol(currentAction);
              }
              
              
             // wait for recovery response from others
             // if there is no response until timeout, then total failure is happend
              else if(!atleastone && myState==StateAC.WAIT_FOR_RECOVER_REP){
            	  	  System.out.println("Seems total failure happens");
            	  	  TotalFailure = true;
            	  	  coordinatorWorking=false;
            	  	  upSet.clear();
            	  	  upSet.add(myID);
            	  	  
            	  	  
              }
              
              else if (!atleastone){
            	 System.out.println("I am Here "+ Integer.toString(myID));
             	 myState = StateAC.IDLE;
             	 break;
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
	        upSet = stResponse.getUpSet();
	        String stateResponse = stResponse.composeWithUpset();

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

	  public void setMsgBound(int numMsgs) {		  
		  msgBound = msgCount + numMsgs;
	  }
	  
	  public void rmMsgBound() {		  
		  msgBound = Integer.MAX_VALUE;
	  }
	  
	  public void sendMsg(int procID, String msg) {
		    System.out.println(myID+" msgCount:"+msgCount+"; msgBound:"+msgBound);
		  	while(msgCount >= msgBound);			//no messages sent until 
			this.nc.sendMsg(procID, msg);			//bound adjusted
			//System.out.println("send message to " + Integer.toString(procID));
			msgCount++;
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
   
		// revival start
		if(revival){
			n.getMessagesAsParticipant();
		}else{
			// fresh start
			n.readActions("ActionList.txt");
			while(true){
				if(n.myID==1){
							
					if(!n.ActionList.isEmpty()){
						MessageParser parser = n.ActionList.getFirst();
						System.out.println("I am sending out" + parser.composeMessage()+" to "+Integer.toString(n.coordinator));
						n.sendMsg(n.coordinator,parser.composeMessage());
						n.ActionList.removeFirst();
					}
				}
				
				if(n.myID == n.coordinator){
					n.getMessageAsCoordinator();
					Thread.sleep(n.getTimeOut()/10);
				}
				else{
					n.getMessagesAsParticipant();
				}
				
      
			}
    }
  }
  
}