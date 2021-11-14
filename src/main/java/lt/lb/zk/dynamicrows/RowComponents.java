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
public class RowComponents {

    public static <R extends Enum> RadioComboboxMapper<R> comboEnum(Class<R> cls, Function<R, String> mapper) {
        RadioComboboxMapper<R> cbm = new RadioComboboxMapper<>();
        return cbm.withOptions(Stream.of(cls.getEnumConstants()).collect(Collectors.toList())).withMapper(mapper);
    }

    public static <R extends Enum> RadioComboboxMapper<R> comboEnumDefaultNames(Class<R> cls) {
        return comboEnum(cls, Enum::name);
    }

    public static <R> RadioComboboxMapper<R> comboMapped(Collection<R> opt, Function<R, String> mapper) {
        RadioComboboxMapper<R> cbm = new RadioComboboxMapper<>();
        return cbm.withOptions(opt).withMapper(mapper);
    }

    public static RadioComboboxMapper<String> comboNames(String... names) {
        RadioComboboxMapper<String> cbm = new RadioComboboxMapper<>();
        return cbm.withOptions(Arrays.asList(names)).withMapper(s -> s);

    }
}
