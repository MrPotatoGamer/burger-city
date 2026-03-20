package game.building;

public class Stop extends Building {

    public static final int COST = 1000;

    public Stop(int x, int y) {
        super("Stop", COST, x, y);
    }

    public void load() {}

    public void unload() {}
}