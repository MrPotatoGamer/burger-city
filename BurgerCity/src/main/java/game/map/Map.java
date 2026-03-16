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

        // Városok
        addCity(new City("Burger City", 2, 2, 3, 3));
        addCity(new City("Meat Town", 10, 1, 4, 3));
        addCity(new City("Wheat Valley", 3, 10, 3, 4));

        // Ipari létesítmények
        addIndustry(new Industry("Green Farm", IndustryType.FARM, 1, 6, 3, 2));
        addIndustry(new Industry("Burger Factory", IndustryType.FACTORY, 12, 8, 2, 3));
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
}