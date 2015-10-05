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
	private int viewNumber;
	private int coordinator;
	private HashSet<Integer> up;
	private DTLog dtLog;
	private boolean running;   //only altered if process shuts down gracefully
	private int myID;
    private State myState = State.IDLE;
    private ArrayList<Integer> DecisionList=new ArrayList<Integer>(viewNumber);
    private ArrayList<Integer> ACKList=new ArrayList<Integer>(viewNumber);
	
	public Node(String configName, String dtL) {
		try {
			config = new Config(configName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		nc = new NetController(config);
		playList = new Hashtable<String,String>();
		up = new HashSet<Integer>();
		dtLog = new DTLog(dtL);
		running = true;
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
	
	public void processReceivedMsg() {
        //return this.nc.getReceivedMsgs();
        List<String> messages = (nc.getReceivedMsgs());

        for (String m : messages){
             if(myID == coordinator){
                    processReceivedMsgAscoordinator(m);
             }else{
                    processReceivedMsgAsParticipant(m);
             }
        }
    }

    /*
    *
    * Process received message as coordinate
    *
    */
    private void processReceivedMsgAscoordinator(String message){
        MessageParser parser = new MessageParser(message);
        // start 3 PC
        if (parser.getMessageHeader().toString().equals(TransitionMsg.CHANGE_REQ.toString())){

        }
        // receive VOTE_DEC YES
        else if (parser.getMessageHeader().toString().equals(TransitionMsg.YES.toString()) ){

        }

        else if( parser.getMessageHeader().toString().equals(TransitionMsg.NO.toString())){

        }
        // receive ACK
        else if (parser.getMessageHeader().toString().equals(TransitionMsg.ACK.toString())){

        }

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

                // edit request
                if (parser.getInstruction().equalsIgnoreCase("edit")){
                        String song = parser.getOldSong();
                        //vote yes
                        if(playList.containsKey(song)){
                            parser.setMessageHeader(TransitionMsg.YES.toString());
                            parser.setSource(Integer.toString(myID));
                            sendMsg(coordinator,parser.composeMessage());
                        }
                        //vote no
                        else{
                            parser.setMessageHeader(TransitionMsg.NO.toString());
                            parser.setSource(Integer.toString(myID));
                            sendMsg(coordinator,parser.composeMessage());
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
                       }
                       //vote no
                       else{
                           parser.setMessageHeader(TransitionMsg.NO.toString());
                           parser.setSource(Integer.toString(myID));
                           sendMsg(coordinator,parser.composeMessage());

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
                       }
                       //vote no
                       else{
                           parser.setMessageHeader(TransitionMsg.NO.toString());
                           parser.setSource(Integer.toString(myID));
                           sendMsg(coordinator,parser.composeMessage());
                       }
                }
                // wrong vote request
                else{

                }
        }

        //  Receive pre-commit message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.PRECOMMIT.toString())){
                // log the message

                // send the message
                parser.setMessageHeader(TransitionMsg.ACK.toString());
                parser.setSource(Integer.toString(myID));
                sendMsg(coordinator,parser.composeMessage());
        }

        //Receive commit message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.COMMIT.toString())){
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

        //  Receive Abort message

        else if (parser.getMessageHeader().toString().equals(TransitionMsg.ABORT.toString())){
                // log the message
        }

        // Receive Wrong Message
        else{

        }
    }


    /*
    *
    * Sending messsage
    *
    */

	//accessors & mutators
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

	public int getID() {
		return this.nc.getConfig().procNum;
	}
	
	public static void main(String[] args) {
		
		Node n = new Node(args[0],args[1]);
		while (n.running); //until graceful shutdown
	}
	
}