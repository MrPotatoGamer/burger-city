package game.map;

import game.building.Building;
import game.building.Garage;

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

    // Forest simulation
    private final Random forestRng = new Random(System.nanoTime());
    private double forestAccumulatorSeconds = 0;

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

        // Random erdők minden új játékindításkor (2-3 összefüggő, szabálytalan folt)
        spawnRandomForests();
    }

    private void spawnRandomForests() {
        Random rng = forestRng;
        int forestPatches = 3 + rng.nextInt(4); // 3..6

        for (int i = 0; i < forestPatches; i++) {
            int attempts = 900;
            int seedX = -1;
            int seedY = -1;

            while (attempts-- > 0) {
                int x = rng.nextInt(width);
                int y = rng.nextInt(height);
                if (!canPlaceForestAt(x, y)) continue;
                // Keep patches separated so we typically get 2-3 distinct clusters.
                if (hasNearbyForest(x, y, 2)) continue;
                // Avoid hugging the very edge (helps the patch grow to an irregular shape)
                if (x < 1 || y < 1 || x > width - 2 || y > height - 2) continue;
                seedX = x;
                seedY = y;
                break;
            }

            if (seedX == -1) continue;

            int targetSize = 20 + rng.nextInt(31); // 20..50 tiles
            growForestPatch(seedX, seedY, targetSize, rng);
        }
    }

    private boolean hasNearbyForest(int x, int y, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (!inBounds(nx, ny)) continue;
                Tile t = getTile(nx, ny);
                if (t != null && t.getType() == TileType.FOREST) return true;
            }
        }
        return false;
    }

    private boolean canPlaceForestAt(int x, int y) {
        if (!inBounds(x, y)) return false;
        Tile t = getTile(x, y);
        if (t == null) return false;
        return t.getType() == TileType.GRASS && !t.isOccupied() && t.getPlacedBuilding() == null;
    }

    private void setForestAt(int x, int y) {
        Tile t = getTile(x, y);
        if (t == null) return;
        // Forest is still an empty field, just with trees.
        t.setType(TileType.FOREST);
        if (t.getForestTrees() <= 0) {
            t.setForestTrees(1 + forestRng.nextInt(4));
        }
        t.setWalkable(true);
        t.setOccupied(false);
    }

    private void setForestAt(int x, int y, int trees) {
        Tile t = getTile(x, y);
        if (t == null) return;
        t.setType(TileType.FOREST);
        t.setForestTrees(trees);
        t.setWalkable(true);
        t.setOccupied(false);
        t.setPlacedBuilding(null);
    }

    private void growForestPatch(int seedX, int seedY, int targetSize, Random rng) {
        // Connected patch growth: always expand from an already-placed forest tile.
        List<int[]> forestTiles = new ArrayList<>();
        List<int[]> frontier = new ArrayList<>();

        setForestAt(seedX, seedY);
        forestTiles.add(new int[]{seedX, seedY});
        frontier.add(new int[]{seedX, seedY});

        int guard = 0;
        while (forestTiles.size() < targetSize && !frontier.isEmpty() && guard++ < targetSize * 40) {
            // Pick a base tile; using the frontier keeps patches blobby, but we randomize for irregularity.
            int[] base = (rng.nextInt(100) < 70)
                    ? frontier.get(rng.nextInt(frontier.size()))
                    : forestTiles.get(rng.nextInt(forestTiles.size()));

            boolean placed = false;
            for (int tries = 0; tries < 8; tries++) {
                int dir = rng.nextInt(4);
                int nx = base[0];
                int ny = base[1];
                if (dir == 0) nx++;
                else if (dir == 1) nx--;
                else if (dir == 2) ny++;
                else ny--;

                if (!canPlaceForestAt(nx, ny)) continue;

                setForestAt(nx, ny);
                int[] added = new int[]{nx, ny};
                forestTiles.add(added);
                frontier.add(added);
                placed = true;
                break;
            }

            if (!placed) {
                // If we can't expand from this edge anymore, drop some frontier tiles.
                if (!frontier.isEmpty() && rng.nextInt(100) < 40) {
                    frontier.remove(rng.nextInt(frontier.size()));
                }
            } else {
                // Occasionally trim frontier to create holes/indentations for a more irregular silhouette.
                if (!frontier.isEmpty() && rng.nextInt(100) < 15) {
                    frontier.remove(rng.nextInt(frontier.size()));
                }
            }
        }
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
     * Returns all placed Garages on the map.
     */
    public List<Garage> getGarages() {
        List<Garage> out = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile t = tiles[x][y];
                if (t == null) continue;
                if (t.getType() != TileType.BUILDING) continue;
                Building b = t.getPlacedBuilding();
                if (b instanceof Garage g) {
                    out.add(g);
                }
            }
        }
        return out;
    }

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

    /**
     * Forest simulation tick:
     * - Trees can appear on empty grass tiles.
     * - A forest tile can grow from 1..4 trees over time.
     * - If a tile has 3 or 4 trees, it can spread to adjacent empty grass tiles.
     */
    public void updateForests(double deltaSeconds) {
        if (deltaSeconds <= 0) return;
        forestAccumulatorSeconds += deltaSeconds;
        while (forestAccumulatorSeconds >= 1.0) {
            forestAccumulatorSeconds -= 1.0;
            forestStep1s();
        }
    }

    private record ForestChange(int x, int y, int newTrees) {}

    private void forestStep1s() {
        // Probabilities are per-second.
        // Slower growth: ~1/8 speed (half of the previous tuning).
        double spontaneousSpawnP = 0.0000025; // any empty grass tile can sprout
        double growP = 0.0125; // forest tile can gain +1 tree (until 4)
        double spreadP = 0.01; // 3-4 tree tile can seed a neighbor

        List<ForestChange> changes = new ArrayList<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Tile t = getTile(x, y);
                if (t == null) continue;
                if (t.getPlacedBuilding() != null) continue;
                TileType type = t.getType();

                if (type == TileType.GRASS) {
                    if (!t.isOccupied() && forestRng.nextDouble() < spontaneousSpawnP) {
                        changes.add(new ForestChange(x, y, 1));
                    }
                    continue;
                }

                if (type != TileType.FOREST) continue;

                int trees = Math.max(0, t.getForestTrees());
                if (trees <= 0) trees = 1;

                if (trees < 4 && forestRng.nextDouble() < growP) {
                    changes.add(new ForestChange(x, y, trees + 1));
                    trees++;
                }

                if (trees >= 3 && forestRng.nextDouble() < spreadP) {
                    int dir = forestRng.nextInt(4);
                    int nx = x + (dir == 0 ? 1 : dir == 1 ? -1 : 0);
                    int ny = y + (dir == 2 ? 1 : dir == 3 ? -1 : 0);
                    if (!inBounds(nx, ny)) continue;
                    Tile nt = getTile(nx, ny);
                    if (nt == null) continue;
                    if (nt.getPlacedBuilding() != null) continue;
                    if (nt.getType() == TileType.GRASS && !nt.isOccupied()) {
                        changes.add(new ForestChange(nx, ny, 1));
                    }
                }
            }
        }

        // Apply changes (avoid overwriting roads/buildings).
        for (ForestChange c : changes) {
            if (!inBounds(c.x, c.y)) continue;
            Tile t = getTile(c.x, c.y);
            if (t == null) continue;
            if (t.getPlacedBuilding() != null) continue;
            if (t.getType() != TileType.GRASS && t.getType() != TileType.FOREST) continue;
            if (t.isOccupied()) continue;
            setForestAt(c.x, c.y, c.newTrees);
        }
    }

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
            // Don't allow overwriting an existing traffic light (or any other placed building on this road tile).
            if (tile.isOccupied() || tile.getPlacedBuilding() != null) return false;

            // Check if it's an intersection (3 or 4 road neighbors)
            int roadNeighbors = countRoadNeighbors(x, y);
            if (roadNeighbors < 3) return false; // Must be at least a 3-way intersection

            // Don't change tile type - keep it as ROAD
            tile.setOccupied(true);
            tile.setPlacedBuilding(building);
            return true;
        }

        // Garage must be connected to the road network (adjacent ROAD tile)
        if (building instanceof Garage) {
            if (!hasAdjacentRoad(x, y)) return false;
        }

        // Regular buildings: only on grass
        if (tile.getType() != TileType.GRASS || tile.isOccupied()) return false;

        tile.setType(TileType.BUILDING);
        tile.setWalkable(false);
        tile.setOccupied(true);
        tile.setPlacedBuilding(building);
        return true;
    }

    private boolean hasAdjacentRoad(int x, int y) {
        return isRoadOnly(x, y - 1) || isRoadOnly(x + 1, y) || isRoadOnly(x, y + 1) || isRoadOnly(x - 1, y);
    }

    private boolean isRoadOnly(int x, int y) {
        if (!inBounds(x, y)) return false;
        Tile t = getTile(x, y);
        return t != null && t.getType() == TileType.ROAD;
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
     * Demolishes a FOREST tile back to GRASS.
     */
    public boolean demolishForest(int x, int y) {
        if (!inBounds(x, y)) return false;
        Tile tile = getTile(x, y);
        if (tile == null) return false;
        if (tile.getType() != TileType.FOREST) return false;

        tile.setForestTrees(0);
        tile.setType(TileType.GRASS);
        tile.setWalkable(true);
        tile.setOccupied(false);
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

    /**
     * Finds a ROAD-only path between two ROAD tiles (inclusive).
     * Returns an empty list if no valid path exists.
     */
    public List<int[]> findRoadPathBetweenRoadTiles(int startX, int startY, int targetX, int targetY) {
        if (!inBounds(startX, startY) || !inBounds(targetX, targetY)) return List.of();
        Tile s = getTile(startX, startY);
        Tile t = getTile(targetX, targetY);
        if (s == null || t == null) return List.of();
        if (s.getType() != TileType.ROAD || t.getType() != TileType.ROAD) return List.of();

        int startKey = toKey(startX, startY);
        int targetKey = toKey(targetX, targetY);
        if (startKey == targetKey) return List.of(new int[]{startX, startY});

        boolean[] isTarget = new boolean[width * height];
        isTarget[targetKey] = true;

        int[] parent = new int[width * height];
        boolean[] visited = new boolean[width * height];
        for (int i = 0; i < parent.length; i++) parent[i] = -1;

        Deque<Integer> q = new ArrayDeque<>();
        visited[startKey] = true;
        parent[startKey] = -2;
        q.addLast(startKey);

        int found = -1;
        while (!q.isEmpty()) {
            int cur = q.removeFirst();
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

        List<int[]> path = new ArrayList<>();
        int cur = found;
        while (cur != -2) {
            int x = cur / height;
            int y = cur % height;
            path.add(new int[]{x, y});
            cur = parent[cur];
        }

        for (int i = 0, j = path.size() - 1; i < j; i++, j--) {
            int[] tmp = path.get(i);
            path.set(i, path.get(j));
            path.set(j, tmp);
        }
        return path;
    }

    /**
     * Returns ROAD tiles adjacent to the given rectangular area.
     */
    public List<int[]> adjacentRoadTilesForArea(int ox, int oy, int w, int h) {
        return adjacentRoadTiles(ox, oy, w, h);
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

        // Only on empty GRASS or FOREST tiles.
        // FOREST is allowed (clearing happens implicitly).
        if (tile.isOccupied() || tile.getPlacedBuilding() != null) {
            return false;
        }
        if (tile.getType() != TileType.GRASS && tile.getType() != TileType.FOREST) {
            return false;
        }

        if (tile.getType() == TileType.FOREST) {
            tile.setForestTrees(0);
        }

        // Út létrehozása
        tile.setType(TileType.ROAD);
        tile.setWalkable(true);
        tile.setOccupied(true);

        return true;
    }
}