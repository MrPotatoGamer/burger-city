package game.building;

public class Road extends Building {

    public static final int COST = 100;

    private int x;
    private int y;

    public Road(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public static int getCost() {
        return COST;
    }
}