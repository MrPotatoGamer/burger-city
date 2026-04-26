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

    // Additional coverage tests

    @Test
    void testAdjacentRoadTilesForArea() {
        // Create a city
        City city = new City("Test City", 10, 10, 4, 4);
        map.addCityForLoad(city);

        // Build roads around the city
        map.buildRoad(9, 10);   // Left
        map.buildRoad(14, 12);  // Right
        map.buildRoad(11, 9);   // Top
        map.buildRoad(12, 14);  // Bottom

        // Get adjacent road tiles
        var adjacentRoads = map.adjacentRoadTilesForArea(10, 10, 4, 4);

        // Should find all 4 roads
        assertFalse(adjacentRoads.isEmpty());
        assertTrue(adjacentRoads.size() >= 4);
    }

    @Test
    void testFindRoadPathBetweenAreas() {
        // Create two cities
        City cityA = new City("City A", 5, 5, 3, 3);
        City cityB = new City("City B", 20, 5, 3, 3);
        map.addCityForLoad(cityA);
        map.addCityForLoad(cityB);

        // Build connecting road
        for (int x = 8; x <= 19; x++) {
            assertTrue(map.buildRoad(x, 6));
        }

        // Find path between the two cities
        var path = map.findRoadPathBetweenAreas(5, 5, 3, 3, 20, 5, 3, 3);

        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    @Test
    void testNoPathBetweenDisconnectedAreas() {
        // Create two cities without connecting roads
        City cityA = new City("City A", 5, 5, 3, 3);
        City cityB = new City("City B", 40, 35, 3, 3);
        map.addCityForLoad(cityA);
        map.addCityForLoad(cityB);

        // Try to find path (should fail - no roads)
        var path = map.findRoadPathBetweenAreas(5, 5, 3, 3, 40, 35, 3, 3);

        assertTrue(path.isEmpty());
    }

    @Test
    void testDemolishRoadOutOfBounds() {
        assertFalse(map.demolishRoad(-1, 0));
        assertFalse(map.demolishRoad(0, -1));
        assertFalse(map.demolishRoad(100, 0));
        assertFalse(map.demolishRoad(0, 100));
    }

    @Test
    void testBuildRoadOnNullTile() {
        // Out of bounds returns false
        assertFalse(map.buildRoad(-5, -5));
        assertFalse(map.buildRoad(1000, 1000));
    }

    @Test
    void testRoadPathSameStartAndEnd() {
        map.buildRoad(10, 10);

        var path = map.findRoadPathBetweenRoadTiles(10, 10, 10, 10);

        assertNotNull(path);
        assertEquals(1, path.size());
        assertEquals(10, path.get(0)[0]);
        assertEquals(10, path.get(0)[1]);
    }

    @Test
    void testRoadPathFromNonRoadTile() {
        map.buildRoad(10, 10);

        // Try to find path from grass tile to road
        var path = map.findRoadPathBetweenRoadTiles(5, 5, 10, 10);

        assertTrue(path.isEmpty());
    }

    @Test
    void testRoadPathToNonRoadTile() {
        map.buildRoad(10, 10);

        // Try to find path from road to grass tile
        var path = map.findRoadPathBetweenRoadTiles(10, 10, 5, 5);

        assertTrue(path.isEmpty());
    }

    @Test
    void testLongRoadPath() {
        // Build a long winding path
        for (int x = 5; x <= 20; x++) {
            assertTrue(map.buildRoad(x, 10));
        }
        for (int y = 11; y <= 20; y++) {
            assertTrue(map.buildRoad(20, y));
        }
        for (int x = 19; x >= 10; x--) {
            assertTrue(map.buildRoad(x, 20));
        }

        var path = map.findRoadPathBetweenRoadTiles(5, 10, 10, 20);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        assertTrue(path.size() > 20);
    }

    @Test
    void testMultipleIndustryTypes() {
        assertTrue(map.buildIndustry(5, 5, IndustryType.FARM));
        assertTrue(map.buildIndustry(10, 10, IndustryType.RANCH));
        assertTrue(map.buildIndustry(15, 15, IndustryType.BAKERY));
        assertTrue(map.buildIndustry(20, 20, IndustryType.PATTY_PLANT));
        assertTrue(map.buildIndustry(25, 25, IndustryType.BURGER_FACTORY));
        assertTrue(map.buildIndustry(30, 30, IndustryType.FACTORY));

        assertEquals(6, map.getIndustries().size());
    }

    @Test
    void testCannotBuildIndustryOnForest() {
        // Create forest tiles
        Tile tile1 = map.getTile(10, 10);
        Tile tile2 = map.getTile(11, 10);
        tile1.setType(TileType.FOREST);
        tile2.setType(TileType.FOREST);

        // Try to build industry on forest (should fail)
        assertFalse(map.buildIndustry(10, 10, IndustryType.FARM));
    }

    @Test
    void testCannotBuildIndustryWithNullType() {
        assertFalse(map.buildIndustry(10, 10, null));
    }

    @Test
    void testCannotBuildIndustryOutOfBounds() {
        assertFalse(map.buildIndustry(-1, 0, IndustryType.FARM));
        assertFalse(map.buildIndustry(0, -1, IndustryType.FARM));
        assertFalse(map.buildIndustry(49, 39, IndustryType.FARM)); // 2x2 would go out of bounds
        assertFalse(map.buildIndustry(100, 100, IndustryType.FARM));
    }

    @Test
    void testAdjacentRoadTilesEmptyWhenNoRoads() {
        City city = new City("Test City", 10, 10, 3, 3);
        map.addCityForLoad(city);

        var adjacentRoads = map.adjacentRoadTilesForArea(10, 10, 3, 3);

        assertTrue(adjacentRoads.isEmpty());
    }

    @Test
    void testRoadNetworkWithMultiplePaths() {
        // Create a grid of roads
        for (int x = 10; x <= 15; x++) {
            for (int y = 10; y <= 15; y++) {
                assertTrue(map.buildRoad(x, y));
            }
        }

        // Find path - should find one of many possible paths
        var path = map.findRoadPathBetweenRoadTiles(10, 10, 15, 15);

        assertNotNull(path);
        assertFalse(path.isEmpty());
        // Path should start and end at correct positions
        assertEquals(10, path.get(0)[0]);
        assertEquals(10, path.get(0)[1]);
        assertEquals(15, path.get(path.size() - 1)[0]);
        assertEquals(15, path.get(path.size() - 1)[1]);
    }

    @Test
    void testDemolishRoadInMiddleOfPath() {
        // Build a straight road
        for (int x = 10; x <= 20; x++) {
            assertTrue(map.buildRoad(x, 15));
        }

        // Verify path exists
        var pathBefore = map.findRoadPathBetweenRoadTiles(10, 15, 20, 15);
        assertFalse(pathBefore.isEmpty());

        // Demolish middle road
        assertTrue(map.demolishRoad(15, 15));

        // Verify path no longer exists
        var pathAfter = map.findRoadPathBetweenRoadTiles(10, 15, 20, 15);
        assertTrue(pathAfter.isEmpty());
    }

    @Test
    void testBuildRoadOnOccupiedGrassTile() {
        Tile tile = map.getTile(10, 10);
        tile.setOccupied(true);

        assertFalse(map.buildRoad(10, 10));
        assertEquals(TileType.GRASS, tile.getType());
    }

    @Test
    void testMapDimensions() {
        assertEquals(50, map.getWidth());
        assertEquals(40, map.getHeight());
    }

    @Test
    void testGetTileReturnsNullForInvalidCoordinates() {
        assertNull(map.getTile(-1, 0));
        assertNull(map.getTile(0, -1));
        assertNull(map.getTile(100, 0));
        assertNull(map.getTile(0, 100));
    }

    @Test
    void testRoadCornersAndTurns() {
        // Build an L-shaped road
        for (int x = 10; x <= 15; x++) {
            assertTrue(map.buildRoad(x, 10));
        }
        for (int y = 11; y <= 15; y++) {
            assertTrue(map.buildRoad(15, y));
        }

        // Verify corner tile
        assertEquals(TileType.ROAD, map.getTile(15, 10).getType());

        // Find path along the L
        var path = map.findRoadPathBetweenRoadTiles(10, 10, 15, 15);
        assertFalse(path.isEmpty());
    }
}
