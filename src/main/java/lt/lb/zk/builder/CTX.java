/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lt.lb.zk.builder;

import java.util.List;
import lt.lb.commons.containers.values.Props;

/**
 *
 * @author laim0nas100
 */
public interface CTX {

    public List<Prop> getProperties();

    public Props getData();
}
