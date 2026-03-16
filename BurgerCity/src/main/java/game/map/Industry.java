package game.map;

public class Industry {

    private String name;
    private IndustryType industryType;
    private int originX;
    private int originY;
    private int width;
    private int height;

    /** Minimum 2x2 */
    public Industry(String name, IndustryType industryType, int originX, int originY, int width, int height) {
        this.name = name;
        this.industryType = industryType;
        this.originX = originX;
        this.originY = originY;
        this.width = Math.max(width, 2);
        this.height = Math.max(height, 2);
    }

    public String getName() { return name; }
    public IndustryType getIndustryType() { return industryType; }
    public int getOriginX() { return originX; }
    public int getOriginY() { return originY; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public boolean occupies(int x, int y) {
        return x >= originX && x < originX + width
            && y >= originY && y < originY + height;
    }
}