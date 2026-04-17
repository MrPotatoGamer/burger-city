package game.building;

public class TrafficLight extends Building {

    public static final int COST = 1000;

    // States: MAIN_GREEN, CROSS_GREEN
    private String currentState;
    private double timeInCurrentState; // Accumulated time in current state (seconds)
    private double greenDurationMain;   // Green light duration for main direction (seconds)
    private double greenDurationCross;  // Green light duration for cross direction (seconds)

    public TrafficLight(int x, int y) {
        super("TrafficLight", COST, x, y);
        this.currentState = "MAIN_GREEN";
        this.timeInCurrentState = 0.0;
        this.greenDurationMain = 5.0;
        this.greenDurationCross = 5.0;
    }

    /**
     * Update the traffic light state based on elapsed time.
     * @param deltaSeconds Time elapsed since last update.
     */
    public void update(double deltaSeconds) {
        if (deltaSeconds <= 0) return;

        timeInCurrentState += deltaSeconds;

        // Check if we need to switch
        if (currentState.equals("MAIN_GREEN") && timeInCurrentState >= greenDurationMain) {
            switchToState("CROSS_GREEN");
        } else if (currentState.equals("CROSS_GREEN") && timeInCurrentState >= greenDurationCross) {
            switchToState("MAIN_GREEN");
        }
    }

    private void switchToState(String newState) {
        this.currentState = newState;
        this.timeInCurrentState = 0.0;
    }

    /**
     * Set the green light durations for both directions.
     * @param main Duration for main direction (North-South) in seconds.
     * @param cross Duration for cross direction (East-West) in seconds.
     */
    public void setDurations(double main, double cross) {
        this.greenDurationMain = Math.max(1.0, main);  // Minimum 1 second
        this.greenDurationCross = Math.max(1.0, cross);
    }

    /**
     * Get current state of the light.
     * @return "MAIN_GREEN" or "CROSS_GREEN"
     */
    public String getCurrentState() {
        return currentState;
    }

    /**
     * Check if a specific direction has green light.
     * @param direction Direction to check (1=N, 2=E, 3=S, 4=W)
     * @return true if green, false if red
     */
    public boolean isGreen(int direction) {
        if (direction == 0) return true; // No direction = always green

        // MAIN = North-South (1, 3)
        // CROSS = East-West (2, 4)
        if (currentState.equals("MAIN_GREEN")) {
            return direction == 1 || direction == 3;
        } else if (currentState.equals("CROSS_GREEN")) {
            return direction == 2 || direction == 4;
        }
        return false;
    }

    public double getGreenDurationMain() {
        return greenDurationMain;
    }

    public double getGreenDurationCross() {
        return greenDurationCross;
    }

    public double getTimeInCurrentState() {
        return timeInCurrentState;
    }

    /**
     * Manually switch the light (for testing/debugging).
     */
    public void switchLight() {
        if (currentState.equals("MAIN_GREEN")) {
            switchToState("CROSS_GREEN");
        } else {
            switchToState("MAIN_GREEN");
        }
    }
}