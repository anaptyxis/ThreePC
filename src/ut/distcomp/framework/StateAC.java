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
    UNCERTAIN,
    COMMITABLE,
    COMMIT,
    ABORT;


    private String string;

    static {
        IDLE.string = "idle";
        START_3PC.string="start";
        WAIT_FOR_VOTE_REQ.string="wait_for_vote_req";
        WAIT_FOR_VOTE_DEC.string="wait_for_vote_dec";
        WAIT_FOR_STATE_RES.string = "wait_for_state_res";
        WAIT_FOR_ACKS.string ="wait_for_acks";
        UNCERTAIN.string = "uncertain";
        COMMITABLE.string = "commitable";
        COMMIT.string="commit";
        ABORT.string="abort";
    }

    public String toString() {
        return string;
    }
}
