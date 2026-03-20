package game.ui;

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
        JLabel header = new JLabel("  \uD83D\uDCCA Game Dashboard", SwingConstants.LEFT);
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
     * Called every game tick to refresh all dashboard data.
     */
    public void refresh() {
        int scrollPos = scrollPane.getVerticalScrollBar().getValue();

        contentPanel.removeAll();
        contentPanel.add(buildFinanceSection());
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(buildVehicleSummarySection());
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(buildCitiesSection());
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(buildIndustriesSection());
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(buildSupplyChainSection());
        contentPanel.add(Box.createVerticalStrut(6));
        contentPanel.add(buildPriceTableSection());
        contentPanel.add(Box.createVerticalGlue());

        contentPanel.revalidate();
        contentPanel.repaint();

        // Restore scroll position
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(scrollPos));
    }

    // ─── Finance ────────────────────────────────────────────────────

    private JPanel buildFinanceSection() {
        JPanel panel = createSection("\uD83D\uDCB0 Finances");

        addRow(panel, "Balance:", formatMoney(player.getMoney()),
                player.getMoney() >= 1000 ? ACCENT_GREEN : ACCENT_RED);

        int busCount = 0, truckCount = 0;
        for (Vehicle v : vehicles) {
            if (v instanceof Bus) busCount++;
            else if (v instanceof Truck) truckCount++;
        }
        int maintenancePerTick = busCount * 2 + truckCount * 3;
        addRow(panel, "Vehicles running:", String.valueOf(vehicles.size()), TEXT_PRIMARY);
        addRow(panel, "Est. maintenance:", maintenancePerTick + "$/tick", ACCENT_ORANGE);

        return panel;
    }

    // ─── Vehicles ───────────────────────────────────────────────────

    private JPanel buildVehicleSummarySection() {
        JPanel panel = createSection("\uD83D\uDE8C Vehicles (" + vehicles.size() + ")");

        if (vehicles.isEmpty()) {
            addInfoRow(panel, "No vehicles yet. Buy one!", TEXT_SECONDARY);
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

        addRow(panel, "\uD83D\uDE8D Buses:", busCount + " (carrying: " + busesCarrying + ")", ACCENT_BLUE);
        if (totalPassengers > 0) {
            addRow(panel, "   Passengers on board:", String.valueOf(totalPassengers), TEXT_SECONDARY);
        }
        addRow(panel, "\uD83D\uDE9A Trucks:", truckCount + " (carrying: " + trucksCarrying + ")", ACCENT_ORANGE);
        if (totalGoods > 0) {
            addRow(panel, "   Goods on board:", String.valueOf(totalGoods), TEXT_SECONDARY);
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
            String status = v.hasPath() ? "En route" : "Idle";
            String pos = "(" + v.getCurrentTileX() + "," + v.getCurrentTileY() + ")";

            addInfoRow(panel, " #" + idx + " " + type + " | " + status + " | " + cargo + " " + pos,
                    v.hasPath() ? ACCENT_GREEN : TEXT_SECONDARY);
            idx++;
        }

        return panel;
    }

    // ─── Cities ─────────────────────────────────────────────────────

    private JPanel buildCitiesSection() {
        JPanel panel = createSection("\uD83C\uDFD9 Cities (" + map.getCities().size() + ")");

        for (City city : map.getCities()) {
            panel.add(Box.createVerticalStrut(2));
            addRow(panel, "\u25A0 " + city.getName(), "Pop: " + formatNumber(city.getPopulation()), ACCENT_BLUE);
            addRow(panel, "   Passengers waiting:",
                    String.valueOf(city.getWaiting().get(ResourceType.PASSENGERS)), TEXT_SECONDARY);
            addRow(panel, "   Passenger rate:",
                    String.format("%.2f/s", city.getPassengersPerSecond()), TEXT_SECONDARY);

            // Demand backlog
            var backlog = city.getDemandBacklog().asUnmodifiableMap();
            if (!backlog.isEmpty()) {
                for (var entry : backlog.entrySet()) {
                    addRow(panel, "   Demand (" + entry.getKey().getDisplayName() + "):",
                            String.valueOf(entry.getValue()), ACCENT_ORANGE);
                }
            }

            // Goods demand rates
            var goodsRates = city.getGoodsPerSecond();
            for (var entry : goodsRates.entrySet()) {
                addRow(panel, "   " + entry.getKey().getDisplayName() + " demand:",
                        String.format("%.3f/s", entry.getValue()), TEXT_SECONDARY);
            }
        }

        return panel;
    }

    // ─── Industries ─────────────────────────────────────────────────

    private JPanel buildIndustriesSection() {
        JPanel panel = createSection("\uD83C\uDFED Industries (" + map.getIndustries().size() + ")");

        for (Industry ind : map.getIndustries()) {
            panel.add(Box.createVerticalStrut(2));

            String prodPercent = String.format("%.0f%%", ind.getProductivity() * 100);
            Color prodColor = ind.getProductivity() >= 0.9 ? ACCENT_GREEN
                    : ind.getProductivity() >= 0.6 ? ACCENT_ORANGE : ACCENT_RED;

            addRow(panel, "\u25A0 " + ind.getName(),
                    ind.getIndustryType().name() + " [" + prodPercent + "]", prodColor);

            // Inputs
            var inputs = ind.getProfile().getInputsPerUnit();
            if (inputs.isEmpty()) {
                addRow(panel, "   Inputs:", "None (raw producer)", TEXT_SECONDARY);
            } else {
                for (var e : inputs.entrySet()) {
                    int stored = ind.getStorage().get(e.getKey());
                    addRow(panel, "   Needs " + e.getKey().getDisplayName() + ":",
                            e.getValue() + "/unit (stored: " + stored + ")",
                            stored > 0 ? ACCENT_GREEN : ACCENT_RED);
                }
            }

            // Outputs
            var outputs = ind.getProfile().getOutputsPerUnit();
            for (var e : outputs.entrySet()) {
                int stored = ind.getStorage().get(e.getKey());
                addRow(panel, "   Produces " + e.getKey().getDisplayName() + ":",
                        e.getValue() + "/unit (stored: " + stored + ")",
                        stored > 0 ? ACCENT_GREEN : TEXT_SECONDARY);
            }

            addRow(panel, "   Base rate:",
                    String.format("%.2f units/s", ind.getProfile().getBaseUnitsPerSecond()), TEXT_SECONDARY);
        }

        return panel;
    }

    // ─── Supply Chain Overview ───────────────────────────────────────

    private JPanel buildSupplyChainSection() {
        JPanel panel = createSection("\uD83D\uDD17 Supply Chain");

        addInfoRow(panel, "FARM \u2192 Wheat", TEXT_SECONDARY);
        addInfoRow(panel, "RANCH \u2192 Meat", TEXT_SECONDARY);
        addInfoRow(panel, "Wheat \u2192 BAKERY \u2192 Bread", TEXT_SECONDARY);
        addInfoRow(panel, "Meat \u2192 PATTY PLANT \u2192 Meat Patty", TEXT_SECONDARY);
        addInfoRow(panel, "Bread + Meat Patty \u2192 BURGER FACTORY \u2192 \uD83C\uDF54", TEXT_SECONDARY);
        panel.add(Box.createVerticalStrut(4));
        addInfoRow(panel, "Deliver Hamburgers to cities for max profit!", ACCENT_GOLD);

        return panel;
    }

    // ─── Price Table ────────────────────────────────────────────────

    private JPanel buildPriceTableSection() {
        JPanel panel = createSection("\uD83D\uDCB5 Revenue per Delivery");

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
