package game.map;

import game.building.Building;

public class Tile {

    private int x;
    private int y;
    private boolean isWalkable;
    private boolean isOccupied;
    private TileType type;
    private Building placedBuilding;

    // Forest state: how many trees are on this tile (0..4).
    // Only meaningful when type == FOREST.
    private int forestTrees;

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.isWalkable = (type == TileType.GRASS);
        this.isOccupied = false;
        this.placedBuilding = null;
        this.forestTrees = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public TileType getType() { return type; }
    public void setType(TileType type) {
        this.type = type;
        if (type != TileType.FOREST) {
            this.forestTrees = 0;
        }
    }
    public boolean isWalkable() { return isWalkable; }
    public void setWalkable(boolean walkable) { isWalkable = walkable; }
    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }

    public Building getPlacedBuilding() {
        return placedBuilding;
    }

    public void setPlacedBuilding(Building placedBuilding) {
        this.placedBuilding = placedBuilding;
    }

    public int getForestTrees() {
        return forestTrees;
    }

    public void setForestTrees(int forestTrees) {
        this.forestTrees = Math.max(0, Math.min(4, forestTrees));
        if (this.forestTrees > 0) {
            this.type = TileType.FOREST;
        } else if (this.type == TileType.FOREST) {
            this.type = TileType.GRASS;
        }
    }
}