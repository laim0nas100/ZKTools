package lt.lb.zk;

import lt.lb.commons.parsing.StringOp;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.EventListener;

/**
 *
 * @author Laimonas-Beniusis-PC
 */
public class ZKTools {

    public static class Event {

        public static void bulkSendEvent(String eventName, Component... comps) {
            for (Component c : comps) {
                org.zkoss.zk.ui.event.Events.sendEvent(eventName, c, null);
            }
        }

        public static void bulkSendEventRecurse(String eventName, Component... comps) {
            for (Component c : comps) {
                org.zkoss.zk.ui.event.Events.sendEvent(eventName, c, null);
                for (Component cc : c.getChildren()) {
                    bulkSendEventRecurse(eventName, cc);
                }
            }
        }

        public static void bulkAddListener(String eventName, EventListener listener, Component... comps) {
            for (Component c : comps) {
                c.addEventListener(eventName, listener);
            }
        }

    }

    public static String styleAppend(HtmlBasedComponent htmlComp, String style) {
        String oldStyle = htmlComp.getStyle();

        htmlComp.setStyle(StringOp.join(oldStyle, style));
        return oldStyle;
    }
    
    public static String styleReplace(HtmlBasedComponent htmlComp, String style) {
        String oldStyle = htmlComp.getStyle();
        htmlComp.setStyle(style);
        return oldStyle;
    }

}
