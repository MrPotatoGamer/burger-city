package game.save;

import game.building.Garage;
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
import game.resource.Resource;
import game.resource.ResourceType;
import game.save.json.Json;
import game.vehicle.Truck;
import game.vehicle.Vehicle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SaveManagerRoundTripTest {

    private static Map newEmptyMap(int w, int h) {
        Map map = new Map(w, h);
        map.initGrassForLoad();
        return map;
    }

    private static TimeManager configuredTimeManager() {
        TimeManager tm = new TimeManager();
        tm.setVeryFast();
        tm.update(1.5);
        tm.update(0.5);
        return tm;
    }

    @SuppressWarnings("unchecked")
    private static void overwriteSavedAtEpochMillis(Path saveFile, long epochMillis) throws Exception {
        String raw = Files.readString(saveFile, StandardCharsets.UTF_8);
        Object parsed = Json.parse(raw);
        assertTrue(parsed instanceof java.util.Map<?, ?>, "Expected save JSON root object");
        java.util.Map<String, Object> obj = (java.util.Map<String, Object>) parsed;
        obj.put("savedAtEpochMillis", epochMillis);
        Files.writeString(saveFile, Json.stringify(obj), StandardCharsets.UTF_8);
    }

    @Test
    void createSave_rejectsDuplicateNameIgnoringCaseAndWhitespace(@TempDir Path tmp) throws Exception {
        SaveManager sm = new SaveManager(tmp);

        Map map = newEmptyMap(8, 8);
        Player player = new Player(100);
        TimeManager tm = configuredTimeManager();

        sm.createSave("My Save", map, player, tm, List.of(), List.of());

        assertThrows(IllegalArgumentException.class, () ->
                sm.createSave("  my save  ", map, player, tm, List.of(), List.of())
        );
    }

    @Test
    void listSaves_returnsNewestFirst_basedOnMetadata(@TempDir Path tmp) throws Exception {
        SaveManager sm = new SaveManager(tmp);

        Map map = newEmptyMap(6, 6);
        Player player = new Player(0);
        TimeManager tm = new TimeManager();

        SaveGame a = sm.createSave("a", map, player, tm, List.of(), List.of());
        SaveGame b = sm.createSave("b", map, player, tm, List.of(), List.of());

        // Force deterministic ordering regardless of filesystem listing order.
        overwriteSavedAtEpochMillis(tmp.resolve(a.getFileName()), 1_111L);
        overwriteSavedAtEpochMillis(tmp.resolve(b.getFileName()), 2_222L);

        List<SaveGame> saves = sm.listSaves();
        assertEquals(2, saves.size());
        assertEquals("b", saves.get(0).getSaveName());
        assertEquals("a", saves.get(1).getSaveName());
        assertTrue(saves.get(0).getSavedAtEpochMillis() > saves.get(1).getSavedAtEpochMillis());
    }

    @Test
    void roundTrip_preservesPlayerMoneyAndTime(@TempDir Path tmp) throws Exception {
        SaveManager sm = new SaveManager(tmp);

        Map map = newEmptyMap(10, 10);
        Player player = new Player(777);
        TimeManager tm = configuredTimeManager();

        SaveGame save = sm.createSave("rt", map, player, tm, List.of(), List.of());
        GameSnapshot snap = sm.loadSnapshot(save);

        assertEquals(777, snap.player().money());
        assertEquals(TimeManager.TimeSpeed.VERY_FAST.name(), snap.time().speed());
        assertEquals(2, snap.time().totalTicks());
        assertEquals(8.0, snap.time().gameTimeSeconds(), 1e-9);

        SaveManager.LoadedState loaded = sm.instantiate(snap);
        assertEquals(777, loaded.player().getMoney());
        assertEquals(TimeManager.TimeSpeed.VERY_FAST, loaded.timeManager().getCurrentSpeed());
        assertEquals(2, loaded.timeManager().getTotalTicks());
        assertEquals(8.0, loaded.timeManager().getGameTimeSeconds(), 1e-9);
    }

    @Test
    void roundTrip_preservesRoadsBuildingsCitiesIndustriesAndInventories(@TempDir Path tmp) throws Exception {
        SaveManager sm = new SaveManager(tmp);

        Map map = newEmptyMap(20, 20);

        City c = new City("TestCity", 1, 1, 3, 3);
        c.getWaiting().add(ResourceType.PASSENGERS, 5);
        c.getDemandBacklog().add(ResourceType.HAMBURGER, 3);
        map.addCityForLoad(c);

        Industry i = new Industry("TestBakery", IndustryType.BAKERY, 12, 12, 2, 2);
        i.getStorage().add(ResourceType.WHEAT, 10);
        i.getStorage().add(ResourceType.MEAT, 2);
        map.addIndustryForLoad(i);

        assertTrue(map.buildRoad(15, 1));
        assertTrue(map.buildRoad(15, 2));
        assertTrue(map.buildRoad(15, 3));

        assertTrue(map.buildBuilding(16, 2, new Garage(16, 2)), "Garage should be buildable next to a road");
        assertTrue(map.buildBuilding(5, 15, new Stop(5, 15)));

        Player player = new Player(1234);
        TimeManager tm = new TimeManager();

        SaveGame save = sm.createSave("map", map, player, tm, List.of(), List.of());
        SaveManager.LoadedState loaded = sm.instantiate(sm.loadSnapshot(save));

        Tile road = loaded.map().getTile(15, 2);
        assertNotNull(road);
        assertEquals(TileType.ROAD, road.getType());

        Tile garageTile = loaded.map().getTile(16, 2);
        assertNotNull(garageTile);
        assertEquals(TileType.BUILDING, garageTile.getType());
        assertTrue(garageTile.getPlacedBuilding() instanceof Garage);

        Tile stopTile = loaded.map().getTile(5, 15);
        assertNotNull(stopTile);
        assertEquals(TileType.BUILDING, stopTile.getType());
        assertTrue(stopTile.getPlacedBuilding() instanceof Stop);

        City loadedCity = loaded.map().getCities().stream()
                .filter(x -> x != null && "TestCity".equals(x.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(5, loadedCity.getWaiting().get(ResourceType.PASSENGERS));
        assertEquals(3, loadedCity.getDemandBacklog().get(ResourceType.HAMBURGER));

        Industry loadedIndustry = loaded.map().getIndustries().stream()
                .filter(x -> x != null && "TestBakery".equals(x.getName()))
                .findFirst()
                .orElseThrow();
        assertEquals(10, loadedIndustry.getStorage().get(ResourceType.WHEAT));
        assertEquals(2, loadedIndustry.getStorage().get(ResourceType.MEAT));
    }

    @Test
    void roundTrip_preservesTrafficLights(@TempDir Path tmp) throws Exception {
        SaveManager sm = new SaveManager(tmp);

        Map map = newEmptyMap(12, 12);

        // Create a 4-way intersection at (5,5)
        assertTrue(map.buildRoad(5, 5));
        assertTrue(map.buildRoad(5, 4));
        assertTrue(map.buildRoad(5, 6));
        assertTrue(map.buildRoad(4, 5));
        assertTrue(map.buildRoad(6, 5));

        TrafficLight tl = new TrafficLight(5, 5);
        tl.setDurations(7.0, 3.0);
        tl.update(7.5); // switches to CROSS_GREEN (time resets)
        tl.update(1.0); // timeInState=1.0 in CROSS_GREEN

        assertTrue(map.buildBuilding(5, 5, tl), "TrafficLight should be buildable on an intersection");

        SaveGame save = sm.createSave("tl", map, new Player(0), new TimeManager(), List.of(), List.of(tl));
        SaveManager.LoadedState loaded = sm.instantiate(sm.loadSnapshot(save));

        assertEquals(1, loaded.trafficLights().size());
        TrafficLight loadedTl = loaded.trafficLights().get(0);
        assertEquals(5, loadedTl.getX());
        assertEquals(5, loadedTl.getY());
        assertEquals("CROSS_GREEN", loadedTl.getCurrentState());
        assertEquals(1.0, loadedTl.getTimeInCurrentState(), 1e-9);
        assertEquals(7.0, loadedTl.getGreenDurationMain(), 1e-9);
        assertEquals(3.0, loadedTl.getGreenDurationCross(), 1e-9);

        Tile roadWithLight = loaded.map().getTile(5, 5);
        assertNotNull(roadWithLight);
        assertEquals(TileType.ROAD, roadWithLight.getType());
        assertTrue(roadWithLight.getPlacedBuilding() instanceof TrafficLight);
    }

    @Test
    void roundTrip_preservesVehiclesWithCargoAndGarageReference(@TempDir Path tmp) throws Exception {
        SaveManager sm = new SaveManager(tmp);

        Map map = newEmptyMap(12, 12);
        assertTrue(map.buildRoad(2, 1));
        assertTrue(map.buildRoad(3, 1));
        assertTrue(map.buildRoad(4, 1));
        assertTrue(map.buildBuilding(2, 2, new Garage(2, 2)));

        Truck truck = new Truck();
        GameSnapshot.VehicleData data = new GameSnapshot.VehicleData(
                "Truck",
                123.0,
                456.0,
                2,
                1,
                null,
                null,
                null,
                null,
                1,
                0,
                2,
                List.of(new GameSnapshot.IntPair(2, 1), new GameSnapshot.IntPair(3, 1), new GameSnapshot.IntPair(4, 1)),
                0,
                true,
                new GameSnapshot.CargoData(ResourceType.MEAT, 7),
                new GameSnapshot.RouteBuildingsData(2, 2, 5, 15),
                List.of(new GameSnapshot.IntPair(2, 1), new GameSnapshot.IntPair(3, 1)),
                true,
                3,
                1,
                12.5,
                5.0,
                false,
                false,
                0.0,
                null,
                null,
                2,
                2,
                null,
                null,
                999
        );
        truck.importSaveData(data, map);

        SaveGame save = sm.createSave("veh", map, new Player(0), new TimeManager(), List.of(truck), List.of());
        SaveManager.LoadedState loaded = sm.instantiate(sm.loadSnapshot(save));

        assertEquals(1, loaded.vehicles().size());
        Vehicle v = loaded.vehicles().get(0);
        assertTrue(v instanceof Truck);
        assertEquals(999, v.getPurchasePrice());

        Resource cargo = v.getCurrentCargo();
        assertNotNull(cargo);
        assertEquals(ResourceType.MEAT, cargo.getType());
        assertEquals(7, cargo.getAmount());

        assertNotNull(v.getHomeGarage(), "Expected home garage to be resolved from coordinates");
        assertEquals(2, v.getHomeGarage().getX());
        assertEquals(2, v.getHomeGarage().getY());

        assertTrue(v.servesBuilding(2, 2));
        assertTrue(v.servesBuilding(5, 15));
        assertTrue(v.hasPath());
        assertEquals(2, v.getCurrentTileX());
        assertEquals(1, v.getCurrentTileY());
    }
}
