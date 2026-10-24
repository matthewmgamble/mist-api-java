package ca.mgamble.mist.query;

import ca.mgamble.mist.Query;

/**
 * Base for typed query builders. Any filter without a dedicated method can be set with
 * {@link #param(String, Object)}.
 *
 * @param <S> the concrete builder type
 */
public abstract class ApiQuery<S extends ApiQuery<S>> {

    protected final Query query = Query.empty();

    @SuppressWarnings("unchecked")
    protected final S self() {
        return (S) this;
    }

    /** Sets any query parameter by its API name; {@code null} removes it. */
    public S param(String key, Object value) {
        query.set(key, value);
        return self();
    }

    /** Results per page (Mist's {@code limit}). Defaults to the client's page size. */
    public S pageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 1000) {
            throw new IllegalArgumentException("pageSize must be between 1 and 1000");
        }
        return param("limit", pageSize);
    }

    /** A copy of the parameters this builder will send. */
    public Query toQuery() {
        return query.copy();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" + query + "}";
    }
}
