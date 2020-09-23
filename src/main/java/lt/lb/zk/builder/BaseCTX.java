package lt.lb.zk.builder;

import java.util.ArrayList;
import java.util.List;
import lt.lb.commons.containers.values.Props;

/**
 *
 * @author laim0nas100
 */
public abstract class BaseCTX implements CTX {

    protected ArrayList<Prop> properties = new ArrayList<>();
    protected Props data = new Props();

    @Override
    public List<Prop> getProperties() {
        return properties;
    }

    @Override
    public Props getData() {
        return data;
    }

    public void add(Prop... props) {
        for (Prop p : props) {
            properties.add(p);
        }
    }
}
