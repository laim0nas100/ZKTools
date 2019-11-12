package lt.lb.zk.dynamicrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.F;
import lt.lb.commons.Log;
import lt.lb.commons.UUIDgenerator;
import lt.lb.commons.containers.caching.LazyDependantValue;
import lt.lb.commons.misc.ExtComparator;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Grid;

/**
 *
 * @author laim0nas100
 */
public class DynamicRows {

    private DynamicRow updaterRow;
    static final int maxTotalColspan = 1000;
    private Component rootComponent;
    private Component rowComponent;
    private Supplier<Component> parentSupplier;
    private Map<String, DynamicRow> rowMap = new HashMap<>();
    private List<String> keyOrder = new ArrayList<>();
    private DynamicRowFactory dynamicRowFactory;
    private String composableKey;

    private Map<String, DynamicRows> composable = new HashMap<>();

    private LazyDependantValue<Map<String, Integer>> rowKeyOrder = new LazyDependantValue<>(() -> {
        HashMap<String, Integer> indexMap = new HashMap<>();
        for (DynamicRow row : rowMap.values()) {
            indexMap.put(row.getKey(), this.getRowIndex(row.getKey()));
        }
//        for (DynamicRows rows : composable.values()) {
//            indexMap.put(rows.composableKey, this.getRowIndex(rows.composableKey));
//        }
        return indexMap;
    });
    private LazyDependantValue<List<DynamicRow>> rowsInOrder = rowKeyOrder.map(m -> {
        List<DynamicRow> collect = rowMap.values().stream().collect(Collectors.toList());
        ExtComparator<DynamicRow> ofValue = ExtComparator.ofValue(r -> m.getOrDefault(r.getKey(), -1));
        Collections.sort(collect, ofValue);
        return collect;
    });

    private LazyDependantValue<List> dynamicRowsAndRowsInOrder = rowsInOrder.map(m -> {

        Map<Integer, List> composed = new HashMap<>();

        F.iterate(this.composable, (key, rows) -> {
            int index = Math.max(rowKeyOrder.get().getOrDefault(key, 0), 0);
            composed.computeIfAbsent(index, i -> new LinkedList<>()).add(rows);
        });

        F.iterate(m, (key, row) -> {
            composed.computeIfAbsent(key, i -> new LinkedList<>()).add(row);
        });

        Stream<Object> flatMap = composed.entrySet().stream().sorted(ExtComparator.ofValue(v -> v.getKey()))
                .map(entry -> entry.getValue())
                .flatMap(list -> list.stream());

        List collect = flatMap.collect(Collectors.toList());
        return collect;
    });

    public DynamicRows(Component root, Component rows, DynamicRowFactory factory, String key) {
        rootComponent = root;
        rowComponent = rows;
        dynamicRowFactory = factory;
        this.updaterRow = dynamicRowFactory.newRow("Updater");
        this.composableKey = key;
        this.parentSupplier = () -> getRootComponent().getParent();
    }

    public DynamicRows(Component root, Component rows, DynamicRowFactory factory) {
        this(root, rows, factory, UUIDgenerator.nextUUID("DynamicRows"));
    }

    public DynamicRows(Grid grid) {
        this(grid, UUIDgenerator.nextUUID("DynamicRows"));
    }

    public DynamicRows(Grid grid, String key) {
        rootComponent = grid;
        rowComponent = grid.getRows();
        Objects.requireNonNull(rowComponent, "row component must not be null");
        dynamicRowFactory = DynamicRowFactory.withRow();
        this.composableKey = key;
        this.updaterRow = dynamicRowFactory.newRow("Updater");
        this.parentSupplier = () -> getRootComponent().getParent();
    }

    public String getComposableKey() {
        return composableKey;
    }

    public DynamicRowFactory getDynamicRowFactory() {
        return dynamicRowFactory;
    }

    public Component getRootComponent() {
        return rootComponent;
    }

    public List<Component> getRowComponents() {
        return rowMap.values().stream().map(m -> m.getRow()).collect(Collectors.toList());
    }

    public boolean isEmpty() {
        return rowMap.isEmpty();
    }

    public List<DynamicRow> getRows() {
        return rowMap.values().stream().collect(Collectors.toList());
    }

    private void putKeyAt(Integer index, String key) {
        if (index >= 0) {
            keyOrder.add(index, key);
        } else {
            keyOrder.add(key);
        }
    }

    private void removeKey(String key) {
        keyOrder.remove(key);
    }

    public void removeIfContainsRow(String key) {
        if (rowMap.containsKey(key)) {
            removeRow(key);
        }
    }

    public void removeRow(String key) {
        if (!rowMap.containsKey(key)) {
            throw new IllegalArgumentException("Row with key:" + key + " is not present");
        }
        DynamicRow remove = rowMap.remove(key);
        removeKey(key);
        rowComponent.getChildren().remove(remove.getRow());
        rowKeyOrder.invalidate();// manual trigger of update
    }

    public void removeAll() {
        composable.clear();
        // in case we have so updaters configured
        rowMap.values().forEach(r -> r.markDeleted(true));
        rowMap.clear();
        keyOrder.clear();
        rowComponent.getChildren().clear();
        rowKeyOrder.invalidate();
        this.rowComponent.invalidate();
    }

    public void addRow(Integer index, DynamicRow row) {
        if (rowMap.containsKey(row.getKey())) {
            throw new IllegalArgumentException("Row with key:" + row.getKey() + " is allready present");
        }

        if (index >= 0) {
            rowComponent.getChildren().add(index, row.getRow());
        } else {
            rowComponent.appendChild(row.getRow());
        }
        rowMap.put(row.getKey(), row);
        putKeyAt(index, row.getKey());
        rowKeyOrder.invalidate();// manual trigger of update

    }

    public void addRowAfter(String key, DynamicRow row) {
        Integer index = rowKeyOrder.get().getOrDefault(key, -10);
        addRow(index + 1, row);
    }

    public void addFirst(DynamicRow row) {
        this.addRow(0, row);
    }

    public void addRow(DynamicRow row) {
        this.addRow(-1, row);
    }

    public Optional<DynamicRow> getLastRow() {
        return this.getRowsInOrder().stream().reduce((first, second) -> second);
    }

    public List<Object> getDynamicRowsAndRowsInOrder() {
        List list = dynamicRowsAndRowsInOrder.get();
//        Log.printLines(list);
        return list;
    }
    
    public List<DynamicRow> getDynamicRowsInOrderNested(){
        ArrayList<DynamicRow> all = new ArrayList<>();
        this.getDynamicRowsAndRowsInOrder().forEach(r->{
            if(r instanceof DynamicRow){
                all.add(F.cast(r));
            }else if(r instanceof DynamicRows){
                DynamicRows dr = F.cast(r);
                all.addAll(dr.getDynamicRowsInOrderNested());
            }
        });
        return all;
    }

    public void composeRows(Integer index, DynamicRows rows) {

        if (composable.containsKey(rows.composableKey)) {
            throw new IllegalArgumentException(rows.composableKey + " is occupied");
        }
        DynamicRow newRow = dynamicRowFactory.newRow(rows.composableKey);
        this.addRow(index, newRow);
        this.composable.put(rows.composableKey, rows);

        newRow.withUpdateListener(r -> {
            r.setVisible(!rows.isEmpty());
        });
        newRow.updateDependsOn(rows.updaterRow);
        newRow.add(rows.getRootComponent()).display();
        rows.parentSupplier = () -> this.getParentComponent();
    }

    public void composeRowsLast(DynamicRows rows) {
        this.composeRows(-1, rows);
    }

    public Integer getRowIndex(String key) {
        return keyOrder.indexOf(key);
    }

    public Optional<DynamicRow> getRowIf(String key, Predicate<DynamicRow> comp) {
        return Optional.ofNullable(rowMap.getOrDefault(key, null)).filter(comp);
    }

    public Optional<DynamicRow> getRow(String key) {
        return getRowIf(key, c -> true);
    }

    public DynamicRow getOrCreate(String key) {
        Optional<DynamicRow> row = getRow(key);
        if (row.isPresent()) {
            return row.get();
        } else {
            DynamicRow newRow = dynamicRowFactory.newRow(key);
            addRow(newRow);
            return newRow;
        }
    }

    public DynamicRow getNewAfter(String key) {
        DynamicRow newRow = dynamicRowFactory.newRow();
        Integer rowIndex = this.getRowIndex(key);
        this.addRow(rowIndex + 1, newRow);
        return newRow;
    }

    public DynamicRow getNew() {
        DynamicRow newRow = dynamicRowFactory.newRow();
        addRow(newRow);
        return newRow;
    }

    @Deprecated
    public void display() {
        //decorate colspans
        List<DynamicRow> rows = rowMap.values().stream()
                .filter(r -> r.getCellCount() >= 1)
                .filter(r -> r.isDone())
                .filter(r -> r.isVisible())
                .collect(Collectors.toList());
        if (rows.isEmpty()) {
            Log.print("Rows are empty");
            return;
        }

//        F.iterate(rows, (i, r) -> {
//            Log.print(r.getKey(),
//                    "Visible:", r.getVisibleIndices(),
//                    "Colspan Prefered:", r.getPreferedColSpan(),
//                    "Visible Prefered:", r.getPreferedColSpanOfVisible(),
//                    "Visible Current", r.getColSpanOfVisible(),
//                    "Total", r.getTotalColSpan(),
//                    "Total Visible:", r.getTotalColSpanVisible()
//            );
//
//        });
        int maxColSpan = maxTotalColspan;

        List<DynamicRow> rowsToChange = rows.stream().filter(r -> r.needUpdate(maxColSpan)).collect(Collectors.toList());
        if (rowsToChange.isEmpty()) {
            Log.print("Rows to change are empty");
            return;
        }
        F.iterate(rowsToChange, (rowIndex, r) -> {
            List<Cell> visibleCells = r.getCells().stream().filter(c -> c.isVisible()).collect(Collectors.toList());
            if (visibleCells.isEmpty()) {
                return;
            }
            ArrayList<Integer> preferedColSpan = r.getPreferedColSpanOfVisible();
            double preferedTotal = preferedColSpan.stream().mapToDouble(m -> m.doubleValue()).sum();

            Integer[] colApply = new Integer[preferedColSpan.size()];
            F.iterate(preferedColSpan, (i, pref) -> {
                double rat = pref / preferedTotal;
                colApply[i] = (int) Math.floor(rat * maxColSpan);
            });

            int newTotalColspan = Stream.of(colApply).mapToInt(m -> m).sum();

            int deficit = maxColSpan - newTotalColspan;

            int cellCount = r.getCellCount();
            while (deficit > 0) {
                for (int i = 0; i < colApply.length; i++) {
                    if (deficit <= 0) {
                        break;
                    }
                    colApply[i] = colApply[i] + 1;
                    deficit--;
                }
            }
            List<Integer> oldColSpan = visibleCells.stream().map(m -> m.getColspan()).collect(Collectors.toList());
            Log.print("Change collspan of", r.getKey(), r.getVisibleIndices(), oldColSpan, Arrays.asList(colApply));

            for (int i = 0; i < colApply.length; i++) {
                visibleCells.get(i).setColspan(colApply[i]);
            }

        });

    }

    public List<DynamicRow> getRowsInOrder() {
        return rowsInOrder.get();
    }

    public boolean checkAndUpdateForm(boolean all) {
        if (checkIsInvalid(all)) {
            return false;
        }
        updateForm();
        return true;
    }

    public boolean checkAndUpdateForm() {
        return checkAndUpdateForm(true);
    }

    @Override
    public String toString() {
        return "DynamicRows{" + "composableKey=" + composableKey + '}';
    }

    public void updateForm() {
        updaterRow.updateForm();
        this.doInOrder(
                rows -> rows.updateForm(),
                row -> row.updateForm()
        );
    }

    public boolean checkIsInvalid(boolean all) {
        boolean invalid = false;
        for (Object ob : this.getDynamicRowsAndRowsInOrder()) {
            boolean localInvalid = true;
            if (ob instanceof DynamicRow) {
                DynamicRow drow = F.cast(ob);
                localInvalid = drow.checkIsInvalid(all);
            } else if (ob instanceof DynamicRows) {
                DynamicRows rows = F.cast(ob);
                localInvalid = rows.checkIsInvalid(all);
            } else {
                throw new IllegalStateException("Found unrecognized object of" + ob);
            }
//            Log.print("Valid?", ob, !localInvalid);
            if (localInvalid) {
                if (!all) {//early return
                    return true;
                } else {
                    invalid = true;
                }
            }
        }
        return invalid;
    }

    public void updateView() {
        this.updaterRow.updateView();
        this.doInOrder(
                rows -> rows.updateView(),
                row -> {
                    if (row.isDone() && row.isVisible()) {
                        row.updateView();
                    }
                }
        );
    }

    public void update() {
        this.updaterRow.update();
        this.doInOrder(
                rows -> rows.update(),
                row -> {
                    if (row.isDone() && row.isVisible()) {
                        row.update();
                    }
                }
        );

    }

    public void doInOrder(Consumer<DynamicRows> rowsCons, Consumer<DynamicRow> rowCons) {
        for (Object ob : this.getDynamicRowsAndRowsInOrder()) {
            if (ob instanceof DynamicRow) {
                rowCons.accept(F.cast(ob));
            } else if (ob instanceof DynamicRows) {
                rowsCons.accept(F.cast(ob));
            } else {
                throw new IllegalStateException("Found unrecognized object of" + ob);
            }
        }
    }

    public void doInOrderRows(Consumer<DynamicRow> rowCons) {
        this.doInOrder(r -> {
        }, rowCons);
    }

    public void setParent(Component comp) {
        comp.appendChild(this.rootComponent);
    }

    public Component getParentComponent() {
        return parentSupplier.get();
    }

}
