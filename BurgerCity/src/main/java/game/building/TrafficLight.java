package game.building;

public class TrafficLight extends Building {

    public static final int COST = 1000;

    public TrafficLight(int x, int y) {
        super("TrafficLight", COST, x, y);
        this.currentState = "MAIN_GREEN";
        this.greenDurationMain = 5;
        this.greenDurationCross = 5;
    }

    private String currentState;
    private int greenDurationMain;
    private int greenDurationCross;

    public void setDurations(int main, int cross) {}

    public void switchLight() {}
}