package lt.lb.zk.rows;

import java.util.Date;
import java.util.List;
import lt.lb.commons.F;
import lt.lb.commons.SafeOpt;
import lt.lb.commons.containers.values.ValueProxy;
import lt.lb.commons.datasync.NodeSync;
import lt.lb.commons.func.Converter;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.impl.InputElement;

/**
 *
 * @author laim0nas100
 */
public class ZKSync<P, D, N extends Component> extends NodeSync<P, D, N, ZKValid<P, N>> {

    public ZKSync(N node) {
        super(node);
    }

    public ZKSync(List<N> nodes) {
        super(nodes);
    }

    @Override
    protected ZKValid<P, N> createValidation() {
        return new ZKValid<>(this.nodes);
    }

    public static <T> ZKSync<T, String, Textbox> ofTextboxFormatted(ValueProxy<T> persistProxy, Textbox tb, T def, Converter<SafeOpt<T>, String> conv) {

        ZKSync<T, String, Textbox> sync = new ZKSync<>(tb);
        SafeOpt<T> defaultSafe = SafeOpt.ofNullable(def);
        sync.withIdentityPersist();
        sync.withDisplayGet(str -> conv.getBackFrom(str).orElse(def));
        sync.withDisplaySet(val -> conv.getFrom(SafeOpt.ofNullable(val).orSafe(() -> defaultSafe)));
        sync.withDisplayProxy(ZKDataSync.quickProxy(tb::getValue, tb::setValue));
        sync.withPersistProxy(persistProxy);

        tb.addEventListener(Events.ON_CHANGE, l -> {
            sync.syncManagedFromDisplay();
        });
        return sync;
    }

    public static <V, M, C extends InputElement> ZKSync<V, M, C> ofInputElement(ValueProxy<V> persistProxy, C tb, Converter<V, M> conv) {
        ZKSync<V, M, C> sync = new ZKSync<>(tb);

        sync.withIdentityPersist();
        sync.withDisplayGet(conv::getBackFrom);
        sync.withDisplaySet(conv::getFrom);
        sync.withDisplayProxy(ZKDataSync.castProxy(tb::getRawValue, tb::setRawValue));
        sync.withPersistProxy(persistProxy);

        tb.addEventListener(Events.ON_CHANGE, l -> {
            sync.syncManagedFromDisplay();
        });
        return sync;
    }

    public static <V, C extends InputElement> ZKSync<V, V, C> ofInputElement(ValueProxy<V> persistProxy, C tb) {
        return ofInputElement(persistProxy, tb, Converter.identity());
    }

    public static ZKSync<String, String, Textbox> ofTextbox(ValueProxy<String> persistProxy, Textbox tb) {
        return ofInputElement(persistProxy, tb);
    }

    public static ZKSync<Date, Date, Datebox> ofDatebox(ValueProxy<Date> persistProxy, Datebox tb) {
        return ofInputElement(persistProxy, tb);
    }

    public static ZKSync<Long, Long, Longbox> ofLongbox(ValueProxy<Long> persistProxy, Longbox tb) {
        return ofInputElement(persistProxy, tb);
    }

    public static ZKSync<Integer, Integer, Intbox> ofIntbox(ValueProxy<Integer> persistProxy, Intbox tb) {
        return ofInputElement(persistProxy, tb);
    }

    public static ZKSync<Double, Double, Doublebox> ofDoublebox(ValueProxy<Double> persistProxy, Doublebox tb) {
        return ofInputElement(persistProxy, tb);
    }

    public static ZKSync<Boolean, Boolean, Checkbox> ofCheckbox(ValueProxy<Boolean> persistProxy, Checkbox tb) {
        ZKSync<Boolean, Boolean, Checkbox> sync = new ZKSync<>(tb);
        sync.withNoConversion();
        sync.withDisplaySup(() -> tb.isChecked());
        sync.withDisplaySync(supl -> {
            boolean con = F.nullWrap(supl, false);
            tb.setChecked(con);
        });
        sync.withPersistProxy(persistProxy);

        tb.addEventListener(Events.ON_CHECK, l -> {
            sync.syncManagedFromDisplay();
        });
        return sync;

    }

}
