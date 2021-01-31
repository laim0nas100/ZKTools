package lt.lb.zk.dialogchain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lt.lb.commons.func.unchecked.UncheckedConsumer;
import lt.lb.commons.func.unchecked.UncheckedRunnable;
import lt.lb.commons.iteration.For;
import lt.lb.zk.DWindow;
import lt.lb.zk.DialogDTO;
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
        For.elements().iterateBackwards(windows, (i, d) -> {
            d.detach();
        });
        windows.clear();
    }

    public <OPT, INFO> ChainDialogPattern<OPT, INFO> then(
            Consumer<ChainDialogPattern<OPT, INFO>> decor,
            UncheckedConsumer<DialogResult<INFO>> consumer) throws Exception {

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

    public <OPT, INFO> UncheckedRunnable thenRunnable(
            Consumer<ChainDialogPattern<OPT, INFO>> decor,
            UncheckedConsumer<DialogResult<INFO>> consumer) throws Exception {

        return () -> {
            then(decor, consumer);
        };

    }

    public void thenDelegated(UncheckedConsumer<UncheckedRunnable> construction, UncheckedRunnable continuation) throws Exception {
        construction.accept(continuation);
    }
    
    public void thenDelegated(UncheckedRunnable construction) throws Exception {
        construction.run();
    }

    public static DialogChain start(Component comp) {
        return new DialogChain(comp);
    }

}
