package game.building;

public class Road extends Building {

    public static final int COST = 60;

    public Road(int x, int y) {
        super("Út", COST, x, y);
    }
}