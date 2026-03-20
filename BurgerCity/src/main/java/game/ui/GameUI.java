package game.ui;

import game.building.Road;
import game.core.Player;
import game.map.City;
import game.map.Industry;
import game.map.Map;
import game.vehicle.Bus;
import game.vehicle.Truck;
import game.vehicle.Vehicle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class GameUI extends JFrame {

    private static final int VEHICLE_COST = 500;

    private MapRenderer mapRenderer;
    private Map map;
    private Player player;
    private JLabel statusBar;
    private String lastStatusMessage;
    private boolean roadBuildMode = false;
    private JButton buildRoadButton;
    private boolean buyVehicleMode = false;
    private JButton buyVehicleButton;

    private final List<Vehicle> vehicles = new ArrayList<>();
    private long lastTickNanos;
    private Timer gameTimer;

    private GameDashboard dashboard;
    private int dashboardRefreshCounter = 0;

    private SelectedBuilding startBuilding;
    private SelectedBuilding endBuilding;

    private record SelectedBuilding(String name, int originX, int originY, int width, int height) {}

    private static final int INITIAL_WINDOW_WIDTH = 1000;
    private static final int INITIAL_WINDOW_HEIGHT = 720;

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
        mapRenderer.setPreferredSize(new Dimension(INITIAL_WINDOW_WIDTH, INITIAL_WINDOW_HEIGHT - 120));
        mapRenderer.setVehicles(vehicles);

        // Egér kezelése: drag (görgetés) és kattintás
        final Point[] dragStart = {null};
        final boolean[] wasDragged = {false};

        mapRenderer.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart[0] = e.getPoint();
                wasDragged[0] = false;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Ha nem volt drag, akkor kattintás volt
                if (roadBuildMode && !wasDragged[0]) {
                    handleRoadBuild(e.getX(), e.getY());
                } else if (buyVehicleMode && !wasDragged[0]) {
                    handleBuyVehicleClick(e.getX(), e.getY());
                }
                dragStart[0] = null;
                wasDragged[0] = false;
            }
        });

        mapRenderer.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart[0] != null) {
                    int dx = dragStart[0].x - e.getX();
                    int dy = dragStart[0].y - e.getY();

                    // Ha elég nagyot mozgott, az drag
                    if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
                        wasDragged[0] = true;
                    }

                    mapRenderer.getCamera().move(dx, dy);
                    dragStart[0] = e.getPoint();
                    mapRenderer.repaint();
                }
            }
        });

        // Egérgörgő zoom
        mapRenderer.addMouseWheelListener(e -> {
            double zoomFactor = Math.pow(1.1, -e.getWheelRotation());

            var camera = mapRenderer.getCamera();
            double newZoom = camera.getZoom() * zoomFactor;

            if (newZoom < 0.1 || newZoom > 10) return;

            camera.zoom(zoomFactor, e.getX(), e.getY());
            mapRenderer.repaint();
        });

        add(mapRenderer, BorderLayout.CENTER);

        // Dashboard panel (right side)
        dashboard = new GameDashboard(player, map, vehicles);
        add(dashboard, BorderLayout.EAST);

        // Felső panel gombokkal
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        buildRoadButton = new JButton("Út építés (100$)");
        buildRoadButton.addActionListener(e -> toggleRoadBuildMode());
        topPanel.add(buildRoadButton);

        buyVehicleButton = new JButton("Jármű vásárlás");
        buyVehicleButton.addActionListener(e -> toggleBuyVehicleMode());
        topPanel.add(buyVehicleButton);

        add(topPanel, BorderLayout.NORTH);

        // Állapotsáv
        lastStatusMessage = "Mini Transport Tycoon | BurgerCity";
        statusBar = new JLabel(" " + lastStatusMessage + " | Pénz: " + player.getMoney() + "$");
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        add(statusBar, BorderLayout.SOUTH);

        // Egyszerű játéktick: járművek mozgatása és újrarajzolás
        lastTickNanos = System.nanoTime();
        gameTimer = new Timer(16, e -> tick());
        gameTimer.start();

        pack();
        setSize(INITIAL_WINDOW_WIDTH + 310, INITIAL_WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private void toggleRoadBuildMode() {
        roadBuildMode = !roadBuildMode;
        if (roadBuildMode) {
            // Módok kizárják egymást
            if (buyVehicleMode) {
                buyVehicleMode = false;
                buyVehicleButton.setBackground(null);
                buyVehicleButton.setText("Jármű vásárlás");
                clearVehicleSelection();
            }
            buildRoadButton.setBackground(Color.GREEN);
            buildRoadButton.setText("Út építés BE (100$)");
            updateStatus("Kattints a térképre az út építéséhez!");
        } else {
            buildRoadButton.setBackground(null);
            buildRoadButton.setText("Út építés (100$)");
            updateStatus("Út építés mód kikapcsolva.");
        }
    }

    private void toggleBuyVehicleMode() {
        buyVehicleMode = !buyVehicleMode;
        if (buyVehicleMode) {
            // Módok kizárják egymást
            if (roadBuildMode) {
                roadBuildMode = false;
                buildRoadButton.setBackground(null);
                buildRoadButton.setText("Út építés (100$)");
            }
            buyVehicleButton.setBackground(Color.GREEN);
            buyVehicleButton.setText("Jármű vásárlás BE (" + VEHICLE_COST + "$)");
            clearVehicleSelection();
            updateStatus("Válassz 2 buildinget (város/ipar) kattintással, majd Bus/Truck.");
        } else {
            buyVehicleButton.setBackground(null);
            buyVehicleButton.setText("Jármű vásárlás");
            clearVehicleSelection();
            updateStatus("Jármű vásárlás mód kikapcsolva.");
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

    private void handleBuyVehicleClick(int screenX, int screenY) {
        int tileSize = 32; // MapRenderer.TILE_SIZE
        Camera camera = mapRenderer.getCamera();
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);

        SelectedBuilding clicked = findBuildingAt(tileX, tileY);
        if (clicked == null) {
            updateStatus("Kattints egy városra vagy iparra (building)!");
            return;
        }

        if (startBuilding == null) {
            startBuilding = clicked;
            updateStatus("Kezdő building kiválasztva: " + startBuilding.name() + ". Válassz cél buildinget.");
            return;
        }

        if (endBuilding == null) {
            endBuilding = clicked;
            if (sameBuilding(startBuilding, endBuilding)) {
                updateStatus("A kezdő és cél building nem lehet ugyanaz. Válassz másikat!");
                endBuilding = null;
                return;
            }
            placeVehicleIfPathValid();
            return;
        }

        // Harmadik kattintás: újrakezdjük a kiválasztást
        clearVehicleSelection();
        startBuilding = clicked;
        updateStatus("Kezdő building kiválasztva: " + startBuilding.name() + ". Válassz cél buildinget.");
    }

    private void placeVehicleIfPathValid() {
        if (startBuilding == null || endBuilding == null) return;

        List<int[]> path = map.findRoadPathBetweenAreas(
                startBuilding.originX(), startBuilding.originY(), startBuilding.width(), startBuilding.height(),
                endBuilding.originX(), endBuilding.originY(), endBuilding.width(), endBuilding.height()
        );

        if (path.isEmpty()) {
            updateStatus("Nincs érvényes úthálózat a két building között! Építs összefüggő utat.");
            return;
        }

        if (!player.spendMoney(VEHICLE_COST)) {
            updateStatus("Nincs elég pénz jármű vásárlásához! Szükséges: " + VEHICLE_COST + "$");
            return;
        }

        Object[] options = {"Bus", "Truck"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Válassz jármű típust:",
                "Jármű vásárlás",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice != 0 && choice != 1) {
            player.addMoney(VEHICLE_COST);
            updateStatus("Jármű vásárlás megszakítva.");
            return;
        }

        // Assignment-aligned constraint: bus transports passengers between cities.
        if (choice == 0 && (!isCityBuilding(startBuilding) || !isCityBuilding(endBuilding))) {
            player.addMoney(VEHICLE_COST);
            updateStatus("Bus csak két város között vásárolható (utas szállítás). Válassz 2 várost!");
            clearVehicleSelection();
            return;
        }

        Vehicle v = (choice == 0) ? new Bus() : new Truck();
        v.setPath(path);
        vehicles.add(v);
        mapRenderer.repaint();
        updateStatus("Jármű lehelyezve: " + options[choice] + " | Útvonal: " + startBuilding.name() + " -> " + endBuilding.name());
        clearVehicleSelection();
    }

    private boolean isCityBuilding(SelectedBuilding b) {
        if (b == null) return false;
        for (City c : map.getCities()) {
            if (c == null) continue;
            if (c.getOriginX() == b.originX()
                    && c.getOriginY() == b.originY()
                    && c.getWidth() == b.width()
                    && c.getHeight() == b.height()) {
                return true;
            }
        }
        return false;
    }

    private void clearVehicleSelection() {
        startBuilding = null;
        endBuilding = null;
    }

    private boolean sameBuilding(SelectedBuilding a, SelectedBuilding b) {
        return a != null && b != null
                && a.originX() == b.originX()
                && a.originY() == b.originY()
                && a.width() == b.width()
                && a.height() == b.height();
    }

    private SelectedBuilding findBuildingAt(int x, int y) {
        for (City c : map.getCities()) {
            if (c.occupies(x, y)) {
                return new SelectedBuilding(c.getName(), c.getOriginX(), c.getOriginY(), c.getWidth(), c.getHeight());
            }
        }
        for (Industry i : map.getIndustries()) {
            if (i.occupies(x, y)) {
                return new SelectedBuilding(i.getName(), i.getOriginX(), i.getOriginY(), i.getWidth(), i.getHeight());
            }
        }
        return null;
    }

    private void tick() {
        long now = System.nanoTime();
        double deltaSeconds = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;

        map.updateEconomy(deltaSeconds);

        for (Vehicle v : vehicles) {
            if (v == null) continue;
            v.update(map, deltaSeconds);
            v.processArrivalEconomy(map, player);
        }

        // Refresh dashboard every ~30 frames (~0.5 seconds) to keep it responsive but efficient
        dashboardRefreshCounter++;
        if (dashboardRefreshCounter >= 30) {
            dashboardRefreshCounter = 0;
            dashboard.refresh();
        }

        mapRenderer.repaint();

        // Always reflect current money even without new status messages.
        statusBar.setText(" " + lastStatusMessage + " | Pénz: " + player.getMoney() + "$");
    }

    private void updateStatus(String message) {
        lastStatusMessage = message;
        statusBar.setText(" " + lastStatusMessage + " | Pénz: " + player.getMoney() + "$");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameUI ui = new GameUI();
            ui.setVisible(true);
        });
    }
}