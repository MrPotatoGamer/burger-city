package game.building;

public class Bakery extends Building {

    public static final int COST = 1600;

    public Bakery(int x, int y) {
        super("Pékség", COST, x, y);
    }
}
