package game.ui;

import game.map.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;

public class MapRenderer extends JPanel {

    private static final int TILE_SIZE = 32;

    private game.map.Map map;
    private final java.util.Map<TileType, Color> tileColors = new HashMap<>();
    private final java.util.Map<IndustryType, Color> industryColors = new HashMap<>();
    private Camera camera;

    public MapRenderer(game.map.Map map) {
        this.map = map;

        tileColors.put(TileType.GRASS,    new Color(100, 180, 80));
        tileColors.put(TileType.CITY,     new Color(180, 180, 180));
        tileColors.put(TileType.INDUSTRY, new Color(200, 140, 60));
        tileColors.put(TileType.ROAD,     new Color(80, 80, 80));

        industryColors.put(IndustryType.FARM,    new Color(160, 210, 80));
        industryColors.put(IndustryType.FACTORY, new Color(180, 100, 100));

        setBackground(Color.BLACK);

        // Kamera inicializálása
        int worldWidth = map.getWidth() * TILE_SIZE;
        int worldHeight = map.getHeight() * TILE_SIZE;
        camera = new Camera(800, 600, worldWidth, worldHeight);

        // Viewport méret frissítése amikor a panel átméretezett
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                camera.setViewportSize(getWidth(), getHeight());
                repaint();
            }
        });
    }

    public Camera getCamera() {
        return camera;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Rendering quality beállítások
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        double zoom = camera.getZoom();
        double cameraX = camera.getX();
        double cameraY = camera.getY();

        // Zoom alkalmazása
        g2.scale(zoom, zoom);

        // Kamera eltolás alkalmazása (zoom utáni koordináta rendszerben)
        double scaledCameraX = cameraX / zoom;
        double scaledCameraY = cameraY / zoom;
        g2.translate(-scaledCameraX, -scaledCameraY);

        // Csak a látható területet rajzoljuk ki
        int startX = Math.max(0, (int) (scaledCameraX / TILE_SIZE) - 1);
        int startY = Math.max(0, (int) (scaledCameraY / TILE_SIZE) - 1);
        int endX = Math.min(map.getWidth(), (int) ((scaledCameraX + getWidth() / zoom) / TILE_SIZE) + 2);
        int endY = Math.min(map.getHeight(), (int) ((scaledCameraY + getHeight() / zoom) / TILE_SIZE) + 2);

        // Mezők kirajzolása
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;

                Color color = tileColors.getOrDefault(tile.getType(), Color.DARK_GRAY);
                g2.setColor(color);
                g2.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                g2.setColor(Color.BLACK);
                g2.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Városok neve
        int cityFontSize = Math.max(8, (int) (9 * zoom));
        g2.setFont(new Font("Arial", Font.BOLD, cityFontSize));
        for (City city : map.getCities()) {
            int px = city.getOriginX() * TILE_SIZE + 2;
            int py = city.getOriginY() * TILE_SIZE + 11;
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(city.getName(), px + 1, py + 1);
            g2.setColor(Color.WHITE);
            g2.drawString(city.getName(), px, py);
        }

        // Ipari létesítmények neve és típusa
        int indFontSize = Math.max(7, (int) (8 * zoom));
        g2.setFont(new Font("Arial", Font.PLAIN, indFontSize));
        for (Industry ind : map.getIndustries()) {
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
