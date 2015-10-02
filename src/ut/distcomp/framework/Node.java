package ut.distcomp.framework;

import java.io.IOException;
import java.util.ArrayList;
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
	private DTLog dtLog;
	
	public Node(String configName, String dtL) {
		try {
			config = new Config(configName);
		} catch (IOException e) {
			e.printStackTrace();
		}
		nc = new NetController(config);
		playList = new Hashtable<String,String>();
		dtLog = new DTLog(dtL);
		
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
			playList.replace(songName, newSongURL);
			if (!songName.equals(newSongName))
				playList.put(newSongName, (playList.remove(songName)));
			return true;
		}
	}
	
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
	}
	
	
}