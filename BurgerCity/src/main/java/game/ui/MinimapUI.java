package game.ui;

import game.map.Tile;
import game.map.TileType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Navigable minimap: shows the whole map + current camera viewport.
 * Click/drag moves the main camera.
 */
public class MinimapUI extends JPanel {

    // Keep consistent with MapRenderer's tile size.
    private static final int TILE_SIZE = 32;

    private final game.map.Map map;
    private final Camera camera;
    private final Runnable onNavigate;

    private final Map<TileType, Color> tileColors = new EnumMap<>(TileType.class);

    public MinimapUI(game.map.Map map, Camera camera, Runnable onNavigate) {
        this.map = Objects.requireNonNull(map, "map");
        this.camera = Objects.requireNonNull(camera, "camera");
        this.onNavigate = (onNavigate == null) ? () -> {} : onNavigate;

        // Simple palette similar to MapRenderer.
        tileColors.put(TileType.GRASS, new Color(100, 180, 80));
        tileColors.put(TileType.CITY, new Color(180, 180, 180));
        tileColors.put(TileType.INDUSTRY, new Color(200, 140, 60));
        tileColors.put(TileType.ROAD, new Color(80, 80, 80));
        tileColors.put(TileType.BUILDING, Color.GRAY);

        setPreferredSize(new Dimension(310, 190));
        setMinimumSize(new Dimension(180, 140));
        setOpaque(true);
        setBackground(new Color(25, 25, 25));

        MouseAdapter adapter = new MouseAdapter() {
            private boolean dragging = false;

            @Override
            public void mousePressed(MouseEvent e) {
                dragging = true;
                navigateTo(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) return;
                navigateTo(e.getX(), e.getY());
            }
        };
        addMouseListener(adapter);
        addMouseMotionListener(adapter);
    }

    private void navigateTo(int mouseX, int mouseY) {
        RenderTransform t = computeTransform();
        if (t.scale <= 0) return;

        double worldX = (mouseX - t.offsetX) / t.scale;
        double worldY = (mouseY - t.offsetY) / t.scale;

        worldX = clamp(worldX, 0, t.worldW);
        worldY = clamp(worldY, 0, t.worldH);

        camera.centerOnWorld(worldX, worldY);
        onNavigate.run();
        repaint();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(v, max));
    }

    private record RenderTransform(int offsetX, int offsetY, double scale, int worldW, int worldH) {}

    private RenderTransform computeTransform() {
        int panelW = Math.max(1, getWidth());
        int panelH = Math.max(1, getHeight());

        int worldW = map.getWidth() * TILE_SIZE;
        int worldH = map.getHeight() * TILE_SIZE;

        if (worldW <= 0 || worldH <= 0) {
            return new RenderTransform(0, 0, 1.0, 1, 1);
        }

        double sx = panelW / (double) worldW;
        double sy = panelH / (double) worldH;
        double scale = Math.min(sx, sy);

        int drawW = (int) Math.round(worldW * scale);
        int drawH = (int) Math.round(worldH * scale);
        int offX = (panelW - drawW) / 2;
        int offY = (panelH - drawH) / 2;

        return new RenderTransform(offX, offY, scale, worldW, worldH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

        RenderTransform t = computeTransform();
        int drawW = (int) Math.round(t.worldW * t.scale);
        int drawH = (int) Math.round(t.worldH * t.scale);

        // Map background area
        g2.setColor(new Color(35, 35, 35));
        g2.fillRect(t.offsetX, t.offsetY, drawW, drawH);

        // Draw tiles
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;
                Color c = tileColors.getOrDefault(tile.getType(), Color.DARK_GRAY);
                g2.setColor(c);

                int wx = x * TILE_SIZE;
                int wy = y * TILE_SIZE;

                int px = t.offsetX + (int) Math.floor(wx * t.scale);
                int py = t.offsetY + (int) Math.floor(wy * t.scale);
                int pw = Math.max(1, (int) Math.ceil(TILE_SIZE * t.scale));
                int ph = Math.max(1, (int) Math.ceil(TILE_SIZE * t.scale));

                g2.fillRect(px, py, pw, ph);
            }
        }

        // Viewport rectangle
        double zoom = camera.getZoom();
        double viewLeftWorld = camera.getX() / zoom;
        double viewTopWorld = camera.getY() / zoom;
        double viewWWorld = camera.getViewportWidth() / zoom;
        double viewHWorld = camera.getViewportHeight() / zoom;

        int rx = t.offsetX + (int) Math.round(viewLeftWorld * t.scale);
        int ry = t.offsetY + (int) Math.round(viewTopWorld * t.scale);
        int rw = (int) Math.round(viewWWorld * t.scale);
        int rh = (int) Math.round(viewHWorld * t.scale);

        g2.setColor(new Color(255, 60, 60));
        g2.drawRect(rx, ry, rw, rh);

        // Border
        g2.setColor(Color.BLACK);
        g2.drawRect(t.offsetX, t.offsetY, drawW, drawH);
    }
}