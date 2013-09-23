package gov.bnl.shift.api;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 *
 * @author eschuhmacher
 */
public class ShiftQuery {

    private final String query;
    private static Executor defaultQueryExecutor = Executors.newSingleThreadExecutor();
    private final Executor queryExecutor;


    /**
     * A new query with the given search string.
     *
     * @param query the query; cannot be null
     * @return a new builder
     */
    public static Builder query(String query) {
        return new Builder(query);
    }


    /**
     * The executor on which the queries are executed.
     *
     * @return the current executor
     */
    public static Executor getDefaultQueryExecutor() {
        return defaultQueryExecutor;
    }

    /**
     * Change the executor on which the queries are executed.
     *
     * @param defaultQueryExecutor the new executor
     */
    public static void setDefaultQueryExecutor(Executor defaultQueryExecutor) {
        if (defaultQueryExecutor == null)
            throw new NullPointerException("Executor can't be null");
        ShiftQuery.defaultQueryExecutor = defaultQueryExecutor;
    }

    /**
     * Result of the query. Groups both result and error so that it's an immutable
     * and atomic combination.
     *
     * @author carcassi
     */
    public static class Result {
        public final Exception exception;
        public final Collection<Shift> shifts;

        public Result(Exception exception, Collection<Shift> shifts) {
            this.exception = exception;
            this.shifts = shifts;
        }

    }

    private volatile Result result;
    // Guarded by this: will keep track whether a query is already running
    private boolean running = false;
    public static class Builder {
        private String query = null;
        private Executor queryExecutor = defaultQueryExecutor;
        private Result result = null;

        private Builder(String query) {
            if (query == null)
                throw new IllegalArgumentException(
                        "query string cannot be null");
            this.query = query;
        }

        /**
         * Pre-fills the cached result with the given shifts and exception.
         *
         * @param shifts the result of the query
         * @param exception the exception for the result; can be null
         * @return this
         */
        public Builder result(Collection<Shift> shifts, Exception exception) {
            result = new Result(exception, shifts);
            return this;
        }

        /**
         * Changes which executor should execute the query.
         *
         * @param executor an executor
         * @return this
         */
        public Builder on(Executor executor) {
            if (executor == null)
                throw new NullPointerException("Executor can't be null");
            this.queryExecutor = executor;
            return this;
        }

        /**
         * Creates the new query. The query is not executed until
         * is needed.
         *
         * @return a new query
         */
        public ShiftQuery build() {
            return new ShiftQuery(this.query, this.queryExecutor, this.result);
        }
    }

    private ShiftQuery(String query, Executor queryExecutor, Result result) {
        super();
        this.query = query;
        this.queryExecutor = queryExecutor;
        this.result = result;
    }

    /**
     * The text of the query.
     *
     * @return the query text
     */
    public String getQuery() {
        return query;
    }

    /**
     * The result of the query, if present.
     *
     * @return result or null if the query was never executed
     */
    public Result getResult() {
        return this.result;
    }

    /**
     * Executes the query and calls the listener with the result.
     * If the query was already executed, the listener is called
     * immediately with the result.
     *
     * @param listener
     */
    public void execute(ChannelQueryListener listener) {
        addShiftQueryListener(listener);

        // Make a local copy to avoid synchronization
        Result localResult = result;

        // If the query was executed, just call the listener
        if (localResult != null) {
            listener.queryExecuted(localResult);
        } else {
            execute();
        }

    }

    /**
     * Triggers a new execution of the query, and calls
     * all the listeners as a result.
     */
    public void refresh() {
        execute();
    }

    private void execute() {
        // If it's already running, do nothing
        synchronized (this) {
            if (running)
                return;
            running = true;
        }

        queryExecutor.execute(new Runnable() {

            @Override
            public void run() {
                Result localResult = null;
                try {
                    Collection<Shift> shifts = client.find(query);
                    localResult = new Result(null, shifts);
                } catch (Exception e) {
                    localResult = new Result(e, null);
                } finally {
                    result = localResult;
                    synchronized (this) {
                        running = false;
                    }
                    fireGetQueryResult(localResult);
                }
            }
        });
    }

    @Override
    public int hashCode() {
        return getQuery().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ShiftQuery) {
            return query.equals(((ShiftQuery) obj).getQuery());
        }

        return false;
    }

    @Override
    public String toString() {
        return getQuery();
    }

}
