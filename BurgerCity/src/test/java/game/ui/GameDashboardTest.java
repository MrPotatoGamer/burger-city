package game.ui;

import game.core.Player;
import game.map.City;
import game.map.Map;
import game.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameDashboardTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void toggleVisibility_updatesVisibilityFlags() {
        Player player = new Player(1000);
        Map map = new Map(5, 5);
        map.initGrassForLoad();
        List<Vehicle> vehicles = new ArrayList<>();

        GameDashboard dash = new GameDashboard(player, map, vehicles);
        assertTrue(dash.isDashboardVisible());

        boolean nowVisible = dash.toggleVisibility();
        assertFalse(nowVisible);
        assertFalse(dash.isDashboardVisible());

        nowVisible = dash.toggleVisibility();
        assertTrue(nowVisible);
        assertTrue(dash.isDashboardVisible());
    }

    @Test
    void inspection_stateMachine_setsAndClearsInspection_withoutThrowing() {
        Player player = new Player(1000);
        Map map = new Map(10, 10);
        map.initGrassForLoad();
        List<Vehicle> vehicles = new ArrayList<>();
        GameDashboard dash = new GameDashboard(player, map, vehicles);

        assertFalse(dash.hasInspection());

        City city = new City("TestCity", 1, 1, 3, 3);
        assertDoesNotThrow(() -> dash.inspectCity(city));
        assertTrue(dash.hasInspection());

        assertDoesNotThrow(dash::clearInspection);
        assertFalse(dash.hasInspection());

        assertDoesNotThrow(dash::refresh);
    }
}
