package lt.lb.zk;

import java.util.concurrent.Callable;
import lt.lb.commons.SafeOpt;

import org.apache.commons.lang3.StringUtils;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.ext.AfterCompose;
import org.zkoss.zul.Window;

/**
 * Simple dialog {@link Window} with useful defaults set.<br>
 * <b>NOTE #1:</b> Handles {@link Events#ON_CLOSE} event properly, without
 * detaching window, effectively allowing showing/hiding dialog as many times as
 * needed.<br>
 * <b>NOTE #2:</b> If dialog window ID is not set,
 * {@link DWindow#DEFAULT_WINDOW_ID} will be used. By default dialog window
 * is invisible, has {@link Window#EMBEDDED} mode and rendered with
 * <i>normal</i> border, <i>closable</i> and <i>centered</i>.
 *
 * @see #show(int)
 * @see #showModal()
 * @see #hide(Object)
 * @see #setResultProvider(Callable)
 */
public class DWindow extends Window implements AfterCompose {

    private static final long serialVersionUID = 3330718814012189169L;

    public static final String DEFAULT_WINDOW_ID = "dialogWindow";
    public static final String CONTROLLER = "controller";
    public static final String DIALOG_CLOSE_EVENT = "dialogCloseEvent";

    private Callable<?> resultProvider;

    /**
     * Constructs dialog window and applies default attributes
     */
    public DWindow() {
        // Setting default dialog window ID
        if (StringUtils.isEmpty(this.getId())) {			// TODO: can id be NOT null at this point?
            this.setId(DEFAULT_WINDOW_ID);
        }

        // Applying default dialog properties (may be overridden by subclasses)
        applyDefaultProperties();
    }

    @Override
    public void afterCompose() {
        addEventListener(Events.ON_CLOSE, new SerializableEventListener<Event>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onEvent(Event event) throws Exception {
                handleCloseEvent(event);
            }
        });

    }

    protected void applyDefaultProperties() {
        setVisible(false);
        setPosition("center");
        setBorder("normal");
        setSizable(true);
        setClosable(true);
    }

    /**
     * Performs clean-up, hides dialog window and posts
     * <code>DIALOG_CLOSE_EVENT</code> event with specified result.
     *
     * @param result data to post with <code>DIALOG_CLOSE_EVENT</code>
     * event
     */
    protected void closeDialog(Object result) {
        // BUG-FIX: removing focus from window to prevent any future events accidentally going back to its children components (e.g. ENTER press)
        setFocus(false);

        // Hiding dialog window
        setVisible(false);

        // Sending dialog close event
        Events.postEvent(DIALOG_CLOSE_EVENT, this, result);
    }

    /**
     * Handles {@link Events#ON_CLOSE} event
     */
    protected void handleCloseEvent(Event event) {
        event.stopPropagation();

        Object result = SafeOpt.selectFirstPresent(
                SafeOpt.ofNullable(resultProvider).map(m -> m.call()),
                SafeOpt.ofNullable(event).map(m -> m.getData())
        ).orElse(null);

        closeDialog(result);
    }

    /**
     * Shows modal dialog window
     */
    public void showModal() {
        show(Window.MODAL);
    }

    /**
     * Shows dialog window in specific mode
     *
     * @param mode dialog window mode, e.g. {@link Window#MODAL}
     */
    public void show(int mode) {
        try {
            setMode(mode);
//            CoreCompUtils.fireOnSize(); // forcing dialog to recalculate position and size

        } catch (Exception e) {
            closeDialog(null);
        }
    }

    /**
     * Hides dialog window and generates {@link App#ON_DIALOG_CLOSED} event
     *
     * @param result optional event result
     */
    public void hide(Object result) {
        closeDialog(result);
    }

    /**
     * Returns dialog window mode as integer, to be able to compare it to
     * constants defined in {@link Window} class, e.g. {@link Window#MODAL}
     */
    public int getModeAsInt() {
        int mode = Window.EMBEDDED;
        String stringMode = getMode();
        if ("modal".equals(stringMode)) {
            mode = Window.MODAL;
        } else if ("popup".equals(stringMode)) {
            mode = Window.POPUP;
        } else if ("overlapped".equals(stringMode)) {
            mode = Window.OVERLAPPED;
        } else if ("highlighted".equals(stringMode)) {
            mode = Window.HIGHLIGHTED;
        }
        return mode;
    }


    public Callable<?> getResultProvider() {
        return resultProvider;
    }

    /**
     * Sets optional dialog result provider which will be used when dialog is
     * closed by Events#ON_CLOSE
     */
    public void setResultProvider(Callable<?> resultProvider) {
        this.resultProvider = resultProvider;
    }
}
