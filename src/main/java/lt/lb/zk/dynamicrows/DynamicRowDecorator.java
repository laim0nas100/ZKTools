/*
 * Copyright @LKPB 
 */
package lt.lb.zk.dynamicrows;


/**
 *
 * @author Laimonas Beniušis
 */
public interface DynamicRowDecorator {
    public void decorate(DynamicRow row);

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
