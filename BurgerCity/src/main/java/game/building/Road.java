package game.building;

public class Road extends Building {

    public static final int COST = 100;

    public Road(int x, int y) {
        super("Road", COST, x, y);
    }
}