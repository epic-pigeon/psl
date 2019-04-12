package ParserPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Collection<T> extends ArrayList<T> {
    public Collection() {
        super();
    }
    public Collection(int k) {
        super(k);
    }
    public<E> Collection<E> map(Function<T, E> fn) {
        Collection<E> collection = new Collection<>();
        for (T obj: this) collection.add(fn.apply(obj));
        return collection;
    }
    public Collection<T> reverse() {
        Collection<T> reverse = new Collection<>(this.size());

        new LinkedList<>(this)
                .descendingIterator()
                .forEachRemaining(reverse::add);

        return reverse;
    }
}
