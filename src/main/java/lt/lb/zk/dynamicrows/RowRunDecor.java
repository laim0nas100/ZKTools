package lt.lb.zk.dynamicrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lt.lb.zk.dynamicrows.DynamicRow.DecorType;

/**
 *
 * @author laim0nas100
 */
public class RowRunDecor {

    public Predicate<DynamicRow> predicate = t -> true;
    public Collection<DynamicRow.DecorType> acceptableTypes = new HashSet<>();
    public Consumer<Runnable> decorator = r -> r.run();

    public RowRunDecor compose(RowRunDecor comp) {
        RowRunDecor decor = new RowRunDecor();
        decor.predicate = this.predicate.and(comp.predicate);
        decor.decorator = r -> {
            this.decorator.accept(() -> comp.decorator.accept(r));
        };
        return decor;
    }
    
    public static class RowRunDecorBuilder{
        RowRunDecor object = new RowRunDecor();
        public RowRunDecorBuilder withAllTypes(){
            return withTypes(DecorType.UPDATE, DecorType.UI, DecorType.VIEW, DecorType.FORM);
        }
        
        public RowRunDecorBuilder withTypes(DecorType ...types){
            object.acceptableTypes.addAll(Arrays.asList(types));
            return this;
        }
        public RowRunDecorBuilder withPredicate(Predicate<DynamicRow> pred){
            object.predicate = pred;
            return this;
        }
        public RowRunDecor withDecorator(Consumer<Runnable> cons){
            object.decorator = cons;
            return object;
        }
    }
    
    public static RowRunDecorBuilder builder(){
        return new RowRunDecorBuilder();
    }
}
