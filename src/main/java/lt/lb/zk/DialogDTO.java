package lt.lb.zk;

import lt.lb.commons.containers.values.ValueProxy;

/**
 *
 *
 * Interface to transfer dialog (or any) result
 *
 * @author laim0nas100
 */
public interface DialogDTO<R> extends ValueProxy<R> {

    public static class DialogDTOProxy<T> implements DialogDTO<T> {

        private T val;
        private boolean setCalled = false;

        @Override
        public T get() {
            if (!setCalled) {
                throw new IllegalStateException("No value was set in DialogDTO before getting it out");
            }
            return val;
        }

        @Override
        public void set(T v) {
            setCalled = true;
            val = v;
        }

    }

    public static <R> DialogDTO<R> ofResult(R result) {
        DialogDTOProxy<R> proxy = new DialogDTOProxy<>();

        proxy.set(result);
        return proxy;
    }

    public static <R> DialogDTO<R> ofFutureResult() {
        return new DialogDTOProxy<>();
    }

}
