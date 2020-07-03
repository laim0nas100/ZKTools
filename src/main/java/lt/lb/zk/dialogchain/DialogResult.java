package lt.lb.zk.dialogchain;

import lt.lb.zk.DialogDTO;
import java.util.Objects;

/**
 *
 * @author laim0nas100
 */
public class DialogResult<INFO> {

    public final boolean earlyExit;
    public final DialogDTO<INFO> future;
    public final DialogExit exit;

    public DialogResult(DialogDTO<INFO> info, DialogExit exit) {
        this.future = Objects.requireNonNull(info);
        this.exit = exit;
        this.earlyExit = exit.disposeDialog;
    }

    public DialogResult(DialogExit exit) {
        this(DialogDTO.ofFutureResult(), exit);
    }

    public INFO getInfo() {
        return future.get();
    }
}
