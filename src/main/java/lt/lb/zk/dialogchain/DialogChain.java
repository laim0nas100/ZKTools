package lt.lb.zk.dialogchain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lt.lb.commons.iteration.For;
import lt.lb.uncheckedutils.func.UncheckedConsumer;
import lt.lb.uncheckedutils.func.UncheckedRunnable;
import lt.lb.zk.DWindow;
import lt.lb.zk.DialogDTO;
import org.zkoss.zk.ui.Component;

/**
 *
 * @author laim0nas100
 */
public class DialogChain {
    
    public static enum DWindowHide {
        HIDE, DETACH
    }
    
    protected DWindowHide hide = DWindowHide.DETACH;
    
    public DialogChain(Component parentComponent, DWindowHide hide) {
        this.parentComponent = Objects.requireNonNull(parentComponent, "Parent component not provided");
        this.hide = Objects.requireNonNull(hide);
    }
    
    public DialogChain(Component parentComponent) {
        this(parentComponent, DWindowHide.DETACH);
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
        hideOrDetach(win);
        return remove;
    }
    
    protected void hideOrDetach(DWindow win){
        switch(hide){
            case DETACH:{
                win.detach();
                break;
            }
            case HIDE:{
                win.hide(null);
                break;
            }
        }
    }
    
    public void detachAll() {
        For.elements().iterateBackwards(windows, (i, d) -> {
            hideOrDetach(d);
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
