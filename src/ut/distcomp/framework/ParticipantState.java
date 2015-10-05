package ut.distcomp.framework;

/**
 * Created by zhangtian on 10/2/15.
 */
public enum  ParticipantState {
    WAIT_FOR_VOTE_REQ,
    SEND_VOTE,
    UNCERTAIN,
    SEND_ACK,
    COMMITTABLE,   // after sending an ACK
    COMMITTED,
    ABORTED,
    INVALID,
    STATE_RES;


    private String string;

    static {
        WAIT_FOR_VOTE_REQ.string="wait_for_vote_req";
        UNCERTAIN.string="uncertain";
        ABORTED.string="aborted";
        COMMITTABLE.string="committable";
        COMMITTED.string="commited";
        INVALID.string="invalid";
        SEND_ACK.string="send_ack";
        SEND_VOTE.string="send_vote";
        STATE_RES.string="state_res";
    }

    public String toString() {
        return string;
    }
}
