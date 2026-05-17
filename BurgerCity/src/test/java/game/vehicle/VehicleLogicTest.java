package game.vehicle;

import game.building.Garage;
import game.building.TrafficLight;
import game.map.Map;
import game.map.TileType;
import game.save.GameSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VehicleLogicTest {

    private static Map newRoadMap(int w, int h) {
        Map map = new Map(w, h);
        map.initGrassForLoad();
        return map;
    }

    @Test
    void maintenanceInterval_decreasesWithAge_andIsClamped() {
        Truck t = new Truck();

        t.ageSeconds = 0;
        assertEquals(120.0, t.getMaintenanceIntervalSeconds(), 1e-9);

        t.ageSeconds = 1000.0;
        assertEquals(30.0, t.getMaintenanceIntervalSeconds(), 1e-9);

        t.ageSeconds = 100.0;
        assertTrue(t.getMaintenanceIntervalSeconds() < 120.0);
        assertTrue(t.getMaintenanceIntervalSeconds() >= 30.0);
    }

    @Test
    void sellValue_isHalfPurchasePrice() {
        Truck t = new Truck();
        t.setPurchasePrice(0);
        assertEquals(0, t.getSellValue());

        t.setPurchasePrice(100);
        assertEquals(50, t.getSellValue());

        t.setPurchasePrice(101);
        assertEquals(50, t.getSellValue());
    }

    @Test
    void tooOld_thresholdIs600Seconds() {
        Truck t = new Truck();
        t.ageSeconds = 599.99;
        assertFalse(t.isTooOld());
        t.ageSeconds = 600.0;
        assertTrue(t.isTooOld());
    }

    @Test
    void pathsCross_detectsPerpendicularMovement() {
        Truck t = new Truck();
        assertTrue(t.pathsCross(1, 2));
        assertTrue(t.pathsCross(3, 4));
        assertFalse(t.pathsCross(1, 3));
        assertFalse(t.pathsCross(2, 4));
        assertFalse(t.pathsCross(0, 2));
    }

    @Test
    void chooseNextTarget_stopsAtRedTrafficLight() {
        Map map = newRoadMap(6, 3);
        assertTrue(map.buildRoad(1, 1));
        assertTrue(map.buildRoad(2, 1));

        Truck t = new Truck();
        t.spawnAt(1, 1);
        t.setPath(List.of(new int[]{1, 1}, new int[]{2, 1}));

        TrafficLight light = new TrafficLight(2, 1);
        // MAIN_GREEN means East/West is red.
        light.restore("MAIN_GREEN", 0.0, 5.0, 5.0);

        t.chooseNextTarget(map, List.of(t), List.of(light));

        assertNull(t.targetTileX);
        assertNull(t.targetTileY);
    }

    @Test
    void chooseNextTarget_waitsWhenIntersectionHasCrossingTraffic() {
        Map map = newRoadMap(4, 4);

        // T-intersection at (1,1)
        assertTrue(map.buildRoad(1, 1));
        assertTrue(map.buildRoad(1, 0));
        assertTrue(map.buildRoad(1, 2));
        assertTrue(map.buildRoad(0, 1));

        assertEquals(TileType.ROAD, map.getTile(1, 1).getType());

        Truck a = new Truck();
        a.spawnAt(0, 1);
        a.setPath(List.of(new int[]{0, 1}, new int[]{1, 1}));

        Truck b = new Truck();
        b.spawnAt(1, 1);
        b.currentDirection = 1; // North/South

        a.chooseNextTarget(map, List.of(a, b), null);

        assertNull(a.targetTileX);
        assertNull(a.targetTileY);
    }

    @Test
    void adjustSpeedForTraffic_stopsWhenVehicleBlocksTargetTileSameDirection() {
        Map map = newRoadMap(3, 1);
        assertTrue(map.buildRoad(0, 0));
        assertTrue(map.buildRoad(1, 0));

        Truck a = new Truck();
        a.spawnAt(0, 0);
        a.targetTileX = 1;
        a.targetTileY = 0;
        a.currentDirection = 2; // East

        Truck b = new Truck();
        b.spawnAt(1, 0);
        b.currentDirection = 2; // East

        a.adjustSpeedForTraffic(List.of(a, b));
        assertEquals(0.0, a.effectiveSpeed, 1e-9);
    }

    @Test
    void importSaveData_resolvesHomeGarageFromMapCoordinates() {
        Map map = newRoadMap(6, 6);
        assertTrue(map.buildRoad(2, 1));
        assertTrue(map.buildBuilding(2, 2, new Garage(2, 2)));

        Truck t = new Truck();
        GameSnapshot.VehicleData data = new GameSnapshot.VehicleData(
                "Truck",
                0.0,
                0.0,
                2,
                1,
                null,
                null,
                null,
                null,
                0,
                0,
                0,
                List.of(new GameSnapshot.IntPair(2, 1)),
                0,
                true,
                null,
                null,
                List.of(),
                false,
                null,
                null,
                0.0,
                0.0,
                false,
                false,
                0.0,
                null,
                null,
                2,
                2,
                null,
                null,
                0
        );

        t.importSaveData(data, map);

        assertNotNull(t.getHomeGarage());
        assertEquals(2, t.getHomeGarage().getX());
        assertEquals(2, t.getHomeGarage().getY());
    }
}
