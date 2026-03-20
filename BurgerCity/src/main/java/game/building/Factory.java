package game.building;

public class Factory extends Building {

    public static final int COST = 1000;

    public Factory(int x, int y) {
        super("Factory", COST, x, y);
    }
}
