package lt.lb.zk.dynamicrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lt.lb.commons.SafeOpt;
import lt.lb.commons.misc.IntRange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Vbox;

/**
 *
 * @author laim0nas100
 */
public class RadioComboboxMapper<T> {

    public Combobox combo;
    public Radiogroup radio;
    private List<T> options = new ArrayList<>();
    private Function<T, String> nameMapper = str -> "" + str;
    private Function<T, String> nullMapper = str -> "";
    private boolean readOnly = true;
    private boolean isDisabled = false;
    private boolean isRadio = false;
    private boolean vertical = true;
    private ArrayList<Runnable> onSelectionUpdate = new ArrayList<>();
    private int preselected = -1;

    public ArrayList<Runnable> getOnSelectionUpdate() {
        return onSelectionUpdate;
    }

    public boolean isRadio() {
        return isRadio;
    }

    public boolean isVertical() {
        return vertical;
    }

    public Function<T, String> mapper() {
        return s -> s == null ? nullMapper.apply(s) : nameMapper.apply(s);
    }

    public RadioComboboxMapper<T> selectIndex(int i) {
        IntRange.of(0, options.size()).assertIndexBoundsExclusive(i);
        if (isRadio()) {
            SafeOpt.ofNullable(radio).ifPresent(b -> b.setSelectedIndex(i)).getError().throwIfErrorNested();
        } else {
            SafeOpt.ofNullable(combo).ifPresent(b -> b.setSelectedIndex(i)).getError().throwIfErrorNested();
        }

        return this;
    }

    public RadioComboboxMapper<T> select(T option) {
        return selectIndex(options.indexOf(option));
    }

    public Optional<T> getSelected() {
        if (isRadio()) {
            return SafeOpt.ofNullable(radio)
                    .map(b -> b.getSelectedIndex())
                    .filter(i -> i >= 0 && i <= options.size())
                    .map(i -> options.get(i))
                    .asOptional();
        } else {
            return SafeOpt.ofNullable(combo)
                    .map(b -> b.getSelectedIndex())
                    .filter(i -> i >= 0 && i <= options.size())
                    .map(i -> options.get(i))
                    .asOptional();
        }
    }

    public List<String> getNames() {
        return options.stream()
                .map(mapper())
                .collect(Collectors.toList());
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public Integer getPreselectedIndex() {
        return preselected;
    }

    public RadioComboboxMapper<T> withMapper(Function<T, String> mapper) {
        this.nameMapper = mapper;
        return this;
    }

    public RadioComboboxMapper<T> withNullOption(String toDisplay) {
        this.nullMapper = s -> toDisplay;
        if (!options.contains(null)) {
            options.add(0, null);
        }
        return this;
    }

    public RadioComboboxMapper<T> withReadOnly(boolean readonly) {
        this.readOnly = readonly;
        return this;
    }

    public RadioComboboxMapper<T> withSelectionUpdateListener(Consumer<Optional<T>> cons) {

        this.onSelectionUpdate.add(() -> {
            cons.accept(this.getSelected());
        });
        return this;
    }

    public RadioComboboxMapper<T> withVertical(boolean ver) {
        this.vertical = ver;
        return this;
    }

    public RadioComboboxMapper<T> withOptions(Collection<T> opt) {
        this.options.addAll(opt);
        return this;
    }

    public RadioComboboxMapper<T> withOption(T opt) {
        this.options.add(opt);
        return this;
    }

    public RadioComboboxMapper<T> withRadio(boolean b) {
        this.isRadio = b;
        return this;
    }

    public RadioComboboxMapper<T> withDisabled(boolean b) {
        this.isDisabled = b;
        return this;
    }

    public RadioComboboxMapper<T> withPreselected(T opt) {
        return withPreselectedIndex(options.indexOf(opt));
    }

    public RadioComboboxMapper<T> withPreselectedIndex(int i) {
        i = Math.max(-1, i);
        if (i >= options.size()) {
            i = -1;
        }
        this.preselected = i;
        return this;
    }

    public Combobox generateCombobox() {
        Combobox com = new Combobox();
        for (String item : this.getNames()) {
            com.appendItem(item);
        }
        com.setReadonly(this.isReadOnly());
        com.setDisabled(this.isDisabled());
        this.combo = com;
        if (this.getPreselectedIndex() != -1) {
            com.setSelectedIndex(this.getPreselectedIndex());
        }
        return com;
    }

    public Radiogroup generateRadio() {
        Radiogroup rad = new Radiogroup();

        Component radioParent;
        if (this.isVertical()) {
            Vbox vb = new Vbox();
            vb.setAlign("left");
            radioParent = vb;
            rad.appendChild(vb);
        } else {
            rad.setOrient("horizontal");
            radioParent = rad;
        }

        for (String item : this.getNames()) {
            Radio r = new Radio(item);
            radioParent.appendChild(r);
            r.setRadiogroup(rad);
            r.setDisabled(this.isDisabled());
        }
        this.radio = rad;
        if (this.getPreselectedIndex() != -1) {
            rad.setSelectedIndex(this.getPreselectedIndex());
        }
        
        return rad;
    }

}
