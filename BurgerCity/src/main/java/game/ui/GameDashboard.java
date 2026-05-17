package game.ui;

import game.building.Garage;
import game.core.Player;
import game.core.ResourcePrices;
import game.map.City;
import game.map.Industry;
import game.map.Map;
import game.resource.ResourceType;
import game.vehicle.Bus;
import game.vehicle.Truck;
import game.vehicle.Vehicle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

/**
 * A live-updating dashboard panel that shows the full state of the game:
 * money, vehicles, cities, industries, production chains, and revenue info.
 */
public class GameDashboard extends JPanel {

    private final Player player;
    private final Map map;
    private final List<Vehicle> vehicles;

    // === Section panels (rebuilt every refresh) ===
    private JPanel contentPanel;
    private JScrollPane scrollPane;
    private JLabel header;

    // === Inspection: currently selected city or industry ===
    private City inspectedCity;
    private Industry inspectedIndustry;

    // Color palette
    private static final Color BG_DARK = new Color(30, 30, 36);
    private static final Color BG_SECTION = new Color(42, 42, 50);
    private static final Color TEXT_PRIMARY = new Color(230, 230, 230);
    private static final Color TEXT_SECONDARY = new Color(170, 170, 180);
    private static final Color ACCENT_GREEN = new Color(80, 200, 120);
    private static final Color ACCENT_GOLD = new Color(255, 215, 80);
    private static final Color ACCENT_BLUE = new Color(100, 160, 255);
    private static final Color ACCENT_RED = new Color(240, 90, 90);
    private static final Color ACCENT_ORANGE = new Color(255, 165, 60);

    public GameDashboard(Player player, Map map, List<Vehicle> vehicles) {
        this.player = player;
        this.map = map;
        this.vehicles = vehicles;

        setLayout(new BorderLayout());
        setBackground(BG_DARK);
        setPreferredSize(new Dimension(310, 0));

        // Header
        header = new JLabel("  \uD83D\uDCCA Játék vezérlőpult", SwingConstants.LEFT);
        header.setFont(new Font("SansSerif", Font.BOLD, 16));
        header.setForeground(ACCENT_GOLD);
        header.setBackground(BG_DARK);
        header.setOpaque(true);
        header.setBorder(new EmptyBorder(10, 5, 10, 5));
        add(header, BorderLayout.NORTH);

        // Scrollable content area
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_DARK);

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG_DARK);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Toggle the dashboard content visibility (collapse/expand).
     * Returns true if dashboard is now visible.
     */
    public boolean toggleVisibility() {
        boolean nowVisible = !scrollPane.isVisible();
        scrollPane.setVisible(nowVisible);
        header.setVisible(nowVisible);
        revalidate();
        repaint();
        return nowVisible;
    }

    public boolean isDashboardVisible() {
        return scrollPane.isVisible();
    }

    /**
     * Show a detailed inspection panel for a specific city.
     * Pass null to clear the inspection.
     */
    public void inspectCity(City city) {
        this.inspectedCity = city;
        this.inspectedIndustry = null;
        refresh();
    }

    /**
     * Show a detailed inspection panel for a specific industry.
     * Pass null to clear the inspection.
     */
    public void inspectIndustry(Industry industry) {
        this.inspectedIndustry = industry;
        this.inspectedCity = null;
        refresh();
    }

    /**
     * Clear any active inspection and go back to the overview.
     */
    public void clearInspection() {
        this.inspectedCity = null;
        this.inspectedIndustry = null;
        refresh();
    }

    public boolean hasInspection() {
        return inspectedCity != null || inspectedIndustry != null;
    }

    /**
     * Called every game tick to refresh all dashboard data.
     */
    public void refresh() {
        if (!scrollPane.isVisible()) return;

        int scrollPos = scrollPane.getVerticalScrollBar().getValue();

        contentPanel.removeAll();

        // If inspecting a specific building, show its detail view
        if (inspectedCity != null) {
            contentPanel.add(buildInspectionHeader());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildCityDetailSection(inspectedCity));
        } else if (inspectedIndustry != null) {
            contentPanel.add(buildInspectionHeader());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildIndustryDetailSection(inspectedIndustry));
        } else {
            // Normal overview
            contentPanel.add(buildFinanceSection());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildVehicleSummarySection());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildGaragesSection());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildCitiesSection());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildIndustriesSection());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildSupplyChainSection());
            contentPanel.add(Box.createVerticalStrut(6));
            contentPanel.add(buildPriceTableSection());
        }

        contentPanel.add(Box.createVerticalGlue());

        contentPanel.revalidate();
        contentPanel.repaint();

        // Restore scroll position
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPos));
    }

    // ─── Finance ────────────────────────────────────────────────────

    private JPanel buildFinanceSection() {
        JPanel panel = createSection("\uD83D\uDCB0 Pénzügyek");

        addRow(panel, "Egyenleg:", formatMoney(player.getMoney()),
                player.getMoney() >= 1000 ? ACCENT_GREEN : ACCENT_RED);

        int busCount = 0, truckCount = 0;
        for (Vehicle v : vehicles) {
            if (v instanceof Bus) busCount++;
            else if (v instanceof Truck) truckCount++;
        }
        int maintenancePerTick = busCount * 2 + truckCount * 3;
        addRow(panel, "Működő járművek:", String.valueOf(vehicles.size()), TEXT_PRIMARY);
        addRow(panel, "Becsült karbantartás:", maintenancePerTick + "$/tick", ACCENT_ORANGE);

        return panel;
    }

    // ─── Vehicles ───────────────────────────────────────────────────

    private JPanel buildVehicleSummarySection() {
        JPanel panel = createSection("\uD83D\uDE8C Járművek (" + vehicles.size() + ")");

        if (vehicles.isEmpty()) {
            addInfoRow(panel, "Még nincs jármű. Vegyél egyet!", TEXT_SECONDARY);
            return panel;
        }

        int busCount = 0, truckCount = 0;
        int busesCarrying = 0, trucksCarrying = 0;
        int totalPassengers = 0, totalGoods = 0;

        for (Vehicle v : vehicles) {
            boolean carrying = v.getCurrentCargo() != null && !v.getCurrentCargo().isEmpty();
            if (v instanceof Bus) {
                busCount++;
                if (carrying) {
                    busesCarrying++;
                    totalPassengers += v.getCurrentCargo().getAmount();
                }
            } else if (v instanceof Truck) {
                truckCount++;
                if (carrying) {
                    trucksCarrying++;
                    totalGoods += v.getCurrentCargo().getAmount();
                }
            }
        }

        addRow(panel, "\uD83D\uDE8D Buszok:", busCount + " (szállít: " + busesCarrying + ")", ACCENT_BLUE);
        if (totalPassengers > 0) {
            addRow(panel, "   Utasok a fedélzeten:", String.valueOf(totalPassengers), TEXT_SECONDARY);
        }
        addRow(panel, "\uD83D\uDE9A Teherautók:", truckCount + " (szállít: " + trucksCarrying + ")", ACCENT_ORANGE);
        if (totalGoods > 0) {
            addRow(panel, "   Áruk a fedélzeten:", String.valueOf(totalGoods), TEXT_SECONDARY);
        }

        // Individual vehicle details
        panel.add(Box.createVerticalStrut(4));
        int idx = 1;
        for (Vehicle v : vehicles) {
            String type = (v instanceof Bus) ? "Bus" : "Truck";
            String cargo = "Empty";
            if (v.getCurrentCargo() != null && !v.getCurrentCargo().isEmpty()) {
                cargo = v.getCurrentCargo().getType().getDisplayName()
                        + " x" + v.getCurrentCargo().getAmount();
            }
            String status = formatVehicleStatus(v);
            String pos = "(" + v.getCurrentTileX() + "," + v.getCurrentTileY() + ")";

            String age = String.format("%.0fs", v.getAgeSeconds());
            String maint = v.isInMaintenance()
                    ? String.format("Maint: %.0fs", v.getMaintenanceSecondsRemaining())
                    : String.format("Maint in: %.0fs", v.getSecondsUntilMaintenanceDue());

                addVehicleRow(panel,
                    " #" + idx + " " + type + " | " + status + " | " + cargo + " | kor " + age + " | " + maint + " " + pos,
                    v);
            idx++;
        }

        return panel;
    }

    // ─── Garages ───────────────────────────────────────────────────

    private JPanel buildGaragesSection() {
        List<Garage> garages = map.getGarages();
        JPanel panel = createSection("\uD83C\uDFE0 Garázsok (" + garages.size() + ")");

        if (garages.isEmpty()) {
            addInfoRow(panel, "Még nincs garázs. Építs egyet a karbantartáshoz.", TEXT_SECONDARY);
            return panel;
        }

        for (Garage g : garages) {
            if (g == null) continue;
            int homeCount = 0;
            int maintHere = 0;
            for (Vehicle v : vehicles) {
                if (v == null) continue;
                if (v.getHomeGarage() == g) homeCount++;
                if (v.isInMaintenance() && v.getMaintenanceGarage() == g) maintHere++;
            }

            panel.add(Box.createVerticalStrut(2));
                addRow(panel, "\u25A0 Garázs", "(" + g.getX() + ", " + g.getY() + ") | járművek: " + homeCount + " | karb: " + maintHere,
                    homeCount > 0 ? ACCENT_GREEN : TEXT_SECONDARY);

            for (Vehicle v : vehicles) {
                if (v == null) continue;
                if (v.getHomeGarage() != g) continue;

                String type = (v instanceof Bus) ? "Bus" : "Truck";
                String age = String.format("%.0fs", v.getAgeSeconds());
                String maint = v.isInMaintenance()
                        ? String.format("Maint: %.0fs", v.getMaintenanceSecondsRemaining())
                        : String.format("Maint in: %.0fs", v.getSecondsUntilMaintenanceDue());

                addVehicleRow(panel, "   - " + type + " | kor " + age + " | " + maint + " | " + formatVehicleStatus(v), v);
            }
        }

        return panel;
    }

    private void addVehicleRow(JPanel parent, String text, Vehicle v) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(parent.getBackground());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        Color color = (v != null && (v.isInMaintenance() || v.isGoingToMaintenance()))
                ? ACCENT_ORANGE
                : (v != null && v.hasPath()) ? ACCENT_GREEN : TEXT_SECONDARY;
        lbl.setForeground(color);

        row.add(lbl, BorderLayout.CENTER);

        if (v != null && v.isTooOld()) {
            JButton sellBtn = new JButton("Elad");
            sellBtn.setFont(new Font("SansSerif", Font.BOLD, 10));
            sellBtn.setForeground(ACCENT_RED);
            sellBtn.setBackground(parent.getBackground());
            sellBtn.setFocusPainted(false);
            sellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            sellBtn.addActionListener(e -> {
                int value = v.getSellValue();
                vehicles.remove(v);
                player.addMoney(value);
                refresh();
            });
            row.add(sellBtn, BorderLayout.EAST);
        }

        parent.add(row);
    }

    private String formatVehicleStatus(Vehicle v) {
        if (v == null) return "Ismeretlen";
        if (v.isInMaintenance()) return "Karbantartás";
        if (v.isGoingToMaintenance()) return "Szervizre";
        if (v.hasPath()) return "Úton";
        return "Áll";
    }

    // ─── Cities ─────────────────────────────────────────────────────

    private JPanel buildCitiesSection() {
        JPanel panel = createSection("\uD83C\uDFD9 Városok (" + map.getCities().size() + ")");

        for (City city : map.getCities()) {
            panel.add(Box.createVerticalStrut(2));
            addRow(panel, "\u25A0 " + city.getName(), "Népesség: " + formatNumber(city.getPopulation()), ACCENT_BLUE);
            addRow(panel, "   Várakozó utasok:",
                String.valueOf(city.getWaiting().get(ResourceType.PASSENGERS)), TEXT_SECONDARY);
            addRow(panel, "   Utashozam:",
                String.format("%.2f/s", city.getPassengersPerSecond()), TEXT_SECONDARY);

            // Demand backlog
            var backlog = city.getDemandBacklog().asUnmodifiableMap();
            if (!backlog.isEmpty()) {
                for (var entry : backlog.entrySet()) {
                    addRow(panel, "   Kereslet (" + entry.getKey().getDisplayName() + "):",
                            String.valueOf(entry.getValue()), ACCENT_ORANGE);
                }
            }

            // Goods demand rates
            var goodsRates = city.getGoodsPerSecond();
            for (var entry : goodsRates.entrySet()) {
                addRow(panel, "   " + entry.getKey().getDisplayName() + " kereslet:",
                        String.format("%.3f/s", entry.getValue()), TEXT_SECONDARY);
            }
        }

        return panel;
    }

    // ─── Industries ─────────────────────────────────────────────────

    private JPanel buildIndustriesSection() {
        JPanel panel = createSection("\uD83C\uDFED Iparok (" + map.getIndustries().size() + ")");

        for (Industry ind : map.getIndustries()) {
            panel.add(Box.createVerticalStrut(2));

            String prodPercent = String.format("%.0f%%", ind.getProductivity() * 100);
            Color prodColor = ind.getProductivity() >= 0.9 ? ACCENT_GREEN
                    : ind.getProductivity() >= 0.6 ? ACCENT_ORANGE : ACCENT_RED;

            addRow(panel, "\u25A0 " + ind.getName(),
                    ind.getIndustryType().name() + " [" + prodPercent + "]", prodColor);

            // Bemenetek
            var inputs = ind.getProfile().getInputsPerUnit();
            if (inputs.isEmpty()) {
                addRow(panel, "   Bemenetek:", "Nincs (nyersanyag-termelő)", TEXT_SECONDARY);
            } else {
                for (var e : inputs.entrySet()) {
                    int stored = ind.getStorage().get(e.getKey());
                    addRow(panel, "   Szükséges " + e.getKey().getDisplayName() + ":",
                            e.getValue() + "/unit (raktáron: " + stored + ")",
                            stored > 0 ? ACCENT_GREEN : ACCENT_RED);
                }
            }

            // Kimenetek
            var outputs = ind.getProfile().getOutputsPerUnit();
            for (var e : outputs.entrySet()) {
                int stored = ind.getStorage().get(e.getKey());
                addRow(panel, "   Termel " + e.getKey().getDisplayName() + ":",
                        e.getValue() + "/unit (raktáron: " + stored + ")",
                        stored > 0 ? ACCENT_GREEN : TEXT_SECONDARY);
            }

            addRow(panel, "   Alapsebesség:",
                    String.format("%.2f units/s", ind.getProfile().getBaseUnitsPerSecond()), TEXT_SECONDARY);
        }

        return panel;
    }

    // ─── Supply Chain Overview ───────────────────────────────────────

    private JPanel buildSupplyChainSection() {
        JPanel panel = createSection("\uD83D\uDD17 Ellátási lánc");

        addInfoRow(panel, "FARM \u2192 Wheat", TEXT_SECONDARY);
        addInfoRow(panel, "RANCH \u2192 Meat", TEXT_SECONDARY);
        addInfoRow(panel, "Wheat \u2192 BAKERY \u2192 Bread", TEXT_SECONDARY);
        addInfoRow(panel, "Meat \u2192 PATTY PLANT \u2192 Meat Patty", TEXT_SECONDARY);
        addInfoRow(panel, "Bread + Meat Patty \u2192 BURGER FACTORY \u2192 \uD83C\uDF54", TEXT_SECONDARY);
        panel.add(Box.createVerticalStrut(4));
        addInfoRow(panel, "Szállíts hamburgereket a városokba a maximális profitért!", ACCENT_GOLD);

        return panel;
    }

    // ─── Inspection Header (back button) ─────────────────────────────

    private JPanel buildInspectionHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(4, 6, 4, 6));

        JButton backBtn = new JButton("\u2190 Vissza az áttekintéshez");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        backBtn.setForeground(ACCENT_BLUE);
        backBtn.setBackground(BG_SECTION);
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(true);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> clearInspection());

        panel.add(backBtn, BorderLayout.WEST);
        return panel;
    }

    // ─── City Detail View ────────────────────────────────────────────

    private JPanel buildCityDetailSection(City city) {
        JPanel panel = createSection("\uD83C\uDFD9 " + city.getName());

        // General info
        addRow(panel, "Népesség:", formatNumber(city.getPopulation()), ACCENT_BLUE);
        addRow(panel, "Hely:", "(" + city.getOriginX() + ", " + city.getOriginY() + ")", TEXT_SECONDARY);
        addRow(panel, "Méret:", city.getWidth() + " x " + city.getHeight() + " tiles", TEXT_SECONDARY);

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDE8D Utasok");

        int passengersWaiting = city.getWaiting().get(ResourceType.PASSENGERS);
        Color waitColor = passengersWaiting > 20 ? ACCENT_RED
                : passengersWaiting > 5 ? ACCENT_ORANGE : ACCENT_GREEN;
        addRow(panel, "Várakozó:", String.valueOf(passengersWaiting), waitColor);
        addRow(panel, "Generálási ráta:", String.format("%.3f /s", city.getPassengersPerSecond()), TEXT_SECONDARY);

        if (passengersWaiting > 20) {
            addInfoRow(panel, "  \u26A0 Utasok torlódnak! Adj hozzá több buszt.", ACCENT_RED);
        } else if (passengersWaiting == 0) {
            addInfoRow(panel, "  \u2713 Nincsenek várakozó utasok.", ACCENT_GREEN);
        }

        // Revenue info for passengers
        int passengerRevenue = ResourcePrices.revenuePerUnit(ResourceType.PASSENGERS);
        addRow(panel, "Passenger revenue:", passengerRevenue + "$ /unit", ACCENT_GREEN);

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDCE6 Goods Demand");

        // Demand backlog
        var backlog = city.getDemandBacklog().asUnmodifiableMap();
        if (backlog.isEmpty()) {
            addInfoRow(panel, "  No pending demand yet.", TEXT_SECONDARY);
        } else {
            for (var entry : backlog.entrySet()) {
                int amount = entry.getValue();
                Color demandColor = amount > 10 ? ACCENT_RED
                        : amount > 3 ? ACCENT_ORANGE : ACCENT_GREEN;
                addRow(panel, "  " + entry.getKey().getDisplayName() + " backlog:",
                        String.valueOf(amount), demandColor);
                int rev = ResourcePrices.revenuePerUnit(entry.getKey());
                addRow(panel, "    Revenue if delivered:", amount * rev + "$ (total)", ACCENT_GREEN);
            }
        }

        // Goods demand rates
        var goodsRates = city.getGoodsPerSecond();
        for (var entry : goodsRates.entrySet()) {
            addRow(panel, "  " + entry.getKey().getDisplayName() + " demand rate:",
                    String.format("%.3f /s", entry.getValue()), TEXT_SECONDARY);
        }

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDE9A Vehicles Serving This City");

        int busesServing = 0;
        int trucksServing = 0;
        for (Vehicle v : vehicles) {
            if (!v.hasPath()) continue;
            // Check if vehicle's path endpoints are adjacent to this city
            if (isVehicleServingCity(v, city)) {
                if (v instanceof Bus) busesServing++;
                else if (v instanceof Truck) trucksServing++;
            }
        }
        if (busesServing == 0 && trucksServing == 0) {
            addInfoRow(panel, "  No vehicles assigned to this city.", ACCENT_ORANGE);
        } else {
            addRow(panel, "  Buses:", String.valueOf(busesServing), ACCENT_BLUE);
            addRow(panel, "  Trucks:", String.valueOf(trucksServing), ACCENT_ORANGE);
        }

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDCA1 Tips");
        addInfoRow(panel, "  \u2022 Send buses between cities for passenger $", TEXT_SECONDARY);
        addInfoRow(panel, "  \u2022 Deliver hamburgers here for " +
                ResourcePrices.revenuePerUnit(ResourceType.HAMBURGER) + "$/unit", TEXT_SECONDARY);

        return panel;
    }

    // ─── Industry Detail View ────────────────────────────────────────

    private JPanel buildIndustryDetailSection(Industry ind) {
        JPanel panel = createSection("\uD83C\uDFED " + ind.getName());

        // General info
        String prodPercent = String.format("%.0f%%", ind.getProductivity() * 100);
        Color prodColor = ind.getProductivity() >= 0.9 ? ACCENT_GREEN
                : ind.getProductivity() >= 0.6 ? ACCENT_ORANGE : ACCENT_RED;

        addRow(panel, "Típus:", ind.getIndustryType().name(), ACCENT_BLUE);
        addRow(panel, "Hely:", "(" + ind.getOriginX() + ", " + ind.getOriginY() + ")", TEXT_SECONDARY);
        addRow(panel, "Méret:", ind.getWidth() + " x " + ind.getHeight() + " tiles", TEXT_SECONDARY);
        addRow(panel, "Termelékenység:", prodPercent, prodColor);
        addRow(panel, "Alapsebesség:", String.format("%.2f units/s", ind.getProfile().getBaseUnitsPerSecond()), TEXT_SECONDARY);
        addRow(panel, "Hatékony ráta:", String.format("%.2f units/s",
                ind.getProfile().getBaseUnitsPerSecond() * ind.getProductivity()), prodColor);

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDCE5 Bemenetek (egy egységhez)");

        var inputs = ind.getProfile().getInputsPerUnit();
        if (inputs.isEmpty()) {
            addInfoRow(panel, "  Nincs — ez nyersanyag-termelő!", ACCENT_GREEN);
        } else {
            for (var e : inputs.entrySet()) {
                int stored = ind.getStorage().get(e.getKey());
                Color storedColor = stored >= 10 ? ACCENT_GREEN
                        : stored > 0 ? ACCENT_ORANGE : ACCENT_RED;
                addRow(panel, "  " + e.getKey().getDisplayName() + " szükséges:", String.valueOf(e.getValue()), TEXT_PRIMARY);
                addRow(panel, "    Raktáron:", String.valueOf(stored), storedColor);

                if (stored == 0) {
                    addInfoRow(panel, "    \u26A0 Elfogyott! A termelés leáll.", ACCENT_RED);
                } else {
                    int unitsAvailable = stored / e.getValue();
                    addRow(panel, "    Előállítható egységek:", unitsAvailable + " units", TEXT_SECONDARY);
                }
            }
        }

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDCE4 Kimenetek (egy egységhez)");

        var outputs = ind.getProfile().getOutputsPerUnit();
        if (outputs.isEmpty()) {
            addInfoRow(panel, "  Nincs — ennek az iparnak nincs kimenete.", TEXT_SECONDARY);
        } else {
            for (var e : outputs.entrySet()) {
                int stored = ind.getStorage().get(e.getKey());
                Color storedColor = stored > 0 ? ACCENT_GREEN : TEXT_SECONDARY;
                addRow(panel, "  " + e.getKey().getDisplayName() + " termelt:", String.valueOf(e.getValue()), TEXT_PRIMARY);
                addRow(panel, "    Raktáron:", String.valueOf(stored), storedColor);
                int rev = ResourcePrices.revenuePerUnit(e.getKey());
                addRow(panel, "    Egységár:", rev + "$/unit", ACCENT_GREEN);
                if (stored > 0) {
                    addRow(panel, "    Összérték raktárban:", stored * rev + "$/unit", ACCENT_GOLD);
                }
            }
        }

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDE9A Vehicles Serving This Industry");

        int trucksServing = 0;
        for (Vehicle v : vehicles) {
            if (!v.hasPath()) continue;
            if (v instanceof Truck && isVehicleServingIndustry(v, ind)) {
                trucksServing++;
            }
        }
        if (trucksServing == 0) {
            addInfoRow(panel, "  No trucks assigned to this industry.", ACCENT_ORANGE);
        } else {
            addRow(panel, "  Trucks:", String.valueOf(trucksServing), ACCENT_ORANGE);
        }

        panel.add(Box.createVerticalStrut(6));
        addSectionDivider(panel, "\uD83D\uDCA1 Tips");

        switch (ind.getIndustryType()) {
            case FARM -> {
                addInfoRow(panel, "  \u2022 Termel búzát — szállíts egy pékhez.", TEXT_SECONDARY);
            }
            case RANCH -> {
                addInfoRow(panel, "  \u2022 Termel húst — szállíts egy húsgyárba.", TEXT_SECONDARY);
            }
            case BAKERY -> {
                addInfoRow(panel, "  \u2022 Szükséges búza, termel kenyeret.", TEXT_SECONDARY);
                addInfoRow(panel, "  \u2022 Szállíts kenyeret egy hamburgergyárba.", TEXT_SECONDARY);
            }
            case PATTY_PLANT -> {
                addInfoRow(panel, "  \u2022 Szükséges hús, termel húspogácsát.", TEXT_SECONDARY);
                addInfoRow(panel, "  \u2022 Szállíts pogácsákat egy hamburgergyárba.", TEXT_SECONDARY);
            }
            case BURGER_FACTORY -> {
                addInfoRow(panel, "  \u2022 Szükséges kenyér + húspogácsák.", TEXT_SECONDARY);
                addInfoRow(panel, "  \u2022 Termel hamburgereket — szállíts a városoknak!", TEXT_SECONDARY);
                addInfoRow(panel, "  \u2022 A hamburgerek a legnagyobb bevételt adják ("
                        + ResourcePrices.revenuePerUnit(ResourceType.HAMBURGER) + "$/unit).", ACCENT_GOLD);
            }
            case FACTORY -> {
                addInfoRow(panel, "  \u2022 Általános gyár — nincs beállított termelés.", TEXT_SECONDARY);
            }
        }

        return panel;
    }

    // ─── Vehicle-Building proximity checks ───────────────────────────

    private boolean isVehicleServingCity(Vehicle v, City city) {
        return isVehicleEndpointAdjacentTo(v, city.getOriginX(), city.getOriginY(),
                city.getWidth(), city.getHeight());
    }

    private boolean isVehicleServingIndustry(Vehicle v, Industry ind) {
        return isVehicleEndpointAdjacentTo(v, ind.getOriginX(), ind.getOriginY(),
                ind.getWidth(), ind.getHeight());
    }

    private boolean isVehicleEndpointAdjacentTo(Vehicle v, int ox, int oy, int w, int h) {
        if (v == null || !v.hasPath()) return false;
        try {
            var pathField = Vehicle.class.getDeclaredField("pathTiles");
            pathField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<int[]> path = (List<int[]>) pathField.get(v);
            if (path == null || path.size() < 2) return false;

            int[] start = path.get(0);
            int[] end = path.get(path.size() - 1);

            return isTileAdjacentToArea(start[0], start[1], ox, oy, w, h)
                    || isTileAdjacentToArea(end[0], end[1], ox, oy, w, h);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTileAdjacentToArea(int tx, int ty, int ox, int oy, int w, int h) {
        // Check if tile (tx,ty) is directly adjacent to the rectangle [ox,oy,w,h]
        for (int x = ox; x < ox + w; x++) {
            if ((tx == x && ty == oy - 1) || (tx == x && ty == oy + h)) return true;
        }
        for (int y = oy; y < oy + h; y++) {
            if ((tx == ox - 1 && ty == y) || (tx == ox + w && ty == y)) return true;
        }
        return false;
    }

    private void addSectionDivider(JPanel parent, String label) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(ACCENT_GOLD);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(2, 0, 2, 0));
        parent.add(lbl);
    }

    // ─── Price Table ────────────────────────────────────────────────

    private JPanel buildPriceTableSection() {
        JPanel panel = createSection("\uD83D\uDCB5 Bevétel szállításonként");

        for (ResourceType type : ResourceType.values()) {
            int price = ResourcePrices.revenuePerUnit(type);
            addRow(panel, "  " + type.getDisplayName() + ":", price + "$ /unit", ACCENT_GREEN);
        }


        return panel;
    }



    // ─── Helpers ────────────────────────────────────────────────────

    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_SECTION);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 70), 1),
                title
        );
        border.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        border.setTitleColor(ACCENT_GOLD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                border,
                new EmptyBorder(4, 6, 6, 6)
        ));

        return panel;
    }

    private void addRow(JPanel parent, String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(parent.getBackground());
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(TEXT_PRIMARY);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 11));
        val.setForeground(valueColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        parent.add(row);
    }

    private void addInfoRow(JPanel parent, String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
    }

    private String formatMoney(int amount) {
        if (amount >= 1_000_000) return String.format("%.1fM$", amount / 1_000_000.0);
        if (amount >= 10_000) return String.format("%.1fK$", amount / 1_000.0);
        return amount + "$";
    }

    private String formatNumber(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 10_000) return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}
