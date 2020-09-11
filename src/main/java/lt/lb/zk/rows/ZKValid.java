package lt.lb.zk.rows;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lt.lb.commons.datasync.base.NodeValid;
import lt.lb.zk.ZKValidation.ExternalValidation;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;

/**
 *
 * @author laim0nas100
 */
public class ZKValid<T, N extends Component> extends NodeValid<T, N> {

    public ZKValid(ExternalValidation external) {
        this.errorSupl = t -> external.message.get();
        this.isValid = t -> external.isValid();
        this.referenceSupl = () -> {
            return Stream.of(external.component.get()).map(m -> (N) m).collect(Collectors.toList());
        };
    }

    public ZKValid() {

    }
    
    public ZKValid(List<N> nodes) {
        this.referenceSupl = () -> nodes;
    }

    @Override
    public void showInvalidation(T from) {
        for (Component c : referenceSupl.get()) {
            Clients.wrongValue(c, this.errorSupl.apply(from));
        }
    }

    @Override
    public void clearInvalidation(T from) {
        Clients.clearWrongValue(new ArrayList<>(this.referenceSupl.get()));
    }

}
