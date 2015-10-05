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
        NO,
        STATE_REQ,
        STATE_RES,
        ELECTION,
        RECOVER_REQ;


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
            STATE_REQ.string ="state_req";
            STATE_RES.string ="state_res";
            ELECTION.string = "election";
            RECOVER_REQ.string = "recover_req"
;        }
        public String toString() {
            return string;
        }
   }

