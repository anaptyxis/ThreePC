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
	
	public void add(String songName, String URL) {
		
		
	}
	
	public void remove(String songName) {
		
		
	}
	
	public void edit(String songName, String newSongName,  String newSongURL) {
		
		
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