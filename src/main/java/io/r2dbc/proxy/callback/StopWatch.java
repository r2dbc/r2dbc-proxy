/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.proxy.callback;

import reactor.util.annotation.Nullable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Utility class to get duration of executions.
 *
 * @author Tadaya Tsuyukubo
 */
class StopWatch {

    private final Clock clock;

    @Nullable
    private Instant startTime;

    StopWatch(Clock clock) {
        this.clock = clock;
    }

    public StopWatch start() {
        this.startTime = this.clock.instant();
        return this;
    }

    public Duration getElapsedDuration() {
        if (this.startTime == null) {
            return Duration.ZERO;  // when stopwatch has not started
        }
        return Duration.between(this.startTime, this.clock.instant());
    }

}
