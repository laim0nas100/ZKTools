package lt.lb.zk.rows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lt.lb.commons.F;
import lt.lb.commons.containers.values.ValueProxy;
import lt.lb.commons.datasync.base.NodeSync;
import lt.lb.commons.func.BiConverter;
import lt.lb.uncheckedutils.SafeOpt;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.ComboitemRenderer;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
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

    public static <T> ZKSync<T, String, Textbox> ofTextboxFormatted(ValueProxy<T> persistProxy, Textbox tb, T def, BiConverter<SafeOpt<T>, String> conv) {

        ZKSync<T, String, Textbox> sync = new ZKSync<>(tb);
        sync.withIdentityPersist();
        sync.withDisplayGet(str -> conv.getBackFrom(str).orElse(def));
        sync.withDisplaySet(val -> conv.getFrom(SafeOpt.ofNullable(val).orGet(() -> def)));
        sync.withDisplayProxy(ZKDataSync.quickProxy(tb::getValue, tb::setValue));
        sync.withPersistProxy(persistProxy);

        tb.addEventListener(Events.ON_CHANGE, l -> {
            sync.syncManagedFromDisplay();
        });
        return sync;
    }

    public static <V, M, C extends InputElement> ZKSync<V, M, C> ofInputElement(ValueProxy<V> persistProxy, C tb, BiConverter<V, M> conv) {
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
        return ofInputElement(persistProxy, tb, BiConverter.identity());
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
            boolean con = supl == null ? false : supl;
            tb.setChecked(con);
        });
        sync.withPersistProxy(persistProxy);

        tb.addEventListener(Events.ON_CHECK, l -> {
            sync.syncManagedFromDisplay();
        });
        return sync;

    }

    public static <T extends Enum<T>> ZKSync<T, T, Combobox> ofComboBox(Combobox box, ValueProxy<T> selectedItem, Class<T> cls, Function<T, String> textExtract) {
        List<T> options = new ArrayList<>(EnumSet.allOf(cls));
        return ofComboBox(box, selectedItem, options, textExtract);
    }

    public static interface ListitemRenderInfoRenderer<T> extends ListitemRenderer<T> {

        @Override
        public default void render(Listitem item, T data, int index) throws Exception {
            render(new ListitemRenderInfo<>(item, data, index));
        }

        public void render(ListitemRenderInfo<T> info) throws Exception;

    }

    public static interface ComboitemRenderInfoRenderer<T> extends ComboitemRenderer<T> {

        @Override
        public default void render(Comboitem item, T data, int index) throws Exception {
            render(new ComboitemRenderInfo<>(item, data, index));
        }

        public void render(ComboitemRenderInfo<T> info) throws Exception;

    }

    public static class ListitemRenderInfo<T> {

        public final Listitem item;
        public final T data;
        public final int index;

        public ListitemRenderInfo(Listitem item, T data, int index) {
            this.item = item;
            this.data = data;
            this.index = index;
        }

    }

    public static class ComboitemRenderInfo<T> {

        public final Comboitem item;
        public final T data;
        public final int index;

        public ComboitemRenderInfo(Comboitem item, T data, int index) {
            this.item = item;
            this.data = data;
            this.index = index;
        }

    }

    public static <T> ZKSync<Collection<T>, Set<Listitem>, Listbox> ofListboxSelect(ValueProxy<Collection<T>> items, List<T> options, ListitemRenderInfoRenderer<T> renderer) {
        return ofListboxSelect(new Listbox(), items, options, renderer);
    }

    public static <T> ZKSync<Collection<T>, Set<Listitem>, Listbox> ofListboxSelect(Listbox box, ValueProxy<Collection<T>> items, List<T> options, ListitemRenderInfoRenderer<T> renderer) {

        ZKSync<Collection<T>, Set<Listitem>, Listbox> sync = new ZKSync<>(box);

        sync.withIdentityPersist();

        sync.withPersistProxy(items);
        sync.withDisplaySup(() -> box.getSelectedItems());
        sync.withDisplaySync(supl -> {
            box.setSelectedItems(supl);
        });
        sync.withDisplayGet(obList -> {
            return obList.stream().filter(m -> m.isSelected()).map(m -> (T) m.getValue()).collect(Collectors.toList());
        });
        sync.withDisplaySet(val -> {
            HashSet<T> set = new HashSet<>(val);
            return box.getItems().stream().filter(f -> set.contains((T) f.getValue())).collect(Collectors.toSet());
        });
        box.setItemRenderer(new ListitemRenderInfoRenderer<T>(){
            @Override
            public void render(ListitemRenderInfo<T> info) throws Exception {
                box.setVisible(true);
                renderer.render(info);
            }
        });
        box.addEventListener(Events.ON_CHANGE, ev -> {
            sync.syncManagedFromDisplay();
        });
        box.setModel(new ListModelList<>(options));

        return sync;
    }

    public static <T> ZKSync<Collection<T>, ListModelList<T>, Listbox> ofListbox(Listbox box, ValueProxy<Collection<T>> items, ListitemRenderInfoRenderer<T> renderer) {

        ZKSync<Collection<T>, ListModelList<T>, Listbox> sync = new ZKSync<>(box);

        sync.withIdentityPersist();

        sync.withPersistProxy(items);
        sync.withDisplaySup(() -> F.cast(box.getListModel()));
        sync.withDisplaySync(supl -> {
            box.setModel(supl);
        });

        sync.withDisplayGet(obList -> {
            return obList.stream().collect(Collectors.toList());
        });
        sync.withDisplaySet(val -> {
            return new ListModelList<>(val);
        });
        box.setItemRenderer(new ListitemRenderInfoRenderer<T>(){
            @Override
            public void render(ListitemRenderInfo<T> info) throws Exception {
                renderer.render(info);
            }
        });
        box.addEventListener(Events.ON_CHANGE, ev -> {
            sync.syncManagedFromDisplay();
        });

        return sync;
    }

    public static <T> ZKSync<T, T, Combobox> ofComboBox(Combobox box, ValueProxy<T> selectedItem, List<T> options, Function<T, String> textExtract) {
        return ofComboBox(box, selectedItem, options, info -> {
            info.item.setLabel(textExtract.apply(info.data));
        });
    }

    public static <T> ZKSync<T, T, Combobox> ofComboBox(Combobox box, ValueProxy<T> selectedItem, List<T> options, ComboitemRenderInfoRenderer<T> renderer) {

        ZKSync<T, T, Combobox> sync = new ZKSync<>(box);

        sync.withPersistProxy(selectedItem);
        sync.withIdentityPersist();
        sync.withIdentityDisplay();
        box.setAutocomplete(true);
        box.setAutodrop(true);
        sync.withDisplaySup(() -> {
            return SafeOpt.of(box).map(m -> m.getSelectedItem()).map(m -> (T) m.getValue()).orElse(null);
        });
        sync.withDisplaySync(supl -> {
            box.setModel(new ListModelList<>(options));

        });
        box.setItemRenderer(new ComboitemRenderInfoRenderer<T>() {
            @Override
            public void render(ComboitemRenderInfo<T> info) throws Exception {

                renderer.render(info);

                if (info.index + 1 == options.size()) {
                    int indexOf = options.indexOf(sync.getManaged());
                    box.setSelectedIndex(indexOf);
                }

            }
        });

        box.addEventListener(Events.ON_CHANGE, ev -> {

            int index = box.getSelectedIndex();
            if (index >= 0) {
                sync.setManaged(options.get(index));
            } else{
                sync.setManaged(null);
            }

        });

        return sync;
    }

}
