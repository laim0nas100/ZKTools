/*
 * Copyright @LKPB 
 */
package lt.lb.zk.dynamicrows;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import lt.lb.commons.F;
import lt.lb.commons.UUIDgenerator;
import lt.lb.commons.containers.tuples.Tuples;
import lt.lb.commons.parsing.StringOp;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zul.Button;
import org.zkoss.zul.Cell;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Row;
import org.zkoss.zul.impl.InputElement;

/**
 *
 * @author Laimonas Beniušis
 */
public interface DynamicRowFactory {

    public static final int LISTBOX_ITEMS_PAGE = 20;

    public static BiConsumer<DynamicRow, Component> listboxDecorator = (dr, comp) -> {
        Listbox box = F.cast(comp);
        box.setWidth("100%");
        box.setMold("paging");
        box.setPageSize(LISTBOX_ITEMS_PAGE);
        dr.withUpdateListener(rr -> {

            int size = box.getModel().getSize();
            boolean visible = size != 0;
            //set cell invisible
            box.getParent().getParent().setVisible(visible);

        });
    };

    public static BiConsumer<DynamicRow, Component> buttonDecorator = (dr, comp) -> {
        Button but = F.cast(comp);
        but.setMold("trendy");
        but.setHflex(""); // just assume best width for this button

    };

    public static BiConsumer<DynamicRow, List<Cell>> simpleRowMaker() {
        return (r, cells) -> {
            if (!cells.isEmpty()) {
                if (cells.size() == 1) {
                    cells.get(0).setAlign("left");
                } else if (cells.size() == 2) {
                    cells.get(0).setAlign("left");
                    cells.get(1).setAlign("right");
                } else if (cells.size() > 2) {
                    F.iterate(cells, (i, c) -> {
                        if (i == 0) {
                            c.setAlign("left");
                        } else if (i == cells.size() - 1) {
                            c.setAlign("right");
                        } else {
                            c.setAlign("center");
                        }

                    });
                }
//                for (Cell c : cells) {
////                        c.setHflex("max");
//                }
                Component row = r.getRow();
                cells.forEach(row::appendChild);
            }
        };
    }

    public static Cell simpleCellTemplate() {
        Cell cellTemplate = new Cell();
        cellTemplate.setStyle("margin: 5px");
        cellTemplate.setStyle("padding: 5px");
//            cellTemplate.setHflex("min");
        cellTemplate.setVflex("min");
        cellTemplate.setAlign("center");
        cellTemplate.setValign("middle");

        return cellTemplate;
    }

    DynamicRow newRow(String key);

    default DynamicRow newRow() {
        return this.newRow(UUIDgenerator.nextUUID("DynamicRow"));
    }

    public static BiConsumer<Boolean, DynamicRow> disableDecorator() {
        String styleString = ";background-color: #e6e6e6 !important;"; //override default odd coloring
        return (dis, r) -> {
            Component comp = r.getRow();
            if (comp instanceof HtmlBasedComponent) {
                HtmlBasedComponent html = F.cast(comp);
                if (dis) {
                    html.setStyle(F.nullWrap(html.getStyle(), "") + styleString);
                } else {
                    html.setStyle(StringOp.remove(html.getStyle(), styleString));
                }
            } else {
                throw new IllegalArgumentException("Style is unsupported in " + comp);
            }

            int size = r.getCellCount();
            for (int i = 0; i < size; i++) {
                Component c = r.getComponent(i);
                if (c instanceof Checkbox) {
                    ((Checkbox) c).setDisabled(dis);
                } else if (c instanceof InputElement) {
                    ((InputElement) c).setDisabled(dis);
                } else if (c instanceof Button) {
                    ((Button) c).setDisabled(dis);
                }
            }
            //disable components

        };
    }

    public static DynamicRowFactory withHbox() {
        return key -> new DynamicRow(
                key,
                new Hbox(),
                simpleCellTemplate(),
                simpleRowMaker(),
                Arrays.asList(
                        Tuples.create(Listbox.class, listboxDecorator),
                        Tuples.create(Button.class, buttonDecorator)
                ),
                disableDecorator()
        );

    }

    public static DynamicRowFactory withRow() {
        return key -> {
            Row row = new Row();
            row.setVflex("min");
//            ZKTools.styleReplace(row, "z-row;");
//            row.setStyle("background:#FFFFFF;");
            return new DynamicRow(
                    key,
                    row,
                    simpleCellTemplate(),
                    simpleRowMaker(),
                    Arrays.asList(
                            Tuples.create(Listbox.class, listboxDecorator),
                            Tuples.create(Button.class, buttonDecorator)
                    ),
                    disableDecorator()
            );

        };
    }

    
}
