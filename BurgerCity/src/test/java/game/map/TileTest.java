package game.map;

import game.building.Building;
import game.building.Garage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TileTest {

    private Tile tile;

    @BeforeEach
    void setUp() {
        tile = new Tile(5, 10, TileType.GRASS);
    }

    // ==================== Constructor Tests ====================

    @Test
    void testConstructorSetsCoordinates() {
        assertEquals(5, tile.getX());
        assertEquals(10, tile.getY());
    }

    @Test
    void testConstructorSetsType() {
        assertEquals(TileType.GRASS, tile.getType());
    }

    @Test
    void testConstructorGrassTileIsWalkable() {
        Tile grassTile = new Tile(0, 0, TileType.GRASS);
        assertTrue(grassTile.isWalkable());
    }

    @Test
    void testConstructorRoadTileIsNotWalkable() {
        Tile roadTile = new Tile(0, 0, TileType.ROAD);
        assertFalse(roadTile.isWalkable());
    }

    @Test
    void testConstructorForestTileIsNotWalkable() {
        Tile forestTile = new Tile(0, 0, TileType.FOREST);
        assertFalse(forestTile.isWalkable());
    }

    @Test
    void testConstructorTileNotOccupied() {
        assertFalse(tile.isOccupied());
    }

    @Test
    void testConstructorNoBuildingPlaced() {
        assertNull(tile.getPlacedBuilding());
    }

    @Test
    void testConstructorForestTreesZero() {
        assertEquals(0, tile.getForestTrees());
    }

    @Test
    void testConstructorWithNegativeCoordinates() {
        Tile negTile = new Tile(-5, -10, TileType.GRASS);
        assertEquals(-5, negTile.getX());
        assertEquals(-10, negTile.getY());
    }

    @Test
    void testConstructorWithLargeCoordinates() {
        Tile largeTile = new Tile(10000, 20000, TileType.ROAD);
        assertEquals(10000, largeTile.getX());
        assertEquals(20000, largeTile.getY());
    }

    // ==================== Type Tests ====================

    @Test
    void testSetTypeChangesType() {
        tile.setType(TileType.ROAD);
        assertEquals(TileType.ROAD, tile.getType());
    }

    @Test
    void testSetTypeToForest() {
        tile.setType(TileType.FOREST);
        assertEquals(TileType.FOREST, tile.getType());
    }

    @Test
    void testSetTypeResetsForestTrees() {
        tile.setForestTrees(3);
        assertEquals(3, tile.getForestTrees());

        tile.setType(TileType.ROAD);
        assertEquals(0, tile.getForestTrees());
    }

    @Test
    void testSetTypeForestDoesNotResetTrees() {
        tile.setForestTrees(3);
        tile.setType(TileType.FOREST);
        assertEquals(3, tile.getForestTrees());
    }

    @Test
    void testAllTileTypes() {
        for (TileType type : TileType.values()) {
            tile.setType(type);
            assertEquals(type, tile.getType());
        }
    }

    // ==================== Walkable Tests ====================

    @Test
    void testSetWalkableTrue() {
        tile.setWalkable(true);
        assertTrue(tile.isWalkable());
    }

    @Test
    void testSetWalkableFalse() {
        tile.setWalkable(false);
        assertFalse(tile.isWalkable());
    }

    @Test
    void testWalkableCanBeChanged() {
        tile.setWalkable(true);
        assertTrue(tile.isWalkable());

        tile.setWalkable(false);
        assertFalse(tile.isWalkable());
    }

    // ==================== Occupied Tests ====================

    @Test
    void testSetOccupiedTrue() {
        tile.setOccupied(true);
        assertTrue(tile.isOccupied());
    }

    @Test
    void testSetOccupiedFalse() {
        tile.setOccupied(false);
        assertFalse(tile.isOccupied());
    }

    @Test
    void testOccupiedCanBeChanged() {
        tile.setOccupied(true);
        assertTrue(tile.isOccupied());

        tile.setOccupied(false);
        assertFalse(tile.isOccupied());
    }

    // ==================== Building Tests ====================

    @Test
    void testSetPlacedBuilding() {
        Building building = new Garage(5, 10);
        tile.setPlacedBuilding(building);
        assertEquals(building, tile.getPlacedBuilding());
    }

    @Test
    void testSetPlacedBuildingToNull() {
        Building building = new Garage(5, 10);
        tile.setPlacedBuilding(building);
        tile.setPlacedBuilding(null);
        assertNull(tile.getPlacedBuilding());
    }

    @Test
    void testPlacedBuildingCanBeReplaced() {
        Building building1 = new Garage(5, 10);
        Building building2 = new Garage(6, 11);

        tile.setPlacedBuilding(building1);
        assertEquals(building1, tile.getPlacedBuilding());

        tile.setPlacedBuilding(building2);
        assertEquals(building2, tile.getPlacedBuilding());
    }

    // ==================== Forest Trees Tests ====================

    @Test
    void testSetForestTreesValidValue() {
        tile.setForestTrees(2);
        assertEquals(2, tile.getForestTrees());
    }

    @Test
    void testSetForestTreesMaximum() {
        tile.setForestTrees(4);
        assertEquals(4, tile.getForestTrees());
    }

    @Test
    void testSetForestTreesExceedsMaximumClamped() {
        tile.setForestTrees(10);
        assertEquals(4, tile.getForestTrees());
    }

    @Test
    void testSetForestTreesNegativeClamped() {
        tile.setForestTrees(-5);
        assertEquals(0, tile.getForestTrees());
    }

    @Test
    void testSetForestTreesZero() {
        tile.setForestTrees(3);
        tile.setForestTrees(0);
        assertEquals(0, tile.getForestTrees());
    }

    @Test
    void testSetForestTreesChangesTypeToForest() {
        assertEquals(TileType.GRASS, tile.getType());

        tile.setForestTrees(2);
        assertEquals(TileType.FOREST, tile.getType());
    }

    @Test
    void testSetForestTreesZeroChangesForestToGrass() {
        tile.setType(TileType.FOREST);
        tile.setForestTrees(3);
        assertEquals(TileType.FOREST, tile.getType());

        tile.setForestTrees(0);
        assertEquals(TileType.GRASS, tile.getType());
    }

    @Test
    void testSetForestTreesDoesNotChangeNonForestType() {
        tile.setType(TileType.ROAD);
        tile.setForestTrees(3);

        // Should become forest when trees > 0
        assertEquals(TileType.FOREST, tile.getType());
    }

    @Test
    void testForestTreesIncrementally() {
        for (int i = 0; i <= 4; i++) {
            tile.setForestTrees(i);
            assertEquals(Math.min(4, i), tile.getForestTrees());
        }
    }

    @Test
    void testForestTreesDecrementally() {
        tile.setForestTrees(4);

        for (int i = 4; i >= 0; i--) {
            tile.setForestTrees(i);
            assertEquals(Math.max(0, i), tile.getForestTrees());
        }
    }

    // ==================== Integration Tests ====================

    @Test
    void testTileLifecycle() {
        // Start as grass
        Tile lifeTile = new Tile(0, 0, TileType.GRASS);
        assertTrue(lifeTile.isWalkable());

        // Grow forest
        lifeTile.setForestTrees(3);
        assertEquals(TileType.FOREST, lifeTile.getType());
        assertEquals(3, lifeTile.getForestTrees());

        // Clear forest
        lifeTile.setForestTrees(0);
        assertEquals(TileType.GRASS, lifeTile.getType());

        // Build road
        lifeTile.setType(TileType.ROAD);
        assertEquals(TileType.ROAD, lifeTile.getType());

        // Place building
        Building building = new Garage(0, 0);
        lifeTile.setPlacedBuilding(building);
        lifeTile.setOccupied(true);
        assertTrue(lifeTile.isOccupied());
        assertNotNull(lifeTile.getPlacedBuilding());
    }

    @Test
    void testMultipleTilesIndependent() {
        Tile tile1 = new Tile(0, 0, TileType.GRASS);
        Tile tile2 = new Tile(1, 1, TileType.ROAD);

        tile1.setOccupied(true);
        tile1.setForestTrees(3);

        // tile2 should be independent
        assertFalse(tile2.isOccupied());
        assertEquals(0, tile2.getForestTrees());
        assertEquals(TileType.ROAD, tile2.getType());
    }

    @Test
    void testCoordinateUniqueness() {
        Tile tile1 = new Tile(5, 10, TileType.GRASS);
        Tile tile2 = new Tile(5, 10, TileType.GRASS);

        // Same coordinates but different objects
        assertNotSame(tile1, tile2);
        assertEquals(tile1.getX(), tile2.getX());
        assertEquals(tile1.getY(), tile2.getY());
    }

    // ==================== Edge Cases ====================

    @Test
    void testSetForestTreesBoundaryValues() {
        int[] testValues = {-100, -1, 0, 1, 2, 3, 4, 5, 10, 100};
        int[] expectedValues = {0, 0, 0, 1, 2, 3, 4, 4, 4, 4};

        for (int i = 0; i < testValues.length; i++) {
            Tile testTile = new Tile(0, 0, TileType.GRASS);
            testTile.setForestTrees(testValues[i]);
            assertEquals(expectedValues[i], testTile.getForestTrees(),
                "Failed for input: " + testValues[i]);
        }
    }

    @Test
    void testMultipleTypeChanges() {
        tile.setType(TileType.GRASS);
        tile.setType(TileType.FOREST);
        tile.setType(TileType.ROAD);
        tile.setType(TileType.GRASS);

        assertEquals(TileType.GRASS, tile.getType());
        assertEquals(0, tile.getForestTrees());
    }

    @Test
    void testBuildingPersistsThroughTypeChange() {
        Building building = new Garage(5, 10);
        tile.setPlacedBuilding(building);

        tile.setType(TileType.ROAD);
        assertEquals(building, tile.getPlacedBuilding());
    }

    @Test
    void testOccupiedPersistsThroughTypeChange() {
        tile.setOccupied(true);
        tile.setType(TileType.ROAD);
        assertTrue(tile.isOccupied());
    }

    @Test
    void testWalkablePersistsThroughManualSet() {
        tile.setWalkable(true);
        tile.setType(TileType.FOREST);
        // setWalkable was manually set to true, should persist
        assertTrue(tile.isWalkable());
    }
}
