/*
 * Copyright @LKPB 
 */
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

/**
 *
 * @author laim0nas100
 */
public class ComboboxMapper<T> {

    Combobox box;
    private List<T> options = new ArrayList<>();
    private Function<T, String> nameMapper = str -> "" + str;
    private boolean readOnly = true;
    private ArrayList<Runnable> onSelectionUpdate = new ArrayList<>();

    public ArrayList<Runnable> getOnSelectionUpdate() {
        return onSelectionUpdate;
    }

    public Optional<T> getSelected() {
        return SafeOpt.of(box)
                .map(b -> b.getSelectedIndex())
                .filter(i -> i >= 0 && i <= options.size())
                .map(i -> options.get(i))
                .asOptional();
    }

    public List<String> getNames() {
        return options.stream()
                .map(nameMapper)
                .collect(Collectors.toList());
    }
    
    public boolean isReadOnly(){
        return readOnly;
    }
    
    public ComboboxMapper<T> withMapper(Function<T, String> mapper){
        this.nameMapper = mapper;
        return this;
    }

    public ComboboxMapper<T> withReadOnly(boolean readonly) {
        this.readOnly = readonly;
        return this;
    }

    public ComboboxMapper<T> withSelectionUpdateListener(Consumer<Optional<T>> cons) {

        this.onSelectionUpdate.add(() -> {
            cons.accept(this.getSelected());
        });
        return this;
    }
    
    public ComboboxMapper<T> withOptions(Collection<T> opt){
        this.options.addAll(opt);
        return this;
    }
    
    public ComboboxMapper<T> withOption(T opt){
        this.options.add(opt);
        return this;
    }
    
    

}
