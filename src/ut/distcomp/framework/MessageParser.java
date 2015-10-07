package ut.distcomp.framework;
import java.util.Arrays;
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
    private HashSet<Integer> upSet;


    public MessageParser(){

    }

    public MessageParser(String input) {
        splitMessage(input);
    }

    public String composeMessage(){
        String result;

        if(instruction.equalsIgnoreCase("edit")) {
            result = source + DELIMITER + instruction + DELIMITER + old_song + DELIMITER1 + song + DELIMITER + url + DELIMITER + messageHeader;
        } else {
            result = source + DELIMITER + instruction + DELIMITER + song + DELIMITER + url + DELIMITER + messageHeader;
        }

        return result;
    }

    public void splitMessage(String input) {
        String[] split_input = input.split(DELIMITER);
        source = split_input[0];
        instruction = split_input[1];

        if(instruction.equalsIgnoreCase("edit")) {
            String[] editSong = split_input[2].split(DELIMITER1);
            old_song = editSong[0];
            song = editSong[1];

            url =split_input[3];

        } else {
            song = split_input[2];
            url = split_input[3];
        }
        if(split_input.length < 5) {
            messageHeader="";
        } else {
            messageHeader = split_input[4];
        }
        if(split_input.length < 6) {
            upSet = new HashSet<Integer>();
        } else {
            initHashSet(split_input[5]);
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
    

    public String composeWithUpset() {
        String result;

        if(instruction.equalsIgnoreCase("edit")) {
            result = source + DELIMITER + instruction + DELIMITER + old_song + DELIMITER1 + song + DELIMITER +
                        url + DELIMITER + messageHeader + DELIMITER + upSet.toString();
        } else {
            result = source + DELIMITER + instruction + DELIMITER + song + DELIMITER + url + DELIMITER +
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
}
