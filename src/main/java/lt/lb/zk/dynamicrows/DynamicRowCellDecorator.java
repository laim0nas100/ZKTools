package lt.lb.zk.dynamicrows;

import java.util.List;
import java.util.function.BiConsumer;
import lt.lb.commons.iteration.For;
import org.zkoss.zul.Cell;

/**
 *
 * @author laim0nas100
 */
public interface DynamicRowCellDecorator {

    public void decorate(List<Cell> cells);

    public default DynamicRowCellDecorator with(DynamicRowCellDecorator other) {
        DynamicRowCellDecorator me = this;
        return (List<Cell> cells) -> {
            me.decorate(cells);
            other.decorate(cells);
        };
    }

    public static DynamicRowCellDecorator combine(DynamicRowCellDecorator... decs) {
        return (List<Cell> cells) -> {
            for (DynamicRowCellDecorator dec : decs) {
                dec.decorate(cells);
            }
        };
    }

    public static DynamicRowCellDecorator cellHflex(String... hflexes) {
        return cellArray((opt, cell) -> {
            cell.setHflex(opt);
        }, hflexes);
    }

    public static DynamicRowCellDecorator cellAlign(String... aligns) {
        return cellArray((opt, cell) -> {
            cell.setAlign(opt);
        }, aligns);
    }
    
    public static DynamicRowCellDecorator cellAlignAll(String align){
        return cellAll((opt,cell)->{
            cell.setAlign(opt);
        }, align);
    }

    public static DynamicRowCellDecorator cellWidth(String... widths) {
        return cellArray((opt, cell) -> {
            cell.setWidth(opt);
        }, widths);
    }
    
    public static DynamicRowCellDecorator cellWidthAll(String widths) {
        return cellAll((opt, cell) -> {
            cell.setWidth(opt);
        }, widths);
    }

    public static DynamicRowCellDecorator cellRowSpan(Integer... rowspans) {
        return (cells) -> {
            For.elements().iterate(rowspans, (i, option) -> {
                cells.get(i).setRowspan(option);
            });
        };
    }
    
    public static DynamicRowCellDecorator cellRowSpanAll(Integer rowspan) {
        return cellAll((opt, cell) -> {
            cell.setRowspan(opt);
        }, rowspan);
    }

    public static <T> DynamicRowCellDecorator cellArray(BiConsumer<T, Cell> consumer, T... options) {
        return (cells) -> {
            For.elements().iterate(options, (i, option) -> {
                consumer.accept(option, cells.get(i));
            });
        };
    }

    public static <T> DynamicRowCellDecorator cellAll(BiConsumer<T, Cell> consumer, T option) {
        return (cells) -> {
            For.elements().iterate(cells, (i, c) -> consumer.accept(option, c));
        };
    }
}
