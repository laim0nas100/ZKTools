package lt.lb.zk.dynamicrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lt.lb.commons.SafeOpt;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Radiogroup;

/**
 *
 * @author laim0nas100
 */
public class RadioComboboxMapper<T> {

    Combobox combo;
    Radiogroup radio;
    private List<T> options = new ArrayList<>();
    private Function<T, String> nameMapper = str -> "" + str;
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
    
    public boolean isVertical(){
        return vertical;
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
                .map(nameMapper)
                .collect(Collectors.toList());
    }

    public boolean isReadOnly() {
        return readOnly;
    }
    
    public boolean isDisabled(){
        return isDisabled;
    }

    public Integer getPreselectedIndex() {
        return preselected;
    }

    public RadioComboboxMapper<T> withMapper(Function<T, String> mapper) {
        this.nameMapper = mapper;
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
    
    public RadioComboboxMapper<T> withVertical(boolean ver){
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

}
