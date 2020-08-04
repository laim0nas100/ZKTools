package lt.lb.zk;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import lt.lb.commons.ArrayOp;
import lt.lb.commons.F;
import lt.lb.commons.Ins;
import lt.lb.commons.reflect.FieldChain;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Longbox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Timebox;
import org.zkoss.zul.impl.InputElement;

/**
 *
 * @author laim0nas100
 */
public class ZKBinds {

    private static HashMap<Class, Class[]> componentMap = initHashMap();

    private static HashMap<Class, Class[]> initHashMap() {
        HashMap<Class, Class[]> map = new HashMap<>();
        map.put(Checkbox.class, ArrayOp.asArray(Boolean.class));
        map.put(Textbox.class, ArrayOp.asArray(String.class));
        map.put(Datebox.class, ArrayOp.asArray(Date.class));
        map.put(Timebox.class, ArrayOp.asArray(Date.class));
        map.put(Intbox.class, ArrayOp.asArray(Integer.class, Long.class, Float.class, Double.class));
        map.put(Longbox.class, ArrayOp.asArray(Integer.class, Long.class, Float.class, Double.class));
        map.put(Doublebox.class, ArrayOp.asArray(Float.class, Double.class));
        return map;
    }

    public static abstract class BindingCallback<T extends Component> {

        public abstract String eventName();

        public abstract void call(T comp, FieldChain.ObjectFieldChain field, Object object) throws Exception;

        public abstract void updateUI(T comp, FieldChain.ObjectFieldChain field, Object object) throws Exception;
    }

    private static BindingCallback<InputElement> inputElementCallback = new BindingCallback<InputElement>() {
        @Override
        public String eventName() {

            return Events.ON_CHANGE;
        }

        @Override
        public void call(InputElement comp, FieldChain.ObjectFieldChain field, Object object) throws Exception {
            field.doSet(object, comp.getRawValue());
        }

        @Override
        public void updateUI(InputElement comp, FieldChain.ObjectFieldChain field, Object object) throws Exception {
            Object doGet = field.doGet(object);
            comp.setRawValue(doGet);
        }
    };

    private static BindingCallback<Checkbox> checkboxCallback = new BindingCallback<Checkbox>() {
        @Override
        public String eventName() {
            return Events.ON_CHECK;
        }

        @Override
        public void call(Checkbox comp, FieldChain.ObjectFieldChain field, Object object) throws Exception {
            field.doSet(object, comp.isChecked());
        }

        @Override
        public void updateUI(Checkbox comp, FieldChain.ObjectFieldChain field, Object object) throws Exception {
            Boolean doGet = (Boolean) field.doGet(object);
            comp.setChecked(doGet);
        }
    };

    private static void doBind(Component cast, FieldChain.ObjectFieldChain field, Object object, boolean init) {
        if (cast instanceof InputElement) {
            doBindWithCallback(cast, field, object, inputElementCallback, init);
            return;
        }
        if (cast instanceof Checkbox) {
            doBindWithCallback(cast, field, object, checkboxCallback, init);
            return;
        }
        throw new IllegalArgumentException("No such binding for " + cast);

    }

    private static void doBindWithCallback(Component cast, FieldChain.ObjectFieldChain field, Object object, BindingCallback callback, boolean init) {
        if (callback != null) {

            cast.addEventListener(callback.eventName(), l -> {
                callback.call(cast, field, object);
            });
            if (init) {
                F.unsafeRun(() -> {
                    callback.updateUI(cast, field, object);
                });
            }

        } else {
            doBind(cast, field, object, init);
        }

    }

    /**
     *
     * @param element Component you wish to bind should be Instance of
     * InputElement or Checkbox
     * @param object Form object, with all public non-primitive fields
     * @param callback
     * @param init
     * @throws Exception
     */
    public static void bindCustom(Object object, Class fieldType, FieldChain.ObjectFieldChain field, Component element, BindingCallback callback, boolean init) throws Exception {
        Class elemClass = element.getClass();

        Ins.InsCl insCl = Ins.of(elemClass);
        Ins.InsCl insF = Ins.of(fieldType);
        if (callback != null) {
            doBindWithCallback(element, field, object, callback, init); //custom element binding
        } else if (insCl.instanceOfAny(InputElement.class, Checkbox.class)) {
            Set<Class> componentClasses = componentMap.keySet();
            if (componentClasses.contains(elemClass)) {
                Class[] possibleTypes = componentMap.get(elemClass);
                if (insF.instanceOfAny(possibleTypes)) {
                    doBindWithCallback(element, field, object, callback, init);
                    return;
                }
            } else {
                for (Class cls : componentClasses) {
                    if (insCl.instanceOf(cls)) {
                        Class[] possibleTypes = componentMap.get(cls);
                        if (insF.instanceOfAny(possibleTypes)) {
                            doBindWithCallback(element, field, object, callback, init);
                            return;
                        }
                    }
                }
            }
        }

        throw new Exception("Not bindable " + element.toString() + " with field:" + Arrays.asList(field.getPath()) + " of type:" + fieldType);

    }

    public static void bind(Component comp, Object object, Class type, String... steps) throws Exception {
        bindCustom(object, type, FieldChain.ObjectFieldChain.ofChain(steps), comp, null, false);
    }

    public static void bindInit(Component comp, Object object, Class type, String... steps) throws Exception {
        bindCustom(object, type, FieldChain.ObjectFieldChain.ofChain(steps), comp, null, true);
    }

    public static void bindString(Textbox box, Object object, String... steps) throws Exception {
        bind(box, object, String.class, steps);
    }

    public static void bindStringInit(Textbox box, Object object, String... steps) throws Exception {
        bindInit(box, object, String.class, steps);
    }

    public static void bindDate(Datebox box, Object object, String... steps) throws Exception {
        bind(box, object, Date.class, steps);
    }

    public static void bindDateInit(Datebox box, Object object, String... steps) throws Exception {
        bindInit(box, object, Date.class, steps);
    }

    public static void bindTime(Timebox box, Object object, String... steps) throws Exception {
        bind(box, object, Date.class, steps);
    }

    public static void bindTimeInit(Timebox box, Object object, String... steps) throws Exception {
        bindInit(box, object, Date.class, steps);
    }

    public static void bindBoolean(Checkbox box, Object object, String... steps) throws Exception {
        bind(box, object, Boolean.class, steps);
    }

    public static void bindBooleanInit(Checkbox box, Object object, String... steps) throws Exception {
        bindInit(box, object, Boolean.class, steps);
    }

}
