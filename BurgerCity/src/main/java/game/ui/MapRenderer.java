package game.ui;

import game.map.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class MapRenderer extends JPanel {

    private static final int TILE_SIZE = 32;

    private game.map.Map map;
    private final java.util.Map<TileType, Color> tileColors = new HashMap<>();
    private final java.util.Map<IndustryType, Color> industryColors = new HashMap<>();

    public MapRenderer(game.map.Map map) {
        this.map = map;

        tileColors.put(TileType.GRASS,    new Color(100, 180, 80));
        tileColors.put(TileType.CITY,     new Color(180, 180, 180));
        tileColors.put(TileType.INDUSTRY, new Color(200, 140, 60));

        industryColors.put(IndustryType.FARM,    new Color(160, 210, 80));
        industryColors.put(IndustryType.FACTORY, new Color(180, 100, 100));

        setPreferredSize(new Dimension(map.getWidth() * TILE_SIZE, map.getHeight() * TILE_SIZE));
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Mezők kirajzolása
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                Color color = tileColors.getOrDefault(tile.getType(), Color.DARK_GRAY);
                g2.setColor(color);
                g2.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                g2.setColor(Color.BLACK);
                g2.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Városok neve
        g2.setFont(new Font("Arial", Font.BOLD, 9));
        for (City city : map.getCities()) {
            int px = city.getOriginX() * TILE_SIZE + 2;
            int py = city.getOriginY() * TILE_SIZE + 11;
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(city.getName(), px + 1, py + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(city.getName(), px, py);
        }

        // Ipari létesítmények neve és típusa
        g2.setFont(new Font("Arial", Font.PLAIN, 8));
        for (Industry ind : map.getIndustries()) {
            // Típus színe
            Color indColor = industryColors.getOrDefault(ind.getIndustryType(), Color.ORANGE);
            int px = ind.getOriginX() * TILE_SIZE + 2;
            int py = ind.getOriginY() * TILE_SIZE + 11;
            g2.setColor(Color.BLACK);
            g2.drawString(ind.getName(), px + 1, py + 1);
            g2.setColor(indColor.brighter());
            g2.drawString(ind.getName(), px, py);
        }
    }

    public void setMap(game.map.Map map) {
        this.map = map;
        repaint();
    }
}
