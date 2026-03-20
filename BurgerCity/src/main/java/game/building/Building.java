package game.building;

public class Building {

    protected int id;
    protected String name;
    protected int cost;

    protected int x;
    protected int y;

    protected Building(String name, int cost, int x, int y) {
        this.name = name;
        this.cost = cost;
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public int getCost() {
        return cost;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}