package lt.lb.zk.rows;

import lt.lb.commons.rows.base.BaseCell;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Cell;

/**
 *
 * @author Laimonas-Beniusis-PC
 */
public class ZKCell extends BaseCell<Component, Component> {
    protected Cell cell = new Cell();
    
    protected String allign;
    
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
