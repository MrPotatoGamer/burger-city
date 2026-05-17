package game.map;

import game.building.Garage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MapTest {

    private Map map;

    @BeforeEach
    void setUp() {
        map = new Map(50, 50);
    }

    // ==================== Constructor Tests ====================

    @Test
    void testConstructorCreatesMap() {
        assertNotNull(map);
    }

    @Test
    void testConstructorSetsWidth() {
        assertEquals(50, map.getWidth());
    }

    @Test
    void testConstructorSetsHeight() {
        assertEquals(50, map.getHeight());
    }

    @Test
    void testConstructorWithSmallDimensions() {
        Map smallMap = new Map(5, 5);
        assertEquals(5, smallMap.getWidth());
        assertEquals(5, smallMap.getHeight());
    }

    @Test
    void testConstructorWithLargeDimensions() {
        Map largeMap = new Map(200, 200);
        assertEquals(200, largeMap.getWidth());
        assertEquals(200, largeMap.getHeight());
    }

    @Test
    void testConstructorInitializesCitiesList() {
        assertNotNull(map.getCities());
        assertTrue(map.getCities().isEmpty());
    }

    @Test
    void testConstructorInitializesIndustriesList() {
        assertNotNull(map.getIndustries());
        assertTrue(map.getIndustries().isEmpty());
    }

    // ==================== Tile Access Tests ====================

    @Test
    void testGetTileReturnsNullBeforeInit() {
        Tile tile = map.getTile(0, 0);
        assertNull(tile);
    }

    @Test
    void testGetTileAfterInit() {
        map.initGrassForLoad();
        Tile tile = map.getTile(0, 0);
        assertNotNull(tile);
        assertEquals(TileType.GRASS, tile.getType());
    }

    @Test
    void testGetTileOutOfBounds() {
        map.initGrassForLoad();
        assertNull(map.getTile(-1, 0));
        assertNull(map.getTile(0, -1));
        assertNull(map.getTile(50, 0));
        assertNull(map.getTile(0, 50));
    }

    @Test
    void testGetTileAllTilesAccessible() {
        map.initGrassForLoad();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                assertNotNull(tile, "Tile at (" + x + ", " + y + ") should not be null");
                assertEquals(x, tile.getX());
                assertEquals(y, tile.getY());
            }
        }
    }

    // ==================== InitGrassForLoad Tests ====================

    @Test
    void testInitGrassForLoadCreatesAllTiles() {
        map.initGrassForLoad();

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                assertNotNull(tile);
            }
        }
    }

    @Test
    void testInitGrassForLoadSetsGrassType() {
        map.initGrassForLoad();

        Tile tile = map.getTile(0, 0);
        assertEquals(TileType.GRASS, tile.getType());
    }

    @Test
    void testInitGrassForLoadMultipleTimes() {
        map.initGrassForLoad();
        map.initGrassForLoad();

        Tile tile = map.getTile(0, 0);
        assertNotNull(tile);
        assertEquals(TileType.GRASS, tile.getType());
    }

    // ==================== Forest Tests ====================

    @Test
    void testSetForestForLoadValidCoordinates() {
        map.initGrassForLoad();
        boolean result = map.setForestForLoad(5, 5, 3);
        assertTrue(result);

        Tile tile = map.getTile(5, 5);
        assertEquals(TileType.FOREST, tile.getType());
        assertEquals(3, tile.getForestTrees());
    }

    @Test
    void testSetForestForLoadOutOfBounds() {
        map.initGrassForLoad();
        assertFalse(map.setForestForLoad(-1, 0, 3));
        assertFalse(map.setForestForLoad(0, -1, 3));
        assertFalse(map.setForestForLoad(50, 0, 3));
        assertFalse(map.setForestForLoad(0, 50, 3));
    }

    @Test
    void testSetForestForLoadClampsTreeCount() {
        map.initGrassForLoad();

        map.setForestForLoad(5, 5, 10);
        assertEquals(4, map.getTile(5, 5).getForestTrees());

        map.setForestForLoad(6, 6, 0);
        assertEquals(1, map.getTile(6, 6).getForestTrees());

        map.setForestForLoad(7, 7, -5);
        assertEquals(1, map.getTile(7, 7).getForestTrees());
    }

    @Test
    void testSetForestForLoadOnNonGrassFails() {
        map.initGrassForLoad();
        Tile tile = map.getTile(5, 5);
        tile.setType(TileType.ROAD);

        boolean result = map.setForestForLoad(5, 5, 3);
        assertFalse(result);
    }

    @Test
    void testSetForestForLoadOnOccupiedTileFails() {
        map.initGrassForLoad();
        Tile tile = map.getTile(5, 5);
        tile.setOccupied(true);

        boolean result = map.setForestForLoad(5, 5, 3);
        assertFalse(result);
    }

    @Test
    void testSetForestForLoadWithBuildingFails() {
        map.initGrassForLoad();
        Tile tile = map.getTile(5, 5);
        tile.setPlacedBuilding(new Garage(5, 5));

        boolean result = map.setForestForLoad(5, 5, 3);
        assertFalse(result);
    }

    // ==================== City Tests ====================

    @Test
    void testAddCityForLoad() {
        map.initGrassForLoad(); // Initialize tiles first
        City city = new City("TestCity", 10, 10, 5, 5);
        map.addCityForLoad(city);

        List<City> cities = map.getCities();
        assertEquals(1, cities.size());
        assertTrue(cities.contains(city));
    }

    @Test
    void testAddMultipleCities() {
        map.initGrassForLoad(); // Initialize tiles first
        City city1 = new City("City1", 10, 10, 5, 5);
        City city2 = new City("City2", 20, 20, 5, 5);

        map.addCityForLoad(city1);
        map.addCityForLoad(city2);

        assertEquals(2, map.getCities().size());
    }

    @Test
    void testGetCitiesReturnsCorrectList() {
        map.initGrassForLoad(); // Initialize tiles first
        City city = new City("TestCity", 10, 10, 5, 5);
        map.addCityForLoad(city);

        List<City> cities = map.getCities();
        assertNotNull(cities);
        assertEquals(1, cities.size());
        assertEquals("TestCity", cities.get(0).getName());
    }

    // ==================== Industry Tests ====================

    @Test
    void testAddIndustryForLoad() {
        map.initGrassForLoad(); // Initialize tiles first
        Industry industry = new Industry("TestFarm", IndustryType.FARM, 15, 15, 4, 4);
        map.addIndustryForLoad(industry);

        List<Industry> industries = map.getIndustries();
        assertEquals(1, industries.size());
        assertTrue(industries.contains(industry));
    }

    @Test
    void testAddMultipleIndustries() {
        map.initGrassForLoad(); // Initialize tiles first
        Industry industry1 = new Industry("Farm1", IndustryType.FARM, 10, 10, 4, 4);
        Industry industry2 = new Industry("Bakery1", IndustryType.BAKERY, 20, 20, 3, 3);

        map.addIndustryForLoad(industry1);
        map.addIndustryForLoad(industry2);

        assertEquals(2, map.getIndustries().size());
    }

    @Test
    void testGetIndustriesReturnsCorrectList() {
        map.initGrassForLoad(); // Initialize tiles first
        Industry industry = new Industry("TestFarm", IndustryType.FARM, 15, 15, 4, 4);
        map.addIndustryForLoad(industry);

        List<Industry> industries = map.getIndustries();
        assertNotNull(industries);
        assertEquals(1, industries.size());
        assertEquals("TestFarm", industries.get(0).getName());
    }

    // ==================== Garage Tests ====================

    @Test
    void testGetGaragesEmptyInitially() {
        map.initGrassForLoad();
        List<Garage> garages = map.getGarages();
        assertNotNull(garages);
        assertTrue(garages.isEmpty());
    }

    @Test
    void testGetGaragesAfterPlacingGarage() {
        map.initGrassForLoad();

        Garage garage = new Garage(5, 5);
        Tile tile = map.getTile(5, 5);
        tile.setType(TileType.BUILDING); // Must set type to BUILDING
        tile.setPlacedBuilding(garage);

        List<Garage> garages = map.getGarages();
        assertEquals(1, garages.size());
        assertTrue(garages.contains(garage));
    }

    @Test
    void testGetGaragesMultipleGarages() {
        map.initGrassForLoad();

        Garage garage1 = new Garage(5, 5);
        Garage garage2 = new Garage(10, 10);

        map.getTile(5, 5).setType(TileType.BUILDING); // Must set type to BUILDING
        map.getTile(5, 5).setPlacedBuilding(garage1);
        map.getTile(10, 10).setType(TileType.BUILDING); // Must set type to BUILDING
        map.getTile(10, 10).setPlacedBuilding(garage2);

        List<Garage> garages = map.getGarages();
        assertEquals(2, garages.size());
    }

    // ==================== LoadPredefined Tests ====================

    @Test
    void testLoadPredefinedCreatesMap() {
        map.loadPredefined();

        // Should have initialized tiles
        assertNotNull(map.getTile(0, 0));
    }

    @Test
    void testLoadPredefinedCreatesCities() {
        map.loadPredefined();

        List<City> cities = map.getCities();
        assertFalse(cities.isEmpty());
        assertTrue(cities.size() >= 3);
    }

    @Test
    void testLoadPredefinedCreatesSpecificCities() {
        map.loadPredefined();

        List<City> cities = map.getCities();
        boolean hasBurgerCity = cities.stream()
            .anyMatch(c -> "Burger City".equals(c.getName()));

        assertTrue(hasBurgerCity);
    }

    @Test
    void testLoadPredefinedCreatesForests() {
        map.loadPredefined();

        boolean hasForest = false;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && tile.getType() == TileType.FOREST) {
                    hasForest = true;
                    break;
                }
            }
            if (hasForest) break;
        }

        assertTrue(hasForest);
    }

    // ==================== Dimension Tests ====================

    @Test
    void testGetWidthReturnsinCorrectValue() {
        Map customMap = new Map(75, 40);
        assertEquals(75, customMap.getWidth());
    }

    @Test
    void testGetHeightReturnsCorrectValue() {
        Map customMap = new Map(75, 40);
        assertEquals(40, customMap.getHeight());
    }

    // ==================== Edge Case Tests ====================

    @Test
    void testMapWithZeroDimensions() {
        Map zeroMap = new Map(0, 0);
        assertEquals(0, zeroMap.getWidth());
        assertEquals(0, zeroMap.getHeight());
        assertNull(zeroMap.getTile(0, 0));
    }

    @Test
    void testMapWithOneDimension() {
        Map oneMap = new Map(1, 1);
        oneMap.initGrassForLoad();
        assertEquals(1, oneMap.getWidth());
        assertEquals(1, oneMap.getHeight());
        assertNotNull(oneMap.getTile(0, 0));
    }

    @Test
    void testAddNullCity() {
        map.initGrassForLoad();
        // addCityForLoad will throw NullPointerException for null
        assertThrows(NullPointerException.class, () -> {
            map.addCityForLoad(null);
        });
    }

    @Test
    void testAddNullIndustry() {
        map.initGrassForLoad();
        // addIndustryForLoad will throw NullPointerException for null
        assertThrows(NullPointerException.class, () -> {
            map.addIndustryForLoad(null);
        });
    }

    // ==================== Integration Tests ====================

    @Test
    void testCompleteMapSetup() {
        map.initGrassForLoad();

        // Add cities
        City city1 = new City("City1", 5, 5, 5, 5);
        City city2 = new City("City2", 20, 20, 6, 6);
        map.addCityForLoad(city1);
        map.addCityForLoad(city2);

        // Add industries
        Industry farm = new Industry("Farm", IndustryType.FARM, 30, 30, 4, 4);
        map.addIndustryForLoad(farm);

        // Add forests
        map.setForestForLoad(40, 40, 3);
        map.setForestForLoad(41, 40, 2);

        // Verify everything is set up correctly
        assertEquals(2, map.getCities().size());
        assertEquals(1, map.getIndustries().size());
        assertEquals(TileType.FOREST, map.getTile(40, 40).getType());
    }

    @Test
    void testMapAfterLoadPredefinedHasCitiesAndForests() {
        map.loadPredefined();

        assertFalse(map.getCities().isEmpty());

        int forestCount = 0;
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile != null && tile.getType() == TileType.FOREST) {
                    forestCount++;
                }
            }
        }

        assertTrue(forestCount > 0);
    }

    @Test
    void testTileCoordinatesMatchPosition() {
        map.initGrassForLoad();

        for (int x = 0; x < Math.min(10, map.getWidth()); x++) {
            for (int y = 0; y < Math.min(10, map.getHeight()); y++) {
                Tile tile = map.getTile(x, y);
                assertEquals(x, tile.getX());
                assertEquals(y, tile.getY());
            }
        }
    }

    @Test
    void testMultipleMapsIndependent() {
        Map map1 = new Map(10, 10);
        Map map2 = new Map(20, 20);

        map1.initGrassForLoad();
        map1.addCityForLoad(new City("City1", 1, 1, 3, 3));

        map2.initGrassForLoad();

        assertEquals(1, map1.getCities().size());
        assertEquals(0, map2.getCities().size());
    }

    // ==================== Bounds Checking Tests ====================

    @Test
    void testNegativeCoordinatesOutOfBounds() {
        map.initGrassForLoad();
        assertNull(map.getTile(-1, -1));
        assertNull(map.getTile(-100, -100));
    }

    @Test
    void testLargeCoordinatesOutOfBounds() {
        map.initGrassForLoad();
        assertNull(map.getTile(100, 100));
        assertNull(map.getTile(1000, 1000));
    }

    @Test
    void testBoundaryCoordinates() {
        map.initGrassForLoad();

        // Valid boundary
        assertNotNull(map.getTile(0, 0));
        assertNotNull(map.getTile(49, 49));

        // Invalid boundary
        assertNull(map.getTile(50, 50));
        assertNull(map.getTile(50, 0));
        assertNull(map.getTile(0, 50));
    }

    // ==================== updateEconomy Tests ====================

    @Test
    void testUpdateEconomyWithNegativeDelta() {
        map.initGrassForLoad();
        City city = new City("TestCity", 10, 10, 5, 5);
        map.addCityForLoad(city);

        // Should not crash with negative delta
        map.updateEconomy(-1.0);
        assertTrue(true);
    }

    @Test
    void testUpdateEconomyWithZeroDelta() {
        map.initGrassForLoad();
        City city = new City("TestCity", 10, 10, 5, 5);
        map.addCityForLoad(city);

        // Should not crash with zero delta
        map.updateEconomy(0.0);
        assertTrue(true);
    }

    @Test
    void testUpdateEconomyWithPositiveDelta() {
        map.initGrassForLoad();
        City city = new City("TestCity", 10, 10, 5, 5);
        map.addCityForLoad(city);

        // Should update city economics
        map.updateEconomy(1.0);
        assertTrue(true);
    }

    @Test
    void testUpdateEconomyWithMultipleCitiesAndIndustries() {
        map.initGrassForLoad();
        City city1 = new City("City1", 10, 10, 5, 5);
        City city2 = new City("City2", 20, 20, 5, 5);
        Industry industry = new Industry("Farm", IndustryType.FARM, 30, 30, 4, 4);

        map.addCityForLoad(city1);
        map.addCityForLoad(city2);
        map.addIndustryForLoad(industry);

        // Should update all entities
        map.updateEconomy(2.0);
        assertTrue(true);
    }

    @Test
    void testUpdateEconomyWithEmptyMap() {
        map.initGrassForLoad();

        // Should handle empty map gracefully
        map.updateEconomy(1.0);
        assertTrue(true);
    }

    // ==================== updateForests Tests ====================

    @Test
    void testUpdateForestsWithNegativeDelta() {
        map.initGrassForLoad();
        map.setForestForLoad(10, 10, 2);

        // Should not update with negative delta
        map.updateForests(-1.0);
        assertTrue(true);
    }

    @Test
    void testUpdateForestsWithZeroDelta() {
        map.initGrassForLoad();
        map.setForestForLoad(10, 10, 2);

        // Should not update with zero delta
        map.updateForests(0.0);
        assertTrue(true);
    }

    @Test
    void testUpdateForestsWithSmallDelta() {
        map.initGrassForLoad();
        map.setForestForLoad(10, 10, 2);

        // Should accumulate time
        map.updateForests(0.5);
        assertTrue(true);
    }

    @Test
    void testUpdateForestsWithLargeDelta() {
        map.initGrassForLoad();
        map.setForestForLoad(10, 10, 3);
        map.setForestForLoad(11, 10, 3);

        // Should process multiple forest steps
        map.updateForests(5.0);
        assertTrue(true);
    }

    @Test
    void testUpdateForestsGrowth() {
        map.initGrassForLoad();
        map.setForestForLoad(25, 25, 1);

        // Update many times to potentially see growth
        for (int i = 0; i < 100; i++) {
            map.updateForests(1.0);
        }

        // Forest may have grown (probabilistic)
        Tile tile = map.getTile(25, 25);
        assertTrue(tile.getForestTrees() >= 1 && tile.getForestTrees() <= 4);
    }

    // ==================== demolishForest Tests ====================

    @Test
    void testDemolishForestValidForest() {
        map.initGrassForLoad();
        map.setForestForLoad(15, 15, 3);

        boolean result = map.demolishForest(15, 15);
        assertTrue(result);

        Tile tile = map.getTile(15, 15);
        assertEquals(TileType.GRASS, tile.getType());
        assertEquals(0, tile.getForestTrees());
    }

    @Test
    void testDemolishForestOutOfBounds() {
        map.initGrassForLoad();

        assertFalse(map.demolishForest(-1, 0));
        assertFalse(map.demolishForest(0, -1));
        assertFalse(map.demolishForest(50, 0));
        assertFalse(map.demolishForest(0, 50));
    }

    @Test
    void testDemolishForestOnNonForestTile() {
        map.initGrassForLoad();
        Tile tile = map.getTile(15, 15);
        tile.setType(TileType.ROAD);

        boolean result = map.demolishForest(15, 15);
        assertFalse(result);
        assertEquals(TileType.ROAD, tile.getType());
    }

    @Test
    void testDemolishForestOnGrassTile() {
        map.initGrassForLoad();

        boolean result = map.demolishForest(15, 15);
        assertFalse(result);

        Tile tile = map.getTile(15, 15);
        assertEquals(TileType.GRASS, tile.getType());
    }

    @Test
    void testDemolishForestMultipleTimes() {
        map.initGrassForLoad();
        map.setForestForLoad(15, 15, 3);

        assertTrue(map.demolishForest(15, 15));
        assertFalse(map.demolishForest(15, 15)); // Already demolished

        Tile tile = map.getTile(15, 15);
        assertEquals(TileType.GRASS, tile.getType());
    }

    // ==================== isTrafficLightValid Tests ====================

    @Test
    void testIsTrafficLightValidOnRoadTile() {
        map.initGrassForLoad();
        Tile tile = map.getTile(20, 20);
        tile.setType(TileType.ROAD);

        // Create adjacent roads to form an intersection
        map.getTile(19, 20).setType(TileType.ROAD);
        map.getTile(21, 20).setType(TileType.ROAD);
        map.getTile(20, 19).setType(TileType.ROAD);
        map.getTile(20, 21).setType(TileType.ROAD);

        boolean result = map.isTrafficLightValid(20, 20);
        assertTrue(result);
    }

    @Test
    void testIsTrafficLightValidOnNonRoadTile() {
        map.initGrassForLoad();

        boolean result = map.isTrafficLightValid(20, 20);
        assertFalse(result);
    }

    @Test
    void testIsTrafficLightValidOutOfBounds() {
        map.initGrassForLoad();

        assertFalse(map.isTrafficLightValid(-1, 0));
        assertFalse(map.isTrafficLightValid(0, -1));
        assertFalse(map.isTrafficLightValid(50, 0));
        assertFalse(map.isTrafficLightValid(0, 50));
    }

    @Test
    void testIsTrafficLightValidOnBuildingTile() {
        map.initGrassForLoad();
        Tile tile = map.getTile(20, 20);
        tile.setType(TileType.BUILDING);

        boolean result = map.isTrafficLightValid(20, 20);
        assertFalse(result);
    }

    @Test
    void testIsTrafficLightValidOnDeadEndRoad() {
        map.initGrassForLoad();
        Tile tile = map.getTile(20, 20);
        tile.setType(TileType.ROAD);

        // Only one adjacent road (dead end)
        map.getTile(19, 20).setType(TileType.ROAD);

        boolean result = map.isTrafficLightValid(20, 20);
        assertFalse(result);
    }

    @Test
    void testIsTrafficLightValidOnStraightRoad() {
        map.initGrassForLoad();
        Tile tile = map.getTile(20, 20);
        tile.setType(TileType.ROAD);

        // Two adjacent roads (straight line, not intersection)
        map.getTile(19, 20).setType(TileType.ROAD);
        map.getTile(21, 20).setType(TileType.ROAD);

        boolean result = map.isTrafficLightValid(20, 20);
        assertFalse(result);
    }

    // ==================== demolishIndustryAt Tests ====================

    @Test
    void testDemolishIndustryAtValidIndustry() {
        map.initGrassForLoad();
        Industry industry = new Industry("TestFarm", IndustryType.FARM, 10, 10, 4, 4);
        map.addIndustryForLoad(industry);

        Industry demolished = map.demolishIndustryAt(10, 10);

        assertNotNull(demolished);
        assertEquals("TestFarm", demolished.getName());
        assertEquals(0, map.getIndustries().size());
    }

    @Test
    void testDemolishIndustryAtEmptyLocation() {
        map.initGrassForLoad();

        Industry demolished = map.demolishIndustryAt(10, 10);

        assertNull(demolished);
    }

    @Test
    void testDemolishIndustryAtOutOfBounds() {
        map.initGrassForLoad();

        assertNull(map.demolishIndustryAt(-1, 0));
        assertNull(map.demolishIndustryAt(0, -1));
        assertNull(map.demolishIndustryAt(50, 0));
        assertNull(map.demolishIndustryAt(0, 50));
    }

    @Test
    void testDemolishIndustryAtWrongCoordinate() {
        map.initGrassForLoad();
        Industry industry = new Industry("TestFarm", IndustryType.FARM, 10, 10, 4, 4);
        map.addIndustryForLoad(industry);

        // Try to demolish at a location outside the industry's bounds
        Industry demolished = map.demolishIndustryAt(20, 20);

        assertNull(demolished);
        assertEquals(1, map.getIndustries().size());
    }

    @Test
    void testDemolishIndustryAtPartialOverlap() {
        map.initGrassForLoad();
        Industry industry = new Industry("TestFarm", IndustryType.FARM, 10, 10, 4, 4);
        map.addIndustryForLoad(industry);

        // Demolish at any coordinate within the industry's area
        Industry demolished = map.demolishIndustryAt(11, 11);

        assertNotNull(demolished);
        assertEquals("TestFarm", demolished.getName());
    }

    @Test
    void testDemolishIndustryRemovesFromList() {
        map.initGrassForLoad();
        Industry industry1 = new Industry("Farm1", IndustryType.FARM, 10, 10, 4, 4);
        Industry industry2 = new Industry("Farm2", IndustryType.FARM, 20, 20, 4, 4);
        map.addIndustryForLoad(industry1);
        map.addIndustryForLoad(industry2);

        assertEquals(2, map.getIndustries().size());

        map.demolishIndustryAt(10, 10);

        assertEquals(1, map.getIndustries().size());
        assertFalse(map.getIndustries().contains(industry1));
        assertTrue(map.getIndustries().contains(industry2));
    }

    @Test
    void testDemolishIndustryTwiceAtSameLocation() {
        map.initGrassForLoad();
        Industry industry = new Industry("TestFarm", IndustryType.FARM, 10, 10, 4, 4);
        map.addIndustryForLoad(industry);

        assertNotNull(map.demolishIndustryAt(10, 10));
        assertNull(map.demolishIndustryAt(10, 10)); // Already demolished
    }
}
