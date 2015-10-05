package ut.distcomp.framework;

/**
 * Created by zhangtian on 10/2/15.
 */
public enum StateAC {
    IDLE,
    START_3PC,
    WAIT_FOR_VOTE,
    SEND_PRECOMMIT,
    WAIT_FOR_ACKS,
    UNCERTAIN,
    COMMITABLE,
    COMMIT,
    ABORT;


    private String string;

    static {
        IDLE.string = "idle";
        START_3PC.string="start";
        WAIT_FOR_VOTE.string="wait_for_vote";
        SEND_PRECOMMIT.string="send_precommit";
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
