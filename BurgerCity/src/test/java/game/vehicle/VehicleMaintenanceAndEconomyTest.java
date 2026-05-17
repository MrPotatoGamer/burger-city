package game.vehicle;

import game.building.Garage;
import game.core.Player;
import game.map.City;
import game.map.Industry;
import game.map.IndustryType;
import game.map.Map;
import game.resource.Resource;
import game.resource.ResourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleMaintenanceAndEconomyTest {

    private static Map newRoadMap(int w, int h) {
        Map map = new Map(w, h);
        map.initGrassForLoad();
        return map;
    }

    @Test
    void update_whenInMaintenance_countsDownAndRejoinsRoute() {
        Map map = newRoadMap(5, 2);
        assertTrue(map.buildRoad(0, 0));
        assertTrue(map.buildRoad(1, 0));
        assertTrue(map.buildRoad(2, 0));

        Truck t = new Truck();
        t.spawnAt(0, 0);

        t.inMaintenance = true;
        t.maintenanceSecondsRemaining = 0.5;
        t.secondsSinceMaintenance = 999.0;

        // After maintenance, join at (2,0)
        t.routePathTiles = List.of(new int[]{2, 0}, new int[]{1, 0}, new int[]{0, 0});

        t.update(map, 1.0);

        assertFalse(t.inMaintenance);
        assertEquals(0.0, t.maintenanceSecondsRemaining, 1e-9);
        assertEquals(0.0, t.secondsSinceMaintenance, 1e-9);
        assertTrue(t.rejoiningRoute);
        assertEquals(2, t.rejoinRouteAtX);
        assertEquals(0, t.rejoinRouteAtY);
        assertTrue(t.hasPath());
    }

    @Test
    void arriveAtTarget_whenArriveAtMaintenanceDestination_entersMaintenanceAndClearsPath() {
        Map map = newRoadMap(4, 1);
        assertTrue(map.buildRoad(0, 0));
        assertTrue(map.buildRoad(1, 0));

        Truck t = new Truck();
        t.spawnAt(0, 0);
        t.setPath(List.of(new int[]{0, 0}, new int[]{1, 0}));

        t.goingToMaintenance = true;
        t.maintenanceDestRoadX = 1;
        t.maintenanceDestRoadY = 0;
        t.targetTileX = 1;
        t.targetTileY = 0;

        t.arriveAtTarget(map);

        assertFalse(t.goingToMaintenance);
        assertTrue(t.inMaintenance);
        assertEquals(5.0, t.maintenanceSecondsRemaining, 1e-9);
        assertFalse(t.hasPath());
    }

    @Test
    void processArrivalEconomy_busLoadsPassengersAtCityWhenEmpty() {
        Map map = newRoadMap(12, 12);

        City city = new City("C", 2, 2, 3, 3);
        city.getWaiting().add(ResourceType.PASSENGERS, 10);
        map.addCityForLoad(city);

        // Road adjacent to city footprint
        assertTrue(map.buildRoad(1, 3));

        Bus bus = new Bus();
        bus.spawnAt(1, 2);
        bus.setPath(List.of(new int[]{1, 3}));

        // Simulate arrival onto (1,3)
        bus.targetTileX = 1;
        bus.targetTileY = 3;
        bus.arriveAtTarget(map);

        Player player = new Player(0);
        bus.processArrivalEconomy(map, player);

        assertNotNull(bus.getCurrentCargo());
        assertEquals(ResourceType.PASSENGERS, bus.getCurrentCargo().getType());
        assertEquals(10, bus.getCurrentCargo().getAmount());
        assertEquals(0, city.getWaiting().get(ResourceType.PASSENGERS));
    }

    @Test
    void processArrivalEconomy_truckDeliversGoodsToIndustryAndGetsPaid() {
        Map map = newRoadMap(12, 12);

        Industry ind = new Industry("Patty", IndustryType.PATTY_PLANT, 5, 5, 2, 2);
        map.addIndustryForLoad(ind);

        assertTrue(map.buildRoad(4, 5)); // adjacent to industry

        Truck t = new Truck();
        t.spawnAt(4, 4);
        t.setPath(List.of(new int[]{4, 5}));

        t.currentCargo = new Resource(ResourceType.MEAT, 4);

        t.targetTileX = 4;
        t.targetTileY = 5;
        t.arriveAtTarget(map);

        Player player = new Player(0);
        t.processArrivalEconomy(map, player);

        assertNull(t.getCurrentCargo());
        assertEquals(4, ind.getStorage().get(ResourceType.MEAT));
        // MEAT revenue is 8 per unit
        assertEquals(32, player.getMoney());
    }

    @Test
    void processArrivalEconomy_truckPicksUpFromIndustryWhenEmptyAndDeliverableToNextCity() {
        Map map = newRoadMap(20, 10);

        Industry farm = new Industry("Farm", IndustryType.FARM, 3, 3, 2, 2);
        farm.getStorage().add(ResourceType.WHEAT, 7);
        map.addIndustryForLoad(farm);

        City city = new City("City", 12, 2, 3, 3);
        city.getDemandBacklog().add(ResourceType.WHEAT, 1);
        map.addCityForLoad(city);

        // Build a simple connected road from farm-adjacent to city-adjacent
        // Farm at (3..4,3..4) -> adjacent road at (3,2)
        // City at (12..14,2..4) -> adjacent road at (11,2)
        for (int x = 2; x <= 11; x++) {
            assertTrue(map.buildRoad(x, 2));
        }

        Truck t = new Truck();
        t.spawnAt(3, 1);
        t.setPath(List.of(
                new int[]{3, 2},
                new int[]{4, 2},
                new int[]{5, 2},
                new int[]{6, 2},
                new int[]{7, 2},
                new int[]{8, 2},
                new int[]{9, 2},
                new int[]{10, 2},
                new int[]{11, 2}
        ));

        // Simulate arrival onto the farm-adjacent road tile
        t.targetTileX = 3;
        t.targetTileY = 2;
        t.arriveAtTarget(map);

        Player player = new Player(0);
        t.processArrivalEconomy(map, player);

        assertNotNull(t.getCurrentCargo(), "Expected pickup from farm outputs");
        assertEquals(ResourceType.WHEAT, t.getCurrentCargo().getType());
        assertTrue(t.getCurrentCargo().getAmount() > 0);
        assertTrue(farm.getStorage().get(ResourceType.WHEAT) < 7);
    }
}
