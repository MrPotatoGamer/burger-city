package game.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TileTypeTest {

    // ==================== Enum Values Tests ====================

    @Test
    void testTileTypeValues() {
        TileType[] types = TileType.values();
        assertNotNull(types);
        assertTrue(types.length > 0);
    }

    @Test
    void testTileTypeGrassExists() {
        assertNotNull(TileType.GRASS);
    }

    @Test
    void testTileTypeRoadExists() {
        assertNotNull(TileType.ROAD);
    }

    @Test
    void testTileTypeForestExists() {
        assertNotNull(TileType.FOREST);
    }

    @Test
    void testTileTypeValueOf() {
        assertEquals(TileType.GRASS, TileType.valueOf("GRASS"));
        assertEquals(TileType.ROAD, TileType.valueOf("ROAD"));
        assertEquals(TileType.FOREST, TileType.valueOf("FOREST"));
    }

    @Test
    void testTileTypeValueOfInvalidThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            TileType.valueOf("INVALID");
        });
    }

    @Test
    void testTileTypeValueOfNull() {
        assertThrows(NullPointerException.class, () -> {
            TileType.valueOf(null);
        });
    }

    @Test
    void testTileTypeComparison() {
        assertNotEquals(TileType.GRASS, TileType.ROAD);
        assertNotEquals(TileType.GRASS, TileType.FOREST);
        assertNotEquals(TileType.ROAD, TileType.FOREST);
    }

    @Test
    void testTileTypeName() {
        assertEquals("GRASS", TileType.GRASS.name());
        assertEquals("ROAD", TileType.ROAD.name());
        assertEquals("FOREST", TileType.FOREST.name());
    }

    @Test
    void testTileTypeToString() {
        assertEquals("GRASS", TileType.GRASS.toString());
        assertEquals("ROAD", TileType.ROAD.toString());
        assertEquals("FOREST", TileType.FOREST.toString());
    }

    @Test
    void testTileTypeOrdinal() {
        assertTrue(TileType.GRASS.ordinal() >= 0);
        assertTrue(TileType.ROAD.ordinal() >= 0);
        assertTrue(TileType.FOREST.ordinal() >= 0);
    }

    @Test
    void testAllTileTypesUnique() {
        TileType[] types = TileType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i], types[j]);
            }
        }
    }
}
