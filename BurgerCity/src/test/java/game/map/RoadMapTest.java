package game.map;

import game.building.Road;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoadMapTest {

    private Map map;

    @BeforeEach
    void setUp() {
        map = new Map(50, 40);
        map.initGrassForLoad();
    }

    // Basic Road Building Tests

    @Test
    void testBuildRoadOnGrass() {
        assertTrue(map.buildRoad(10, 10));
        Tile tile = map.getTile(10, 10);
        assertEquals(TileType.ROAD, tile.getType());
        assertTrue(tile.isOccupied());
        assertTrue(tile.isWalkable());
    }

    @Test
    void testBuildMultipleRoads() {
        assertTrue(map.buildRoad(5, 5));
        assertTrue(map.buildRoad(5, 6));
        assertTrue(map.buildRoad(5, 7));

        assertEquals(TileType.ROAD, map.getTile(5, 5).getType());
        assertEquals(TileType.ROAD, map.getTile(5, 6).getType());
        assertEquals(TileType.ROAD, map.getTile(5, 7).getType());
    }

    @Test
    void testBuildRoadNetwork() {
        // Build a cross pattern
        assertTrue(map.buildRoad(10, 10));
        assertTrue(map.buildRoad(9, 10));
        assertTrue(map.buildRoad(11, 10));
        assertTrue(map.buildRoad(10, 9));
        assertTrue(map.buildRoad(10, 11));

        // All should be roads
        assertEquals(TileType.ROAD, map.getTile(10, 10).getType());
        assertEquals(TileType.ROAD, map.getTile(9, 10).getType());
        assertEquals(TileType.ROAD, map.getTile(11, 10).getType());
        assertEquals(TileType.ROAD, map.getTile(10, 9).getType());
        assertEquals(TileType.ROAD, map.getTile(10, 11).getType());
    }

    // Tests for building on CITY tiles

    @Test
    void testCannotBuildRoadOnCity() {
        City city = new City("Test City", 5, 5, 3, 3);
        map.addCityForLoad(city);

        // Try to build road on city tiles
        assertFalse(map.buildRoad(5, 5));
        assertFalse(map.buildRoad(6, 6));
        assertFalse(map.buildRoad(7, 7));

        // Verify tiles are still city
        assertEquals(TileType.CITY, map.getTile(5, 5).getType());
        assertEquals(TileType.CITY, map.getTile(6, 6).getType());
        assertEquals(TileType.CITY, map.getTile(7, 7).getType());
    }

    @Test
    void testCanBuildRoadAroundCity() {
        City city = new City("Test City", 10, 10, 4, 4);
        map.addCityForLoad(city);

        // Build roads around the city (not on it)
        assertTrue(map.buildRoad(9, 10));  // Left
        assertTrue(map.buildRoad(14, 10)); // Right
        assertTrue(map.buildRoad(10, 9));  // Top
        assertTrue(map.buildRoad(10, 14)); // Bottom

        assertEquals(TileType.ROAD, map.getTile(9, 10).getType());
        assertEquals(TileType.ROAD, map.getTile(14, 10).getType());
        assertEquals(TileType.ROAD, map.getTile(10, 9).getType());
        assertEquals(TileType.ROAD, map.getTile(10, 14).getType());
    }

    // Tests for building on INDUSTRY tiles

    @Test
    void testCannotBuildRoadOnIndustry() {
        assertTrue(map.buildIndustry(15, 15, IndustryType.FARM));

        // Try to build road on industry tiles (2x2 area)
        assertFalse(map.buildRoad(15, 15));
        assertFalse(map.buildRoad(16, 15));
        assertFalse(map.buildRoad(15, 16));
        assertFalse(map.buildRoad(16, 16));

        // Verify tiles are still industry
        assertEquals(TileType.INDUSTRY, map.getTile(15, 15).getType());
        assertEquals(TileType.INDUSTRY, map.getTile(16, 16).getType());
    }

    @Test
    void testCanBuildRoadAroundIndustry() {
        assertTrue(map.buildIndustry(20, 20, IndustryType.BAKERY));

        // Build roads around the industry
        assertTrue(map.buildRoad(19, 20)); // Left
        assertTrue(map.buildRoad(22, 20)); // Right
        assertTrue(map.buildRoad(20, 19)); // Top
        assertTrue(map.buildRoad(20, 22)); // Bottom

        assertEquals(TileType.ROAD, map.getTile(19, 20).getType());
        assertEquals(TileType.ROAD, map.getTile(22, 20).getType());
        assertEquals(TileType.ROAD, map.getTile(20, 19).getType());
        assertEquals(TileType.ROAD, map.getTile(20, 22).getType());
    }

    // Tests for building on FOREST tiles

    @Test
    void testCanBuildRoadOnForest() {
        // Create a forest tile
        Tile tile = map.getTile(25, 25);
        tile.setType(TileType.FOREST);
        tile.setForestTrees(3);

        assertTrue(map.buildRoad(25, 25));

        // Verify it's now a road and trees are cleared
        assertEquals(TileType.ROAD, map.getTile(25, 25).getType());
        assertEquals(0, map.getTile(25, 25).getForestTrees());
    }

    @Test
    void testBuildRoadClearsForest() {
        // Create multiple forest tiles
        for (int i = 0; i < 5; i++) {
            Tile tile = map.getTile(30 + i, 10);
            tile.setType(TileType.FOREST);
            tile.setForestTrees(2 + i);
        }

        // Build road through forest
        for (int i = 0; i < 5; i++) {
            assertTrue(map.buildRoad(30 + i, 10));
            assertEquals(TileType.ROAD, map.getTile(30 + i, 10).getType());
            assertEquals(0, map.getTile(30 + i, 10).getForestTrees());
        }
    }

    // Tests for Road Demolition

    @Test
    void testDemolishRoad() {
        assertTrue(map.buildRoad(12, 12));
        assertEquals(TileType.ROAD, map.getTile(12, 12).getType());

        assertTrue(map.demolishRoad(12, 12));

        Tile tile = map.getTile(12, 12);
        assertEquals(TileType.GRASS, tile.getType());
        assertFalse(tile.isOccupied());
        assertTrue(tile.isWalkable());
    }

    @Test
    void testDemolishMultipleRoads() {
        // Build a road line
        for (int i = 0; i < 5; i++) {
            assertTrue(map.buildRoad(15 + i, 20));
        }

        // Demolish them
        for (int i = 0; i < 5; i++) {
            assertTrue(map.demolishRoad(15 + i, 20));
            assertEquals(TileType.GRASS, map.getTile(15 + i, 20).getType());
        }
    }

    @Test
    void testCannotDemolishNonRoadTile() {
        assertFalse(map.demolishRoad(5, 5)); // Grass tile

        City city = new City("Test City", 10, 10, 3, 3);
        map.addCityForLoad(city);
        assertFalse(map.demolishRoad(10, 10)); // City tile
    }

    @Test
    void testRebuildRoadAfterDemolition() {
        assertTrue(map.buildRoad(8, 8));
        assertTrue(map.demolishRoad(8, 8));
        assertTrue(map.buildRoad(8, 8)); // Should be able to rebuild

        assertEquals(TileType.ROAD, map.getTile(8, 8).getType());
    }

    // Boundary Tests

    @Test
    void testCannotBuildRoadOutOfBounds() {
        assertFalse(map.buildRoad(-1, 10));
        assertFalse(map.buildRoad(10, -1));
        assertFalse(map.buildRoad(100, 10));
        assertFalse(map.buildRoad(10, 100));
    }

    @Test
    void testBuildRoadAtBoundaries() {
        assertTrue(map.buildRoad(0, 0));
        assertTrue(map.buildRoad(49, 0));
        assertTrue(map.buildRoad(0, 39));
        assertTrue(map.buildRoad(49, 39));

        assertEquals(TileType.ROAD, map.getTile(0, 0).getType());
        assertEquals(TileType.ROAD, map.getTile(49, 0).getType());
        assertEquals(TileType.ROAD, map.getTile(0, 39).getType());
        assertEquals(TileType.ROAD, map.getTile(49, 39).getType());
    }

    // Tests for building on already occupied tiles

    @Test
    void testCannotBuildRoadOnOccupiedTile() {
        Tile tile = map.getTile(20, 20);
        tile.setOccupied(true);

        assertFalse(map.buildRoad(20, 20));
    }

    @Test
    void testCannotBuildRoadOnTileWithBuilding() {
        Road road = new Road(15, 15);
        map.buildBuilding(15, 15, road);

        // Tile now has a building
        assertFalse(map.buildRoad(15, 15));
    }

    @Test
    void testCannotBuildRoadTwiceOnSameTile() {
        assertTrue(map.buildRoad(25, 30));
        assertFalse(map.buildRoad(25, 30)); // Already a road
    }

    // Test road connectivity and pathfinding

    @Test
    void testRoadPathfindingBetweenTwoRoads() {
        // Build a simple path
        for (int i = 0; i < 5; i++) {
            assertTrue(map.buildRoad(10 + i, 15));
        }

        var path = map.findRoadPathBetweenRoadTiles(10, 15, 14, 15);
        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertEquals(5, path.size());
    }

    @Test
    void testNoPathWhenRoadsNotConnected() {
        map.buildRoad(5, 5);
        map.buildRoad(45, 35);

        var path = map.findRoadPathBetweenRoadTiles(5, 5, 45, 35);
        assertTrue(path.isEmpty());
    }

    // Edge Cases

    @Test
    void testRoadStateAfterBuilding() {
        assertTrue(map.buildRoad(18, 18));

        Tile tile = map.getTile(18, 18);
        assertTrue(tile.isWalkable());
        assertTrue(tile.isOccupied());
        assertEquals(TileType.ROAD, tile.getType());
        assertNull(tile.getPlacedBuilding());
    }

    @Test
    void testDemolishRoadClearsAllProperties() {
        map.buildRoad(22, 22);
        map.demolishRoad(22, 22);

        Tile tile = map.getTile(22, 22);
        assertEquals(TileType.GRASS, tile.getType());
        assertFalse(tile.isOccupied());
        assertTrue(tile.isWalkable());
        assertNull(tile.getPlacedBuilding());
    }

    @Test
    void testComplexRoadNetworkWithCitiesAndIndustries() {
        // Add a city
        City city = new City("Center City", 15, 15, 4, 4);
        map.addCityForLoad(city);

        // Add an industry
        assertTrue(map.buildIndustry(25, 15, IndustryType.FARM));

        // Build roads connecting them
        for (int x = 14; x >= 10; x--) {
            assertTrue(map.buildRoad(x, 16), "Failed at x: " + x + ", y: 16");
        }
        for (int x = 19; x <= 24; x++) {
            assertTrue(map.buildRoad(x, 16), "Failed at x: " + x + ", y: 16");
        }
        for (int y = 15; y >= 10; y--) {
            assertTrue(map.buildRoad(10, y), "Failed at x: 10, y: " + y);
        }

        // Verify roads exist
        assertEquals(TileType.ROAD, map.getTile(10, 10).getType());
        assertEquals(TileType.ROAD, map.getTile(14, 16).getType());
        assertEquals(TileType.ROAD, map.getTile(24, 16).getType());
    }
}
