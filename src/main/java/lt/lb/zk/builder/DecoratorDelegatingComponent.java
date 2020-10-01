package lt.lb.zk.builder;

import lt.lb.commons.containers.values.Props;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.select.annotation.Wire;

/**
 *
 * @author laim0nas100
 */
public abstract class DecoratorDelegatingComponent extends HtmlMacroComponent {

    @Wire
    public Component root;
    public ComponentBuilder builder;

    private String type;

    protected abstract ComponentBuilder getBuilder(String type);

    @Override
    public void afterCompose() {
        super.afterCompose();
        builder = getBuilder(type);

        if (builder == null) {
            throw new IllegalArgumentException("Failed to get decorator of type: " + type);
        }

        CTX ctx = builder.getNewContext();
        final Props props = ctx.getData();
        for (Prop prop : ctx.getProperties()) {
            String k = prop.propKey;
            if (!hasDynamicProperty(k)) {
                if (!prop.optional) {
                    throw new IllegalArgumentException("no required property: " + k);
                }

            } else {
                String dynamicProperty = String.valueOf(getDynamicProperty(k));
                prop.insert(props, dynamicProperty);
            }

        }

        try {

            builder.build(root, ctx);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
