package lt.lb.zk.rows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lt.lb.commons.F;
import lt.lb.commons.Ins;
import lt.lb.commons.datasync.Valid;
import lt.lb.commons.iteration.For;
import lt.lb.commons.parsing.StringOp;
import lt.lb.commons.rows.base.BaseDrowSyncConf;
import lt.lb.uncheckedutils.SafeOpt;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Rows;

/**
 *
 * @author laim0nas100
 */
public class ZKBaseDrowConf<R extends ZKBaseDrow, DR extends ZKBaseDrows<R, DR>> extends BaseDrowSyncConf<R, ZKCell, Component, ZKLine<R, DR>, ZKUpdates, ZKBaseDrowConf<R, DR>> {

    public String defaultGridValign = "middle";
    public boolean preferCenterAllign = false;

    public ZKBaseDrowConf() {
    }

    @Override
    public Component getEnclosingNode(ZKBaseDrow drow) {
        Hlayout hlayout = new Hlayout();
        hlayout.setHflex("1");
        return hlayout;
    }

    @Override
    public ZKCell createCell(List<Component> nodes, Component enclosingNode, R drow) {
        ZKCell cell = new ZKCell();
        cell.setNodes(nodes);
        cell.setEnclosed(F.cast(enclosingNode));
        return cell;
    }

    @Override
    public void renderRow(R row, boolean dirty) {
        ZKLine<R, DR> line = F.cast(row.getLine());
        DR rows = line.getRows().getLastParentOrMe();
        int rowIndex = rows.getVisibleRowIndex(row.getKey());
        if (!baseDerenderContinue(line, rowIndex, dirty)) {
            return;
        }
        Rows zkRows = rows.getZKRows();
        line.setDerender(() -> {
            line.row.detach();
            line.row.getChildren().clear();

            line.getCells().clear();
            line.getRenderedNodes().clear();
        });

        if (!row.isActive()) {
            return;
        }

        if (rowIndex < 0) {
            throw new IllegalArgumentException(row.getKey() + " was not in " + rows.getComposableKey());
        }
        zkRows.getChildren().add(rowIndex, line.row);
        List visibleCells = row.getVisibleCells();
        for (Object object : visibleCells) {
            ZKCell zkCell = F.cast(object);
            line.getCells().add(zkCell);
            if (zkCell.getEnclosed().isPresent()) {
                Component enclosed = zkCell.getEnclosed().get();
                enclosed.getChildren().clear();
                enclosed.getChildren().addAll(zkCell.getNodes());

                Cell cell = zkCell.getCell();
                cell.getChildren().clear();
                cell.setColspan(zkCell.getColSpan());
                cell.setAlign(zkCell.getAllign());
                cell.appendChild(enclosed);
                cell.invalidate();

                line.getRenderedNodes().add(cell);
                line.row.appendChild(cell);

            } else {

                Cell cell = zkCell.getCell();
                cell.getChildren().clear();
                cell.getChildren().addAll(zkCell.getNodes());
                cell.setColspan(zkCell.getColSpan());
                cell.setAlign(zkCell.getAllign());
                cell.invalidate();

                line.getRenderedNodes().add(cell);
                line.row.appendChild(cell);
            }
        }

        if (line.getRenderedNodes().size() == 1) {
            conditionalAlligment(line, 0, "center");
        } else if (line.getRenderedNodes().size() >= 2) {
            final int last = line.getCells().size() - 1;
            For.elements().iterate(line.getRenderedNodes(), (i, n) -> {
                if (i == 0) {
                    conditionalAlligment(line, i, preferCenterAllign ? "right" : "left");
                } else if (i == last) {
                    conditionalAlligment(line, i, preferCenterAllign ? "left" : "right");
                } else {
                    conditionalAlligment(line, i, "center");
                }

            });
        }

    }

    protected void conditionalAlligment(ZKLine<R, DR> line, int index, String align) {

        if (StringOp.isNotEmpty(line.getCells().get(index).getAllign())) { // something is defined, don't use default
            return;
        }
        SafeOpt.of(line.getRenderedNodes())
                .map(m -> m.get(index))
                .select(Cell.class)
                .peek(m -> m.setValign(defaultGridValign))
                .peek(m -> m.setAlign(align));

    }

    @Override
    public ZKUpdates createUpdates(String type, R object) {
        return new ZKUpdates(type);
    }

    @Override
    public void doUpdates(ZKUpdates updates, R object) {
        updates.commit();
    }

    @Override
    public void configureUpdates(Map<String, ZKUpdates> updates, R object) {
        super.configureUpdates(updates, object);
    }

    public <N extends Component> void withComponentDecorator(Class<N> cls, BiConsumer<R, N> cons) {
        this.withUpdateDisplay(r -> {
            Ins.InsCl<N> of = Ins.of(cls);
            Stream<N> map = r.getNodes().stream().filter(of::superClassOf).map(m -> (N) m);
            map.forEach(node -> {
                cons.accept(r, node);
            });
        });
    }

    @Override
    public <M> Valid<M> createValidation(R row, ZKCell cell, Component node, Predicate<M> isValid, Function<? super M, String> error) {
        Objects.requireNonNull(node);
        ZKValid<M, Component> valid = new ZKValid<>(Arrays.asList(node));
        valid.errorSupl = error;
        valid.isValid = isValid;
        return valid;
    }

    @Override
    public <M> Valid<M> createValidation(R row, Predicate<M> isValid, Function<? super M, String> error) {
        ZKLine<R, DR> line = F.cast(row.getLine());
        return createValidation(row, null, line.row, isValid, error);
    }

    @Override
    public ZKBaseDrowConf<R, DR> me() {
        return this;
    }

}
