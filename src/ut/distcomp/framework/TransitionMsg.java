package ut.distcomp.framework;

/**
 * Created by zhangtian on 10/2/15.
 */

 public enum TransitionMsg {
	    CHANGE_REQ,
        START_3PC,
        VOTE_REQ,
        PRECOMMIT,
        ACK,
        COMMIT,
        ABORT,
        YES,
        NO;


        private String string;
        static {
        	CHANGE_REQ.string = "change_req";
            START_3PC.string= "start_3pc";
            VOTE_REQ.string="vote_req";
            PRECOMMIT.string="precommit";
            ACK.string="ack";
            COMMIT.string="commit";
            ABORT.string="abort";
            YES.string="yes";
            NO.string="no";
        }
        public String toString() {
            return string;
        }
   }

