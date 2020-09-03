package lt.lb.zk.rows;

import java.util.Map;
import lt.lb.commons.F;
import lt.lb.commons.rows.BasicUpdates;
import lt.lb.commons.rows.RowFactory;
import lt.lb.commons.rows.base.BaseDrowsBindsConf;

/**
 *
 * @author Lemmin
 */
public class ZKBaseDrowsConfig<R extends ZKBaseDrow, DR extends ZKBaseDrows<R, DR>> extends BaseDrowsBindsConf<DR, R, ZKUpdates> {

    public RowFactory<R, DR, ZKBaseDrowsConfig<R, DR>> rowFactory;

    public ZKBaseDrowsConfig(RowFactory<R, DR, ZKBaseDrowsConfig<R, DR>> factory) {
        this.rowFactory = factory;
    }

    @Override
    public ZKUpdates createUpdates(String type, DR object) {
        return new ZKUpdates(type);
    }

    @Override
    public void doUpdates(ZKUpdates updates, DR object) {
        updates.commit();
    }

    @Override
    public void removeRowDecorate(DR parentRows, R childRow) {
        super.removeRowDecorate(parentRows, childRow);
        ZKLine line = F.cast(childRow.getLine());
        line.row.detach();
    }

    @Override
    public void uncomposeDecorate(DR parentRows, DR childRows) {
        super.uncomposeDecorate(parentRows, childRows);
        childRows.doInOrderNested(r -> {
            ZKLine line = F.cast(r.getLine());
            line.row.detach();
        });
    }

    @Override
    public void configureUpdates(Map<String, ZKUpdates> updates, DR object) {
        object.initUpdates();
        object.withUpdate(BasicUpdates.INVALIDATE, 0, r -> {
            r.grid.invalidate();
        });
    }

    public R newRow(DR rows, String key) {
        return rowFactory.createRow(this, rows, key);
    }

}
