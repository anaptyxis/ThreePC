package ut.distcomp.framework;

/**
 * Created by zhangtian on 10/2/15.
 */
public enum StateAC {
    IDLE,
    START_3PC,
    WAIT_FOR_VOTE_REQ,
    WAIT_FOR_VOTE_DEC,
    WAIT_FOR_ACKS,
    WAIT_FOR_STATE_RES,
    DECIDE_YES,
    DECIDE_NO,
    UNCERTAIN,
    COMMITABLE,
    COMMIT,
    ABORT,
    WAIT_FOR_RECOVER_REP;


    private String string;

    static {
        IDLE.string = "idle";
        START_3PC.string="start";
        WAIT_FOR_VOTE_REQ.string="wait_for_vote_req";
        WAIT_FOR_VOTE_DEC.string="wait_for_vote_dec";
        WAIT_FOR_STATE_RES.string = "wait_for_state_res";
        WAIT_FOR_ACKS.string ="wait_for_acks";
        DECIDE_YES.string = "decision_yes";
        DECIDE_NO.string = "decision_no";
        UNCERTAIN.string = "uncertain";
        COMMITABLE.string = "commitable";
        COMMIT.string="commit";
        ABORT.string="abort";
        WAIT_FOR_RECOVER_REP.string = "wait_for_recover_rep";
    }

    public String toString() {
        return string;
    }
}
