package lt.lb.zk.builder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.SafeOpt;
import lt.lb.commons.containers.values.Props;

/**
 *
 * @author laim0nas100
 */
public class Prop<T> extends Props.PropGet<String> {

    public T defaultVal;
    public boolean optional;
    public Props props;

    public T getDefault(){
        return defaultVal;
    }
    
    public boolean isOptional(){
        return optional;
    }
    
    public SafeOpt<Prop<T>> getSafe(){
        return SafeOpt.of(this);
    }
    
    public boolean getBool() {
        return Boolean.parseBoolean(get(props));
    }

    public int getInt() {
        return Integer.parseInt(get(props));
    }

    public long getLong() {
        return Long.parseLong(get(props));
    }

    public float getFloat() {
        return Float.parseFloat(get(props));
    }

    public double getDouble() {
        return Double.parseDouble(get(props));
    }

    public List<Integer> getIntList() {
        return Stream.of(parseArray(get(props))).map(s -> Integer.parseInt(s)).collect(Collectors.toList());
    }

    public List<Long> getLongList() {
        return Stream.of(parseArray(get(props))).map(s -> Long.parseLong(s)).collect(Collectors.toList());
    }

    public List<Float> getFloatList() {
        return Stream.of(parseArray(get(props))).map(s -> Float.parseFloat(s)).collect(Collectors.toList());
    }

    public List<Double> getDoubleList() {
        return Stream.of(parseArray(get(props))).map(s -> Double.parseDouble(s)).collect(Collectors.toList());
    }

    private String[] parseArray(String string) {
        return string.replace("[", "").replace("]", "").split(", ");
    }

    public Prop(String propKey) {
        this(propKey, true);
    }

    public Prop(String propKey, boolean optional) {
        super(propKey);
        this.optional = optional;
    }
}
