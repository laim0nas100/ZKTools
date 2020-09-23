package lt.lb.zk.builder;

import org.zkoss.zk.ui.Component;

/**
 *
 * @author laim0nas100
 */
public interface ComponentBuilder<C extends CTX,R> {
    public R build(Component root);

    public C getContext();
}