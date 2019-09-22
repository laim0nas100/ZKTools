package lt.lb.zk.dynamicrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author laim0nas100
 */
public class RowComp {

    public static <R extends Enum> ComboboxMapper<R> comboEnum(Class<R> cls, Function<R, String> mapper) {
        ComboboxMapper<R> cbm = new ComboboxMapper<>();
        return cbm.withOptions(Stream.of(cls.getEnumConstants()).collect(Collectors.toList())).withMapper(mapper);
    }

    public static <R extends Enum> ComboboxMapper<R> comboEnumDefaultNames(Class<R> cls) {
        return comboEnum(cls, Enum::name);
    }

    public static <R> ComboboxMapper<R> comboMapped(Collection<R> opt, Function<R, String> mapper) {
        ComboboxMapper<R> cbm = new ComboboxMapper<>();
        return cbm.withOptions(opt).withMapper(mapper);
    }

    public static ComboboxMapper<String> comboNames(String... names) {
        ComboboxMapper<String> cbm = new ComboboxMapper<>();
        return cbm.withOptions(Arrays.asList(names)).withMapper(s -> s);

    }
}
