package game.ui;

import game.building.Road;
import game.core.Player;
import game.map.Map;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameUI extends JFrame {

    private MapRenderer mapRenderer;
    private Map map;
    private Player player;
    private JLabel statusBar;
    private boolean roadBuildMode = false;
    private JButton buildRoadButton;

    public GameUI() {
        setTitle("Mini Transport Tycoon - BurgerCity");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Játékos létrehozása kezdőpénzzel
        player = new Player(10000);

        // Térkép létrehozása és betöltése
        map = new Map(50, 40);
        map.loadPredefined();

        mapRenderer = new MapRenderer(map);

        // Egér kezelése: drag (görgetés) és kattintás
        final Point[] dragStart = {null};

        mapRenderer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart[0] = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart[0] = null;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (roadBuildMode && dragStart[0] != null &&
                    Math.abs(dragStart[0].x - e.getX()) < 3 &&
                    Math.abs(dragStart[0].y - e.getY()) < 3) {
                    handleRoadBuild(e.getX(), e.getY());
                }
            }
        });

        mapRenderer.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart[0] != null) {
                    int dx = dragStart[0].x - e.getX();
                    int dy = dragStart[0].y - e.getY();
                    mapRenderer.getCamera().move(dx, dy);
                    dragStart[0] = e.getPoint();
                    mapRenderer.repaint();
                }
            }
        });

        // Egérgörgő zoom
        mapRenderer.addMouseWheelListener(e -> {
            double zoomFactor = e.getWheelRotation() < 0 ? 1.1 : 0.9;
            mapRenderer.getCamera().zoom(zoomFactor, e.getX(), e.getY());
            mapRenderer.repaint();
        });

        add(mapRenderer, BorderLayout.CENTER);

        // Felső panel gombokkal
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        buildRoadButton = new JButton("Út építés (100$)");
        buildRoadButton.addActionListener(e -> toggleRoadBuildMode());
        topPanel.add(buildRoadButton);

        add(topPanel, BorderLayout.NORTH);

        // Állapotsáv
        statusBar = new JLabel(" Mini Transport Tycoon | BurgerCity | Pénz: " + player.getMoney() + "$");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private void toggleRoadBuildMode() {
        roadBuildMode = !roadBuildMode;
        if (roadBuildMode) {
            buildRoadButton.setBackground(Color.GREEN);
            buildRoadButton.setText("Út építés BE (100$)");
            updateStatus("Kattints a térképre az út építéséhez!");
        } else {
            buildRoadButton.setBackground(null);
            buildRoadButton.setText("Út építés (100$)");
            updateStatus("Út építés mód kikapcsolva.");
        }
    }

    private void handleRoadBuild(int screenX, int screenY) {
        int tileSize = 32; // MapRenderer.TILE_SIZE
        // Kamera segítségével képernyő koordinátából világ koordinátába
        Camera camera = mapRenderer.getCamera();
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);

        // Ellenőrzés: van-e elég pénz
        if (!player.spendMoney(Road.COST)) {
            updateStatus("Nincs elég pénz az út építéséhez! Szükséges: " + Road.COST + "$");
            return;
        }

        // Út építése
        if (map.buildRoad(tileX, tileY)) {
            mapRenderer.repaint();
            updateStatus("Út sikeresen megépítve (" + tileX + ", " + tileY + "). Pénz: " + player.getMoney() + "$");
        } else {
            // Ha nem sikerült, visszaadjuk a pénzt
            player.addMoney(Road.COST);
            updateStatus("Erre a mezőre nem építhető út!");
        }
    }

    private void updateStatus(String message) {
        statusBar.setText(" " + message + " | Pénz: " + player.getMoney() + "$");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameUI ui = new GameUI();
            ui.setVisible(true);
        });
    }
}