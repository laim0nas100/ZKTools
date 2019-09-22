/*
 * Copyright @LKPB 
 */
package lt.lb.zk;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 *
 * @author Laimonas Beniušis
 */
public interface Builder<T> {

    public Builder<T> with(Consumer<T> cons);

    public T build();

    public static class EagerBuilder<T> implements Builder<T> {

        protected T value;

        public EagerBuilder(T initial) {
            this.value = initial;
        }

        @Override
        public T build() {
            return value;
        }

        @Override
        public Builder<T> with(Consumer<T> cons) {
            cons.accept(value);
            return this;
        }

    }

    public static class LazyBuilder<T> extends EagerBuilder<T> {

        private LinkedList<Consumer<T>> modifications = new LinkedList<>();

        public LazyBuilder(T initial) {
            super(initial);
        }

        @Override
        public T build() {
            for (Consumer<T> cons : modifications) {
                cons.accept(this.value);
            }
            modifications.clear();
            return this.value;
        }

        @Override
        public Builder<T> with(Consumer<T> cons) {
            modifications.add(cons);
            return this;
        }

    }
}
