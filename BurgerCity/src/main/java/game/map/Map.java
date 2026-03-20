package game.map;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
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

    /** Előre definiált térkép betöltése */
    public void loadPredefined() {
        initGrass();

        // Városok - nagyobb méretűek és több van belőlük
        addCity(new City("Burger City", 3, 3, 8, 7));          // Nagy központi város
        addCity(new City("Meat Town", 35, 5, 6, 5));           // Középméretű város jobbra fent
        addCity(new City("Wheat Valley", 5, 28, 7, 6));        // Középméretű város lent balra
        addCity(new City("Factory District", 40, 30, 5, 5));   // Ipari város jobb alsó sarokban
        addCity(new City("Green Hills", 20, 15, 6, 5));        // Középső város

        // Ipari létesítmények
        // Supply chain:
        // FARM(WHEAT) -> BAKERY(BREAD)
        // RANCH(MEAT) -> PATTY_PLANT(MEAT_PATTY)
        // BREAD + MEAT_PATTY -> BURGER_FACTORY(HAMBURGER)
        addIndustry(new Industry("Green Farm", IndustryType.FARM, 2, 15, 5, 4));
        addIndustry(new Industry("Wheat Fields", IndustryType.FARM, 15, 5, 6, 3));
        addIndustry(new Industry("Valley Farm", IndustryType.FARM, 12, 32, 5, 5));
        addIndustry(new Industry("North Ranch", IndustryType.RANCH, 30, 15, 4, 4));

        addIndustry(new Industry("Food Plant", IndustryType.BAKERY, 15, 25, 4, 3));
        addIndustry(new Industry("Meat Processing", IndustryType.PATTY_PLANT, 35, 20, 5, 4));
        addIndustry(new Industry("Burger Factory", IndustryType.BURGER_FACTORY, 25, 25, 4, 5));
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