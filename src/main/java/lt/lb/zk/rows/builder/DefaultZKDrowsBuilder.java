package lt.lb.zk.rows.builder;

import lt.lb.zk.rows.ZKBaseDrow;
import lt.lb.zk.rows.ZKBaseDrowConf;
import lt.lb.zk.rows.ZKBaseDrows;
import lt.lb.zk.rows.ZKBaseDrowsConfig;
import org.zkoss.zk.ui.Component;
import lt.lb.zk.builder.CTX;

/**
 *
 * @author laim0nas100
 */
public abstract class DefaultZKDrowsBuilder<R extends ZKBaseDrow<R, RR>, RR extends ZKBaseDrows<R, RR>, C extends CTX> implements ZKRowsBuilder<C, RR> {

    protected Component root;
    protected RR rows;
    protected ZKBaseDrowConf<R, RR> rowConf;
    protected ZKBaseDrowsConfig<R, RR> rowsConf;

    protected C context;

    @Override
    public abstract RR build(Component root);

    protected abstract C produceContext();

    @Override
    public C getContext() {
        if (context == null) {
            context = produceContext();
        }
        return context;
    }

}
