/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.zk.rows;

import lt.lb.commons.Java;
import lt.lb.commons.rows.Updates;

/**
 *
 * @author Laimonas-Beniusis-PC
 */
public class ZKUpdates extends Updates<ZKUpdates> {

    public ZKUpdates(String type) {
        super(type);
    }

    protected ZKUpdates(ZKUpdates up) {
        super(up);
    }

    @Override
    protected ZKUpdates me() {
        return this;
    }

    @Override
    public ZKUpdates clone() {
        return new ZKUpdates(this);
    }

    public void commit() {
        if (active) {
            triggerUpdate(Java.getNanoTime());
        }
    }

}
