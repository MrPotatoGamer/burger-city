package game.ui;

import game.building.Garage;
import game.building.Road;
import game.building.Stop;
import game.building.TrafficLight;
import game.core.Player;
import game.core.TimeManager;
import game.map.City;
import game.map.Industry;
import game.map.IndustryType;
import game.map.Map;
import game.map.Tile;
import game.map.TileType;
import game.save.GameSnapshot;
import game.save.SaveGame;
import game.save.SaveManager;
import game.vehicle.AdvancedBus;
import game.vehicle.AdvancedTruck;
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
    private static final int BUILDING_COST = 1000;
    private static final int INDUSTRY_COST = 1000;

    private static final int ROAD_CLEAR_COST_PER_TREE = 25;

    private MapRenderer mapRenderer;
    private Map map;
    private Player player;
    private JLabel statusBar;
    private String lastStatusMessage;
    private boolean roadBuildMode = false;
    private JButton buildRoadButton;
    private boolean buyVehicleMode = false;
    private JButton buyVehicleButton;

    private boolean buyBuildingMode = false;
    private JButton buyBuildingButton;
    private BuildableBuilding selectedBuildableBuilding;

    private boolean buyIndustryMode = false;
    private JButton buyIndustryButton;
    private IndustryType selectedIndustryType;

    private boolean demolishMode = false;
    private JButton demolishButton;

    private enum BuildableBuilding {
        GARAGE,
        STOP,
        TRAFFIC_LIGHT
    }

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<TrafficLight> trafficLights = new ArrayList<>();
    private long lastTickNanos;
    private Timer gameTimer;

    private GameDashboard dashboard;
    private MinimapUI minimap;
    private int dashboardRefreshCounter = 0;
    private JButton toggleDashboardButton;

    private TimeManager timeManager;
    private TimeControlPanel timeControlPanel;

    private final SaveManager saveManager = new SaveManager();

    private String currentSaveName;

    private JButton saveGameButton;
    private JButton loadGameButton;

    private SelectedBuilding startBuilding;
    private SelectedBuilding endBuilding;

    private Garage selectedGarage;
    private Integer selectedGarageRoadX;
    private Integer selectedGarageRoadY;

    private List<SelectedBuilding> routeBuildings = new ArrayList<>();

    private record SelectedBuilding(String name, int originX, int originY, int width, int height) {}

    private static final int INITIAL_WINDOW_WIDTH = 1000;
    private static final int INITIAL_WINDOW_HEIGHT = 720;

    public GameUI() {
        this((GameSnapshot) null);
    }

    public GameUI(String initialSaveName) {
        this((GameSnapshot) null);
        this.currentSaveName = normalizeSaveName(initialSaveName);
    }

    public GameUI(GameSnapshot snapshot) {
        setTitle("Mini Transport Tycoon - BurgerCity");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        if (snapshot == null) {
            // Time manager initialization
            timeManager = new TimeManager();

            // Játékos létrehozása kezdőpénzzel
            player = new Player(10000);

            // Térkép létrehozása és betöltése
            map = new Map(50, 40);
            map.loadPredefined();
        } else {
            SaveManager.LoadedState loaded = saveManager.instantiate(snapshot);
            timeManager = loaded.timeManager();
            player = loaded.player();
            map = loaded.map();
            vehicles.addAll(loaded.vehicles());
            trafficLights.addAll(loaded.trafficLights());
        }

        mapRenderer = new MapRenderer(map);
        mapRenderer.setPreferredSize(new Dimension(INITIAL_WINDOW_WIDTH, INITIAL_WINDOW_HEIGHT - 120));
        mapRenderer.setVehicles(vehicles);
        mapRenderer.setTrafficLights(trafficLights);

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
                } else if (buyBuildingMode && !wasDragged[0]) {
                    handleBuildingBuild(e.getX(), e.getY());
                } else if (buyIndustryMode && !wasDragged[0]) {
                    handleIndustryBuild(e.getX(), e.getY());
                } else if (demolishMode && !wasDragged[0]) {
                    handleDemolishClick(e.getX(), e.getY());
                } else if (!wasDragged[0]) {
                    // No mode active — inspect clicked building
                    handleInspectClick(e.getX(), e.getY());
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

        // Right side: dashboard + navigable minimap
        dashboard = new GameDashboard(player, map, vehicles);
        minimap = new MinimapUI(map, mapRenderer.getCamera(), () -> mapRenderer.repaint());

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(dashboard, BorderLayout.CENTER);
        rightPanel.add(minimap, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        // Wrapper for top panels (time control + buttons)
        JPanel topWrapper = new JPanel();
        topWrapper.setLayout(new BorderLayout());

        // Time control panel
        timeControlPanel = new TimeControlPanel(timeManager);
        topWrapper.add(timeControlPanel, BorderLayout.NORTH);

        // Felső panel gombokkal
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        buildRoadButton = new JButton("Út építés (" + Road.COST + "$)");
        buildRoadButton.addActionListener(e -> toggleRoadBuildMode());
        styleButton(buildRoadButton, new Color(34, 139, 34));
        topPanel.add(buildRoadButton);

        buyVehicleButton = new JButton("Jármű vásárlás");
        buyVehicleButton.addActionListener(e -> toggleBuyVehicleMode());
        styleButton(buyVehicleButton, new Color(65, 105, 225));
        topPanel.add(buyVehicleButton);

        buyBuildingButton = new JButton("Épület vásárlás");
        buyBuildingButton.addActionListener(e -> toggleBuyBuildingMode());
        styleButton(buyBuildingButton, new Color(184, 134, 11));
        topPanel.add(buyBuildingButton);

        buyIndustryButton = new JButton("Industry vásárlás");
        buyIndustryButton.addActionListener(e -> toggleBuyIndustryMode());
        styleButton(buyIndustryButton, new Color(148, 0, 211));
        topPanel.add(buyIndustryButton);

        demolishButton = new JButton("Rombolás");
        demolishButton.addActionListener(e -> toggleDemolishMode());
        styleButton(demolishButton, new Color(220, 20, 60));
        topPanel.add(demolishButton);

        saveGameButton = new JButton("Mentés");
        saveGameButton.addActionListener(e -> saveGame());
        styleButton(saveGameButton, new Color(70, 130, 180));
        topPanel.add(saveGameButton);

        loadGameButton = new JButton("Betöltés");
        loadGameButton.addActionListener(e -> loadGame());
        styleButton(loadGameButton, new Color(100, 149, 237));
        topPanel.add(loadGameButton);

        toggleDashboardButton = new JButton("Dashboard \u25C0");
        toggleDashboardButton.addActionListener(e -> toggleDashboard());
        styleButton(toggleDashboardButton, new Color(105, 105, 105));
        topPanel.add(toggleDashboardButton);

        topWrapper.add(topPanel, BorderLayout.SOUTH);
        add(topWrapper, BorderLayout.NORTH);

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

    private void saveGame() {
        String proposed = currentSaveName;
        while (true) {
            if (proposed == null) {
                proposed = JOptionPane.showInputDialog(this, "Mentés neve:", "Játék mentése", JOptionPane.PLAIN_MESSAGE);
                if (proposed == null) return;
            }

            String saveName = normalizeSaveName(proposed);
            if (saveName == null) saveName = "save";

            if (isSaveNameTaken(saveName)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Már létezik ilyen nevű mentés: " + saveName + "\nAdj meg másik nevet.",
                        "Mentés",
                        JOptionPane.WARNING_MESSAGE
                );
                proposed = null;
                continue;
            }

            try {
                SaveGame sg = saveManager.createSave(saveName, map, player, timeManager, vehicles, trafficLights);
                currentSaveName = normalizeSaveName(sg.getSaveName());
                updateStatus("Mentés elkészült: " + sg.getSaveName());
                return;
            } catch (IllegalArgumentException ex) {
                // Duplicate name (or other validation) coming from SaveManager.
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Mentés", JOptionPane.WARNING_MESSAGE);
                proposed = null;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Mentés sikertelen: " + ex.getMessage(), "Hiba", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
    }

    private boolean isSaveNameTaken(String saveName) {
        String normalized = normalizeSaveName(saveName);
        if (normalized == null) return false;
        try {
            for (SaveGame sg : saveManager.listSaves()) {
                String existing = normalizeSaveName(sg.getSaveName());
                if (existing != null && existing.equalsIgnoreCase(normalized)) return true;
            }
        } catch (Exception ignored) {
            // If we can't list saves, don't block saving here; SaveManager will still validate.
        }
        return false;
    }

    private static String normalizeSaveName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void loadGame() {
        try {
            List<SaveGame> saves = saveManager.listSaves();
            if (saves.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nincs elérhető mentés.", "Betöltés", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            SaveGame selected = (SaveGame) JOptionPane.showInputDialog(
                    this,
                    "Válassz mentést:",
                    "Játék betöltése",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    saves.toArray(),
                    saves.get(0)
            );

            if (selected == null) return;

            GameSnapshot snapshot = saveManager.loadSnapshot(selected);
            dispose();
            SwingUtilities.invokeLater(() -> {
                GameUI ui = new GameUI(snapshot);
                ui.setVisible(true);
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Betöltés sikertelen: " + ex.getMessage(), "Hiba", JOptionPane.ERROR_MESSAGE);
        }
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
            if (buyBuildingMode) {
                buyBuildingMode = false;
                buyBuildingButton.setBackground(null);
                buyBuildingButton.setText("Épület vásárlás");
                selectedBuildableBuilding = null;
            }
            if (buyIndustryMode) {
                buyIndustryMode = false;
                buyIndustryButton.setBackground(null);
                buyIndustryButton.setText("Industry vásárlás");
                selectedIndustryType = null;
            }
            if (demolishMode) {
                demolishMode = false;
                demolishButton.setBackground(null);
                demolishButton.setText("Rombolás");
            }
            buildRoadButton.setBackground(Color.GREEN);
            buildRoadButton.setText("Út építés BE (" + Road.COST + "$)");
            updateStatus("Kattints a térképre az út építéséhez!");
        } else {
            buildRoadButton.setBackground(null);
            buildRoadButton.setText("Út építés (" + Road.COST + "$)");
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
                buildRoadButton.setText("Út építés (" + Road.COST + "$)");
            }
            if (buyBuildingMode) {
                buyBuildingMode = false;
                buyBuildingButton.setBackground(null);
                buyBuildingButton.setText("Épület vásárlás");
                selectedBuildableBuilding = null;
            }
            if (buyIndustryMode) {
                buyIndustryMode = false;
                buyIndustryButton.setBackground(null);
                buyIndustryButton.setText("Industry vásárlás");
                selectedIndustryType = null;
            }
            if (demolishMode) {
                demolishMode = false;
                demolishButton.setBackground(null);
                demolishButton.setText("Rombolás");
            }
            buyVehicleButton.setBackground(Color.GREEN);
            buyVehicleButton.setText("Jármű vásárlás BE (" + VEHICLE_COST + "$)");
            clearVehicleSelection();
            updateStatus("Kattints egy garázsra, majd válassz buildingeket. Kattints újra az elsőre a körút lezárásához.");
        } else {
            buyVehicleButton.setBackground(null);
            buyVehicleButton.setText("Jármű vásárlás");
            clearVehicleSelection();
            updateStatus("Jármű vásárlás mód kikapcsolva.");
        }
    }

    private void toggleBuyBuildingMode() {
        buyBuildingMode = !buyBuildingMode;
        if (buyBuildingMode) {
            // Módok kizárják egymást
            if (roadBuildMode) {
                roadBuildMode = false;
                buildRoadButton.setBackground(null);
                buildRoadButton.setText("Út építés (" + Road.COST + "$)");
            }
            if (buyVehicleMode) {
                buyVehicleMode = false;
                buyVehicleButton.setBackground(null);
                buyVehicleButton.setText("Jármű vásárlás");
                clearVehicleSelection();
            }
            if (buyIndustryMode) {
                buyIndustryMode = false;
                buyIndustryButton.setBackground(null);
                buyIndustryButton.setText("Industry vásárlás");
                selectedIndustryType = null;
            }
            if (demolishMode) {
                demolishMode = false;
                demolishButton.setBackground(null);
                demolishButton.setText("Rombolás");
            }

            BuildableBuilding chosen = chooseBuildableBuilding();
            if (chosen == null) {
                buyBuildingMode = false;
                buyBuildingButton.setBackground(null);
                buyBuildingButton.setText("Épület vásárlás");
                updateStatus("Épület vásárlás megszakítva.");
                return;
            }
            selectedBuildableBuilding = chosen;

            buyBuildingButton.setBackground(Color.GREEN);
            buyBuildingButton.setText("Épület vásárlás BE (" + BUILDING_COST + "$)");
            updateStatus("Kattints a térképre az épület lerakásához: " + selectedBuildableBuilding);
        } else {
            buyBuildingButton.setBackground(null);
            buyBuildingButton.setText("Épület vásárlás");
            selectedBuildableBuilding = null;
            updateStatus("Épület vásárlás mód kikapcsolva.");
        }
    }

    private BuildableBuilding chooseBuildableBuilding() {
        JDialog dialog = new JDialog(this, "Épület vásárlás", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(184, 134, 11));
        JLabel titleLabel = new JLabel("Válassz épület típust (ára: " + BUILDING_COST + "$)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Options panel
        JPanel optionsPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        BuildableBuilding[] result = {null};

        JButton garageBtn = createDialogButton("🏠 Garage", "Járművek tárolása és karbantartása", new Color(70, 130, 180));
        garageBtn.addActionListener(e -> {
            result[0] = BuildableBuilding.GARAGE;
            dialog.dispose();
        });

        JButton stopBtn = createDialogButton("🚏 Stop", "Utasok fel- és leszállása", new Color(34, 139, 34));
        stopBtn.addActionListener(e -> {
            result[0] = BuildableBuilding.STOP;
            dialog.dispose();
        });

        JButton trafficLightBtn = createDialogButton("🚦 Traffic Light", "Forgalom irányítása kereszteződésben", new Color(220, 20, 60));
        trafficLightBtn.addActionListener(e -> {
            result[0] = BuildableBuilding.TRAFFIC_LIGHT;
            dialog.dispose();
        });

        optionsPanel.add(garageBtn);
        optionsPanel.add(stopBtn);
        optionsPanel.add(trafficLightBtn);

        // Cancel button
        JPanel bottomPanel = new JPanel();
        JButton cancelBtn = new JButton("Mégse");
        styleButton(cancelBtn, new Color(128, 128, 128));
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(cancelBtn);

        dialog.add(titlePanel, BorderLayout.NORTH);
        dialog.add(optionsPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private void toggleBuyIndustryMode() {
        buyIndustryMode = !buyIndustryMode;
        if (buyIndustryMode) {
            // Módok kizárják egymást
            if (roadBuildMode) {
                roadBuildMode = false;
                buildRoadButton.setBackground(null);
                buildRoadButton.setText("Út építés (" + Road.COST + "$)");
            }
            if (buyVehicleMode) {
                buyVehicleMode = false;
                buyVehicleButton.setBackground(null);
                buyVehicleButton.setText("Jármű vásárlás");
                clearVehicleSelection();
            }
            if (buyBuildingMode) {
                buyBuildingMode = false;
                buyBuildingButton.setBackground(null);
                buyBuildingButton.setText("Épület vásárlás");
                selectedBuildableBuilding = null;
            }
            if (demolishMode) {
                demolishMode = false;
                demolishButton.setBackground(null);
                demolishButton.setText("Rombolás");
            }

            IndustryType chosen = chooseIndustryType();
            if (chosen == null) {
                buyIndustryMode = false;
                buyIndustryButton.setBackground(null);
                buyIndustryButton.setText("Industry vásárlás");
                updateStatus("Industry vásárlás megszakítva.");
                return;
            }
            selectedIndustryType = chosen;

            buyIndustryButton.setBackground(Color.GREEN);
            buyIndustryButton.setText("Industry vásárlás BE (" + INDUSTRY_COST + "$)");
            updateStatus("Kattints a térképre az industry lerakásához (2x2): " + selectedIndustryType);
        } else {
            buyIndustryButton.setBackground(null);
            buyIndustryButton.setText("Industry vásárlás");
            selectedIndustryType = null;
            updateStatus("Industry vásárlás mód kikapcsolva.");
        }
    }

    private IndustryType chooseIndustryType() {
        JDialog dialog = new JDialog(this, "Industry vásárlás", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(148, 0, 211));
        JLabel titleLabel = new JLabel("Válassz industry típust (ára: " + INDUSTRY_COST + "$ | foglal: 2x2)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Options panel
        JPanel optionsPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        IndustryType[] result = {null};

        JButton farmBtn = createDialogButton("🌾 Farm", "Gabona termelés", new Color(107, 142, 35));
        farmBtn.addActionListener(e -> {
            result[0] = IndustryType.FARM;
            dialog.dispose();
        });

        JButton ranchBtn = createDialogButton("🐄 Ranch", "Állattenyésztés", new Color(160, 82, 45));
        ranchBtn.addActionListener(e -> {
            result[0] = IndustryType.RANCH;
            dialog.dispose();
        });

        JButton bakeryBtn = createDialogButton("🍞 Bakery", "Buci készítés", new Color(210, 180, 140));
        bakeryBtn.addActionListener(e -> {
            result[0] = IndustryType.BAKERY;
            dialog.dispose();
        });

        JButton pattyBtn = createDialogButton("🥩 Patty Plant", "Húspogácsa előállítás", new Color(178, 34, 34));
        pattyBtn.addActionListener(e -> {
            result[0] = IndustryType.PATTY_PLANT;
            dialog.dispose();
        });

        JButton burgerBtn = createDialogButton("🍔 Burger Factory", "Hamburger készítés", new Color(255, 140, 0));
        burgerBtn.addActionListener(e -> {
            result[0] = IndustryType.BURGER_FACTORY;
            dialog.dispose();
        });

        optionsPanel.add(farmBtn);
        optionsPanel.add(ranchBtn);
        optionsPanel.add(bakeryBtn);
        optionsPanel.add(pattyBtn);
        optionsPanel.add(burgerBtn);

        // Cancel button
        JPanel bottomPanel = new JPanel();
        JButton cancelBtn = new JButton("Mégse");
        styleButton(cancelBtn, new Color(128, 128, 128));
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(cancelBtn);

        dialog.add(titlePanel, BorderLayout.NORTH);
        dialog.add(optionsPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    private void toggleDemolishMode() {
        demolishMode = !demolishMode;
        if (demolishMode) {
            if (roadBuildMode) {
                roadBuildMode = false;
                buildRoadButton.setBackground(null);
                buildRoadButton.setText("Út építés (" + Road.COST + "$)");
            }
            if (buyVehicleMode) {
                buyVehicleMode = false;
                buyVehicleButton.setBackground(null);
                buyVehicleButton.setText("Jármű vásárlás");
                clearVehicleSelection();
            }
            if (buyBuildingMode) {
                buyBuildingMode = false;
                buyBuildingButton.setBackground(null);
                buyBuildingButton.setText("Épület vásárlás");
                selectedBuildableBuilding = null;
            }
            if (buyIndustryMode) {
                buyIndustryMode = false;
                buyIndustryButton.setBackground(null);
                buyIndustryButton.setText("Industry vásárlás");
                selectedIndustryType = null;
            }

            demolishButton.setBackground(Color.GREEN);
            demolishButton.setText("Rombolás BE");
            updateStatus("Kattints egy útra / épületre / industry-re / erdőre a romboláshoz (út: 0$ vissza, épület: 50%, industry: 50%, erdő: +20$/tile).");
        } else {
            demolishButton.setBackground(null);
            demolishButton.setText("Rombolás");
            updateStatus("Rombolás mód kikapcsolva.");
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

        Tile target = map.getTile(tileX, tileY);
        if (target == null) return;

        int cost = Road.COST;
        boolean clearing = false;
        if (target.getType() == TileType.FOREST) {
            clearing = true;
            cost += ROAD_CLEAR_COST_PER_TREE * Math.max(1, target.getForestTrees());
        }

        // Ellenőrzés: van-e elég pénz
        if (!player.spendMoney(cost)) {
            updateStatus("Nincs elég pénz az út építéséhez! Szükséges: " + cost + "$");
            return;
        }

        // Út építése
        if (map.buildRoad(tileX, tileY)) {
            mapRenderer.repaint();
            updateStatus("Út sikeresen megépítve" + (clearing ? " (irtás)" : "") +
                    " (" + tileX + ", " + tileY + "). Pénz: " + player.getMoney() + "$");
        } else {
            // Ha nem sikerült, visszaadjuk a pénzt
            player.addMoney(cost);
            updateStatus("Erre a mezőre nem építhető út!");
        }
    }

    private void handleBuildingBuild(int screenX, int screenY) {
        if (selectedBuildableBuilding == null) {
            updateStatus("Nincs kiválasztott épület típus. Kapcsold be újra az Épület vásárlást.");
            return;
        }

        int tileSize = 32;
        Camera camera = mapRenderer.getCamera();
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);

        Tile targetTile = map.getTile(tileX, tileY);
        if (targetTile == null) {
            updateStatus("Ide nem lehet építeni (térképen kívül): (" + tileX + ", " + tileY + ").");
            return;
        }

        // Special validation for Traffic Light: must be on ROAD intersection
        if (selectedBuildableBuilding == BuildableBuilding.TRAFFIC_LIGHT) {
            if (targetTile.getType() != TileType.ROAD) {
                updateStatus("Traffic Lights can only be built on roads!");
                return;
            }
            boolean alreadyHasLight = trafficLights.stream()
                    .anyMatch(tl -> tl.getX() == tileX && tl.getY() == tileY);
            if (alreadyHasLight) {
                updateStatus("There is already a Traffic Light here!");
                return;
            }
            // Intersection check will be done in map.buildBuilding()
        } else {
            // Regular buildings: must be on GRASS
            if (targetTile.getType() != TileType.GRASS) {
                updateStatus("Csak üres fű mezőre lehet épületet rakni! Aktuális: " + targetTile.getType() + ".");
                return;
            }

            if (targetTile.isOccupied()) {
                updateStatus("Ez a mező foglalt, ide nem lehet építeni.");
                return;
            }
        }

        if (!player.spendMoney(BUILDING_COST)) {
            updateStatus("Nincs elég pénz az épület vásárlásához! Szükséges: " + BUILDING_COST + "$");
            return;
        }

        var building = switch (selectedBuildableBuilding) {
            case GARAGE -> new Garage(tileX, tileY);
            case STOP -> new Stop(tileX, tileY);
            case TRAFFIC_LIGHT -> new TrafficLight(tileX, tileY);
        };

        if (map.buildBuilding(tileX, tileY, building)) {
            // Add traffic light to the list for updates
            if (building instanceof TrafficLight) {
                trafficLights.add((TrafficLight) building);
                mapRenderer.setTrafficLights(trafficLights);
            }
            mapRenderer.repaint();
            updateStatus("Épület sikeresen lerakva: " + building.getName() + " (" + tileX + ", " + tileY + "). Pénz: " + player.getMoney() + "$");
        } else {
            player.addMoney(BUILDING_COST);
            if (building instanceof TrafficLight) {
                updateStatus("Közlekedési lámpát csak kereszteződésre (3 vagy 4 utas) lehet építeni!");
            } else {
                updateStatus("Erre a mezőre nem építhető épület!");
            }
        }
    }

    private void handleIndustryBuild(int screenX, int screenY) {
        if (selectedIndustryType == null) {
            updateStatus("Nincs kiválasztott industry típus. Kapcsold be újra az Industry vásárlást.");
            return;
        }

        int tileSize = 32;
        Camera camera = mapRenderer.getCamera();
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);

        // Pre-check the whole 2x2 footprint so we can explain failures.
        Tile t00 = map.getTile(tileX, tileY);
        Tile t10 = map.getTile(tileX + 1, tileY);
        Tile t01 = map.getTile(tileX, tileY + 1);
        Tile t11 = map.getTile(tileX + 1, tileY + 1);
        if (t00 == null || t10 == null || t01 == null || t11 == null) {
            updateStatus("Az industry 2x2-t foglal, itt nem fér el (térképen kívül). ");
            return;
        }

        Tile[] footprint = {t00, t10, t01, t11};
        for (Tile t : footprint) {
            if (t.getType() != TileType.GRASS) {
                updateStatus("Az industry csak üres fűre tehető (2x2). Talált: " + t.getType() + ".");
                return;
            }
            if (t.isOccupied()) {
                updateStatus("Az industry helye foglalt (2x2 terület). ");
                return;
            }
        }

        if (!player.spendMoney(INDUSTRY_COST)) {
            updateStatus("Nincs elég pénz industry vásárlásához! Szükséges: " + INDUSTRY_COST + "$");
            return;
        }

        if (map.buildIndustry(tileX, tileY, selectedIndustryType)) {
            mapRenderer.repaint();
            updateStatus("Industry sikeresen lerakva: " + selectedIndustryType + " (" + tileX + ", " + tileY + ") [2x2]. Pénz: " + player.getMoney() + "$");
        } else {
            player.addMoney(INDUSTRY_COST);
            updateStatus("Erre a helyre nem tehető industry (2x2)!");
        }
    }

    private void handleDemolishClick(int screenX, int screenY) {
        int tileSize = 32;
        Camera camera = mapRenderer.getCamera();
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);

        Tile tile = map.getTile(tileX, tileY);
        if (tile == null) {
            updateStatus("Érvénytelen mező.");
            return;
        }

        if (tile.getType() == TileType.ROAD) {
            if (map.demolishRoad(tileX, tileY)) {
                mapRenderer.repaint();
                updateStatus("Út lerombolva (" + tileX + ", " + tileY + "). Visszatérítés: 0$.");
            } else {
                updateStatus("Itt nincs lerombolható út.");
            }
            return;
        }

        if (tile.getType() == TileType.BUILDING) {
            var b = map.demolishBuilding(tileX, tileY);
            if (b != null) {
                int refund = b.getCost() / 2;
                player.addMoney(refund);
                mapRenderer.repaint();
                updateStatus("Épület lerombolva: " + b.getName() + " (" + tileX + ", " + tileY + "). Visszatérítés: " + refund + "$.");
            } else {
                updateStatus("Itt nincs lerombolható épület.");
            }
            return;
        }

        if (tile.getType() == TileType.INDUSTRY) {
            var ind = map.demolishIndustryAt(tileX, tileY);
            if (ind != null) {
                // Remove vehicles serving this industry
                removeVehiclesServingBuilding(ind.getOriginX(), ind.getOriginY());

                int refund = INDUSTRY_COST / 2;
                player.addMoney(refund);
                mapRenderer.repaint();
                updateStatus("Industry lerombolva: " + ind.getName() + " (" + ind.getOriginX() + ", " + ind.getOriginY() + ") [" + ind.getIndustryType() + "]. Visszatérítés: " + refund + "$.");
            } else {
                updateStatus("Itt nincs lerombolható industry.");
            }
            return;
        }

        if (tile.getType() == TileType.FOREST) {
            if (map.demolishForest(tileX, tileY)) {
                player.addMoney(20);
                mapRenderer.rebuildGrassLayerCache();
                mapRenderer.repaint();
                updateStatus("Erdő kivágva (" + tileX + ", " + tileY + "). Bevétel: +20$. Pénz: " + player.getMoney() + "$");
            } else {
                updateStatus("Itt nincs kivágható erdő.");
            }
            return;
        }

        updateStatus("Itt nincs lerombolható út/épület/industry/erdő.");
    }

    private void handleBuyVehicleClick(int screenX, int screenY) {
        int tileSize = 32;
        Camera camera = mapRenderer.getCamera();
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);

        // Step 1: choose a garage
        if (selectedGarage == null) {
            Tile t = map.getTile(tileX, tileY);
            if (t == null || t.getPlacedBuilding() == null || !(t.getPlacedBuilding() instanceof Garage g)) {
                updateStatus("Először kattints egy garázsra a jármű vásárláshoz!");
                return;
            }
            var roads = map.adjacentRoadTilesForArea(tileX, tileY, 1, 1);
            if (roads.isEmpty()) {
                updateStatus("A garázsnak út mellé kell kerülnie (nincs szomszédos ROAD). ");
                return;
            }
            int[] r0 = roads.get(0);
            selectedGarage = g;
            selectedGarageRoadX = r0[0];
            selectedGarageRoadY = r0[1];
            updateStatus("Garázs kiválasztva: (" + tileX + ", " + tileY + "). Válassz buildingeket az útvonalon.");
            return;
        }

        SelectedBuilding clicked = findBuildingAt(tileX, tileY);
        if (clicked == null) {
            updateStatus("Kattints egy városra vagy iparra (building)!");
            return;
        }

        // Ha az első buildingre kattintottak újra és van legalább 2 building
        if (!routeBuildings.isEmpty() && sameBuilding(routeBuildings.get(0), clicked) && routeBuildings.size() >= 2) {
            // Körút lezárása
            placeVehicleWithMultiStopRoute();
            return;
        }

        // Ugyanazt a buildinget nem adhatjuk hozzá kétszer egymás után
        if (!routeBuildings.isEmpty() && sameBuilding(routeBuildings.get(routeBuildings.size() - 1), clicked)) {
            updateStatus("Ez a building már az útvonal utolsó állomása. Válassz másikat!");
            return;
        }

        // Building hozzáadása az útvonalhoz
        routeBuildings.add(clicked);
        if (routeBuildings.size() == 1) {
            updateStatus("Első állomás: " + clicked.name() + ". Válassz további állomásokat vagy kattints újra az elsőre a lezáráshoz.");
        } else {
            updateStatus("Állomás hozzáadva (" + routeBuildings.size() + "): " + clicked.name() + ". Kattints az elsőre (" + routeBuildings.get(0).name() + ") a lezáráshoz.");
        }
    }

    private void placeVehicleWithMultiStopRoute() {
        if (selectedGarage == null || selectedGarageRoadX == null || selectedGarageRoadY == null) {
            updateStatus("Nincs kiválasztott garázs. Kattints egy garázsra!");
            return;
        }
        if (routeBuildings.size() < 2) {
            updateStatus("Legalább 2 állomás szükséges az útvonalhoz!");
            return;
        }

        // Körút útvonalának építése: összekötjük az összes buildingot körbe
        List<int[]> fullPath = buildCircularRoutePath();
        if (fullPath == null || fullPath.isEmpty()) {
            updateStatus("Nincs érvényes úthálózat az állomások között! Építs összefüggő utat.");
            return;
        }

        int[] routeStart = fullPath.get(0);
        List<int[]> fromGarage = map.findRoadPathBetweenRoadTiles(
                selectedGarageRoadX, selectedGarageRoadY,
                routeStart[0], routeStart[1]
        );
        if (fromGarage.isEmpty()) {
            updateStatus("Nincs összefüggő út a garázstól a kiválasztott útvonalhoz! Kösd rá a garázst az úthálózatra.");
            return;
        }

        if (!player.spendMoney(VEHICLE_COST)) {
            updateStatus("Nincs elég pénz jármű vásárlásához! Szükséges: " + VEHICLE_COST + "$");
            return;
        }

        Integer choice = chooseVehicleType();

        if (choice == null) {
            player.addMoney(VEHICLE_COST);
            updateStatus("Jármű vásárlás megszakítva.");
            return;
        }

        // Bus csak városok között mehet
        if ((choice == 0 || choice == 2) && !allBuildingsAreCities()) {
            player.addMoney(VEHICLE_COST);
            updateStatus("Bus csak városok között vásárolható (utas szállítás). Minden állomásnak városnak kell lennie!");
            clearVehicleSelection();
            return;
        }

        String[] vehicleNames = {"Bus", "Truck", "Advanced Bus", "Advanced Truck"};
        Vehicle v = switch (choice) {
            case 0 -> new Bus();
            case 1 -> new Truck();
            case 2 -> new AdvancedBus();
            case 3 -> new AdvancedTruck();
            default -> new Bus();
        };
        v.setHomeGarage(selectedGarage);
        v.setPurchasePrice(VEHICLE_COST);
        v.setRoutePath(fullPath);
        v.setRejoinRouteAt(routeStart[0], routeStart[1]);
        v.setPath(fromGarage);

        // Store all buildings in the route
        storeRouteBuildingsInVehicle(v);

        vehicles.add(v);
        mapRenderer.repaint();

        String routeDescription = buildRouteDescription();
        updateStatus("Jármű megvásárolva garázsból: " + vehicleNames[choice] + " | Körút: " + routeDescription);
        clearVehicleSelection();
    }

    private List<int[]> buildCircularRoutePath() {
        List<int[]> fullPath = new ArrayList<>();

        // Összekötjük az összes buildingot sorban, majd visszatérünk az elsőhöz
        for (int i = 0; i < routeBuildings.size(); i++) {
            SelectedBuilding current = routeBuildings.get(i);
            SelectedBuilding next = routeBuildings.get((i + 1) % routeBuildings.size());

            List<int[]> segment = map.findRoadPathBetweenAreas(
                    current.originX(), current.originY(), current.width(), current.height(),
                    next.originX(), next.originY(), next.width(), next.height()
            );

            if (segment.isEmpty()) {
                return null; // Nincs útvonal
            }

            // Hozzáadjuk a szegmenst, de az utolsó pontot csak egyszer (ne duplikáljuk)
            for (int j = 0; j < segment.size(); j++) {
                if (i == 0 || j > 0) { // Skip first point of non-first segments to avoid duplication
                    fullPath.add(segment.get(j));
                }
            }
        }

        return fullPath;
    }

    private boolean allBuildingsAreCities() {
        for (SelectedBuilding b : routeBuildings) {
            if (!isCityBuilding(b)) {
                return false;
            }
        }
        return true;
    }

    private void storeRouteBuildingsInVehicle(Vehicle v) {
        if (routeBuildings.size() >= 2) {
            SelectedBuilding first = routeBuildings.get(0);
            SelectedBuilding last = routeBuildings.get(routeBuildings.size() - 1);
            v.setRouteBuildings(first.originX(), first.originY(), last.originX(), last.originY());
        }
    }

    private String buildRouteDescription() {
        if (routeBuildings.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < routeBuildings.size(); i++) {
            sb.append(routeBuildings.get(i).name());
            if (i < routeBuildings.size() - 1) {
                sb.append(" → ");
            }
        }
        sb.append(" → ").append(routeBuildings.get(0).name());
        return sb.toString();
    }

    private void placeVehicleIfPathValid() {
        if (selectedGarage == null || selectedGarageRoadX == null || selectedGarageRoadY == null) {
            updateStatus("Nincs kiválasztott garázs. Kattints egy garázsra!");
            return;
        }
        if (startBuilding == null || endBuilding == null) return;

        List<int[]> path = map.findRoadPathBetweenAreas(
                startBuilding.originX(), startBuilding.originY(), startBuilding.width(), startBuilding.height(),
                endBuilding.originX(), endBuilding.originY(), endBuilding.width(), endBuilding.height()
        );

        if (path.isEmpty()) {
            updateStatus("Nincs érvényes úthálózat a két building között! Építs összefüggő utat.");
            return;
        }

        int[] routeStart = path.get(0);
        List<int[]> fromGarage = map.findRoadPathBetweenRoadTiles(
                selectedGarageRoadX, selectedGarageRoadY,
                routeStart[0], routeStart[1]
        );
        if (fromGarage.isEmpty()) {
            updateStatus("Nincs összefüggő út a garázstól a kiválasztott útvonalhoz! Kösd rá a garázst az úthálózatra.");
            return;
        }

        if (!player.spendMoney(VEHICLE_COST)) {
            updateStatus("Nincs elég pénz jármű vásárlásához! Szükséges: " + VEHICLE_COST + "$");
            return;
        }

        Integer choice = chooseVehicleType();

        if (choice == null) {
            player.addMoney(VEHICLE_COST);
            updateStatus("Jármű vásárlás megszakítva.");
            return;
        }

        // Assignment-aligned constraint: bus transports passengers between cities.
        if ((choice == 0 || choice == 2) && (!isCityBuilding(startBuilding) || !isCityBuilding(endBuilding))) {
            player.addMoney(VEHICLE_COST);
            updateStatus("Bus csak két város között vásárolható (utas szállítás). Válassz 2 várost!");
            clearVehicleSelection();
            return;
        }

        String[] vehicleNames = {"Bus", "Truck", "Advanced Bus", "Advanced Truck"};
        Vehicle v = switch (choice) {
            case 0 -> new Bus();
            case 1 -> new Truck();
            case 2 -> new AdvancedBus();
            case 3 -> new AdvancedTruck();
            default -> new Bus();
        };
        v.setHomeGarage(selectedGarage);
        v.setPurchasePrice(VEHICLE_COST);
        v.setRoutePath(path);
        v.setRejoinRouteAt(routeStart[0], routeStart[1]);
        v.setPath(fromGarage);
        v.setRouteBuildings(startBuilding.originX(), startBuilding.originY(),
                            endBuilding.originX(), endBuilding.originY());
        vehicles.add(v);
        mapRenderer.repaint();
        updateStatus("Jármű megvásárolva garázsból: " + vehicleNames[choice] + " | Útvonal: " + startBuilding.name() + " -> " + endBuilding.name());
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
        selectedGarage = null;
        selectedGarageRoadX = null;
        selectedGarageRoadY = null;
        routeBuildings.clear();
    }

    private void clearVehicleRouteSelection() {
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

    private void handleInspectClick(int screenX, int screenY) {
        int tileSize = 32;
        Camera camera = mapRenderer.getCamera();
        double worldX = camera.screenToWorldX(screenX);
        double worldY = camera.screenToWorldY(screenY);
        int tileX = (int) (worldX / tileSize);
        int tileY = (int) (worldY / tileSize);

        // Check cities
        for (City c : map.getCities()) {
            if (c.occupies(tileX, tileY)) {
                // Make dashboard visible if hidden
                if (!dashboard.isDashboardVisible()) {
                    toggleDashboard();
                }
                dashboard.inspectCity(c);
                updateStatus("Inspecting city: " + c.getName());
                return;
            }
        }

        // Check industries
        for (Industry i : map.getIndustries()) {
            if (i.occupies(tileX, tileY)) {
                if (!dashboard.isDashboardVisible()) {
                    toggleDashboard();
                }
                dashboard.inspectIndustry(i);
                updateStatus("Inspecting industry: " + i.getName());
                return;
            }
        }

        // Check traffic lights
        for (TrafficLight light : trafficLights) {

            if (light != null && light.getX() == tileX && light.getY() == tileY) {
                showTrafficLightSettings(light);
                updateStatus("Configuring traffic light at (" + tileX + ", " + tileY + ")");
                return;
            }
        }

        // Clicked on empty space — clear inspection
        if (dashboard.hasInspection()) {
            dashboard.clearInspection();
            updateStatus("Mini Transport Tycoon | BurgerCity");
        }
    }

    private void showTrafficLightSettings(TrafficLight light) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        // Settings panel
        JPanel settingsPanel = new JPanel(new GridLayout(2, 2, 5, 5));

        JLabel mainLabel = new JLabel("North-South (Main) duration (sec):");
        JTextField mainField = new JTextField(String.valueOf((int)light.getGreenDurationMain()));

        JLabel crossLabel = new JLabel("East-West (Cross) duration (sec):");
        JTextField crossField = new JTextField(String.valueOf((int)light.getGreenDurationCross()));

        settingsPanel.add(mainLabel);
        settingsPanel.add(mainField);
        settingsPanel.add(crossLabel);
        settingsPanel.add(crossField);

        // Delete button
        JButton deleteButton = new JButton("Delete Traffic Light");
        deleteButton.setForeground(Color.RED);

        panel.add(settingsPanel, BorderLayout.CENTER);
        panel.add(deleteButton, BorderLayout.SOUTH);

        // Custom dialog with options
        Object[] options = {"Save", "Cancel"};
        final boolean[] deleted = {false};

        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete this traffic light?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                // Remove from list
                trafficLights.remove(light);
                mapRenderer.setTrafficLights(trafficLights);

                // Remove from map tile
                Tile tile = map.getTile(light.getX(), light.getY());
                if (tile != null) {
                    tile.setOccupied(false);
                    tile.setPlacedBuilding(null);
                }

                mapRenderer.repaint();
                updateStatus("Traffic light deleted at (" + light.getX() + ", " + light.getY() + ")");
                deleted[0] = true;

                // Close the settings dialog
                Window window = SwingUtilities.getWindowAncestor(panel);
                if (window != null) {
                    window.dispose();
                }
            }
        });

        int result = JOptionPane.showOptionDialog(
            this,
            panel,
            "Traffic Light Settings at (" + light.getX() + ", " + light.getY() + ")",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        );

        // Only update settings if not deleted and user clicked Save
        if (!deleted[0] && result == JOptionPane.OK_OPTION) {
            try {
                double mainDuration = Double.parseDouble(mainField.getText());
                double crossDuration = Double.parseDouble(crossField.getText());

                if (mainDuration < 1 || crossDuration < 1) {
                    JOptionPane.showMessageDialog(this, "Durations must be at least 1 second!");
                    return;
                }

                light.setDurations(mainDuration, crossDuration);
                updateStatus("Traffic light settings updated: Main=" + mainDuration + "s, Cross=" + crossDuration + "s");

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid number format!");
            }
        }
    }

    private void toggleDashboard() {
        boolean nowVisible = dashboard.toggleVisibility();
        toggleDashboardButton.setText(nowVisible ? "Dashboard \u25C0" : "Dashboard \u25B6");

        // Do not resize the window; only relayout content.
        revalidate();
        repaint();
    }

    private void tick() {
        long now = System.nanoTime();
        double realDeltaSeconds = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;

        // Update time manager and get game-adjusted delta time
        double gameDeltaSeconds = timeManager.update(realDeltaSeconds);

        // Only update game logic if not paused (gameDeltaSeconds will be 0 when paused)
        if (!timeManager.isPaused()) {
            map.updateEconomy(gameDeltaSeconds);
            map.updateForests(gameDeltaSeconds);

            // Check and remove invalid traffic lights
            removeInvalidTrafficLights();

            // Update traffic lights
            for (TrafficLight light : trafficLights) {
                if (light != null) light.update(gameDeltaSeconds);
            }

            for (Vehicle v : vehicles) {
                if (v == null) continue;
                v.update(map, gameDeltaSeconds, vehicles, trafficLights);
                v.processArrivalEconomy(map, player);
            }
        }

        // Refresh dashboard every ~30 frames (~0.5 seconds) to keep it responsive but efficient
        dashboardRefreshCounter++;
        if (dashboardRefreshCounter >= 30) {
            dashboardRefreshCounter = 0;
            dashboard.refresh();
        }

        // Always refresh time control panel
        timeControlPanel.refresh();

        mapRenderer.repaint();
        if (minimap != null) minimap.repaint();

        // Always reflect current money even without new status messages.
        statusBar.setText(" " + lastStatusMessage + " | Pénz: " + player.getMoney() + "$");
    }

    private void updateStatus(String message) {
        lastStatusMessage = message;
        statusBar.setText(" " + lastStatusMessage + " | Pénz: " + player.getMoney() + "$");
    }

    /**
     * Remove traffic lights that are no longer valid.
     * A traffic light is invalid if:
     * - The tile is no longer ROAD
     * - The tile is no longer an intersection (< 3 road neighbors)
     */
    private void removeInvalidTrafficLights() {
        List<TrafficLight> toRemove = new ArrayList<>();

        for (TrafficLight light : trafficLights) {
            if (light == null) continue;

            if (!map.isTrafficLightValid(light.getX(), light.getY())) {
                toRemove.add(light);

                // Clear the tile
 
                Tile tile = map.getTile(light.getX(), light.getY());
                if (tile != null) {
                    tile.setOccupied(false);
                    tile.setPlacedBuilding(null);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            trafficLights.removeAll(toRemove);
            mapRenderer.setTrafficLights(trafficLights);

            if (toRemove.size() == 1) {
                updateStatus("Traffic light removed (no longer at intersection)");
            } else {
                updateStatus(toRemove.size() + " traffic lights removed (no longer at intersections)");
            }
        }
    }

    /**
     * Remove all vehicles that serve a building at the given origin coordinates.
     * Called when a City or Industry is demolished.
     */
    public void removeVehiclesServingBuilding(int originX, int originY) {
        List<Vehicle> toRemove = new ArrayList<>();

        for (Vehicle v : vehicles) {
            if (v != null && v.servesBuilding(originX, originY)) {
                toRemove.add(v);
            }
        }

        if (!toRemove.isEmpty()) {
            vehicles.removeAll(toRemove);
            mapRenderer.setVehicles(vehicles);

            if (toRemove.size() == 1) {
                updateStatus("1 vehicle removed (building destroyed)");
            } else {
                updateStatus(toRemove.size() + " vehicles removed (building destroyed)");
            }
        }
    }

    /**
     * Create a styled vehicle type selection dialog
     */
    private Integer chooseVehicleType() {
        JDialog dialog = new JDialog(this, "Jármű vásárlás", true);
        dialog.setLayout(new BorderLayout(10, 10));

        // Title panel
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(65, 105, 225));
        JLabel titleLabel = new JLabel("Válassz jármű típust (ára: " + VEHICLE_COST + "$)");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);

        // Options panel
        JPanel optionsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        optionsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        Integer[] result = {null};

        JButton busBtn = createDialogButton("🚌 Bus", "Utasok szállítása városok között", new Color(30, 144, 255));
        busBtn.addActionListener(e -> {
            result[0] = 0;
            dialog.dispose();
        });

        JButton truckBtn = createDialogButton("🚚 Truck", "Áruk szállítása", new Color(70, 130, 180));
        truckBtn.addActionListener(e -> {
            result[0] = 1;
            dialog.dispose();
        });

        JButton advBusBtn = createDialogButton("🚍 Advanced Bus", "Nagyobb kapacitású utasszállítás", new Color(0, 191, 255));
        advBusBtn.addActionListener(e -> {
            result[0] = 2;
            dialog.dispose();
        });

        JButton advTruckBtn = createDialogButton("🚛 Advanced Truck", "Nagyobb kapacitású áruszállítás", new Color(100, 149, 237));
        advTruckBtn.addActionListener(e -> {
            result[0] = 3;
            dialog.dispose();
        });

        optionsPanel.add(busBtn);
        optionsPanel.add(truckBtn);
        optionsPanel.add(advBusBtn);
        optionsPanel.add(advTruckBtn);

        // Cancel button
        JPanel bottomPanel = new JPanel();
        JButton cancelBtn = new JButton("Mégse");
        styleButton(cancelBtn, new Color(128, 128, 128));
        cancelBtn.addActionListener(e -> dialog.dispose());
        bottomPanel.add(cancelBtn);

        dialog.add(titlePanel, BorderLayout.NORTH);
        dialog.add(optionsPanel, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        return result[0];
    }

    /**
     * Create a styled dialog button with title and description
     */
    private JButton createDialogButton(String title, String description, Color color) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout(5, 5));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);

        JLabel descLabel = new JLabel("<html><i>" + description + "</i></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        descLabel.setForeground(new Color(240, 240, 240));

        JPanel textPanel = new JPanel(new BorderLayout(0, 5));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel, BorderLayout.NORTH);
        textPanel.add(descLabel, BorderLayout.CENTER);

        button.add(textPanel, BorderLayout.CENTER);

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(color.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(color);
            }
        });

        return button;
    }

    /**
     * Apply modern styling to a button with specified base color.
     */
    private void styleButton(JButton button, Color baseColor) {
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(baseColor.darker(), 2),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.getBackground() != Color.GREEN) {
                    button.setBackground(baseColor.brighter());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.getBackground() != Color.GREEN) {
                    button.setBackground(baseColor);
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenuUI mainMenu = new MainMenuUI();
            mainMenu.setVisible(true);
        });
    }
}