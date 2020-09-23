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
            p.props = data;
        }
    }

    public <T> Prop<T> withOptional(String str) {
        Prop<T> prop = new Prop<>(str, true);
        add(prop);
        return prop;
    }

    public <T> Prop<T> withOptional(String str, T defaultVal) {
        Prop<T> prop = new Prop<>(str, true);
        prop.defaultVal = defaultVal;
        add(prop);
        return prop;
    }

    public <T> Prop<T> withRequired(String str) {
        Prop<T> prop = new Prop(str, false);
        add(prop);
        return prop;
    }
}
