package game.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndustryTypeTest {

    @Test
    void testAllIndustryTypesExist() {
        assertNotNull(IndustryType.FARM);
        assertNotNull(IndustryType.RANCH);
        assertNotNull(IndustryType.BAKERY);
        assertNotNull(IndustryType.PATTY_PLANT);
        assertNotNull(IndustryType.BURGER_FACTORY);
        assertNotNull(IndustryType.FACTORY);
    }

    @Test
    void testIndustryTypeValues() {
        IndustryType[] values = IndustryType.values();
        assertEquals(6, values.length);
    }

    @Test
    void testIndustryTypeValueOf() {
        assertEquals(IndustryType.FARM, IndustryType.valueOf("FARM"));
        assertEquals(IndustryType.RANCH, IndustryType.valueOf("RANCH"));
        assertEquals(IndustryType.BAKERY, IndustryType.valueOf("BAKERY"));
        assertEquals(IndustryType.PATTY_PLANT, IndustryType.valueOf("PATTY_PLANT"));
        assertEquals(IndustryType.BURGER_FACTORY, IndustryType.valueOf("BURGER_FACTORY"));
        assertEquals(IndustryType.FACTORY, IndustryType.valueOf("FACTORY"));
    }

    @Test
    void testIndustryTypeValueOfInvalidThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            IndustryType.valueOf("INVALID_TYPE");
        });
    }

    @Test
    void testIndustryTypeEquality() {
        assertEquals(IndustryType.FARM, IndustryType.FARM);
        assertNotEquals(IndustryType.FARM, IndustryType.RANCH);
    }

    @Test
    void testIndustryTypeInSwitch() {
        IndustryType type = IndustryType.BAKERY;
        String result = switch (type) {
            case FARM -> "Farm";
            case RANCH -> "Ranch";
            case BAKERY -> "Bakery";
            case PATTY_PLANT -> "Patty Plant";
            case BURGER_FACTORY -> "Burger Factory";
            case FACTORY -> "Factory";
        };
        assertEquals("Bakery", result);
    }

    @Test
    void testIndustryTypeOrdinal() {
        assertTrue(IndustryType.FARM.ordinal() >= 0);
        assertTrue(IndustryType.FACTORY.ordinal() < 6);
    }

    @Test
    void testIndustryTypeName() {
        assertEquals("FARM", IndustryType.FARM.name());
        assertEquals("RANCH", IndustryType.RANCH.name());
        assertEquals("BAKERY", IndustryType.BAKERY.name());
        assertEquals("PATTY_PLANT", IndustryType.PATTY_PLANT.name());
        assertEquals("BURGER_FACTORY", IndustryType.BURGER_FACTORY.name());
        assertEquals("FACTORY", IndustryType.FACTORY.name());
    }
}
