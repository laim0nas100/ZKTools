package lt.lb.zk.dialogchain;

/**
 *
 * @author laim0nas100
 */
public class DialogExit {

    public final boolean disposeDialog;
    public final boolean okPressed;

    public DialogExit(boolean okPressed, boolean disposeDialog) {
        this.disposeDialog = disposeDialog;
        this.okPressed = okPressed;
    }
}
