package game.building;

public class Stop extends Building {

    public static final int COST = 600;

    public Stop(int x, int y) {
        super("Megálló", COST, x, y);
    }

    public void load() {}

    public void unload() {}
}