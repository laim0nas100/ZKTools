package lt.lb.zk.dynamicrows;

import java.util.List;
import java.util.function.BiConsumer;
import lt.lb.commons.F;
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
        return cellAny((opt, cell) -> {
            cell.setHflex(opt);
        }, hflexes);
    }

    public static DynamicRowCellDecorator cellAlign(String... aligns) {
        return cellAny((opt, cell) -> {
            cell.setAlign(opt);
        }, aligns);
    }

    public static DynamicRowCellDecorator cellWidth(String... widths) {
        return cellAny((opt, cell) -> {
            cell.setWidth(opt);
        }, widths);
    }

    public static DynamicRowCellDecorator cellRowSpan(Integer... rowspans) {
        return (cells) -> {
            F.iterate(rowspans, (i, option) -> {
                cells.get(i).setRowspan(option);
            });
        };
    }

    public static DynamicRowCellDecorator cellAny(BiConsumer<String, Cell> consumer, String... options) {
        return (cells) -> {
            F.iterate(options, (i, option) -> {
                consumer.accept(option, cells.get(i));
            });
        };
    }
}
