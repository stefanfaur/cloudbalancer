package com.cloudbalancer.dispatcher.util;

import com.cloudbalancer.common.model.BackoffStrategy;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class BackoffCalculator {

    public static Instant calculateNextRetryTime(BackoffStrategy strategy, int attemptNumber,
                                                  long baseDelaySeconds, Instant now) {
        long delaySeconds = switch (strategy) {
            case FIXED -> baseDelaySeconds;
            case EXPONENTIAL -> baseDelaySeconds * (long) Math.pow(2, attemptNumber - 1);
            case EXPONENTIAL_WITH_JITTER -> {
                long expDelay = baseDelaySeconds * (long) Math.pow(2, attemptNumber - 1);
                long jitterMs = ThreadLocalRandom.current().nextLong(0, baseDelaySeconds * 1000);
                yield (expDelay * 1000 + jitterMs) / 1000;
            }
        };
        return now.plusSeconds(delaySeconds);
    }
}
