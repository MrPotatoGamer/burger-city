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
        topPanel.add(buildRoadButton);

        buyVehicleButton = new JButton("Jármű vásárlás");
        buyVehicleButton.addActionListener(e -> toggleBuyVehicleMode());
        topPanel.add(buyVehicleButton);

        buyBuildingButton = new JButton("Épület vásárlás");
        buyBuildingButton.addActionListener(e -> toggleBuyBuildingMode());
        topPanel.add(buyBuildingButton);

        buyIndustryButton = new JButton("Industry vásárlás");
        buyIndustryButton.addActionListener(e -> toggleBuyIndustryMode());
        topPanel.add(buyIndustryButton);

        demolishButton = new JButton("Rombolás");
        demolishButton.addActionListener(e -> toggleDemolishMode());
        topPanel.add(demolishButton);

        saveGameButton = new JButton("Mentés");
        saveGameButton.addActionListener(e -> saveGame());
        topPanel.add(saveGameButton);

        loadGameButton = new JButton("Betöltés");
        loadGameButton.addActionListener(e -> loadGame());
        topPanel.add(loadGameButton);

        toggleDashboardButton = new JButton("Dashboard \u25C0");
        toggleDashboardButton.addActionListener(e -> toggleDashboard());
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
            updateStatus("Kattints egy garázsra, majd válassz 2 buildinget (város/ipar) kattintással, majd Bus/Truck.");
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
        Object[] options = {"Garage", "Stop", "TrafficLight"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Válassz épület típust (mindegyik ára: " + BUILDING_COST + "$):",
                "Épület vásárlás",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return switch (choice) {
            case 0 -> BuildableBuilding.GARAGE;
            case 1 -> BuildableBuilding.STOP;
            case 2 -> BuildableBuilding.TRAFFIC_LIGHT;
            default -> null;
        };
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
        Object[] options = {"Farm", "Ranch", "Bakery", "Patty Plant", "Burger Factory", "Factory"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Válassz industry típust (ára: " + INDUSTRY_COST + "$ | foglal: 2x2):",
                "Industry vásárlás",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        return switch (choice) {
            case 0 -> IndustryType.FARM;
            case 1 -> IndustryType.RANCH;
            case 2 -> IndustryType.BAKERY;
            case 3 -> IndustryType.PATTY_PLANT;
            case 4 -> IndustryType.BURGER_FACTORY;
            case 5 -> IndustryType.FACTORY;
            default -> null;
        };
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
        int tileSize = 32; // MapRenderer.TILE_SIZE
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
            updateStatus("Garázs kiválasztva: (" + tileX + ", " + tileY + "). Válassz kezdő buildinget.");
            return;
        }

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
        clearVehicleRouteSelection();
        // Keep the garage selection; user only restarts the route selection.
        startBuilding = clicked;
        updateStatus("Kezdő building kiválasztva: " + startBuilding.name() + ". Válassz cél buildinget.");
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

        Object[] options = {"Bus", "Truck", "Advanced Bus", "Advanced Truck"};
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

        if (choice < 0 || choice > 3) {
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
        updateStatus("Jármű megvásárolva garázsból: " + options[choice] + " | Útvonal: " + startBuilding.name() + " -> " + endBuilding.name());
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenuUI mainMenu = new MainMenuUI();
            mainMenu.setVisible(true);
        });
    }
}