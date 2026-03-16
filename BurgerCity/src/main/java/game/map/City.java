package game.map;

public class City {

    private String name;
    private int population;
    private int originX;
    private int originY;
    private int width;
    private int height;

    /** Minimum 3x3 */
    public City(String name, int originX, int originY, int width, int height) {
        this.name = name;
        this.originX = originX;
        this.originY = originY;
        this.width = Math.max(width, 3);
        this.height = Math.max(height, 3);
        this.population = 0;
    }

    public String getName() { return name; }
    public int getPopulation() { return population; }
    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean occupies(int x, int y) {
        return x >= originX && x < originX + width
            && y >= originY && y < originY + height;
    }

    public void generatePassengers() {}

    public void acceptGoods() {}
}