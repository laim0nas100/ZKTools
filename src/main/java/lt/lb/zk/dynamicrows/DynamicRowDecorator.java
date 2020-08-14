package lt.lb.zk.dynamicrows;

import java.util.function.Consumer;

/**
 *
 * @author laim0nas100
 */
public interface DynamicRowDecorator extends Consumer<DynamicRow> {

    public void decorate(DynamicRow row);
    
    @Override
    public default void accept(DynamicRow t) {
        decorate(t);
    }
    
    public default DynamicRowDecorator with(DynamicRowDecorator other) {
        DynamicRowDecorator me = this;
        return (DynamicRow row) -> {
            me.decorate(row);
            other.decorate(row);
        };
    }
    
    public static DynamicRowDecorator of(DynamicRowDecorator... decs) {
        return (DynamicRow row) -> {
            for (DynamicRowDecorator dec : decs) {
                dec.decorate(row);
            }
        };
    }
}
