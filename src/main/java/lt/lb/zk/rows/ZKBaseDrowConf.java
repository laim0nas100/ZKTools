package lt.lb.zk.rows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import lt.lb.commons.F;
import lt.lb.commons.Ins;
import lt.lb.commons.SafeOpt;
import lt.lb.commons.datasync.Valid;
import lt.lb.commons.rows.base.BaseDrowSyncConf;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

/**
 *
 * @author Laimonas-Beniusis-PC
 */
public class ZKBaseDrowConf<R extends ZKBaseDrow, DR extends ZKBaseDrows<R, DR>> extends BaseDrowSyncConf<R, ZKCell, Component, ZKLine<R, DR>, ZKUpdates, ZKBaseDrowConf<R, DR>> {
    
    public String defaultGridValign = "middle";
    
    public ZKBaseDrowConf() {
        withComponentDecorator(Textbox.class, c -> c.setHflex("1"));
        withComponentDecorator(Button.class, c -> c.setMold("trendy"));
    }
    
    @Override
    public Component getEnclosingNode(ZKBaseDrow drow) {
        Hlayout hlayout = new Hlayout();
        hlayout.setHflex("max");
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
    public void renderRow(R row) {
        ZKLine<R, DR> line = F.cast(row.getLine());
        DR rows = line.getRows();
        Rows zkRows = rows.getZKRows();
        line.derender();
        line.setDerender(() -> {
            line.row.detach();
            line.row.getChildren().clear();
            
            line.getCells().clear();
            line.getRenderedNodes().clear();
        });
        
        if (!row.isRendable()) {
            return;
        }
        
        Integer rowIndex = rows.getVisibleRowIndex(row.getKey());
        if (rowIndex == -1) {
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
                
                line.getRenderedNodes().add(cell);
                line.row.appendChild(cell);
            } else {
                throw new IllegalArgumentException("no enclosing component");
            }
        }
        
        if (line.getRenderedNodes().size() == 1) {
            conditionalAlligment(line.getRenderedNodes().get(0), "center");
        } else if (line.getRenderedNodes().size() == 2) {
            conditionalAlligment(line.getRenderedNodes().get(0), "left");
            conditionalAlligment(line.getRenderedNodes().get(1), "right");
        } else if (line.getRenderedNodes().size() > 2) {
            final int last = line.getCells().size() - 1;
            F.iterate(line.getRenderedNodes(), (i, n) -> {
                if (i == 0) {
                    conditionalAlligment(n, "left");
                } else if (i == last) {
                    conditionalAlligment(n, "right");
                } else {
                    conditionalAlligment(n, "center");
                }
                
            });
        }
        
    }
    
    protected void conditionalAlligment(Component n, String align) {
        SafeOpt.of(n)
                .select(Cell.class)
                .ifPresent(m -> m.setValign(defaultGridValign))
                .ifPresent(m -> m.setAlign(align));
        
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
    
    public <N extends Component> void withComponentDecorator(Class<N> cls, Consumer<N> cons) {
        this.withUpdateDisplay(r -> {
            Ins.InsCl<N> of = Ins.of(cls);
            r.getNodes().stream().filter(of::superClassOf).map(m -> (N) m).forEach(cons);
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
