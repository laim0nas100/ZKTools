package lt.lb.zk.builder;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.F;
import lt.lb.commons.Ins;
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

    public T getDefault() {
        return defaultVal;
    }

    public void set(Object obj) {
        String string = obj == null ? null : String.valueOf(obj);
        insert(props, string);
    }

    public boolean isOptional() {
        return optional;
    }

    public SafeOpt<Prop<T>> getSafe() {
        return SafeOpt.of(this);
    }

    public boolean getBool() {
        return Boolean.parseBoolean(get(props));
    }

    public SafeOpt<Boolean> getSafeBool() {
        return getSafe().map(m -> m.getBool());
    }

    public int getInt() {
        return Integer.parseInt(get(props));
    }

    public SafeOpt<Integer> getSafeInt() {
        return getSafe().map(m -> m.getInt());
    }

    public long getLong() {
        return Long.parseLong(get(props));
    }

    public SafeOpt<Long> getSafeLong() {
        return getSafe().map(m -> m.getLong());
    }

    public float getFloat() {
        return Float.parseFloat(get(props));
    }

    public SafeOpt<Float> getSafeFloat() {
        return getSafe().map(m -> m.getFloat());
    }

    public double getDouble() {
        return Double.parseDouble(get(props));
    }

    public SafeOpt<Double> getSafeDouble() {
        return getSafe().map(m -> m.getDouble());
    }

    public List<Integer> getIntList() {
        return Stream.of(parseArray(get(props))).map(s -> Integer.parseInt(s)).collect(Collectors.toList());
    }

    public SafeOpt<List<Integer>> getSafeIntList() {
        return getSafe().map(m -> m.getIntList());
    }

    public List<Long> getLongList() {
        return Stream.of(parseArray(get(props))).map(s -> Long.parseLong(s)).collect(Collectors.toList());
    }

    public SafeOpt<List<Long>> getSafeLongList() {
        return getSafe().map(m -> m.getLongList());
    }

    public List<Float> getFloatList() {
        return Stream.of(parseArray(get(props))).map(s -> Float.parseFloat(s)).collect(Collectors.toList());
    }

    public SafeOpt<List<Float>> getSafeFloatList() {
        return getSafe().map(m -> m.getFloatList());
    }

    public List<Double> getDoubleList() {
        return Stream.of(parseArray(get(props))).map(s -> Double.parseDouble(s)).collect(Collectors.toList());
    }

    public SafeOpt<List<Double>> getSafeDoubleList() {
        return getSafe().map(m -> m.getDoubleList());
    }

    private String[] parseArray(String string) {
        return string.replace("[", "").replace("]", "").split(", ");
    }

    public List<String> getList() {
        return Stream.of(parseArray(get(props))).collect(Collectors.toList());
    }

    public SafeOpt<List<String>> getSafeList() {
        return getSafe().map(m -> m.getList());
    }

    public boolean getOrDefaultBool() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeBool().orElse((Boolean) def);
    }
    
    public Integer getOrDefaultInt() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeInt().orElse((Integer) def);
    }
    
    public Long getOrDefaultLong() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeLong().orElse((Long) def);
    }
    
    public Float getOrDefaultFloat() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeFloat().orElse((Float) def);
    }
    
    public Double getOrDefaultDouble() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeDouble().orElse((Double) def);
    }
    
    public List<Integer> getOrDefaultIntList() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeIntList().orElse((List<Integer>) def);
    }
    
    public List<Long> getOrDefaultLongList() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeLongList().orElse((List<Long>) def);
    }
    
    public List<Float> getOrDefaultFloatList() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeFloatList().orElse((List<Float>) def);
    }
    
    public List<Double> getOrDefaultDoubleList() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeDoubleList().orElse((List<Double>) def);
    }
    
    public List<String> getOrDefaultList() {
        T def = this.defaultVal;
        if (def == null) {
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        return this.getSafeList().orElse((List<String>) def);
    }


    public Prop(String propKey) {
        this(propKey, true);
    }

    public Prop(String propKey, boolean optional) {
        super(propKey);
        this.optional = optional;
    }
}
