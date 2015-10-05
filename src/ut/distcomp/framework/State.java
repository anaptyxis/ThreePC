package ut.distcomp.framework;

/**
 * Created by zhangtian on 10/2/15.
 */
public enum State {
    START_3PC,
    WAIT_FOR_VOTE,
    SEND_PRECOMMIT,
    WAIT_FOR_ACKS,
    COMMIT,
    INVALID,
    ABORT;


    private String string;

    static {
        START_3PC.string="start";
        WAIT_FOR_VOTE.string="wait_for_vote";
        SEND_PRECOMMIT.string="send_precommit";
        WAIT_FOR_ACKS.string ="wait_for_acks";
        COMMIT.string="commit";
        INVALID.string="invalid";
        ABORT.string="abort";
    }

    public String toString() {
        return string;
    }
}
