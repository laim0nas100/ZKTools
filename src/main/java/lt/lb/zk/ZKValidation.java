package lt.lb.zk;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lt.lb.commons.F;
import lt.lb.commons.SafeOpt;
import lt.lb.commons.containers.caching.LazyDependantValue;
import lt.lb.commons.containers.caching.LazyValue;
import lt.lb.commons.containers.values.BooleanValue;
import lt.lb.commons.containers.values.Value;
import lt.lb.commons.containers.values.ValueProxy;
import lt.lb.commons.func.unchecked.UnsafeSupplier;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.iteration.TreeVisitor;
import lt.lb.commons.iteration.Visitor;
import lt.lb.commons.parsing.StringOp;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Div;
import org.zkoss.zul.impl.InputElement;

/**
 *
 * @author laim0nas100
 */
public class ZKValidation {

    public static class ExternalValidationBuilder {

        private ExternalValidator validator;
        boolean staticComps = true;
        List<Supplier<? extends Component>> components = new ArrayList<>();

        public ExternalValidationBuilder with(Component... comps) {
            for (Component c : comps) {
                with(() -> c);
            }
            return this;
        }

        public ExternalValidationBuilder with(Supplier<? extends Component> c) {
            components.add(c);
            staticComps = false;
            return this;
        }

        public ExternalValidationBuilder1 withMessage(String str) {
            return withMessage(() -> str);
        }

        public ExternalValidationBuilder1 withMessage(Supplier<String> str) {
            if (this.components.isEmpty()) {
                throw new IllegalArgumentException("No components specified");
            }
            ExternalValidationBuilder1 b = new ExternalValidationBuilder1();
            b.prev = this;
            b.msg = str;
            b.validator = this.validator;
            return b;

        }
    }

    public static class ExternalValidationBuilder1 {

        private ExternalValidator validator;

        private ExternalValidationBuilder1() {
        }

        private ExternalValidationBuilder prev;
        private Supplier<String> msg;

        public ExternalValidation withValidation(Supplier<Boolean> supp) {

            ExternalValidation v = new ExternalValidation(supplier(this.prev), supp, msg);
            if (validator != null) {
                validator.add(v);
            }
            return v;
        }

        public ExternalValidation withValidation(Predicate<Component[]> pred) {
            Supplier<Component[]> supplier = supplier(this.prev);
            ExternalValidation v = new ExternalValidation(supplier, () -> pred.test(supplier.get()), msg);
            if (validator != null) {
                validator.add(v);
            }
            return v;
        }

        public ExternalValidation withSafeValidation(UnsafeSupplier<Boolean> supp) {
            return withValidation(() -> {
                try {
                    return supp.get();
                } catch (Throwable e) {
                    return false;
                }
            });
        }

        private Supplier<Component[]> supplier(ExternalValidationBuilder b) {

            Supplier<Component[]> supplier = _supplier(b);
            if (b.staticComps) { //optimize (call only once)
                return new LazyValue<>(supplier);
            }
            return supplier;
        }

        private Supplier<Component[]> _supplier(ExternalValidationBuilder b) {
            return () -> {
                return b.components.stream().map(m -> m.get()).toArray(s -> new Component[s]);
            };
        }
    }

    public static class ExternalValidation {

        public static ExternalValidationBuilder builder() {
            return new ExternalValidationBuilder();
        }

        private final Supplier<Component[]> component;
        private final Supplier<Boolean> valid;
        private final Supplier<String> message;

        public static ExternalValidation alwaysValid() {
            return builder().with(new Div()).withMessage("").withValidation(() -> true);
        }

        public ExternalValidation(Supplier<Component[]> c, Supplier<Boolean> valid, Supplier<String> msg) {
            component = c;
            this.valid = valid;
            message = msg;
        }

        public boolean isValid() {
            return valid.get();
        }

        public ExternalValidation addTo(ExternalValidator validator) {
            validator.add(this);
            return this;
        }

    }

    public static class ExternalValidator {

        private List<ExternalValidation> validations = new LinkedList<>();

        public ExternalValidator add(ExternalValidation extVal) {
            validations.add(extVal);
            return this;
        }

        public ExternalValidationBuilder builder() {
            ExternalValidationBuilder b = new ExternalValidationBuilder();
            b.validator = this;
            return b;
        }

        public ExternalValidator addRecursive(Component comp, boolean full) {
            ZKValidation.collectConstraints(t -> true, comp).stream()
                    .map(ref -> ref.buildExternalValidation(true))
                    .forEach(this::add);
            return this;
        }

        public ExternalValidator combine(ExternalValidator other) {
            ExternalValidator combined = new ExternalValidator();
            combined.validations.addAll(this.validations);
            combined.validations.addAll(other.validations);
            return combined;
        }

        public boolean isValid(boolean full) {
            return externalValidation(validations, full);
        }

        public boolean isValid() {
            return isValid(true);
        }

        public boolean isInvalid(boolean full) {
            return !isValid(full);
        }

        public boolean isInvalid() {
            return !isValid(true);
        }

    }

    // try to ensure uniqueness
    public static final String WRONG_VALUE_KEY = System.nanoTime() % 100 + "_ZK_Wrong_Value_Key_" + (System.nanoTime() + 50) % 1000;

    /**
     * Mark component with wrong value (does not affect display)
     *
     * @param comp
     * @param str
     */
    public static void _wrongValue(Component comp, String str) {
        comp.setAttribute(WRONG_VALUE_KEY, str);
    }

    /**
     * Mark component with no wrong value (does not affect display)
     *
     * @param comp
     */
    public static void _clearWrongValue(Component comp) {
        comp.removeAttribute(WRONG_VALUE_KEY);
    }

    public static boolean _markValidation(Component comp) {
        if (comp.hasAttribute(WRONG_VALUE_KEY)) {
            String msg = (String) comp.getAttribute(WRONG_VALUE_KEY);
            Clients.wrongValue(comp, msg);
            return false;
        } else {
            Clients.clearWrongValue(comp);
            return true;
        }
    }

    private static boolean _isValid(Component rootComp) {
        boolean isValid = _markValidation(rootComp);
        if (isValid && rootComp instanceof InputElement) {
            InputElement input = (InputElement) rootComp;
            Constraint cons = input.getConstraint();
            if (cons != null) {
                if (!(cons instanceof TransformedContraint)) {
                    TransformedContraint transformed = transformOld(cons);
                    input.setConstraint(transformed);
                }
                input.getText(); // invoke error if any
                boolean secondValidation = _markValidation(input);
                return secondValidation;
            }

        }
        return isValid;
    }

    private static class CompReformed {

        InputElement input;

        public CompReformed(InputElement input) {
            this.input = input;
        }

        public ExternalValidation buildExternalValidation(boolean replace) {
            LazyDependantValue<Optional<WrongValueException>> exSupl;
            if (!replace) {
                exSupl = new LazyDependantValue<>(() -> {
                    try {
                        input.getText();
                        return Optional.empty();
                    } catch (WrongValueException e) {
                        return Optional.ofNullable(e);
                    }
                });

            } else {
                Constraint constraint = input.getConstraint();
                
                Value<WrongValueException> proxyConst = new Value<>();
                exSupl = new LazyDependantValue<>(() -> {
                    input.getText();
                    return proxyConst.toOptional();
                });

                TransformedContraint newCon = (Component comp, Object value) -> {
                    try {
                        constraint.validate(comp, value);
                        proxyConst.set(null);
                    } catch (WrongValueException ex) {
                        proxyConst.set(ex);
                    }
                };

                input.setConstraint(newCon);
            }

            LazyDependantValue<String> msgSupl = exSupl.map(m -> m.map(ex -> ex.getMessage()).orElse(""));
            LazyDependantValue<Boolean> validLazySupl = exSupl.map(m -> !m.isPresent());
            Supplier<Boolean> validSupl = ()->{
                exSupl.invalidate();
                return validLazySupl.get();
            };
            return new ExternalValidationBuilder().with(input).withMessage(msgSupl).withValidation(validSupl);
        }

    }

    private static List<CompReformed> collectConstraints(Predicate<Component> includeFilter, Component root) {
        List<CompReformed> list = new ArrayList<>();
        getTreeVisitor(includeFilter, comp -> {
            if (comp instanceof InputElement) {
                InputElement input = (InputElement) comp;
                Constraint cons = input.getConstraint();
                if (cons != null) {
                    list.add(new CompReformed(input));
                }
            }
            return false;
        }).BFS(root, new HashSet<>());
        return list;

    }

    public static boolean externalValidation(Collection<ExternalValidation> validations, boolean full) {
        BooleanValue valid = new BooleanValue(true);
        Map<Component, List<ExternalValidation>> map = new LinkedHashMap<>();
        for (ExternalValidation v : validations) {
            Component[] get = v.component.get();
            for (Component c : get) {
                List<ExternalValidation> computeIfAbsent = map.computeIfAbsent(c, k -> new ArrayList<>());
                computeIfAbsent.add(v);
            }
        }
        F.find(map, (comp, validationList) -> {
            Optional<ExternalValidation> finalVal = F.find(validationList, (i, validation) -> !validation.isValid()).map(m -> m.g2);
            if (finalVal.isPresent()) { // invalid
                _wrongValue(comp, finalVal.get().message.get());
                valid.setFalse();
            } else {
                _clearWrongValue(comp);
            }
            _markValidation(comp);
            return !full && valid.not(); // early return

        });
        return valid.get();
    }

    public static boolean externalValidation(Collection<ExternalValidation> validations) {
        return externalValidation(validations, false);
    }

    public static boolean externalValidation(ExternalValidation... validations) {
        return externalValidation(Arrays.asList(validations), false);
    }

    public static boolean recursiveIsValid(Component... rootComps) {
        return recursiveIsValid(c -> true, rootComps);
    }

    public static boolean recursiveIsValidFull(Component... rootComps) {
        return recursiveIsValidFull(c -> true, rootComps);
    }

    public static boolean recursiveIsValid(Predicate<Component> includeFilter, Component... rootComps) {
        return recursiveIsValidTree(includeFilter, false, rootComps);
    }

    public static boolean recursiveIsValidFull(Predicate<Component> includeFilter, Component... rootComps) {
        return recursiveIsValidTree(includeFilter, true, rootComps);
    }

    private static TreeVisitor<Component> getTreeVisitor(Predicate<Component> includeFilter, Visitor<Component> compVisitor) {
        return TreeVisitor.of(compVisitor, c -> {
            if (includeFilter.test(c)) {
                return ReadOnlyIterator.of(c.getChildren());
            } else {
                return ReadOnlyIterator.of();
            }

        });
    }

    private static TreeVisitor<Component> getTreeValidationVisitor(Predicate<Component> includeFilter, boolean full, ValueProxy<Boolean> validSafe) {
        Visitor<Component> compVisitor = item -> {

            if (includeFilter.test(item)) {

                boolean valid = _isValid(item);

                if (!valid) {
                    validSafe.set(valid);
                }
                return !full && !valid;
            } else {
                return false;
            }
        };
        return TreeVisitor.of(compVisitor, c -> {
            if (includeFilter.test(c)) {
                return ReadOnlyIterator.of(c.getChildren());
            } else {
                return ReadOnlyIterator.of();
            }

        });
    }

    private static boolean recursiveIsValidTree(Predicate<Component> includeFilter, boolean full, Component... rootComps) {
        BooleanValue value = new BooleanValue(true);
        for (Component c : rootComps) {
            getTreeValidationVisitor(includeFilter, full, value).BFS(c);
            if (!full && value.not()) {
                return false;
            }
        }
        return value.get();
    }

    private static interface TransformedContraint extends Constraint {

    }

    private static TransformedContraint transformOld(Constraint cons) {
        return (Component comp, Object value) -> {
            try {
                cons.validate(comp, value);
                _clearWrongValue(comp);
            } catch (WrongValueException e) {
                _wrongValue(comp, e.getMessage());
            }
        };
    }

    public static class Premade {

        public static Supplier<Boolean> validationEnsureSelected(Combobox box) {
            return () -> box.getSelectedIndex() != -1;
        }

        public static Supplier<Boolean> validationNotBlank(InputElement elem) {
            return () -> StringOp.isNotBlank(elem.getText());
        }

        public static Supplier<Boolean> validationNoFuture(Datebox elem, boolean nullTolerance) {
            return () -> SafeOpt.of(elem)
                    .map(m -> m.getValue())
                    .map(m -> m.toInstant())
                    .map(m -> m.isBefore(Instant.now()))
                    .orElse(nullTolerance);

        }

        public static Supplier<Boolean> validationNoPast(Datebox elem, boolean nullTolerance) {
            return () -> SafeOpt.of(elem)
                    .map(m -> m.getValue())
                    .map(m -> m.toInstant())
                    .map(m -> m.isAfter(Instant.now()))
                    .orElse(nullTolerance);
        }

        public static Supplier<Boolean> validationEnsureContinuity(Datebox from, Datebox to, boolean nullTolerance) {

            return () -> SafeOpt.of(from)
                    .map(m -> m.getValue())
                    .map(m -> m.toInstant())
                    .flatMap(m -> {
                        return SafeOpt.ofNullable(to).map(n -> n.getValue())
                                .map(n -> n.toInstant()).map(n -> m.isBefore(n));
                    })
                    .orElse(nullTolerance);
        }
    }

}
