package ut.distcomp.framework;

/**
 * Created by zhangtian on 10/2/15.
 */

 public enum TransitionMsg {
        VOTE_REQ,
        VOTE,
        PRECOMMIT,
        ACK,
        COMMIT,
        ABORT,
        YES,
        NO;


        private String string;
        static {
            VOTE_REQ.string="vote_req";
            VOTE.string="vote";
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

