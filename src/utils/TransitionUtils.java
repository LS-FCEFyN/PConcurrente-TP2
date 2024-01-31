package utils;

import java.util.Arrays;
import java.util.function.Consumer;

public class TransitionUtils {
    @SafeVarargs
    public static <T> Consumer<T> combine(Consumer<T> first, Consumer<T>... others) {
        return Arrays.stream(others).reduce(first, Consumer::andThen);
    }
}

