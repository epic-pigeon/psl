package ParserPackage;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Collection<T> extends ArrayList<T> {
    public Collection() {
        super();
    }
    public Collection(int k) {
        super(k);
    }
    public Collection(T... args) {
        super(Arrays.asList(args));
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
    public String join(String glue) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < this.size(); i++) {
            if (i != 0) result.append(glue);
            result.append(this.get(i).toString());
        }
        return result.toString();
    }
    public String join() {
        return join(" ");
    }
    public T findFirst(Function<T, Boolean> fn) {
        for (T element: this) if (fn.apply(element)) return element;
        return null;
    }
}
