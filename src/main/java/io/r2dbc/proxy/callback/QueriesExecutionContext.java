package io.r2dbc.proxy.callback;

import io.r2dbc.spi.Result;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * The context of queries execution.
 * <p>
 * Holds the count of {@link Result} produced by {@code Statement#execute} and count that has
 * consumed {@code Result#[map|getRowsUpdated]}.
 *
 * @author Thomas Deblock
 * @author Tadaya Tsuyukubo
 */
public class QueriesExecutionContext {

    private static final AtomicIntegerFieldUpdater<QueriesExecutionContext> PRODUCED_COUNT_INCREMENTER =
        AtomicIntegerFieldUpdater.newUpdater(QueriesExecutionContext.class, "resultProducedCount");

    private static final AtomicIntegerFieldUpdater<QueriesExecutionContext> CONSUMED_COUNT_INCREMENTER =
        AtomicIntegerFieldUpdater.newUpdater(QueriesExecutionContext.class, "resultConsumedCount");

    /**
     * Increment this count when a publisher from {@code Statement#execute} produced a {@link Result}.
     * Accessed via {@link #PRODUCED_COUNT_INCREMENTER}.
     */
    private volatile int resultProducedCount;

    /**
     * Increment this count when a publisher from {@code Result#[map|getRowsUpdated]} is consumed.
     * Accessed via {@link #CONSUMED_COUNT_INCREMENTER}.
     */
    private volatile int resultConsumedCount;

    private final StopWatch stopWatch;

    private boolean allProduced;

    public QueriesExecutionContext(Clock clock) {
        this.stopWatch = new StopWatch(clock);
    }

    /**
     * Increment the count of produced {@link Result} from {@code Statement#execute}.
     */
    public void incrementProducedCount() {
        PRODUCED_COUNT_INCREMENTER.incrementAndGet(this);
    }

    /**
     * Increment the count of consumptions from {@code Result#[map|getRowsUpdated]}.
     */
    public void incrementConsumedCount() {
        CONSUMED_COUNT_INCREMENTER.incrementAndGet(this);
    }

    /**
     * Retrieve the elapsed time from the stopwatch that has started by {@link #startStopwatch()}.
     *
     * @return duration from start
     */
    public Duration getElapsedDuration() {
        return this.stopWatch.getElapsedDuration();
    }

    /**
     * Start the stopwatch.
     */
    public void startStopwatch() {
        this.stopWatch.start();
    }

    /**
     * Whether the executed queries have finished and results are consumed.
     * <p>
     * The query is considered finished when the publisher from {@code Statement#execute()} have produced
     * {@link Result}s and those are consumed via {@code Result#getRowsUpdated} or {@code Result#map}.
     *
     * @return {@code true} if all {@code Result} are produced and consumed.
     */
    public boolean isQueryFinished() {
        return this.allProduced && isAllConsumed();
    }

    /**
     * Whether currently all produced {@link Result}s are consumed.
     *
     * @return {@code true} if all produced {@link Result}s are consumed.
     */
    public boolean isAllConsumed() {
        return this.resultConsumedCount >= this.resultProducedCount;
    }


    /**
     * When {@link QueryInvocationSubscriber} produced all {@link Result} objects.
     */
    public void markAllProduced() {
        this.allProduced = true;
    }

}
