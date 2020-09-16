package lt.lb.zk.rows;

import lt.lb.commons.LineStringBuilder;
import lt.lb.commons.rows.base.BaseCell;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Cell;

/**
 *
 * @author Laimonas-Beniusis-PC
 */
public class ZKCell extends BaseCell<Component, Component> {
    protected Cell cell = makeCell();
    
    protected String allign;
    
    protected Cell makeCell(){
        Cell cellTemplate = new Cell();
        LineStringBuilder sb = new LineStringBuilder(";\n");
        sb
                .appendLine("margin-top: 1px")
                .appendLine("margin-right: 3px")
                .appendLine("margin-bottom: 1px")
                .appendLine("margin-left: 3px");
        cellTemplate.setStyle(sb.toString());
//        cellTemplate.setStyle("padding: 5px");
//            cellTemplate.setHflex("min");
        cellTemplate.setVflex("min");
        cellTemplate.setAlign("center");
        cellTemplate.setValign("middle");
        return cellTemplate;
    }
    
    public ZKCell() {
    }

    public String getAllign() {
        return allign;
    }

    public void setAllign(String allign) {
        this.allign = allign;
    }
    
    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

}
