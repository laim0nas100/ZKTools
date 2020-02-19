package lt.lb.zk.dynamicrows;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.BindingValue;
import lt.lb.commons.F;
import lt.lb.commons.Java;
import lt.lb.commons.Log;
import lt.lb.commons.ReflectionUtils;
import lt.lb.commons.containers.tuples.Tuple;
import lt.lb.commons.containers.tuples.Tuples;
import lt.lb.commons.containers.values.ValueProxy;
import lt.lb.commons.iteration.ReadOnlyIterator;
import lt.lb.commons.misc.ExtComparator;
import lt.lb.commons.misc.NestedException;
import lt.lb.zk.ZKComponents;
import lt.lb.zk.ZKValidation;
import lt.lb.zk.ZKValidation.ExternalValidation;
import lt.lb.zk.ZKValidation.ExternalValidator;
import org.zkoss.bind.BindUtils;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.event.CheckEvent;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listhead;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

/**
 *
 * @author laim0nas100
 */
public class DynamicRow {

    @Override
    public String toString() {
        return "DynamicRow{" + "key=" + key + '}';
    }

    public static class UiUpdate {

        public Supplier<Component> comp;
        public ValueProxy proxy;
        public Optional<BiConsumer<Component, Consumer>> updateForm = Optional.empty();
        public Optional<BiConsumer<Supplier, Component>> updateUi = Optional.empty();

        public void updateForm() {
            updateForm.ifPresent(consumer -> {
                if (comp != null && proxy != null) {
                    consumer.accept(comp.get(), proxy);
                }
            });
        }

        public void updateUI() {
            updateUi.ifPresent(consumer -> {
                if (comp != null && proxy != null) {
                    consumer.accept(proxy, comp.get());
                }
            });
        }
    }

    private Map<Class, BiConsumer<DynamicRow, Component>> decorators = new HashMap<>();
    private Cell cellTemplate;
    private Component row;
    private BindingValue<Boolean> disabled = new BindingValue<>(false);
    private boolean done = false;
    private ArrayList<Cell> cells = new ArrayList<>();
    private ArrayList<Integer> cellColSpan = new ArrayList<>();
    private final String key;
    private BindingValue<Long> updater = new BindingValue<>(Java.getNanoTime());
    private BindingValue<Long> updateFinal = new BindingValue<>();
    private BindingValue<Boolean> visibleListener = new BindingValue<>(true);
    private ExternalValidator validator = new ZKValidation.ExternalValidator();
    private BiConsumer<DynamicRow, List<Cell>> rowMaker;
    private List<Runnable> onDisplay = new LinkedList<>();
    private List<Runnable> viewUpdatesList = new LinkedList<>();
    private List<Runnable> formUpdatesList = new LinkedList<>();

    private List<Tuple<Integer, Consumer<DynamicRow>>> listeners = new ArrayList<>();
    private Set<String> tags = new HashSet<>();
    private boolean deleted;

    private ArrayList<RowRunDecor> runDecor = new ArrayList<>();

    public static enum DecorType {
        UPDATE, UI, VIEW, FORM
    }

    private void doRun(DecorType type, Runnable action) {
        runDecor.stream()
                .filter(f -> f.predicate.test(this))
                .filter(f -> f.acceptableTypes.contains(type))
                .reduce((a, b) -> a.compose(b))
                .get().decorator.accept(action); //must be atleast one for every type
    }

    private BiConsumer<Long, Long> updateFinalListener = (oldTime, newTime) -> {
//        Log.print("Update " + getKey());
        if (deleted) {
            return;
        }
        this.row.invalidate();
        this.updateDisplay();
        for (Cell cell : cells) {
            cell.invalidate();
            for (Component c : cell.getChildren()) {
                BindUtils.postNotifyChange(null, null, c, "*");
                c.invalidate();
            }

        }
    };

    private void updateDisplay() {

        int maxColSpan = DynamicRows.maxTotalColspan;
        if (!this.needUpdate(maxColSpan)) {
            return;
        }
        List<Cell> visibleCells = this.getCells().stream().filter(c -> c.isVisible()).collect(Collectors.toList());
        if (visibleCells.isEmpty()) {
            return;
        }
        ArrayList<Integer> preferedColSpan = this.getPreferedColSpanOfVisible();
        double preferedTotal = preferedColSpan.stream().mapToDouble(m -> m.doubleValue()).sum();

        Integer[] colApply = new Integer[preferedColSpan.size()];
        F.iterate(preferedColSpan, (i, pref) -> {
            double rat = pref / preferedTotal;
            colApply[i] = (int) Math.floor(rat * maxColSpan);
        });

        int newTotalColspan = Stream.of(colApply).mapToInt(m -> m).sum();

        int deficit = maxColSpan - newTotalColspan;

//        int cellCount = this.getCellCount();
        while (deficit > 0) {
            for (int i = 0; i < colApply.length; i++) {
                if (deficit <= 0) {
                    break;
                }
                colApply[i] = colApply[i] + 1;
                deficit--;
            }
        }
//        List<Integer> oldColSpan = visibleCells.stream().map(m -> m.getColspan()).collect(Collectors.toList());
//        Log.print("Change collspan of", this.getKey(), this.getVisibleIndices(), oldColSpan, Arrays.asList(colApply));

        for (int i = 0; i < colApply.length; i++) {
            visibleCells.get(i).setColspan(colApply[i]);
        }
    }

    public DynamicRow(String key, Component row, Cell template,
            BiConsumer<DynamicRow, List<Cell>> rowMaker,
            Collection<Tuple<Class, BiConsumer<DynamicRow, Component>>> dec,
            BiConsumer<Boolean, DynamicRow> disableDecorator) {
        this.row = row;
        this.key = key;
        this.cellTemplate = template;

        updater.bindPropogate(updateFinal);
        updateFinal.addListener(updateFinalListener);
        disabled.addListener((b4, now) -> {
            if (!Objects.equals(b4, now)) {
                disableDecorator.accept(now, this);
            }
        });
        visibleListener.addListener((b4, now) -> {
            if (deleted) {
                return;
            }
            row.setVisible(now);
        });

        updater.addListener(fireListeners -> {
            ExtComparator<Tuple<Integer, Consumer<DynamicRow>>> cmp = ExtComparator.ofValue(v -> v.g1);
            listeners.stream().sorted(cmp.reversed()).map(m -> m.g2).forEach(action -> {
                doRun(DecorType.UPDATE, () -> action.accept(this));
            });
        });
        F.iterate(dec, (i, t) -> {
            this.decorators.put(t.g1, t.g2);
        });
        this.rowMaker = rowMaker;

        onDisplay.add(() -> {
            this.rowMaker.accept(this, cells);
        });
        onDisplay.add(() -> {
            F.iterate(mainIterator(), (i, tup) -> {
                Component comp = tup.getG2();
                BiConsumer<DynamicRow, Component> get = decorators.get(comp.getClass());
                if (get != null) {
                    get.accept(this, comp);
                }
            });

        });
        //add default decorator
        this.runDecor.add(new RowRunDecor.RowRunDecorBuilder().withAllTypes().withDecorator(Runnable::run));

    }

    public Component getRow() {
        return row;
    }

    public String getKey() {
        return key;
    }

    public DynamicRow add(Component comp) {

        finalAdd(Arrays.asList(comp), Hlayout::new, this.cells, 1, this.cellColSpan);
        return this;
    }

    private void finalAdd(List<Component> comp, Supplier<? extends HtmlBasedComponent> enclosingSupp, List<Cell> cellArray, int colSpan, List<Integer> cellColSpanArray) {
        Cell cell = (Cell) cellTemplate.clone();
        HtmlBasedComponent enclosing = enclosingSupp.get();
        for (Component c : comp) {
            if (c instanceof HtmlBasedComponent) {
                HtmlBasedComponent hc = F.cast(c);
                hc.setHflex("1");
            }
            enclosing.appendChild(c);
            enclosing.setHflex("max");

        }
        int indexToAdd = cellArray.size();
        cellArray.add(cell);
        cell.setColspan(colSpan);

        cell.appendChild(enclosing);
        if (indexToAdd >= cellColSpanArray.size()) {
            cellColSpanArray.add(cell.getColspan());
        }

    }
    
    public DynamicRow mergeLast(Supplier<? extends HtmlBasedComponent> enclosing, int lastCount){
        int[] indexes = new int[lastCount];
        int lastIndex = this.getComponentCount() - 1;
        
        for(int i = 0; i < lastCount; i++){
            int j = indexes.length - 1 - i;
            indexes[j] = lastIndex;
            lastIndex--;
            
        }
        
        return merge(enclosing, indexes);
        
    }

    public DynamicRow merge(Supplier<? extends HtmlBasedComponent> enclosing, int... comps) {
        if (comps.length <= 1) {// no op
            return this;
        }
        for (int i = 1; i < comps.length; i++) {
            int prev = comps[i - 1];
            if (prev + 1 != comps[i]) { // sequential
                throw new IllegalArgumentException("Only sequential merging is allowed");
            }
        }
        for (int i = 0; i < comps.length; i++) {
            final int num = comps[i];
            Cell get = getCellSupplier(p -> p.g1 == num).get();
            if (get.getChildren().get(0).getChildren().size() != 1) {
                throw new IllegalArgumentException("component num:" + num + " is part of a merged cell, can only merge free cells");
            }
        }

        ArrayList<Tuple<Integer, Cell>> before = new ArrayList<>();
        ArrayList<Tuple<Integer, Cell>> merge = new ArrayList<>();
        ArrayList<Tuple<Integer, Cell>> after = new ArrayList<>();

        ArrayList<Cell> newCells = new ArrayList<>();
        ArrayList<Integer> newColSpans = new ArrayList<>();

        int beforeIndex = comps[0];
        int afterIndex = comps[comps.length - 1];

        F.iterate(mainIterator(), (i, tup) -> {
            Cell cell = tup.g1;
            int cellIndex = cells.indexOf(cell);
            Integer colSpan = cellColSpan.get(cellIndex);
            Tuple<Integer, Cell> tuple = Tuples.create(colSpan, cell);

            if (i < beforeIndex) {
                if (!before.contains(tuple)) {
                    before.add(tuple);

                }

            } else if (i <= afterIndex) {
                //should not be shared cells, because we checked before
                merge.add(tuple);
            } else if (i > afterIndex) {
                if (!after.contains(tuple)) {
                    after.add(tuple);
                }
            }

        });
        //insert before merged

        F.iterate(before, (i, tup) -> {
            newCells.add(tup.getG2());
            newColSpans.add(tup.g1);
        });

        int mergedColspan = merge.stream().mapToInt(m -> m.g1).sum();
        List<Component> mergedComp = merge.stream()
                .map(m -> m.g2.getChildren().get(0).getChildren().get(0))
                .collect(Collectors.toList());
        this.finalAdd(mergedComp, enclosing, newCells, mergedColspan, newColSpans);

        //insert after merged
        F.iterate(after, (i, tup) -> {
            newCells.add(tup.getG2());
            newColSpans.add(tup.g1);
        });

        this.cells = newCells;
        this.cellColSpan = newColSpans;

        return this;

    }

    public <T extends Component> DynamicRow add(T comp, Consumer<T> cons) {
        cons.accept(comp);
        return this.add(comp);
    }

    public DynamicRow add(Supplier<Component> sup) {
        return this.add(sup.get());
    }

    public DynamicRow addSpace() {
        return addSpace("5px");
    }
    public DynamicRow addSpace(String spacing) {
        Space s = new Space();
        s.setSpacing(spacing);
        return this.add(s);
    }

    public DynamicRow addLabel(String str) {
        Label label = new Label(str);
        return add(label);
    }

    public DynamicRow addLabelWithUpdate(Supplier<String> stringSupplier) {
        Label label = new Label();
        this.add(label);

        return this.withUpdateListener(r -> {
            label.setValue(stringSupplier.get());
        });

    }

    public <T> DynamicRow addRadioCombobox(RadioComboboxMapper<T> mapper) {

        boolean updatesNotEmpty = !mapper.getOnSelectionUpdate().isEmpty();
        if (mapper.isRadio()) {
            Radiogroup radio = new Radiogroup();

            Component radioParent;
            if (mapper.isVertical()) {
                Vbox vb = new Vbox();
                vb.setAlign("left");
                radioParent = vb;
                radio.appendChild(vb);
            } else {
                radio.setOrient("horizontal");
                radioParent = radio;
            }

            for (String item : mapper.getNames()) {
                Radio r = new Radio(item);
                radioParent.appendChild(r);
                r.setRadiogroup(radio);
                r.setDisabled(mapper.isDisabled());
            }
            mapper.radio = radio;
            if (mapper.getPreselectedIndex() != -1) {
                radio.setSelectedIndex(mapper.getPreselectedIndex());
            }
            if (updatesNotEmpty) {
                radio.addEventListener(Events.ON_SELECT, l -> {
                    this.update();
                });
            }
        } else {
            Combobox combo = new Combobox();
            for (String item : mapper.getNames()) {
                combo.appendItem(item);
            }
            combo.setReadonly(mapper.isReadOnly());
            combo.setDisabled(mapper.isDisabled());
            mapper.combo = combo;
            if (mapper.getPreselectedIndex() != -1) {
                combo.setSelectedIndex(mapper.getPreselectedIndex());
            }
            combo.addEventListener(Events.ON_SELECT, l -> {
                this.update();
            });
        }
        if (updatesNotEmpty) {
            this.withUpdateListener(r -> {
                mapper.getOnSelectionUpdate().forEach(Runnable::run);
            });
        }

        return mapper.isRadio() ? this.add(mapper.radio) : this.add(mapper.combo);

    }

    public DynamicRow addRadiobox(String... things) {
        return addRadioCombobox(RowComp.comboNames(things).withRadio(true));
    }

    public DynamicRow addCombobox(String... things) {
        return addRadioCombobox(RowComp.comboNames(things).withReadOnly(true));
    }

    public DynamicRow addComponentDirectly(String compName) {
        return this.add(() -> ZKComponents.createZulComponent(compName));
    }

    public <T extends Component> DynamicRow addComponentDirectly(String compName, Consumer<T> decorator) {
        return this.add((T) ZKComponents.createZulComponent(compName), decorator);
    }

    public DynamicRow addZulDirectly(String path) {
        return this.add(() -> ZKComponents.createZul(path));
    }

    public DynamicRow addHeader1(String str) {
        Label label = new Label(str);
        label.setSclass("path-link-inner");
        label.setWidth("100%");

        return add(label);
    }

    public DynamicRow addHeader1Update(Supplier<String> str) {
        Label label = new Label();
        label.setSclass("path-link-inner");
        label.setWidth("100%");
        add(label);

        return this.withUpdateListener(r -> {
            label.setValue(str.get());
        });
    }

    public DynamicRow addHeader2(String str) {
        Label label = new Label(str);
        label.setSclass("path-link-inner2");
        label.setWidth("100%");

        return add(label);
    }

    public DynamicRow addHeader2Update(Supplier<String> str) {
        Label label = new Label();
        label.setSclass("path-link-inner2");
        label.setWidth("100%");
        add(label);

        return this.withUpdateListener(r -> {
            label.setValue(str.get());
        });
    }

    public DynamicRow addBoundElement(Supplier<Component> comp, ValueProxy proxy, BiConsumer<Component, Consumer> updateForm, BiConsumer<Supplier, Component> updateView) {
        UiUpdate ui = new UiUpdate();
        ui.comp = comp;
        ui.proxy = proxy;
        ui.updateForm = Optional.ofNullable(updateForm);
        ui.updateUi = Optional.ofNullable(updateView);
        withFormUpdate(r -> {
            ui.updateForm();
        });
        withViewUpdate(r -> {
            ui.updateUI();
        });
        return add(comp);
    }

    public <T extends Component> DynamicRow addBoundElementSimple(Supplier<T> comp, Consumer<T> updateForm, Consumer<T> updateView) {
        T component = comp.get();

        this.formUpdatesList.add(() -> {
            updateForm.accept(component);
        });

        this.viewUpdatesList.add(() -> {
            updateView.accept(component);
        });

        return add(component);
    }

    public DynamicRow addBoundTextbox(Textbox box, ValueProxy<String> data) {
        BiConsumer<Component, Consumer> formUpdate = (comp, cons) -> {
            Textbox tb = F.cast(comp);
            cons.accept(tb.getText());
        };
        BiConsumer<Supplier, Component> uiUpdate = (supp, comp) -> {
            Textbox tb = F.cast(comp);
            tb.setText(F.cast(supp.get()));
        };
        return this.addBoundElement(() -> box, data, formUpdate, uiUpdate);
    }

    public DynamicRow addBoundTextbox(Supplier<Textbox> supplier, ValueProxy<String> data) {
        return this.addBoundTextbox(supplier.get(), data);
    }

    public DynamicRow addBoundCheckbox(Checkbox box, ValueProxy<Boolean> data) {
        BiConsumer<Component, Consumer> formUpdate = (comp, cons) -> {
            Checkbox check = F.cast(comp);
            cons.accept(check.isChecked());
        };
        BiConsumer<Supplier, Component> uiUpdate = (supp, comp) -> {
            Checkbox check = F.cast(comp);
            check.setChecked(F.cast(supp.get()));
        };
        return this.addBoundElement(() -> box, data, formUpdate, uiUpdate);
    }

    public DynamicRow addBoundCheckbox(Supplier<Checkbox> supplier, ValueProxy<Boolean> data) {
        return this.addBoundCheckbox(supplier.get(), data);
    }

    public DynamicRow addBoundCheckbox(Supplier<Checkbox> supplier, Supplier<Boolean> get, Consumer<Boolean> set) {
        return this.addBoundCheckbox(supplier.get(), new ValueProxy<Boolean>() {
            @Override
            public Boolean get() {
                return get.get();
            }

            @Override
            public void set(Boolean t) {
                set.accept(t);
            }
        });
    }

    public DynamicRow addBoundDatebox(Datebox box, ValueProxy<Date> data) {
        BiConsumer<Component, Consumer> formUpdate = (comp, cons) -> {
            Datebox datebox = F.cast(comp);
            cons.accept(datebox.getValue());
        };
        BiConsumer<Supplier, Component> uiUpdate = (supp, comp) -> {
            Datebox datebox = F.cast(comp);
            datebox.setValue(F.cast(supp.get()));
        };
        return this.addBoundElement(() -> box, data, formUpdate, uiUpdate);
    }

    public DynamicRow addBoundDatebox(Supplier<Datebox> box, ValueProxy<Date> data) {
        return this.addBoundDatebox(box.get(), data);
    }

    public DynamicRow withViewUpdate(Consumer<DynamicRow> update) {
        DynamicRow me = this;
        this.viewUpdatesList.add(0, () -> {
            update.accept(me);
        });
        return this;
    }

    public DynamicRow withFormUpdate(Consumer<DynamicRow> update) {
        DynamicRow me = this;
        this.formUpdatesList.add(() -> {
            update.accept(me);
        });
        return this;
    }

    public DynamicRow updateView() {
        if (deleted || !isVisible()) {
            return this;
        }
        viewUpdatesList.forEach(action -> {
            doRun(DecorType.VIEW, () -> action.run());
        });
        return update();
    }

    public DynamicRow updateForm() {
        if (deleted || isDisabled()) {
            return this;
        }

        formUpdatesList.forEach(action -> {
            doRun(DecorType.FORM, () -> action.run());
        });
        this.update();
        return this;
    }

    public DynamicRow addList(ListModel<String> list) {
        Listbox listbox = new Listbox();
        listbox.setModel(list);
        return add(listbox);
    }

    public <T> DynamicRow addListElems(ListitemRenderer<T> renderer, ListModel<T> list) {
        Listbox listbox = new Listbox();
        listbox.appendChild(new Listhead());
        listbox.setModel(list);
        listbox.setItemRenderer(renderer);
        return add(listbox);
    }

    public <T> DynamicRow addList(ListitemRenderer<T> renderer, Supplier<Collection<T>> supp) {
        ListModelList<T> model = new ListModelList<>();

        this.withUpdateListener(r -> {
            model.clear();
            model.addAll(supp.get());
        });
        return addListElems(renderer, model);
    }

    public <T, R> DynamicRow addList(ListitemRenderer<R> renderer, Supplier<Collection<T>> collection, Function<? super T, R> mapper) {
        return addList(renderer, () -> {
            return collection.get().stream().map(mapper).collect(Collectors.toList());
        });

    }

    public DynamicRow addButton(String title, EventListener event) {
        Button but = new Button(title);
        but.addEventListener(Events.ON_CLICK, e -> {
            doRun(DecorType.UI, () -> F.unsafeRun(() -> event.onEvent(e)));
        });
        return this.add(but);
    }

    public DynamicRow addButton(Button but, EventListener event) {
        but.addEventListener(Events.ON_CLICK, e -> {
            doRun(DecorType.UI, () -> F.unsafeRun(() -> event.onEvent(e)));
        });
        return this.add(but);
    }

    public DynamicRow addCheckbox(boolean initial, Consumer<Boolean> onChange) {
        Checkbox box = new Checkbox();
        box.setChecked(initial);
        box.addEventListener(Events.ON_CHECK, (CheckEvent ev) -> {
            onChange.accept(ev.isChecked());
        });
        return this.add(box);
    }

    public <T extends Component> DynamicRow addCloned(T comp, Consumer<T> decorator) {
        return this.add(F.cast(comp.clone()), decorator);
    }

    public DynamicRow setVisible(boolean b) {
        this.visibleListener.accept(b);
        return this;
    }

    public DynamicRow visible() {
        return setVisible(true);
    }

    public DynamicRow invisible() {
        return setVisible(false);
    }

    public boolean isVisible() {
        return visibleListener.get();
    }

    public void markDeleted(boolean del) {
        this.deleted = del;
    }

    public DynamicRow withUpdateListener(Consumer<DynamicRow> listener) {
        this.listeners.add(Tuples.create(0, listener));
        return this;
    }

    public DynamicRow withUpdateListener(int priority, Consumer<DynamicRow> listener) {
        this.listeners.add(Tuples.create(priority, listener));
        return this;
    }

    public DynamicRow withVisibleListener(Consumer<DynamicRow> listener) {
        this.visibleListener.addListener((o, n) -> {
            listener.accept(this);
        });
        return this;
    }

    public DynamicRow withTag(String... tag) {
        for (String t : tag) {
            this.tags.add(t);
        }
        return this;
    }

    public DynamicRow withStyle(int index, String style) {
        return this.withComponentDecorator(index, c -> {
            if (c instanceof HtmlBasedComponent) {
                HtmlBasedComponent comp = F.cast(c);
                comp.setStyle(style);
            }
        });
    }

    public Cell getCell(int index) {
        if (index < 0 || index >= cells.size()) {
            throw new IllegalArgumentException("Cell index out of range " + index + " size:" + cells.size());
        }
        return cells.get(index);
    }

    private ReadOnlyIterator<Tuple<Cell, Component>> mainIterator() {
        ArrayList<Cell> cellCopy = new ArrayList<>(cells.size());
        cells.forEach(cellCopy::add);
        int total = getComponentCount();
        return new ReadOnlyIterator<Tuple<Cell, Component>>() {
            int cellIndex = 0;
            int compIndex = -1;
            int totalIndex = -1;

            private Cell getCurrentCell() {
                return cellCopy.get(cellIndex);
            }

            private Component getCurrentComp() {
                return getCurrentCell().getChildren().get(0).getChildren().get(compIndex);
            }

            @Override
            public Tuple<Cell, Component> getCurrent() {
                return totalIndex >= 0 ? Tuples.create(getCurrentCell(), getCurrentComp()) : null;
            }

            @Override
            public Integer getCurrentIndex() {
                return totalIndex;
            }

            @Override
            public boolean hasNext() {
                return totalIndex + 1 < total;
            }

            private int currentCellChildrenSize() {
                return getCurrentCell().getChildren().get(0).getChildren().size();
            }

            @Override
            public Tuple<Cell, Component> next() {
                if (hasNext()) {

                    if (compIndex + 1 < currentCellChildrenSize()) { // must get component from this cell
                        compIndex++;
                        totalIndex++;
                        return getCurrent();
                    } else { // increment cell index
                        compIndex = 0;
                        cellIndex++;
                        totalIndex++;
                        return getCurrent();
                    }
                } else {
                    throw new NoSuchElementException();
                }

            }
        };
    }
    
    public int getComponentCount(){
        return cells.stream().mapToInt(m -> m.getChildren().get(0).getChildren().size()).sum();
    }
    
    public List<Component> getComponents(){
        return mainIterator().map(m->m.g2).toArrayList();
    }

    public Supplier<Cell> getCellSupplier(Predicate<Tuple<Integer, Component>> pred) {
        return () -> {
            return F.find(mainIterator(), (i, tuple) -> pred.test(Tuples.create(i, tuple.g2)))
                    .map(m -> m.g2.g1).orElse(null);
        };
    }

    public Supplier<Component> getComponentSupplier(Integer i) {
        return () -> {
            return F.find(mainIterator(), (j, tuple) -> Objects.equals(i, j))
                    .map(m -> m.g2.g2).orElse(null);
        };
    }

    public Supplier<Component> getComponentSupplier(Predicate<Component> pred) {

        return () -> {
            return F.find(mainIterator(), (j, tuple) -> pred.test(tuple.g2))
                    .map(m -> m.g2.g2).orElse(null);
        };
    }

    public <T extends Component> T getComponent(Predicate<Component> pred) {
        return F.cast(this.getComponentSupplier(pred).get());
    }

    public <T extends Component> T getComponent(Integer i) {
        return F.cast(getComponentSupplier(i).get());
    }

    public DynamicRow updateVisible() {
        if (deleted) {
            return this;
        }
        return this.setVisible(this.isVisible());
    }

    public DynamicRow update() {
        if (deleted) {
            return this;
        }
//        Log.print("UPDATE", this.getKey());
        this.updater.accept(Java.getNanoTime());
        return this;
    }

    public DynamicRow updateDependsOn(DynamicRow other) {
        other.updater.bindPropogate(this.updater);
        return this;
    }

    public DynamicRow updatePropagateTo(DynamicRow other) {
        this.updater.bindPropogate(other.updater);
        return this;
    }

    public DynamicRow visibleDependsOn(DynamicRow other) {
        other.visibleListener.bindPropogate(this.visibleListener);
        return this;
    }

    /**
     * Only if false, then hide me as well
     *
     * @param other
     * @return
     */
    public DynamicRow visibleDependsOnHide(DynamicRow other) {
        other.visibleListener.addListener((ovis, nvis) -> {
            if (!nvis) {
                this.invisible();
            }
        });
        return this;
    }

    /**
     * Only if false, then hide me as well
     *
     * @param other
     * @return
     */
    public DynamicRow visiblePropogateToHide(DynamicRow other) {
        this.visibleListener.addListener((ovis, nvis) -> {
            if (!nvis) {
                other.invisible();
            }
        });
        return this;
    }

    public DynamicRow visiblePropagateTo(DynamicRow other) {
        this.visibleListener.bindPropogate(other.visibleListener);
        return this;
    }

    public DynamicRow disablePropogateTo(DynamicRow other) {
        this.disabled.bindPropogate(other.disabled);
        return this;
    }

    public DynamicRow disableDependsOn(DynamicRow other) {
        other.disabled.bindPropogate(this.disabled);
        return this;
    }

    public DynamicRow setDisabled(boolean dis) {
        disabled.set(dis);
        return this;
    }

    public boolean checkIsInvalid(boolean full) {
        return validator.isInvalid(full);
    }

    public DynamicRow withValidation(ZKValidation.ExternalValidation valid) {
        this.validator.add(valid);
        return this;
    }

    public DynamicRow withValidationMaker(Function<DynamicRow, ZKValidation.ExternalValidation> maker) {
        return withValidation(maker.apply(this));
    }

    public DynamicRow withValidation(Supplier<String> msg, Supplier<Boolean> isValid) {

        return withValidation(ExternalValidation.builder().with(this.getRow()).withMessage(msg).withValidation(isValid));
    }

    public DynamicRow withValidation(String msg, Supplier<Boolean> isValid) {
        return this.withValidation(() -> msg, isValid);
    }

    public DynamicRow withValidation(Supplier<String> msg, Function<DynamicRow, Boolean> isValid) {
        return this.withValidation(msg, () -> isValid.apply(this));
    }

    public DynamicRow withValidation(String msg, Function<DynamicRow, Boolean> isValid) {
        return this.withValidation(msg, () -> isValid.apply(this));
    }

    public DynamicRow withCellDecorator(DynamicRowCellDecorator... decs) {
        for (DynamicRowCellDecorator dec : decs) {
            addOnDisplayAndRunIfDone(() -> {
                dec.decorate(cells);
            });
        }
        return this;
    }

    private DynamicRow addOnDisplayAndRunIfDone(Runnable run) {
        if (done) {
            run.run();
        }
        onDisplay.add(run);
        return this;
    }

    public DynamicRow withPreferedColspan(Integer... spans) {
        F.iterate(spans, (i, spa) -> {
            this.cellColSpan.set(i, spa);
            this.getCell(i).setColspan(spa);
        });

        return this;
    }

    public DynamicRow withRowDecorator(DynamicRowDecorator... decs) {
        for (DynamicRowDecorator dec : decs) {
            dec.decorate(this);
        }
        return this;
    }

    public DynamicRow withComponentDecorator(int index, Consumer<Component> cons) {
        return this.addOnDisplayAndRunIfDone(() -> {
            cons.accept(getComponent(index));
        });
    }

    public <T> DynamicRow withComponentTypeDecorator(Class<T> cls, BiConsumer<DynamicRow, Component> decorator) {
        this.decorators.put(cls, decorator);
        return this;
    }

    public DynamicRow withRunDecor(RowRunDecor decor) {
        this.runDecor.add(decor);
        return this;
    }

    public DynamicRow display(boolean updateView) {

        if (!done) {
            Optional<Throwable> optionalException = F.checkedRun(() -> {
                onDisplay.forEach(runnable -> runnable.run());
                this.update();
            });
            optionalException.ifPresent(ex -> {
                throw NestedException.of(ex);
            });
            done = true;
        }
        if (updateView) {
            this.updateView();
        }

        return this;

    }

    public DynamicRow display() {
        return display(true);
    }

    public boolean isDisabled() {
        return this.disabled.get();
    }

    public boolean isDone() {
        return this.done;
    }

    public int getCellCount() {
        return this.cells.size();
    }

    public int getTotalColSpan() {
        return this.getCells().stream().mapToInt(m -> m.getColspan()).sum();
    }

    public int getTotalColSpanVisible() {
        return this.getCells().stream().filter(c -> c.isVisible()).mapToInt(m -> m.getColspan()).sum();
    }

    public boolean needUpdate(int maxVisibleRowSpan) {
        int totalVisible = this.getTotalColSpanVisible();

        if (maxVisibleRowSpan != totalVisible || totalVisible > DynamicRows.maxTotalColspan) {
            return true;
        }
        //calculate real colspans
        ArrayList<Integer> index = this.getVisibleIndices();
        int count = index.size();

        ArrayList<Integer> preferedColSpanOfVisible = this.getPreferedColSpanOfVisible();
        double total = preferedColSpanOfVisible.stream().mapToInt(m -> m).sum();

        for (int i = 0; i < count; i++) {
            double ratioPrefered = preferedColSpanOfVisible.get(i) / total;
            int cellIndex = index.get(i);
            double ratioCurrent = getCell(cellIndex).getColspan();
            ratioCurrent = ratioCurrent / totalVisible;
            if (!equalWithinMargin(ratioCurrent, ratioPrefered, 0.05)) {
                return true;
            }

        }
        return false;
    }

    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }

    private static boolean equalWithinMargin(double d1, double d2, double margin) {
        return Math.abs(d1 - d2) <= Math.abs(margin);
    }

    public ArrayList<Integer> getVisibleIndices() {
        ArrayList<Integer> list = new ArrayList<>();
        F.iterate(cells, (i, cell) -> {
            if (cell.isVisible()) {
                list.add(i);
            }
        });

        return list;
    }

    public ArrayList<Cell> getCells() {
        return this.cells;
    }

    public ArrayList<Integer> getPreferedColSpan() {
        return this.cellColSpan;
    }

    public ArrayList<Integer> getPreferedColSpanOfVisible() {
        ArrayList<Integer> prefVis = new ArrayList<>();
        F.iterate(cells, (i, c) -> {
            if (c.isVisible()) {
                prefVis.add(getPreferedColSpan().get(i));
            }
        });
        return prefVis;
    }

    public ArrayList<Integer> getColSpanOfVisible() {
        ArrayList<Integer> prefVis = new ArrayList<>();
        F.iterate(cells, (i, c) -> {
            if (c.isVisible()) {
                prefVis.add(c.getColspan());
            }
        });
        return prefVis;
    }

    public DynamicRow clear() {
        done = false;
        cells.clear();
        return this;
    }

    private void printCellConfig() {
        StringBuilder rowww = new StringBuilder();
        for (Cell c : cells) {
            StringBuilder sb = new StringBuilder();
            sb.append("cell=").append(c).append(" with =");
            sb.append(c.getChildren().get(0));
            F.unsafeRun(() -> {
                Class cl = c.getClass();
                while (!Object.class.equals(cl)) {
                    Field field = cl.getDeclaredField("_auxinf");
                    field.setAccessible(true);
                    Object auxinfo = field.get(c);
                    sb.append(ReflectionUtils.reflectionString(auxinfo, 1));
                    cl = cl.getSuperclass();
                }

            });
            rowww.append(sb).append(" ");
        }
        Log.print(rowww);
    }

}
