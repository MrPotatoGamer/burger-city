package game.core;

/**
 * Manages game time with multiple speed settings: pause, normal (1x), fast (2x), very fast (4x).
 * Tracks elapsed game time in ticks.
 */
public class TimeManager {

    public enum TimeSpeed {
        PAUSED(0.0, "⏸ Szünet"),
        NORMAL(1.0, "▶ Normál"),
        FAST(2.0, "▶▶ Gyors (2x)"),
        VERY_FAST(4.0, "▶▶▶ Nagyon gyors (4x)");

        private final double multiplier;
        private final String displayName;

        TimeSpeed(double multiplier, String displayName) {
            this.multiplier = multiplier;
            this.displayName = displayName;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private TimeSpeed currentSpeed;
    private long totalTicks;
    private double accumulatedGameTime; // in seconds

    public TimeManager() {
        this.currentSpeed = TimeSpeed.NORMAL;
        this.totalTicks = 0;
        this.accumulatedGameTime = 0.0;
    }

    /**
     * Updates the game time based on real delta time and current speed multiplier.
     * @param realDeltaSeconds Real time elapsed since last update
     * @return Game time delta (adjusted by speed multiplier)
     */
    public double update(double realDeltaSeconds) {
        double gameDelta = realDeltaSeconds * currentSpeed.getMultiplier();
        accumulatedGameTime += gameDelta;
        if (currentSpeed != TimeSpeed.PAUSED) {
            totalTicks++;
        }
        return gameDelta;
    }

    public void setSpeed(TimeSpeed speed) {
        this.currentSpeed = speed;
    }

    public TimeSpeed getCurrentSpeed() {
        return currentSpeed;
    }

    public void pause() {
        setSpeed(TimeSpeed.PAUSED);
    }

    public void setNormal() {
        setSpeed(TimeSpeed.NORMAL);
    }

    public void setFast() {
        setSpeed(TimeSpeed.FAST);
    }

    public void setVeryFast() {
        setSpeed(TimeSpeed.VERY_FAST);
    }

    public boolean isPaused() {
        return currentSpeed == TimeSpeed.PAUSED;
    }

    /**
     * Returns total game ticks elapsed (not affected by speed, only counts updates when not paused).
     */
    public long getTotalTicks() {
        return totalTicks;
    }

    /**
     * Returns accumulated game time in seconds (affected by speed multiplier).
     */
    public double getGameTimeSeconds() {
        return accumulatedGameTime;
    }

    /**
     * Returns game time formatted as days and hours.
     * Assuming 1 game day = 120 real seconds at 1x speed.
     */
    public String getFormattedGameTime() {
        int totalGameSeconds = (int) accumulatedGameTime;
        int secondsPerDay = 120; // 2 minutes = 1 game day
        int days = totalGameSeconds / secondsPerDay;
        int remainingSeconds = totalGameSeconds % secondsPerDay;
        int hours = (remainingSeconds * 24) / secondsPerDay;
        
        return String.format("Nap %d, Óra %d", days, hours);
    }
}
