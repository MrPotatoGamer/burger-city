package game.map;

import game.building.Building;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Map {

    private int width;
    private int height;
    private Tile[][] tiles;

    private List<City> cities;
    private List<Industry> industries;

    public Map(int width, int height) {
        this.width = width;
        this.height = height;
        this.cities = new ArrayList<>();
        this.industries = new ArrayList<>();
        this.tiles = new Tile[width][height];
    }

    /**
     * Initializes all tiles to GRASS. Intended for save/load reconstruction.
     * (The normal game flow uses {@link #loadPredefined()}.)
     */
    public void initGrassForLoad() {
        initGrass();
    }

    /** Előre definiált térkép betöltése */
    public void loadPredefined() {
        initGrass();

        // Városok - nagyobb méretűek és több van belőlük
        addCity(new City("Burger City", 3, 3, 8, 7));          // Nagy központi város
        addCity(new City("Meat Town", 35, 5, 6, 5));           // Középméretű város jobbra fent
        addCity(new City("Wheat Valley", 5, 28, 7, 6));        // Középméretű város lent balra
        addCity(new City("Factory District", 40, 30, 5, 5));   // Ipari város jobb alsó sarokban
        addCity(new City("Green Hills", 20, 15, 6, 5));        // Középső város
    }

    private void initGrass() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(x, y, TileType.GRASS);
            }
        }
    }

    private void addCity(City city) {
        cities.add(city);
        for (int x = city.getOriginX(); x < city.getOriginX() + city.getWidth(); x++) {
            for (int y = city.getOriginY(); y < city.getOriginY() + city.getHeight(); y++) {
                if (inBounds(x, y)) {
                    tiles[x][y].setType(TileType.CITY);
                    tiles[x][y].setWalkable(false);
                    tiles[x][y].setOccupied(true);
                }
            }
        }
    }

    /**
     * Adds a city and marks its footprint on the tile grid.
     * Public for save/load reconstruction.
     */
    public void addCityForLoad(City city) {
        addCity(city);
    }

    private void addIndustry(Industry industry) {
        industries.add(industry);
        for (int x = industry.getOriginX(); x < industry.getOriginX() + industry.getWidth(); x++) {
            for (int y = industry.getOriginY(); y < industry.getOriginY() + industry.getHeight(); y++) {
                if (inBounds(x, y)) {
                    tiles[x][y].setType(TileType.INDUSTRY);
                    tiles[x][y].setWalkable(false);
                    tiles[x][y].setOccupied(true);
                }
            }
        }
    }

    /**
     * Adds an industry and marks its footprint on the tile grid.
     * Public for save/load reconstruction.
     */
    public void addIndustryForLoad(Industry industry) {
        addIndustry(industry);
    }

    /**
     * Player-placed industry placement.
     * Industries occupy a 2x2 area and are only allowed on unoccupied GRASS tiles.
     */
    public boolean buildIndustry(int originX, int originY, IndustryType type) {
        if (type == null) return false;
        if (!inBounds(originX, originY)) return false;

        int w = 2;
        int h = 2;
        if (!inBounds(originX + w - 1, originY + h - 1)) return false;

        if (overlapsAny(originX, originY, w, h)) return false;

        // Enforce GRASS base terrain for the whole footprint.
        for (int x = originX; x < originX + w; x++) {
            for (int y = originY; y < originY + h; y++) {
                Tile t = getTile(x, y);
                if (t == null) return false;
                if (t.getType() != TileType.GRASS) return false;
                if (t.isOccupied()) return false;
            }
        }

        String name = switch (type) {
            case FARM -> "Farm";
            case RANCH -> "Ranch";
            case BAKERY -> "Bakery";
            case PATTY_PLANT -> "Patty Plant";
            case BURGER_FACTORY -> "Burger Factory";
            case FACTORY -> "Factory";
        };

        addIndustry(new Industry(name, type, originX, originY, w, h));
        return true;
    }

    private boolean overlapsAny(int ox, int oy, int w, int h) {
        for (int x = ox; x < ox + w; x++) {
            for (int y = oy; y < oy + h; y++) {
                if (inBounds(x, y) && tiles[x][y].isOccupied()) return true;
            }
        }
        return false;
    }

    private boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public Tile getTile(int x, int y) {
        if (inBounds(x, y)) return tiles[x][y];
        return null;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public List<City> getCities() { return cities; }
    public List<Industry> getIndustries() { return industries; }

    /**
     * Economy tick: updates city demand/passenger generation and industry production.
     */
    public void updateEconomy(double deltaSeconds) {
        if (deltaSeconds <= 0) return;
        for (City c : cities) {
            if (c != null) c.update(deltaSeconds);
        }
        for (Industry i : industries) {
            if (i != null) i.update(deltaSeconds);
        }
    }

    public void updateForests() {}

    /**
     * Player-built building placement.
     * Most buildings only allowed on unoccupied GRASS tiles.
     * TrafficLight is special: only allowed on ROAD intersections (3+ connections).
     */
    public boolean buildBuilding(int x, int y, Building building) {
        if (building == null) return false;
        if (!inBounds(x, y)) return false;

        Tile tile = getTile(x, y);
        if (tile == null) return false;

        // Special case: TrafficLight can only be built on road intersections
        if (building instanceof game.building.TrafficLight) {
            if (tile.getType() != TileType.ROAD) return false;
            if (tile.isOccupied() && tile.getType() != TileType.ROAD) return false; // Can't build if already has a traffic light

            // Check if it's an intersection (3 or 4 road neighbors)
            int roadNeighbors = countRoadNeighbors(x, y);
            if (roadNeighbors < 3) return false; // Must be at least a 3-way intersection

            // Don't change tile type - keep it as ROAD
            tile.setOccupied(true);
            tile.setPlacedBuilding(building);
            return true;
        }

        // Regular buildings: only on grass
        if (tile.getType() != TileType.GRASS || tile.isOccupied()) return false;

        tile.setType(TileType.BUILDING);
        tile.setWalkable(false);
        tile.setOccupied(true);
        tile.setPlacedBuilding(building);
        return true;
    }

    /**
     * Count how many neighbors of a tile are ROAD or BUILDING (for road connectivity).
     */
    private int countRoadNeighbors(int x, int y) {
        int count = 0;
        if (isRoadOrBuilding(x, y - 1)) count++; // North
        if (isRoadOrBuilding(x + 1, y)) count++; // East
        if (isRoadOrBuilding(x, y + 1)) count++; // South
        if (isRoadOrBuilding(x - 1, y)) count++; // West
        return count;
    }

    private boolean isRoadOrBuilding(int x, int y) {
        if (!inBounds(x, y)) return false;
        Tile t = getTile(x, y);
        if (t == null) return false;
        TileType type = t.getType();
        return type == TileType.ROAD || type == TileType.BUILDING;
    }

    /**
     * Demolishes a player-built building tile and returns the building if any.
     */
    public Building demolishBuilding(int x, int y) {
        if (!inBounds(x, y)) return null;
        Tile tile = getTile(x, y);
        if (tile == null) return null;

        if (tile.getType() != TileType.BUILDING) return null;
        Building b = tile.getPlacedBuilding();
        if (b == null) return null;

        tile.setPlacedBuilding(null);
        tile.setType(TileType.GRASS);
        tile.setWalkable(true);
        tile.setOccupied(false);
        return b;
    }

    /**
     * Demolishes a road tile (no refund handled here).
     */
    public boolean demolishRoad(int x, int y) {
        if (!inBounds(x, y)) return false;
        Tile tile = getTile(x, y);
        if (tile == null) return false;
        if (tile.getType() != TileType.ROAD) return false;

        tile.setType(TileType.GRASS);
        tile.setWalkable(true);
        tile.setOccupied(false);
        tile.setPlacedBuilding(null); // Remove any traffic light
        return true;
    }

    /**
     * Check if a traffic light at (x, y) is still valid.
     * A traffic light is valid if:
     * - The tile is ROAD
     * - The tile has at least 3 road neighbors (intersection)
     */
    public boolean isTrafficLightValid(int x, int y) {
        if (!inBounds(x, y)) return false;
        Tile tile = getTile(x, y);
        if (tile == null) return false;
        if (tile.getType() != TileType.ROAD) return false;

        // Check if it's still an intersection
        int roadNeighbors = countRoadNeighbors(x, y);
        return roadNeighbors >= 3;
    }

    /**
     * Demolishes an industry by clicking any tile inside its footprint.
     * Returns the removed industry, or null if there is none.
     */
    public Industry demolishIndustryAt(int x, int y) {
        if (!inBounds(x, y)) return null;

        Iterator<Industry> it = industries.iterator();
        while (it.hasNext()) {
            Industry ind = it.next();
            if (ind == null) continue;
            if (!ind.occupies(x, y)) continue;

            // Clear footprint
            for (int tx = ind.getOriginX(); tx < ind.getOriginX() + ind.getWidth(); tx++) {
                for (int ty = ind.getOriginY(); ty < ind.getOriginY() + ind.getHeight(); ty++) {
                    if (!inBounds(tx, ty)) continue;
                    Tile tile = getTile(tx, ty);
                    if (tile == null) continue;
                    tile.setType(TileType.GRASS);
                    tile.setWalkable(true);
                    tile.setOccupied(false);
                }
            }

            it.remove();
            return ind;
        }

        return null;
    }

    /**
     * Finds a ROAD-only path between two rectangular areas (e.g., City/Industry footprints).
     * The path starts at a ROAD tile adjacent to area A and ends at a ROAD tile adjacent to area B.
     * Returns an empty list if no valid path exists.
     */
    public List<int[]> findRoadPathBetweenAreas(int ax, int ay, int aw, int ah,
                                                int bx, int by, int bw, int bh) {
        List<int[]> starts = adjacentRoadTiles(ax, ay, aw, ah);
        if (starts.isEmpty()) return List.of();

        boolean[] isTarget = new boolean[width * height];
        for (int[] t : adjacentRoadTiles(bx, by, bw, bh)) {
            isTarget[toKey(t[0], t[1])] = true;
        }
        boolean anyTarget = false;
        for (boolean v : isTarget) {
            if (v) { anyTarget = true; break; }
        }
        if (!anyTarget) return List.of();

        int[] parent = new int[width * height];
        boolean[] visited = new boolean[width * height];
        for (int i = 0; i < parent.length; i++) parent[i] = -1;

        Deque<Integer> q = new ArrayDeque<>();
        for (int[] s : starts) {
            int key = toKey(s[0], s[1]);
            if (visited[key]) continue;
            visited[key] = true;
            parent[key] = -2;
            q.addLast(key);
        }

        int found = -1;
        while (!q.isEmpty()) {
            int cur = q.removeFirst();
            if (isTarget[cur]) {
                found = cur;
                break;
            }
            int cx = cur / height;
            int cy = cur % height;

            found = bfsEnqueueRoadNeighbor(cx + 1, cy, cur, visited, parent, q, isTarget);
            if (found != -1) break;
            found = bfsEnqueueRoadNeighbor(cx - 1, cy, cur, visited, parent, q, isTarget);
            if (found != -1) break;
            found = bfsEnqueueRoadNeighbor(cx, cy + 1, cur, visited, parent, q, isTarget);
            if (found != -1) break;
            found = bfsEnqueueRoadNeighbor(cx, cy - 1, cur, visited, parent, q, isTarget);
            if (found != -1) break;
        }

        if (found == -1) return List.of();

        // Reconstruct path
        List<int[]> path = new ArrayList<>();
        int cur = found;
        while (cur != -2) {
            int x = cur / height;
            int y = cur % height;
            path.add(new int[]{x, y});
            cur = parent[cur];
        }

        // Reverse in-place
        for (int i = 0, j = path.size() - 1; i < j; i++, j--) {
            int[] tmp = path.get(i);
            path.set(i, path.get(j));
            path.set(j, tmp);
        }
        return path;
    }

    private int bfsEnqueueRoadNeighbor(int nx, int ny, int fromKey,
                                      boolean[] visited, int[] parent, Deque<Integer> q, boolean[] isTarget) {
        if (!inBounds(nx, ny)) return -1;
        Tile t = getTile(nx, ny);
        if (t == null || t.getType() != TileType.ROAD) return -1;
        int key = toKey(nx, ny);
        if (visited[key]) return -1;
        visited[key] = true;
        parent[key] = fromKey;
        if (isTarget[key]) return key;
        q.addLast(key);
        return -1;
    }

    private int toKey(int x, int y) {
        return x * height + y;
    }

    private List<int[]> adjacentRoadTiles(int ox, int oy, int w, int h) {
        List<int[]> result = new ArrayList<>();

        // Above and below
        for (int x = ox; x < ox + w; x++) {
            addIfRoad(result, x, oy - 1);
            addIfRoad(result, x, oy + h);
        }

        // Left and right
        for (int y = oy; y < oy + h; y++) {
            addIfRoad(result, ox - 1, y);
            addIfRoad(result, ox + w, y);
        }

        return result;
    }

    private void addIfRoad(List<int[]> out, int x, int y) {
        if (!inBounds(x, y)) return;
        Tile t = getTile(x, y);
        if (t != null && t.getType() == TileType.ROAD) {
            out.add(new int[]{x, y});
        }
    }

    /**
     * Út építése a megadott koordinátára
     * @param x Az út x koordinátája
     * @param y Az út y koordinátája
     * @return true ha sikeres volt az építés, false egyébként
     */
    public boolean buildRoad(int x, int y) {
        if (!inBounds(x, y)) {
            return false;
        }

        Tile tile = getTile(x, y);

        // Csak üres füves mezőre lehet utat építeni
        if (tile.getType() != TileType.GRASS || tile.isOccupied()) {
            return false;
        }

        // Út létrehozása
        tile.setType(TileType.ROAD);
        tile.setWalkable(true);
        tile.setOccupied(true);

        return true;
    }
}