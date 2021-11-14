package lt.lb.zk;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import lt.lb.commons.F;
import lt.lb.commons.LineStringBuilder;
import lt.lb.commons.containers.tuples.Tuple;
import lt.lb.commons.iteration.For;
import lt.lb.commons.misc.Range;
import lt.lb.commons.misc.UUIDgenerator;
import lt.lb.fastid.FastIDGen;
import lt.lb.zk.Builder.EagerBuilder;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Foot;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 *
 * @author laim0nas100
 */
public class ZKComponents {

    public static Textbox textboxMultiline(String text) {
        Textbox tb = new Textbox(text);

        tb.setStyle(F.nullWrap(tb.getStyle(), "") + "; resize:vertical;");
        tb.setVflex("max");
        tb.setMultiline(true);
        return tb;
    }

    public static Textbox textboxMultiline() {
        return textboxMultiline(null);
    }

    public static Textbox textbox(int length) {
        return builderOf(new Textbox()).with(t -> t.setMaxlength(length)).build();

    }

    public static <T extends Component> Builder<T> builderOf(T comp) {
        return new EagerBuilder<>(comp);
    }

    public static class DialogBuilder extends EagerBuilder<DWindow> {

        static FastIDGen gen = new FastIDGen();

        public DialogBuilder(DWindow initial) {
            super(initial);
            initial.setId("DWindow_" + gen.getAndIncrement().toString());
        }

        public DialogBuilder() {
            this(new DWindow());
        }

        @Override
        public DialogBuilder with(Consumer<DWindow> cons) {
            cons.accept(value);
            return this;
        }

        public DialogBuilder setTitle(String title) {
            return with(c -> c.setTitle(title));
        }

        public DialogBuilder setParent(Component parent) {
            return with(c -> c.setParent(parent));
        }

        public DialogBuilder setClosable(boolean closable) {
            return with(c -> c.setClosable(closable));
        }

        public DialogBuilder setHeight(int h) {
            return with(c -> {
                if (h < 0 || h > 100) {
                    throw new IllegalArgumentException("Height percent out of bounds:" + h);
                }
                c.setHeight(h + "%");
            });
        }

        public DialogBuilder setWidth(int w) {
            return with(c -> {
                if (w < 0 || w > 100) {
                    throw new IllegalArgumentException("Width percent out of bounds:" + w);
                }
                c.setWidth(w + "%");
            });
        }

        public DialogBuilder makeFlexibleFitWidth(int w) {
            return setWidth(w).with(c -> {
                c.setHeight("min");
                c.setContentStyle("overflow:auto;");
            });
        }
        
        public DialogBuilder makeFlexibleFitWidthNotClosable(int w) {
            return setWidth(w).with(c -> {
                c.setHeight("min");
                c.setContentStyle("overflow:auto;");
                c.setClosable(false);
            });
        }

        public DialogBuilder makeFlexible() {
            return with(c -> {
                c.setContentStyle("overflow:auto;");
            });
        }

        public DialogBuilder makeFitSize() {
            return with(c -> {
                c.setHeight("min");
                c.setWidth("min");
            });
        }

    }

    public static Window getFlexibleWindow() {
        Window window = new Window();
        window.setHeight("min");
        window.setWidth("min");
        window.setContentStyle("overflow:auto;");
        return window;
    }

    public static DWindow getFlexibleDialogWindow(String title, Component parent) {
        DWindow window = new DWindow();
        window.setId(UUIDgenerator.nextUUID("DialogWindow"));
        window.setParent(parent);
        window.setTitle(title);
        window.setContentStyle("overflow:auto;");
        return window;
    }

    public static DWindow getFlexibleDialogWindowAdjustedNotClosable(String title, Component parent) {
        DWindow window = new DWindow();
        window.setId(UUIDgenerator.nextUUID("DialogWindow"));
        window.setParent(parent);
        window.setTitle(title);
        window.setContentStyle("overflow:auto;");
        window.setClosable(false);
        window.setHeight("min");
        window.setWidth("min");

        return window;
    }

    public static Div getFlexDiv(int heightPercent, int widthPercent) {
        Range range = Range.of(1, 100);
        if (range.inRangeInclusive(heightPercent) && range.inRangeInclusive(widthPercent)) {
            Div div = new Div();
            div.setHeight(String.valueOf(heightPercent) + "%");
            div.setWidth(String.valueOf(widthPercent) + "%");
            return div;
        } else {
            throw new IllegalArgumentException(heightPercent + " " + widthPercent);
        }
    }

    public static void componentPairActionChain(BiConsumer<Component, Component> cons, Component comp, Component... others) {
        Component node = comp;
        for (Component next : others) {
            cons.accept(node, next);
            node = next;
        }
    }

    public static Grid getGridWithComponents() {
        Grid grid = new Grid();
        grid.appendChild(new Auxhead());
        grid.appendChild(new Columns());

        grid.appendChild(new Rows());
        grid.appendChild(new Foot());

        return grid;
    }

    public static <T extends Component> T createZulComponent(String compName, Map<?, ?> args, Component parent, Tuple<String, Object>... params) {
        LineStringBuilder zk = new LineStringBuilder();
        zk.append("<").append(compName);
        For.elements().iterate(params, (i, t) -> {
            zk.append(" ").append(t.g1).append("=\"").append(t.g2).append("\" ");
        });
        zk.append("></").append(compName).append(">");

        return F.cast(Executions.createComponentsDirectly(zk.toString(), "zul", parent, args));
    }

    public static <T extends Component> T createZulComponent(String compName, Tuple<String, Object>... params) {
        return createZulComponent(compName, new HashMap<>(), null, params);
    }

    public static <T extends Component> T createZulComponent(String compName, Component parent, Tuple<String, Object>... params) {
        return createZulComponent(compName, new HashMap<>(), parent, params);
    }

    public static <T extends Component> T createZul(String path) {
        return createZul(path, null, null);
    }

    public static <T extends Component> T createZul(String path, Tuple... arguments) {
        Map args = new HashMap<>();
        for (Tuple tup : arguments) {
            args.put(tup.g1, tup.g2);
        }
        return createZul(path, args);
    }

    public static <T extends Component> T createZul(String path, Map<?, ?> args) {
        return createZul(path, null, args);
    }

    public static <T extends Component> T createZul(String path, Component parent, Map<?, ?> args) {
        return F.cast(Executions.createComponents(path, parent, args));
    }
}
