package game.vehicle;

import game.route.Route;
import game.core.Player;
import game.core.ResourcePrices;
import game.resource.Resource;
import game.resource.ResourceType;
import game.building.Garage;
import game.map.City;
import game.map.Industry;
import game.map.Map;
import game.map.Tile;
import game.map.TileType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vehicle {

    public static final int TILE_SIZE_PX = 32;

    protected int speed;
    protected int capacity;
    protected int maintenanceCost;
    protected int age;
    protected int timeSinceMaintenance;

    protected Route route;
    protected Resource currentCargo;
    protected Garage garage;

    // World-position (pixel) coordinates, centered on tiles
    protected double worldX;
    protected double worldY;

    protected int currentTileX;
    protected int currentTileY;

    protected Integer targetTileX;
    protected Integer targetTileY;

    protected Integer previousTileX;
    protected Integer previousTileY;

    protected int lastMoveDx;
    protected int lastMoveDy;

    // If set, vehicle will follow this path in order.
    protected List<int[]> pathTiles = List.of();
    protected int pathIndex = 0;
    protected boolean pathForward = true;

    private boolean arrivedThisUpdate = false;

    public Vehicle() {
        // Interpreted as tiles per second (converted internally to pixels/sec)
        this.speed = 2;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    public int getCurrentTileX() {
        return currentTileX;
    }

    public int getCurrentTileY() {
        return currentTileY;
    }

    public boolean isSpawned() {
        return targetTileX != null || (worldX != 0 || worldY != 0);
    }

    public void spawnAt(int tileX, int tileY) {
        this.currentTileX = tileX;
        this.currentTileY = tileY;
        this.targetTileX = null;
        this.targetTileY = null;
        this.previousTileX = null;
        this.previousTileY = null;
        this.lastMoveDx = 0;
        this.lastMoveDy = 0;
        this.worldX = tileCenterX(tileX);
        this.worldY = tileCenterY(tileY);
    }

    /**
     * Assign a ROAD-tile path for the vehicle to follow.
     * If the path is empty/null, the vehicle will remain idle.
     */
    public void setPath(List<int[]> pathTiles) {
        this.pathTiles = (pathTiles == null) ? List.of() : pathTiles;
        this.pathIndex = 0;
        this.pathForward = true;
        this.targetTileX = null;
        this.targetTileY = null;
        this.previousTileX = null;
        this.previousTileY = null;
        this.lastMoveDx = 0;
        this.lastMoveDy = 0;

        if (!this.pathTiles.isEmpty()) {
            int[] first = this.pathTiles.get(0);
            spawnAt(first[0], first[1]);
        }
    }

    public boolean hasPath() {
        return pathTiles != null && !pathTiles.isEmpty();
    }

    /**
     * Move along connected ROAD tiles.
     * @param map The game map.
     * @param deltaSeconds Time elapsed since last update.
     */
    public void update(Map map, double deltaSeconds) {
        Objects.requireNonNull(map, "map");
        if (deltaSeconds <= 0) return;

        // Vehicle only moves when a valid path is assigned.
        if (!hasPath()) return;

        // If we don't have a target yet, try to acquire one.
        if (targetTileX == null || targetTileY == null) {
            chooseNextTarget(map);
            return;
        }

        double targetX = tileCenterX(targetTileX);
        double targetY = tileCenterY(targetTileY);
        double dx = targetX - worldX;
        double dy = targetY - worldY;
        double dist = Math.hypot(dx, dy);

        // Arrived (snap).
        if (dist < 0.01) {
            arriveAtTarget(map);
            return;
        }

        double pixelsPerSecond = speed * (double) TILE_SIZE_PX;
        double step = pixelsPerSecond * deltaSeconds;
        if (step >= dist) {
            worldX = targetX;
            worldY = targetY;
            arriveAtTarget(map);
            return;
        }

        worldX += (dx / dist) * step;
        worldY += (dy / dist) * step;
    }

    /**
     * Minimal economy hook: call after {@link #update(Map, double)}.
     * Acts only when the vehicle arrived at a tile in the last update, and only at path endpoints.
     */
    public void processArrivalEconomy(Map map, Player player) {
        if (!arrivedThisUpdate) return;
        arrivedThisUpdate = false;
        if (map == null || player == null) return;

        if (!hasPath() || pathTiles.size() < 2) return;
        int idx = indexOfTile(pathTiles, currentTileX, currentTileY);
        if (idx != 0 && idx != pathTiles.size() - 1) return;

        City adjacentCity = findAdjacentCity(map, currentTileX, currentTileY);
        Industry adjacentIndustry = findAdjacentIndustry(map, currentTileX, currentTileY);

        // Prioritize a city if both are adjacent (rare, but can happen in tight maps).
        if (adjacentCity != null) {
            handleCityInteraction(adjacentCity, player);
        } else if (adjacentIndustry != null) {
            handleIndustryInteraction(adjacentIndustry, player);
        }
    }

    protected void arriveAtTarget(Map map) {
        if (targetTileX == null || targetTileY == null) return;
        previousTileX = currentTileX;
        previousTileY = currentTileY;

        int newTileX = targetTileX;
        int newTileY = targetTileY;
        lastMoveDx = Integer.compare(newTileX, currentTileX);
        lastMoveDy = Integer.compare(newTileY, currentTileY);

        currentTileX = newTileX;
        currentTileY = newTileY;
        targetTileX = null;
        targetTileY = null;

        arrivedThisUpdate = true;
        chooseNextTarget(map);
    }

    protected void chooseNextTarget(Map map) {
        if (!hasPath()) {
            targetTileX = null;
            targetTileY = null;
            return;
        }

        // Ensure current index is consistent.
        if (pathIndex < 0) pathIndex = 0;
        if (pathIndex >= pathTiles.size()) pathIndex = pathTiles.size() - 1;

        // If we're not exactly on the expected tile, try to resync.
        int[] expected = pathTiles.get(pathIndex);
        if (expected[0] != currentTileX || expected[1] != currentTileY) {
            int idx = indexOfTile(pathTiles, currentTileX, currentTileY);
            if (idx >= 0) {
                pathIndex = idx;
            }
        }

        int nextIndex = pathForward ? pathIndex + 1 : pathIndex - 1;
        if (nextIndex >= pathTiles.size()) {
            pathForward = false;
            nextIndex = pathIndex - 1;
        } else if (nextIndex < 0) {
            pathForward = true;
            nextIndex = pathIndex + 1;
        }

        if (nextIndex < 0 || nextIndex >= pathTiles.size()) {
            targetTileX = null;
            targetTileY = null;
            return;
        }

        int[] next = pathTiles.get(nextIndex);
        // Safety: only move onto ROAD.
        if (!isRoad(map, next[0], next[1])) {
            targetTileX = null;
            targetTileY = null;
            return;
        }

        targetTileX = next[0];
        targetTileY = next[1];
        pathIndex = nextIndex;
    }

    private static int indexOfTile(List<int[]> tiles, int x, int y) {
        for (int i = 0; i < tiles.size(); i++) {
            int[] t = tiles.get(i);
            if (t != null && t.length >= 2 && t[0] == x && t[1] == y) return i;
        }
        return -1;
    }

    protected boolean isPrevious(int x, int y) {
        return previousTileX != null && previousTileY != null && previousTileX == x && previousTileY == y;
    }

    protected static boolean isRoad(Map map, int x, int y) {
        Tile tile = map.getTile(x, y);
        return tile != null && tile.getType() == TileType.ROAD;
    }

    protected static List<int[]> roadNeighbors(Map map, int x, int y) {
        List<int[]> result = new ArrayList<>(4);
        if (isRoad(map, x + 1, y)) result.add(new int[]{x + 1, y});
        if (isRoad(map, x - 1, y)) result.add(new int[]{x - 1, y});
        if (isRoad(map, x, y + 1)) result.add(new int[]{x, y + 1});
        if (isRoad(map, x, y - 1)) result.add(new int[]{x, y - 1});
        return result;
    }

    protected static double tileCenterX(int tileX) {
        return tileX * (double) TILE_SIZE_PX + (TILE_SIZE_PX / 2.0);
    }

    protected static double tileCenterY(int tileY) {
        return tileY * (double) TILE_SIZE_PX + (TILE_SIZE_PX / 2.0);
    }

    public void transport() {}

    public boolean needsMaintenance() {
        return false;
    }

    public void goToGarage() {}

    protected boolean canCarry(ResourceType type) {
        return true;
    }

    private void handleCityInteraction(City city, Player player) {
        if (city == null) return;

        if (currentCargo == null || currentCargo.isEmpty()) {
            // Load passengers only (goods are not produced by cities in this minimal model).
            if (!canCarry(ResourceType.PASSENGERS)) return;
            int taken = city.load(ResourceType.PASSENGERS, Math.max(0, capacity));
            if (taken > 0) {
                currentCargo = new Resource(ResourceType.PASSENGERS, taken);
            }
            return;
        }

        // Unload.
        ResourceType type = currentCargo.getType();
        int amount = currentCargo.getAmount();
        if (amount <= 0) return;

        int accepted = city.deliver(type, amount);
        accepted = Math.max(0, Math.min(accepted, amount));
        if (accepted <= 0) return;

        int revenue = accepted * ResourcePrices.revenuePerUnit(type);
        if (revenue != 0) player.addMoney(revenue);
        currentCargo.removeUpTo(accepted);
        if (currentCargo.isEmpty()) currentCargo = null;
    }

    private void handleIndustryInteraction(Industry industry, Player player) {
        if (industry == null) return;

        if (currentCargo == null || currentCargo.isEmpty()) {
            // Load one of the industry's produced goods.
            for (ResourceType type : industry.getProfile().getOutputsPerUnit().keySet()) {
                if (type == null) continue;
                if (type == ResourceType.PASSENGERS) continue;
                if (!canCarry(type)) continue;

                int taken = industry.takeFromStorage(type, Math.max(0, capacity));
                if (taken > 0) {
                    currentCargo = new Resource(type, taken);
                    return;
                }
            }
            return;
        }

        // Deliver inputs to industries that consume them.
        ResourceType type = currentCargo.getType();
        int amount = currentCargo.getAmount();
        if (amount <= 0) return;

        if (!industry.consumes(type)) return;
        industry.deliverToStorage(type, amount);

        int revenue = amount * ResourcePrices.revenuePerUnit(type);
        if (revenue != 0) player.addMoney(revenue);
        currentCargo = null;
    }

    private static City findAdjacentCity(Map map, int roadX, int roadY) {
        for (City c : map.getCities()) {
            if (c == null) continue;
            if (c.occupies(roadX + 1, roadY)
                    || c.occupies(roadX - 1, roadY)
                    || c.occupies(roadX, roadY + 1)
                    || c.occupies(roadX, roadY - 1)) {
                return c;
            }
        }
        return null;
    }

    private static Industry findAdjacentIndustry(Map map, int roadX, int roadY) {
        for (Industry i : map.getIndustries()) {
            if (i == null) continue;
            if (i.occupies(roadX + 1, roadY)
                    || i.occupies(roadX - 1, roadY)
                    || i.occupies(roadX, roadY + 1)
                    || i.occupies(roadX, roadY - 1)) {
                return i;
            }
        }
        return null;
    }
}