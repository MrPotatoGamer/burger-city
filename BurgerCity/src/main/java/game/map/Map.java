package game.map;

import java.util.ArrayList;
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

        // Ipari létesítmények - nagyobb méretűek és több van belőlük
        // Farmok
        addIndustry(new Industry("Green Farm", IndustryType.FARM, 2, 15, 5, 4));
        addIndustry(new Industry("Wheat Fields", IndustryType.FARM, 15, 5, 6, 3));
        addIndustry(new Industry("Valley Farm", IndustryType.FARM, 12, 32, 5, 5));
        addIndustry(new Industry("North Farm", IndustryType.FARM, 30, 15, 4, 4));

        // Gyárak
        addIndustry(new Industry("Burger Factory", IndustryType.FACTORY, 25, 25, 4, 5));
        addIndustry(new Industry("Meat Processing", IndustryType.FACTORY, 35, 20, 5, 4));
        addIndustry(new Industry("Food Plant", IndustryType.FACTORY, 15, 25, 4, 3));
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

    public void updateForests() {}

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