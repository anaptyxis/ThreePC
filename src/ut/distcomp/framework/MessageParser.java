package ut.distcomp.framework;
import java.util.HashSet;
/**
 * Created by zhangtian on 10/2/15.
 */
public class MessageParser {
    private final String DELIMITER=";";
    private final String DELIMITER1=",";
    private String source;
    private String instruction;
    private String old_song;
    private String song;
    private String url;
    private String messageHeader;
    private String stateinfo;
    private HashSet<Integer> upSet;


    public MessageParser(){

    }

    public MessageParser(String input) {
        splitMessage(input);
    }

    public String composeMessage(){
        String result;

        if(instruction.equalsIgnoreCase("edit")) {
            result = source + DELIMITER + instruction + DELIMITER + old_song + DELIMITER1 + song + DELIMITER + url + DELIMITER+ stateinfo+ DELIMITER + messageHeader;
        } else {
            result = source + DELIMITER + instruction + DELIMITER + song + DELIMITER + url + DELIMITER + stateinfo + DELIMITER +messageHeader;
        }

        return result;
    }

    public void splitMessage(String input) {
        String[] split_input = input.split(DELIMITER);
        source = split_input[0];
        instruction = split_input[1];
        stateinfo = split_input[4];

        if(instruction.equalsIgnoreCase("edit")) {
            String[] editSong = split_input[2].split(DELIMITER1);
            old_song = editSong[0];
            song = editSong[1];

            url =split_input[3];

        } else {
            song = split_input[2];
            url = split_input[3];
        }
        if(split_input.length < 6) {
            messageHeader="";
        } else {
            messageHeader = split_input[5];
        }
        if(split_input.length < 7) {
            upSet = new HashSet<Integer>();
        } else {
            initHashSet(split_input[6]);
        }

    }
    
    public void initHashSet(String input) {
        input = input.replace("[", "") ;
        input = input.replace("]", "") ;
        input = input.replaceAll(" ", "");

        String[] items=input.split(",");
        upSet = new HashSet<Integer>();

        for(String item: items) {
            upSet.add(Integer.valueOf(Integer.parseInt(item)));
        }
    }
    /*
     *      IDLE,
		    START_3PC,
		    WAIT_FOR_VOTE_REQ,
		    WAIT_FOR_VOTE_DEC,
		    WAIT_FOR_ACKS,
		    WAIT_FOR_STATE_RES,
		    UNCERTAIN,
		    COMMITABLE,
		    COMMIT,
		    ABORT;
     * 
     */
    public StateAC getStateInfo() {
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
    

    public String composeWithUpset() {
        String result;

        if(instruction.equalsIgnoreCase("edit")) {
            result = source + DELIMITER + instruction + DELIMITER + old_song + DELIMITER1 + song + DELIMITER +
                        url + DELIMITER + stateinfo+ DELIMITER  + messageHeader + DELIMITER + upSet.toString();
        } else {
            result = source + DELIMITER + instruction + DELIMITER + song + DELIMITER + url + DELIMITER + stateinfo + DELIMITER +
                        messageHeader + DELIMITER + upSet.toString();
        }

        return result;
    }

    public void setUpSet(HashSet<Integer> list) {
        upSet = list;
    }

    public HashSet<Integer> getUpSet() {
        return upSet;
    }


    public String getTransaction() {
        String result="";
        if(instruction.equalsIgnoreCase("edit")) {
            result = DELIMITER + instruction + DELIMITER + old_song + DELIMITER1 + song + DELIMITER + url + DELIMITER;
        } else {
            result = DELIMITER + instruction + DELIMITER + song + DELIMITER + url + DELIMITER;
        }
        return result;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setMessageHeader(String messageHeader){
        this.messageHeader = messageHeader;
    }

    public void setStateInfo(StateAC state) {
		this.stateinfo = state.toString();
	}
    public String getSource(){
        return source;
    }
    public String getInstruction(){
        return instruction;
    }
    public String getSong(){
        return song;
    }
    
    

    public String getOldSong() {
        return old_song;
    }
    public String getUrl(){
        return url;
    }
    public String getMessageHeader(){
        return messageHeader;
    }
    
    /*
     *  Set song URL and action 
     */
    
    public void setAction(String act) {
		this.instruction = act;
   	}
    
    public void setSong(String songName) {
		this.song = songName;
   	}
    
    public void setURl(String urlString) {
    	this.url = urlString;
	}
}
