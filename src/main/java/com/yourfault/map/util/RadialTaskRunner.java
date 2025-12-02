package com.yourfault.map.util;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Executes a collection of radial operations in order of their distance from the center.
 */
public final class RadialTaskRunner extends BukkitRunnable {
    private final List<Step> steps;
    private final int operationsPerTick;
    private final Runnable onComplete;
    private final Consumer<Exception> onError;
    private int cursor;

    public RadialTaskRunner(List<Step> steps,
                            int operationsPerTick,
                            Runnable onComplete,
                            Consumer<Exception> onError) {
        this.steps = new ArrayList<>(Objects.requireNonNull(steps, "steps"));
        this.steps.sort(Comparator.comparingDouble(Step::radialDistance));
        this.operationsPerTick = Math.max(1, operationsPerTick);
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete");
        this.onError = Objects.requireNonNull(onError, "onError");
    }

    @Override
    public void run() {
        try {
            int processed = 0;
            while (cursor < steps.size() && processed < operationsPerTick) {
                steps.get(cursor++).action().run();
                processed++;
            }
            if (cursor >= steps.size()) {
                cancel();
                onComplete.run();
            }
        } catch (Exception ex) {
            cancel();
            onError.accept(ex);
        }
    }

    public boolean isComplete() {
        return cursor >= steps.size();
    }

    public record Step(double radialDistance, ThrowingRunnable action) {
        public Step {
            Objects.requireNonNull(action, "action");
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
