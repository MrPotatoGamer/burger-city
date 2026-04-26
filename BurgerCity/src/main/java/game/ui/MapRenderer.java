package game.ui;

import game.map.*;
import game.vehicle.AdvancedBus;
import game.vehicle.AdvancedTruck;
import game.vehicle.Bus;
import game.vehicle.Truck;
import game.vehicle.Vehicle;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;

public class MapRenderer extends JPanel {

    private static final int TILE_SIZE = 32;

    private game.map.Map map;
    private final java.util.Map<TileType, Color> tileColors = new HashMap<>();
    private final java.util.Map<IndustryType, Color> industryColors = new HashMap<>();
    private Camera camera;
    private List<Vehicle> vehicles = List.of();
    private List<game.building.TrafficLight> trafficLights = List.of();
    private BufferedImage grassTexture;
    private BufferedImage grassLayerCache;
    private TexturePaint grassBackgroundPaint;
    private BufferedImage cityTexture;
    private BufferedImage truckTexture;
    private BufferedImage advancedTruckTexture;
    private BufferedImage busTexture;
    private BufferedImage advancedBusTexture;
    private BufferedImage wheatTexture;
    private BufferedImage bakeryTexture;
    private BufferedImage burgerFactoryTexture;
    private BufferedImage ranchTexture;
    private BufferedImage pattyPlantTexture;
    private BufferedImage treeTexture;

    private static BufferedImage loadImageResource(String resourcePath) throws IOException {
        try (InputStream inputStream = MapRenderer.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found on classpath: " + resourcePath);
            }

            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("Unsupported or unreadable image: " + resourcePath);
            }
            return image;
        }
    }

    public MapRenderer(game.map.Map map) {
        this.map = map;

        tileColors.put(TileType.GRASS,    new Color(100, 180, 80));
        tileColors.put(TileType.FOREST,   new Color(60, 140, 70));
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

        // Fű textúra betöltése és előre skálázása
        try {
            BufferedImage originalGrass = loadImageResource("/game/assets/grass.png");
            // Előre skálázzuk a textúrát TILE_SIZE-ra
            grassTexture = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = grassTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalGrass, 0, 0, TILE_SIZE, TILE_SIZE, null);
            g.dispose();

            grassBackgroundPaint = new TexturePaint(grassTexture, new Rectangle(0, 0, TILE_SIZE, TILE_SIZE));

            // Cache: előre rendereljük az összes fű tile-t egy nagy képbe
            buildGrassLayerCache();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a fű textúrát: " + e.getMessage());
            grassTexture = null;
            grassLayerCache = null;
        }

        // City textúra betöltése és előre skálázása
        try {
            BufferedImage originalCity = loadImageResource("/game/assets/city.png");
            // Előre skálázzuk TILE_SIZE-ra
            cityTexture = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = cityTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalCity, 0, 0, TILE_SIZE, TILE_SIZE, null);
            g.dispose();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a város textúrát: " + e.getMessage());
            cityTexture = null;
        }

        // Truck textúra betöltése és előre skálázása
        try {
            BufferedImage originalTruck = loadImageResource("/game/assets/truck.png");
            int vehicleSize = TILE_SIZE;
            truckTexture = new BufferedImage(vehicleSize, vehicleSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = truckTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalTruck, 0, 0, vehicleSize, vehicleSize, null);
            g.dispose();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a truck textúrát: " + e.getMessage());
            truckTexture = null;
        }
        try {
            BufferedImage advanced_originalTruck = loadImageResource("/game/assets/advanced_truck.png");
            int vehicleSize = TILE_SIZE;
            advancedTruckTexture = new BufferedImage(vehicleSize, vehicleSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = advancedTruckTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(advanced_originalTruck, 0, 0, vehicleSize, vehicleSize, null);
            g.dispose();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni az advanced truck textúrát: " + e.getMessage());
            truckTexture = null;
        }

        // Bus textúra betöltése és előre skálázása
        try {
            BufferedImage originalBus = loadImageResource("/game/assets/bus.png");
            int vehicleSize = TILE_SIZE;
            busTexture = new BufferedImage(vehicleSize, vehicleSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = busTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalBus, 0, 0, vehicleSize, vehicleSize, null);
            g.dispose();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a bus textúrát: " + e.getMessage());
            busTexture = null;
        }

        try {
            BufferedImage advanced_originalBus = loadImageResource("/game/assets/advanced_bus.png");
            int vehicleSize = TILE_SIZE;
            advancedBusTexture = new BufferedImage(vehicleSize, vehicleSize, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = advancedBusTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(advanced_originalBus, 0, 0, vehicleSize, vehicleSize, null);
            g.dispose();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni az advanced bus textúrát: " + e.getMessage());
            busTexture = null;
        }

        // Wheat textúra betöltése és előre skálázása
        try {
            BufferedImage originalWheat = loadImageResource("/game/assets/wheat.png");
            wheatTexture = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = wheatTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalWheat, 0, 0, TILE_SIZE, TILE_SIZE, null);
            g.dispose();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a wheat textúrát: " + e.getMessage());
            wheatTexture = null;
        }

        // Bakery textúra betöltése
        try {
            bakeryTexture = loadImageResource("/game/assets/bakery.png");
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a bakery textúrát: " + e.getMessage());
            bakeryTexture = null;
        }

        // Burger Factory textúra betöltése
        try {
            burgerFactoryTexture = loadImageResource("/game/assets/burger_factory.png");
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a burger_factory textúrát: " + e.getMessage());
            burgerFactoryTexture = null;
        }

        // Ranch textúra betöltése
        try {
            ranchTexture = loadImageResource("/game/assets/ranch.jpg");
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a ranch textúrát: " + e.getMessage());
            ranchTexture = null;
        }

        // Patty Plant textúra betöltése
        try {
            pattyPlantTexture = loadImageResource("/game/assets/patty_plant.png");
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a patty_plant textúrát: " + e.getMessage());
            pattyPlantTexture = null;
        }

        // Tree textúra betöltése (forest overlay)
        try {
            BufferedImage originalTree = loadImageResource("/game/assets/tree1.png");
            treeTexture = new BufferedImage(TILE_SIZE, TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = treeTexture.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalTree, 0, 0, TILE_SIZE, TILE_SIZE, null);
            g.dispose();
        } catch (IOException e) {
            System.err.println("Nem sikerült betölteni a tree textúrát: " + e.getMessage());
            treeTexture = null;
        }

        setBackground(tileColors.getOrDefault(TileType.GRASS, Color.DARK_GRAY));

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

    public void setTrafficLights(List<game.building.TrafficLight> trafficLights) {
        this.trafficLights = (trafficLights == null) ? List.of() : trafficLights;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Rendering beállítások - teljesítmény optimalizálva
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

        double zoom = camera.getZoom();
        double cameraX = camera.getX();
        double cameraY = camera.getY();

        // Zoom alkalmazása
        g2.scale(zoom, zoom);

        // Kamera eltolás alkalmazása (zoom utáni koordináta rendszerben)
        double scaledCameraX = cameraX / zoom;
        double scaledCameraY = cameraY / zoom;
        g2.translate(-scaledCameraX, -scaledCameraY);

        // Fill the visible world area with a grass pattern.
        // This is intentionally done AFTER applying the camera transform so the texture scrolls with the map.
        int viewWorldW = (int) Math.ceil(getWidth() / zoom) + TILE_SIZE * 2;
        int viewWorldH = (int) Math.ceil(getHeight() / zoom) + TILE_SIZE * 2;
        int fillX = (int) Math.floor(scaledCameraX) - TILE_SIZE;
        int fillY = (int) Math.floor(scaledCameraY) - TILE_SIZE;

        if (grassBackgroundPaint != null) {
            Paint oldPaint = g2.getPaint();
            g2.setPaint(grassBackgroundPaint);
            g2.fillRect(fillX, fillY, viewWorldW, viewWorldH);
            g2.setPaint(oldPaint);
        } else {
            g2.setColor(tileColors.getOrDefault(TileType.GRASS, getBackground()));
            g2.fillRect(fillX, fillY, viewWorldW, viewWorldH);
        }

        // Csak a látható területet rajzoljuk ki
        int startX = Math.max(0, (int) (scaledCameraX / TILE_SIZE) - 1);
        int startY = Math.max(0, (int) (scaledCameraY / TILE_SIZE) - 1);
        int endX = Math.min(map.getWidth(), (int) ((scaledCameraX + getWidth() / zoom) / TILE_SIZE) + 2);
        int endY = Math.min(map.getHeight(), (int) ((scaledCameraY + getHeight() / zoom) / TILE_SIZE) + 2);

        // Fű réteg cache rajzolása (1 nagy kép az összes fű helyett)
        if (grassLayerCache != null) {
            g2.drawImage(grassLayerCache, 0, 0, null);
        }

        // Mezők kirajzolása
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;

                if (tile.getType() == TileType.ROAD) {
                    // Speciális útrajzolás
                    drawRoad(g2, x, y);
                } else if (tile.getType() == TileType.FOREST) {
                    // Forest: grass base + tree overlay
                    int px = x * TILE_SIZE;
                    int py = y * TILE_SIZE;

                    if (grassTexture != null) {
                        g2.drawImage(grassTexture, px, py, null);
                    } else {
                        g2.setColor(tileColors.getOrDefault(TileType.GRASS, getBackground()));
                        g2.fillRect(px, py, TILE_SIZE, TILE_SIZE);
                    }

                    if (treeTexture != null) {
                        int n = Math.max(1, Math.min(4, tile.getForestTrees()));
                        int tw = TILE_SIZE / 2;
                        int th = TILE_SIZE / 2;

                        // Deterministic placement: up to 4 trees in quadrants.
                        if (n >= 1) g2.drawImage(treeTexture, px, py, tw, th, null);
                        if (n >= 2) g2.drawImage(treeTexture, px + tw, py, tw, th, null);
                        if (n >= 3) g2.drawImage(treeTexture, px, py + th, tw, th, null);
                        if (n >= 4) g2.drawImage(treeTexture, px + tw, py + th, tw, th, null);
                    }
                } else if (tile.getType() == TileType.GRASS) {
                    // Fű tile-ok át lesznek ugorva, mert egy cache réteget rajzolunk
                } else if (tile.getType() == TileType.CITY) {
                    // City tile-ok külön renderelve lesznek (az egész város egyben)
                } else if (tile.getType() == TileType.INDUSTRY) {
                    // Industry tile-ok külön renderelve lesznek
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

        // Városok renderelése (textúra + név egyben)
        int cityFontSize = Math.max(8, (int) (9 * zoom));
        g2.setFont(new Font("Arial", Font.BOLD, cityFontSize));
        for (City city : map.getCities()) {
            drawCity(g2, city);
        }

        // Ipari létesítmények renderelése
        int indFontSize = Math.max(7, (int) (8 * zoom));
        g2.setFont(new Font("Arial", Font.PLAIN, indFontSize));
        for (Industry ind : map.getIndustries()) {
            drawIndustry(g2, ind);
        }

        // Traffic lights kirajzolása
        for (game.building.TrafficLight light : trafficLights) {
            if (light == null) continue;
            drawTrafficLight(g2, light);
        }

        // Járművek kirajzolása
        int vehicleSize = TILE_SIZE / 2;
        for (Vehicle v : vehicles) {
            if (v == null) continue;
            int vx = (int) Math.round(v.getWorldX() - vehicleSize / 2.0);
            int vy = (int) Math.round(v.getWorldY() - vehicleSize / 2.0);

            BufferedImage vehicleImage = null;
            Color fallbackColor = Color.WHITE;

            if (v instanceof Truck) {
                vehicleImage = truckTexture;
                fallbackColor = Color.RED;
            } else if (v instanceof Bus) {
                vehicleImage = busTexture;
                fallbackColor = Color.YELLOW;
            } else if (v instanceof AdvancedBus) {
                vehicleImage = advancedBusTexture;
                fallbackColor = Color.RED;
            } else if (v instanceof AdvancedTruck) {
                vehicleImage = advancedTruckTexture;
                fallbackColor = Color.GREEN;
            }

            if (vehicleImage != null) {
                // Textúra rajzolása
                g2.drawImage(vehicleImage, vx, vy, null);
            } else {
                // Fallback: színes kör
                g2.setColor(Color.BLACK);
                g2.fillOval(vx + 1, vy + 1, vehicleSize, vehicleSize);
                g2.setColor(fallbackColor);
                g2.fillOval(vx, vy, vehicleSize, vehicleSize);
            }
        }
    }

    private void drawTrafficLight(Graphics2D g2, game.building.TrafficLight light) {
        int x = light.getX();
        int y = light.getY();
        int px = x * TILE_SIZE;
        int py = y * TILE_SIZE;

        String state = light.getCurrentState();

        // Draw small traffic light circles at corners
        int circleSize = 8;
        int offset = 4;

        // North-South lights (main direction)
        if (state.equals("MAIN_GREEN")) {
            // North light (top)
            g2.setColor(Color.GREEN);
            g2.fillOval(px + TILE_SIZE/2 - circleSize/2, py + offset, circleSize, circleSize);
            // South light (bottom)
            g2.fillOval(px + TILE_SIZE/2 - circleSize/2, py + TILE_SIZE - offset - circleSize, circleSize, circleSize);
        } else {
            g2.setColor(Color.RED);
            g2.fillOval(px + TILE_SIZE/2 - circleSize/2, py + offset, circleSize, circleSize);
            g2.fillOval(px + TILE_SIZE/2 - circleSize/2, py + TILE_SIZE - offset - circleSize, circleSize, circleSize);
        }

        // East-West lights (cross direction)
        if (state.equals("CROSS_GREEN")) {
            g2.setColor(Color.GREEN);
            // East light (right)
            g2.fillOval(px + TILE_SIZE - offset - circleSize, py + TILE_SIZE/2 - circleSize/2, circleSize, circleSize);
            // West light (left)
            g2.fillOval(px + offset, py + TILE_SIZE/2 - circleSize/2, circleSize, circleSize);
        } else {
            g2.setColor(Color.RED);
            g2.fillOval(px + TILE_SIZE - offset - circleSize, py + TILE_SIZE/2 - circleSize/2, circleSize, circleSize);
            g2.fillOval(px + offset, py + TILE_SIZE/2 - circleSize/2, circleSize, circleSize);
        }

        // Draw black outline for better visibility
        g2.setColor(Color.BLACK);
        g2.drawOval(px + TILE_SIZE/2 - circleSize/2, py + offset, circleSize, circleSize);
        g2.drawOval(px + TILE_SIZE/2 - circleSize/2, py + TILE_SIZE - offset - circleSize, circleSize, circleSize);
        g2.drawOval(px + TILE_SIZE - offset - circleSize, py + TILE_SIZE/2 - circleSize/2, circleSize, circleSize);
        g2.drawOval(px + offset, py + TILE_SIZE/2 - circleSize/2, circleSize, circleSize);
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
        int connectionCount = (north ? 1 : 0) + (south ? 1 : 0) + (east ? 1 : 0) + (west ? 1 : 0);
        
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
        } else if (connectionCount == 2) {
            // Kanyar (pontosan 2 kapcsolódás, nem szemben)
            drawCorner(g2, px, py, margin, roadWidth, north, south, east, west);
        } else {
            // Kereszteződés vagy T-elágazás (3+ kapcsolódás)
            g2.fillRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            
            if (connectionCount >= 3) {
                // Kereszteződés vagy T-elágazás jelölése
                g2.setColor(new Color(200, 200, 200));
                int centerSize = 4;
                g2.fillRect(px + TILE_SIZE / 2 - centerSize / 2, 
                           py + TILE_SIZE / 2 - centerSize / 2, 
                           centerSize, centerSize);
            }
        }
    }

    private void drawCorner(Graphics2D g2, int px, int py, int margin, int roadWidth, 
                           boolean north, boolean south, boolean east, boolean west) {
        // Kanyar rajzolása - L alakú út két szakaszból
        g2.setColor(new Color(80, 80, 80));
        
        int centerX = px + margin + roadWidth / 2;
        int centerY = py + margin + roadWidth / 2;
        
        if (north && east) {
            // Észak-Kelet kanyar (┗ alakú)
            g2.fillRect(px + margin, py, roadWidth, TILE_SIZE / 2 + margin);  // Függőleges rész
            g2.fillRect(px + margin, py + margin, TILE_SIZE - margin, roadWidth);  // Vízszintes rész
            
            // Középvonalak
            drawDashedLine(g2, centerX, py, centerX, centerY, new Color(220, 220, 220), 1);  // Függőleges
            drawDashedLine(g2, centerX, centerY, px + TILE_SIZE, centerY, new Color(220, 220, 220), 1);  // Vízszintes
        } else if (north && west) {
            // Észak-Nyugat kanyar (┛ alakú)
            g2.fillRect(px + margin, py, roadWidth, TILE_SIZE / 2 + margin);  // Függőleges rész
            g2.fillRect(px, py + margin, TILE_SIZE - margin, roadWidth);  // Vízszintes rész
            
            // Középvonalak
            drawDashedLine(g2, centerX, py, centerX, centerY, new Color(220, 220, 220), 1);  // Függőleges
            drawDashedLine(g2, px, centerY, centerX, centerY, new Color(220, 220, 220), 1);  // Vízszintes
        } else if (south && east) {
            // Dél-Kelet kanyar (┏ alakú)
            g2.fillRect(px + margin, py + margin, roadWidth, TILE_SIZE - margin);  // Függőleges rész
            g2.fillRect(px + margin, py + margin, TILE_SIZE - margin, roadWidth);  // Vízszintes rész
            
            // Középvonalak
            drawDashedLine(g2, centerX, centerY, centerX, py + TILE_SIZE, new Color(220, 220, 220), 1);  // Függőleges
            drawDashedLine(g2, centerX, centerY, px + TILE_SIZE, centerY, new Color(220, 220, 220), 1);  // Vízszintes
        } else if (south && west) {
            // Dél-Nyugat kanyar (┓ alakú)
            g2.fillRect(px + margin, py + margin, roadWidth, TILE_SIZE - margin);  // Függőleges rész
            g2.fillRect(px, py + margin, TILE_SIZE - margin, roadWidth);  // Vízszintes rész
            
            // Középvonalak
            drawDashedLine(g2, centerX, centerY, centerX, py + TILE_SIZE, new Color(220, 220, 220), 1);  // Függőleges
            drawDashedLine(g2, px, centerY, centerX, centerY, new Color(220, 220, 220), 1);  // Vízszintes
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

    private void buildGrassLayerCache() {
        if (grassTexture == null) {
            grassLayerCache = null;
            return;
        }

        int width = map.getWidth() * TILE_SIZE;
        int height = map.getHeight() * TILE_SIZE;

        grassLayerCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = grassLayerCache.createGraphics();

        // Gyors rendering beállítások
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        // Rendereljük az összes fű tile-t egyszer. A nem-fű részeket átlátszóra hagyjuk,
        // hogy a háttér (grassBackgroundPaint) látszódjon.
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && tile.getType() == TileType.GRASS) {
                    g.drawImage(grassTexture, x * TILE_SIZE, y * TILE_SIZE, null);
                }
            }
        }

        g.dispose();
    }

    public void rebuildGrassLayerCache() {
        buildGrassLayerCache();
    }

    private void drawCity(Graphics2D g2, City city) {
        int px = city.getOriginX() * TILE_SIZE;
        int py = city.getOriginY() * TILE_SIZE;
        int width = city.getWidth() * TILE_SIZE;
        int height = city.getHeight() * TILE_SIZE;

        if (cityTexture != null) {
            // City textúra skálázva az egész város méretére
            g2.drawImage(cityTexture, px, py, width, height, null);

            // Átmeneti szegély hogy jobban elkülönüljön
            g2.setColor(new Color(50, 50, 50, 120));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRect(px, py, width - 1, height - 1);
            g2.setStroke(new BasicStroke(1.0f));
        } else {
            // Fallback: szürke terület
            Color color = tileColors.get(TileType.CITY);
            g2.setColor(color);
            g2.fillRect(px, py, width, height);
            g2.setColor(Color.BLACK);
            g2.drawRect(px, py, width - 1, height - 1);
        }

        // Város neve
        int nameX = px + 2;
        int nameY = py + 11;
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(city.getName(), nameX + 1, nameY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(city.getName(), nameX, nameY);
    }

    private void drawIndustry(Graphics2D g2, Industry ind) {
        int px = ind.getOriginX() * TILE_SIZE;
        int py = ind.getOriginY() * TILE_SIZE;
        int width = 2;  // Industries are 2x2 tiles
        int height = 2;
        int totalWidth = width * TILE_SIZE;
        int totalHeight = height * TILE_SIZE;

        BufferedImage industryTexture = null;

        // Textúra kiválasztása az industry típusa alapján
        switch (ind.getIndustryType()) {
            case FARM:
                // Farm esetén wheat textúrát használunk tile-onként
                if (wheatTexture != null) {
                    for (int dx = 0; dx < width; dx++) {
                        for (int dy = 0; dy < height; dy++) {
                            int tileX = px + dx * TILE_SIZE;
                            int tileY = py + dy * TILE_SIZE;
                            g2.drawImage(wheatTexture, tileX, tileY, null);
                        }
                    }
                    // Szegély az egész farm körül
                    g2.setColor(new Color(100, 80, 50, 100));
                    g2.setStroke(new BasicStroke(2.0f));
                    g2.drawRect(px, py, totalWidth - 1, totalHeight - 1);
                    g2.setStroke(new BasicStroke(1.0f));
                }
                break;
            case BAKERY:
                industryTexture = bakeryTexture;
                break;
            case BURGER_FACTORY:
                industryTexture = burgerFactoryTexture;
                break;
            case RANCH:
                industryTexture = ranchTexture;
                break;
            case PATTY_PLANT:
                industryTexture = pattyPlantTexture;
                break;
            default:
                industryTexture = null;
                break;
        }

        // Ha van textúra és nem Farm típus, rajzoljuk meg az egész területre
        if (industryTexture != null && ind.getIndustryType() != IndustryType.FARM) {
            g2.drawImage(industryTexture, px, py, totalWidth, totalHeight, null);

            // Szegély az egész industry körül
            g2.setColor(new Color(50, 50, 50, 120));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawRect(px, py, totalWidth - 1, totalHeight - 1);
            g2.setStroke(new BasicStroke(1.0f));
        } else if (ind.getIndustryType() != IndustryType.FARM) {
            // Fallback: színes terület ha nincs textúra
            Color indColor = industryColors.getOrDefault(ind.getIndustryType(), Color.ORANGE);
            g2.setColor(indColor);
            g2.fillRect(px, py, totalWidth, totalHeight);
            g2.setColor(Color.BLACK);
            g2.drawRect(px, py, totalWidth - 1, totalHeight - 1);
        }

        // Industry neve
        int nameX = px + 2;
        int nameY = py + 11;
        Color indColor = industryColors.getOrDefault(ind.getIndustryType(), Color.ORANGE);
        g2.setColor(Color.BLACK);
        g2.drawString(ind.getName(), nameX + 1, nameY + 1);
        g2.setColor(indColor.brighter());
        g2.drawString(ind.getName(), nameX, nameY);
    }
}
