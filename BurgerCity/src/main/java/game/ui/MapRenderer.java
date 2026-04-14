package game.ui;

import game.map.*;
import game.vehicle.Bus;
import game.vehicle.Truck;
import game.vehicle.Vehicle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.List;

public class MapRenderer extends JPanel {

    private static final int TILE_SIZE = 32;

    private game.map.Map map;
    private final java.util.Map<TileType, Color> tileColors = new HashMap<>();
    private final java.util.Map<IndustryType, Color> industryColors = new HashMap<>();
    private Camera camera;
    private List<Vehicle> vehicles = List.of();

    public MapRenderer(game.map.Map map) {
        this.map = map;

        tileColors.put(TileType.GRASS,    new Color(100, 180, 80));
        tileColors.put(TileType.CITY,     new Color(180, 180, 180));
        tileColors.put(TileType.INDUSTRY, new Color(200, 140, 60));
        tileColors.put(TileType.ROAD,     new Color(80, 80, 80));
        tileColors.put(TileType.BUILDING, Color.GRAY);

        industryColors.put(IndustryType.FARM,           new Color(160, 210, 80));
        industryColors.put(IndustryType.RANCH,          new Color(180, 200, 120));
        industryColors.put(IndustryType.BAKERY,         new Color(220, 180, 120));
        industryColors.put(IndustryType.PATTY_PLANT,    new Color(200, 130, 130));
        industryColors.put(IndustryType.BURGER_FACTORY, new Color(180, 100, 100));
        industryColors.put(IndustryType.FACTORY,        new Color(180, 100, 100));

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

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = (vehicles == null) ? List.of() : vehicles;
        repaint();
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

                if (tile.getType() == TileType.ROAD) {
                    // Speciális útrajzolás
                    drawRoad(g2, x, y);
                } else {
                    // Normál mezők rajzolása
                    Color color = tileColors.getOrDefault(tile.getType(), Color.DARK_GRAY);
                    g2.setColor(color);
                    g2.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    g2.setColor(Color.BLACK);
                    g2.drawRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

                    if (tile.getType() == TileType.BUILDING && tile.getPlacedBuilding() != null) {
                        String label = tile.getPlacedBuilding().getName();
                        String letter = (label == null || label.isEmpty()) ? "B" : label.substring(0, 1).toUpperCase();
                        g2.setColor(Color.BLACK);
                        g2.setFont(new Font("Arial", Font.BOLD, 12));
                        g2.drawString(letter, x * TILE_SIZE + 12, y * TILE_SIZE + 20);
                    }
                }
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

        // Járművek kirajzolása
        int vehicleSize = TILE_SIZE / 2;
        for (Vehicle v : vehicles) {
            if (v == null) continue;
            int vx = (int) Math.round(v.getWorldX() - vehicleSize / 2.0);
            int vy = (int) Math.round(v.getWorldY() - vehicleSize / 2.0);

            Color vehicleColor = Color.WHITE;
            if (v instanceof Truck) vehicleColor = Color.RED;
            if (v instanceof Bus) vehicleColor = Color.BLUE;

            g2.setColor(Color.BLACK);
            g2.fillOval(vx + 1, vy + 1, vehicleSize, vehicleSize);
            g2.setColor(vehicleColor);
            g2.fillOval(vx, vy, vehicleSize, vehicleSize);
        }
    }

    public void setMap(game.map.Map map) {
        this.map = map;
        repaint();
    }

    private void drawRoad(Graphics2D g2, int x, int y) {
        // Ellenőrizzük a szomszédos mezőket
        boolean north = isRoadOrBuilding(x, y - 1);
        boolean south = isRoadOrBuilding(x, y + 1);
        boolean east = isRoadOrBuilding(x + 1, y);
        boolean west = isRoadOrBuilding(x - 1, y);

        int px = x * TILE_SIZE;
        int py = y * TILE_SIZE;

        // Alap út háttér (sötét szürke aszfalt)
        g2.setColor(new Color(60, 60, 60));
        g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        // Út szélek (még sötétebb)
        g2.setColor(new Color(40, 40, 40));
        g2.drawRect(px, py, TILE_SIZE - 1, TILE_SIZE - 1);

        // Út fő része (világosabb szürke)
        g2.setColor(new Color(80, 80, 80));
        
        int roadWidth = (int)(TILE_SIZE * 0.7);
        int margin = (TILE_SIZE - roadWidth) / 2;

        // Alapértelmezett: teljes mező
        int roadX = px;
        int roadY = py;
        int roadW = TILE_SIZE;
        int roadH = TILE_SIZE;

        // Határozzuk meg az út irányát és rajzoljuk meg
        if ((north || south) && !(east || west)) {
            // Függőleges út
            roadX = px + margin;
            roadW = roadWidth;
            g2.fillRect(roadX, roadY, roadW, roadH);
            
            // Középvonal (szaggatott fehér)
            drawDashedLine(g2, roadX + roadW / 2, roadY, roadX + roadW / 2, roadY + TILE_SIZE, 
                          new Color(220, 220, 220), 1);
        } else if ((east || west) && !(north || south)) {
            // Vízszintes út
            roadY = py + margin;
            roadH = roadWidth;
            g2.fillRect(roadX, roadY, roadW, roadH);
            
            // Középvonal (szaggatott fehér)
            drawDashedLine(g2, roadX, roadY + roadH / 2, roadX + TILE_SIZE, roadY + roadH / 2, 
                          new Color(220, 220, 220), 1);
        } else {
            // Kereszteződés, sarok vagy komplex forma
            g2.fillRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            
            // Kereszteződés vagy T-elágazás jelölése (kisebb fehér négyzet a közepén)
            int count = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
            if (count >= 3) {
                // Kereszteződés vagy T-elágazás
                g2.setColor(new Color(200, 200, 200));
                int centerSize = 4;
                g2.fillRect(px + TILE_SIZE / 2 - centerSize / 2, 
                           py + TILE_SIZE / 2 - centerSize / 2, 
                           centerSize, centerSize);
            }
        }
    }

    private void drawDashedLine(Graphics2D g2, int x1, int y1, int x2, int y2, Color color, int thickness) {
        g2.setColor(color);
        Stroke oldStroke = g2.getStroke();
        g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 
                                    10.0f, new float[]{4.0f, 4.0f}, 0.0f));
        g2.drawLine(x1, y1, x2, y2);
        g2.setStroke(oldStroke);
    }

    private boolean isRoadOrBuilding(int x, int y) {
        Tile tile = map.getTile(x, y);
        if (tile == null) return false;
        return tile.getType() == TileType.ROAD || tile.getType() == TileType.BUILDING;
    }
}
