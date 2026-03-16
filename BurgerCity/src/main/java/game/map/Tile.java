package game.map;

public class Tile {

    private int x;
    private int y;
    private boolean isWalkable;
    private boolean isOccupied;
    private TileType type;

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.isWalkable = (type == TileType.GRASS);
        this.isOccupied = false;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public TileType getType() { return type; }
    public void setType(TileType type) { this.type = type; }
    public boolean isWalkable() { return isWalkable; }
    public void setWalkable(boolean walkable) { isWalkable = walkable; }
    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }
}