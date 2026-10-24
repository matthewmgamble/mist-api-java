package ca.mgamble.mist;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A lazily fetched, multi-page result.
 * <p>
 * Pages are requested only as you consume elements, so {@code stream().limit(50)} or {@link #first()}
 * stops making requests once it has enough. Each call to {@link #iterator()}, {@link #stream()} or
 * {@link #toList()} starts again from the first page.
 *
 * @param <T> element type
 */
public final class Paged<T> implements Iterable<T> {

    private final Supplier<Iterator<T>> iterators;

    Paged(Supplier<Iterator<T>> iterators) {
        this.iterators = iterators;
    }

    @Override
    public Iterator<T> iterator() {
        return iterators.get();
    }

    public Stream<T> stream() {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    /** Fetches every page. */
    public List<T> toList() {
        return stream().toList();
    }

    /** Fetches only the first page, returning its first element. */
    public Optional<T> first() {
        Iterator<T> it = iterator();
        return it.hasNext() ? Optional.of(it.next()) : Optional.empty();
    }
}
