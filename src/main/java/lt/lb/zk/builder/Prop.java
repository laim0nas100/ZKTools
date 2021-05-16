package lt.lb.zk.builder;

import lt.lb.commons.containers.values.Props;
import lt.lb.commons.parsing.StringParserWithDefaultValue;
import lt.lb.uncheckedutils.SafeOpt;

/**
 *
 * @author laim0nas100
 */
public class Prop<T> extends Props.PropGet<String> implements StringParserWithDefaultValue<String,T> {

    public T defaultVal;
    public boolean optional;
    public Props props;

    @Override
    public T getDefault() {
        if(defaultVal == null){
            throw new IllegalArgumentException("No default value for " + this.propKey);
        }
        
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
    
     @Override
    public SafeOpt<String> parseOptString(String p) {
        return SafeOpt.ofNullable(p);
    }

    @Override
    public String get() {
        return props.getString(propKey);
    }


    public Prop(String propKey) {
        this(propKey, true);
    }

    public Prop(String propKey, boolean optional) {
        super(propKey);
        this.optional = optional;
    }
}
