package io.r2dbc.proxy.callback;

import java.time.Duration;

/**
 * Utility class to know how many result are processing and if all result has been processed.
 *
 * @author Thomas Deblock
 */
public class QueriesExecutionCounter {
    private int numberOfGeneratedResult;
    private int numberOfProcessedResult;
    private boolean allResultHasBeenGenerated;
    private final StopWatch stopWatch;

    public QueriesExecutionCounter(StopWatch stopWatch) {
        this.numberOfGeneratedResult = 0;
        this.numberOfProcessedResult = 0;
        this.allResultHasBeenGenerated = false;
        this.stopWatch = stopWatch;
    }

    public void addGeneratedResult() {
        this.numberOfGeneratedResult++;
    }

    public Duration getElapsedDuration() {
        return this.stopWatch.getElapsedDuration();
    }

    public void queryStarted() {
        this.stopWatch.start();
    }

    public void resultProcessed() {
        this.numberOfProcessedResult++;
    }

    public boolean isQueryEnded() {
        return this.areAllResultProcessed() && this.areAllResultGenerated();
    }

    public boolean areAllResultProcessed() {
        return this.numberOfProcessedResult >= this.numberOfGeneratedResult;
    }

    public boolean areAllResultGenerated() {
        return this.allResultHasBeenGenerated;
    }

    public void allResultHasBeenGenerated() {
        this.allResultHasBeenGenerated = true;
    }
}
