package lt.lb.zk.rows;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import lt.lb.zk.rows.ZKSync;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lt.lb.commons.F;
import lt.lb.commons.misc.NestedException;
import lt.lb.commons.rows.SyncDrow;
import lt.lb.zk.ZKValidation;
import lt.lb.zk.dynamicrows.RadioComboboxMapper;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radiogroup;
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

        return addButton(but, event);
    }

    public R addButton(Button but, Consumer<ZKBaseDrow> event) {
        AtomicBoolean pressed = new AtomicBoolean(false);
        but.addEventListener(Events.ON_CLICK, e -> {
            if (pressed.compareAndSet(false, true)) {
                Optional<Throwable> checkedRun = F.checkedRun(() -> {
                    event.accept(me());
                });

                pressed.set(false);
                checkedRun.ifPresent(NestedException::nestedThrow);
            }

        });
        return this.add(but);
    }
    
    public <T> R withValidationMaker(Function<R, ZKValidation.ExternalValidation> fun) {
        return addOnDisplayAndRunIfDone(() -> {
            ZKValidation.ExternalValidation apply = fun.apply(me());
            ZKValid zkValid = new ZKValid(apply);

            addValidationPersist(zkValid);
        });
    }

    public <T> R addRadioCombobox(RadioComboboxMapper<T> mapper) {

        boolean updatesEmpty = mapper.getOnSelectionUpdate().isEmpty();
        if (mapper.isRadio()) {

            Radiogroup radio = mapper.generateRadio();
            mapper.radio = radio;
            if (!updatesEmpty) {
                radio.addEventListener(Events.ON_SELECT, l -> {
                    mapper.getOnSelectionUpdate().forEach(Runnable::run);
                });
            }
        } else {
            Combobox combo = mapper.generateCombobox();
            if (!updatesEmpty) {
                combo.addEventListener(Events.ON_SELECT, l -> {
                    mapper.getOnSelectionUpdate().forEach(Runnable::run);
                });
            }

        }

        return mapper.isRadio() ? this.add(mapper.radio) : this.add(mapper.combo);

    }

    public <T> R addList(ListitemRenderer<T> renderer, Supplier<Collection<T>> supp) {
        ListModelList<T> model = new ListModelList<>();

        this.withUpdateRefresh(r -> {
            model.clear();
            model.addAll(supp.get());
        });
        return addListElems(renderer, model);
    }

    public <T, E> R addList(ListitemRenderer<E> renderer, Supplier<Collection<T>> collection, Function<? super T, E> mapper) {
        return addList(renderer, () -> {
            return collection.get().stream().map(mapper).collect(Collectors.toList());
        });

    }
    
    public R withStyle(int index, String style) {
        return this.withNodeDecorator(index, c -> {
            if (c instanceof HtmlBasedComponent) {
                HtmlBasedComponent comp = F.cast(c);
                comp.setStyle(style);
            }
        });
    }

    public R addListbox(Listbox listbox) {
        return add(listbox);
    }

    public R addList(ListModel<String> list) {
        Listbox listbox = new Listbox();
        listbox.setModel(list);
        return addListbox(listbox);
    }

    public <T> R addListElems(ListitemRenderer<T> renderer, ListModel<T> list) {
        Listbox listbox = new Listbox();
        listbox.appendChild(new Listhead());
        listbox.setItemRenderer(renderer);
        listbox.setModel(list);
        return addListbox(listbox);
    }

}
