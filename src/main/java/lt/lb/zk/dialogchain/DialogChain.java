package lt.lb.zk.dialogchain;

import lt.lb.zk.DialogDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lt.lb.commons.F;
import lt.lb.commons.func.unchecked.UnsafeConsumer;
import lt.lb.commons.func.unchecked.UnsafeRunnable;
import lt.lb.zk.DWindow;
import org.zkoss.zk.ui.Component;

/**
 *
 * @author laim0nas100
 */
public class DialogChain {


    public DialogChain(Component parentComponent) {
        this.parentComponent = parentComponent;
    }

    protected Component parentComponent;

    protected List<DWindow> windows = new ArrayList<>();

    public void dialog(DWindow window) throws Exception {
        windows.add(window);
        window.setParent(parentComponent);
        window.showModal();
    }

    public boolean detach(DWindow win) {
        boolean remove = windows.remove(win);
        win.detach();
        return remove;
    }

    public void detachAll() {
        F.iterateBackwards(windows, (i, d) -> {
            d.detach();
        });
        windows.clear();
    }

    public <OPT, INFO> ChainDialogPattern<OPT, INFO> then(
            Consumer<ChainDialogPattern<OPT, INFO>> decor,
            UnsafeConsumer<DialogResult<INFO>> consumer) throws Exception {

        ChainDialogPattern<OPT, INFO> pattern = new ChainDialogPattern<>(parentComponent);
        pattern.future = DialogDTO.ofFutureResult();
        pattern.continuation = result -> {
            if (result.exit.disposeDialog) {
                this.detach(pattern.dialog);
            }
            consumer.accept(new DialogResult<>(pattern.future, result.exit));
        };
        decor.accept(pattern);
        this.dialog(pattern.dialog);

        return pattern;

    }

    public <OPT, INFO> UnsafeRunnable thenRunnable(
            Consumer<ChainDialogPattern<OPT, INFO>> decor,
            UnsafeConsumer<DialogResult<INFO>> consumer) throws Exception {

        return () -> {
            then(decor, consumer);
        };

    }

    public void thenDelegated(UnsafeConsumer<UnsafeRunnable> construction, UnsafeRunnable continuation) throws Exception {
        construction.accept(continuation);
    }

    public static DialogChain start(Component comp) {
        return new DialogChain(comp);
    }

}
