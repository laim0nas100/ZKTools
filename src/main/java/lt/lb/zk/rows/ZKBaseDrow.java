package lt.lb.zk.rows;

import lt.lb.zk.rows.ZKSync;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lt.lb.commons.F;
import lt.lb.commons.rows.SyncDrow;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Space;

/**
 *
 * Base ZK row to extend and add new methods
 *
 */
public abstract class ZKBaseDrow<R extends ZKBaseDrow<R, DR>, DR extends ZKBaseDrows<R, DR>> extends SyncDrow<ZKCell, Component, ZKLine<R, DR>, ZKUpdates, ZKBaseDrowConf<R, DR>, R> {

    public ZKBaseDrow(ZKLine line, ZKBaseDrowConf<R, DR> config, String key) {
        super(line, config, key);
    }

    @Override
    public R display() {
        return display(false);
    }

    public R add(Supplier<Component> sup) {
        return this.add(sup.get());
    }

    public R addSpace() {
        return addSpace("1px");
    }

    public R addSpace(String spacing) {
        Space s = new Space();
        s.setSpacing(spacing);
        return this.add(s);
    }

    public R addLabel(String str) {
        Label label = new Label(str);
        return add(label);
    }

    public R withPreferedAllign(String... aligns) {
        return addOnDisplayAndRunIfDone(() -> {
            F.iterate(aligns, (i, align) -> {
                getCell(i).setAllign(align);
            });
        });
    }

    public <N extends Component> R addZKSync(ZKSync<?, ?, N> sync) {
        for (Component c : sync.nodes) {
            add(c);
        }
        return addDataSyncValidation(sync);
    }

    public R addLabelWithUpdate(Supplier<String> stringSupplier) {
        Label label = new Label();
        this.add(label);

        return this.withUpdateRefresh(r -> {
            label.setValue(stringSupplier.get());
        });
    }

    public R addButton(String title, Consumer<ZKBaseDrow> event) {
        Button but = new Button(title);
        but.addEventListener(Events.ON_CLICK, e -> {
            event.accept(me());
        });
        return this.add(but);
    }

    public R addButton(Button but, Consumer<ZKBaseDrow> event) {
        but.addEventListener(Events.ON_CLICK, e -> {
            event.accept(me());
        });
        return this.add(but);
    }

}
