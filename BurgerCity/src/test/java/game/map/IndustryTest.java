package game.map;

import game.resource.ResourceType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IndustryTest {

    @Test
    void testConstructorWithValidParameters() {
        Industry industry = new Industry("Test Farm", IndustryType.FARM, 10, 20, 5, 5);

        assertNotNull(industry);
        assertEquals("Test Farm", industry.getName());
        assertEquals(IndustryType.FARM, industry.getIndustryType());
        assertEquals(10, industry.getOriginX());
        assertEquals(20, industry.getOriginY());
        assertEquals(5, industry.getWidth());
        assertEquals(5, industry.getHeight());
    }

    @Test
    void testConstructorEnforcesMinimumWidth() {
        Industry industry = new Industry("Small", IndustryType.FARM, 0, 0, 1, 5);
        assertEquals(2, industry.getWidth());
    }

    @Test
    void testConstructorEnforcesMinimumHeight() {
        Industry industry = new Industry("Small", IndustryType.FARM, 0, 0, 5, 1);
        assertEquals(2, industry.getHeight());
    }

    @Test
    void testConstructorEnforcesMinimumWidthAndHeight() {
        Industry industry = new Industry("Tiny", IndustryType.FARM, 0, 0, 0, 0);
        assertEquals(2, industry.getWidth());
        assertEquals(2, industry.getHeight());
    }

    @Test
    void testConstructorWithNegativeWidthAndHeight() {
        Industry industry = new Industry("Negative", IndustryType.FARM, 0, 0, -5, -3);
        assertEquals(2, industry.getWidth());
        assertEquals(2, industry.getHeight());
    }

    @Test
    void testInitialProductivityIsOne() {
        Industry industry = new Industry("Test", IndustryType.FARM, 0, 0, 3, 3);
        assertEquals(1.0, industry.getProductivity());
    }

    @Test
    void testStorageInitiallyEmpty() {
        Industry industry = new Industry("Test", IndustryType.FARM, 0, 0, 3, 3);
        assertNotNull(industry.getStorage());
        assertTrue(industry.getStorage().isEmpty());
    }

    @Test
    void testProfileIsSetCorrectly() {
        Industry industry = new Industry("Test", IndustryType.FARM, 0, 0, 3, 3);
        assertNotNull(industry.getProfile());
        assertEquals(0.35, industry.getProfile().getBaseUnitsPerSecond());
    }

    @Test
    void testFarmProfile() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);
        IndustryProfile profile = farm.getProfile();

        assertTrue(profile.getInputsPerUnit().isEmpty());
        assertEquals(1, profile.getOutputsPerUnit().get(ResourceType.WHEAT));
        assertEquals(0.35, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testRanchProfile() {
        Industry ranch = new Industry("Ranch", IndustryType.RANCH, 0, 0, 3, 3);
        IndustryProfile profile = ranch.getProfile();

        assertTrue(profile.getInputsPerUnit().isEmpty());
        assertEquals(1, profile.getOutputsPerUnit().get(ResourceType.MEAT));
        assertEquals(0.25, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testBakeryProfile() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);
        IndustryProfile profile = bakery.getProfile();

        assertEquals(2, profile.getInputsPerUnit().get(ResourceType.WHEAT));
        assertEquals(1, profile.getOutputsPerUnit().get(ResourceType.BREAD));
        assertEquals(0.22, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testPattyPlantProfile() {
        Industry pattyPlant = new Industry("Patty", IndustryType.PATTY_PLANT, 0, 0, 3, 3);
        IndustryProfile profile = pattyPlant.getProfile();

        assertEquals(1, profile.getInputsPerUnit().get(ResourceType.MEAT));
        assertEquals(1, profile.getOutputsPerUnit().get(ResourceType.MEAT_PATTY));
        assertEquals(0.25, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testBurgerFactoryProfile() {
        Industry burgerFactory = new Industry("Burger", IndustryType.BURGER_FACTORY, 0, 0, 3, 3);
        IndustryProfile profile = burgerFactory.getProfile();

        assertEquals(1, profile.getInputsPerUnit().get(ResourceType.BREAD));
        assertEquals(1, profile.getInputsPerUnit().get(ResourceType.MEAT_PATTY));
        assertEquals(1, profile.getOutputsPerUnit().get(ResourceType.HAMBURGER));
        assertEquals(0.18, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testFactoryProfile() {
        Industry factory = new Industry("Factory", IndustryType.FACTORY, 0, 0, 3, 3);
        IndustryProfile profile = factory.getProfile();

        assertTrue(profile.getInputsPerUnit().isEmpty());
        assertTrue(profile.getOutputsPerUnit().isEmpty());
        assertEquals(0.0, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testNullIndustryTypeDefaultsToFactory() {
        Industry industry = new Industry("Null", null, 0, 0, 3, 3);
        IndustryProfile profile = industry.getProfile();

        assertEquals(0.0, profile.getBaseUnitsPerSecond());
    }

    @Test
    void testConsumesReturnsTrueForRequiredInput() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);
        assertTrue(bakery.consumes(ResourceType.WHEAT));
    }

    @Test
    void testConsumesReturnsFalseForNonRequiredInput() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);
        assertFalse(bakery.consumes(ResourceType.MEAT));
    }

    @Test
    void testConsumesReturnsFalseForNull() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);
        assertFalse(bakery.consumes(null));
    }

    @Test
    void testConsumesForIndustryWithNoInputs() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);
        assertFalse(farm.consumes(ResourceType.WHEAT));
        assertFalse(farm.consumes(ResourceType.MEAT));
    }

    @Test
    void testDeliverToStorage() {
        Industry industry = new Industry("Test", IndustryType.BAKERY, 0, 0, 3, 3);
        industry.deliverToStorage(ResourceType.WHEAT, 10);

        assertEquals(10, industry.getStorage().get(ResourceType.WHEAT));
    }

    @Test
    void testDeliverToStorageMultipleTimes() {
        Industry industry = new Industry("Test", IndustryType.BAKERY, 0, 0, 3, 3);
        industry.deliverToStorage(ResourceType.WHEAT, 10);
        industry.deliverToStorage(ResourceType.WHEAT, 5);

        assertEquals(15, industry.getStorage().get(ResourceType.WHEAT));
    }

    @Test
    void testTakeFromStorage() {
        Industry industry = new Industry("Test", IndustryType.FARM, 0, 0, 3, 3);
        industry.deliverToStorage(ResourceType.WHEAT, 20);

        int taken = industry.takeFromStorage(ResourceType.WHEAT, 5);
        assertEquals(5, taken);
        assertEquals(15, industry.getStorage().get(ResourceType.WHEAT));
    }

    @Test
    void testTakeFromStorageMoreThanAvailable() {
        Industry industry = new Industry("Test", IndustryType.FARM, 0, 0, 3, 3);
        industry.deliverToStorage(ResourceType.WHEAT, 5);

        int taken = industry.takeFromStorage(ResourceType.WHEAT, 10);
        assertEquals(5, taken);
        assertEquals(0, industry.getStorage().get(ResourceType.WHEAT));
    }

    @Test
    void testTakeFromEmptyStorage() {
        Industry industry = new Industry("Test", IndustryType.FARM, 0, 0, 3, 3);

        int taken = industry.takeFromStorage(ResourceType.WHEAT, 10);
        assertEquals(0, taken);
    }

    @Test
    void testOccupiesWithinBounds() {
        Industry industry = new Industry("Test", IndustryType.FARM, 10, 20, 5, 5);

        assertTrue(industry.occupies(10, 20));
        assertTrue(industry.occupies(14, 24));
        assertTrue(industry.occupies(12, 22));
    }

    @Test
    void testOccupiesOutsideBounds() {
        Industry industry = new Industry("Test", IndustryType.FARM, 10, 20, 5, 5);

        assertFalse(industry.occupies(9, 20));
        assertFalse(industry.occupies(15, 20));
        assertFalse(industry.occupies(10, 19));
        assertFalse(industry.occupies(10, 25));
    }

    @Test
    void testOccupiesAtBoundaryEdges() {
        Industry industry = new Industry("Test", IndustryType.FARM, 10, 20, 5, 5);

        assertTrue(industry.occupies(10, 20));
        assertFalse(industry.occupies(15, 25));
        assertFalse(industry.occupies(15, 20));
        assertFalse(industry.occupies(10, 25));
    }

    @Test
    void testUpdateWithZeroDeltaDoesNothing() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);
        double initialProductivity = farm.getProductivity();

        farm.update(0.0);

        assertEquals(initialProductivity, farm.getProductivity());
        assertTrue(farm.getStorage().isEmpty());
    }

    @Test
    void testUpdateWithNegativeDeltaDoesNothing() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);
        double initialProductivity = farm.getProductivity();

        farm.update(-1.0);

        assertEquals(initialProductivity, farm.getProductivity());
        assertTrue(farm.getStorage().isEmpty());
    }

    @Test
    void testUpdateWithZeroProductionRate() {
        Industry factory = new Industry("Factory", IndustryType.FACTORY, 0, 0, 3, 3);
        factory.update(1.0);

        assertTrue(factory.getStorage().isEmpty());
    }

    @Test
    void testUpdateProducesOutputForFarm() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);

        // Update for enough time to produce at least one unit
        farm.update(3.0);

        assertTrue(farm.getStorage().get(ResourceType.WHEAT) > 0);
    }

    @Test
    void testUpdateConsumesInputsAndProducesOutputs() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);
        bakery.deliverToStorage(ResourceType.WHEAT, 100);

        // Update for enough time to produce
        bakery.update(5.0);

        assertTrue(bakery.getStorage().get(ResourceType.WHEAT) < 100);
        assertTrue(bakery.getStorage().get(ResourceType.BREAD) > 0);
    }

    @Test
    void testUpdateDoesNotProduceWithoutInputs() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);

        bakery.update(5.0);

        assertEquals(0, bakery.getStorage().get(ResourceType.BREAD));
    }

    @Test
    void testUpdateConsumesCorrectAmountOfInputs() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);
        bakery.deliverToStorage(ResourceType.WHEAT, 10);

        // Force production of exactly 1 unit
        bakery.update(10.0);

        int wheatRemaining = bakery.getStorage().get(ResourceType.WHEAT);
        int breadProduced = bakery.getStorage().get(ResourceType.BREAD);

        // Bakery consumes 2 wheat per 1 bread
        assertTrue(breadProduced > 0);
        assertEquals(10 - (breadProduced * 2), wheatRemaining);
    }

    @Test
    void testUpdateStopsWhenInputsRunOut() {
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 0, 0, 3, 3);
        bakery.deliverToStorage(ResourceType.WHEAT, 3);

        // Try to produce for a long time
        bakery.update(100.0);

        // Should only produce 1 bread (consuming 2 wheat), leaving 1 wheat
        assertEquals(1, bakery.getStorage().get(ResourceType.BREAD));
        assertEquals(1, bakery.getStorage().get(ResourceType.WHEAT));
    }

    @Test
    void testUpdateWithMultipleInputs() {
        Industry burgerFactory = new Industry("Burger", IndustryType.BURGER_FACTORY, 0, 0, 3, 3);
        burgerFactory.deliverToStorage(ResourceType.BREAD, 10);
        burgerFactory.deliverToStorage(ResourceType.MEAT_PATTY, 10);

        burgerFactory.update(10.0);

        assertTrue(burgerFactory.getStorage().get(ResourceType.HAMBURGER) > 0);
        assertTrue(burgerFactory.getStorage().get(ResourceType.BREAD) < 10);
        assertTrue(burgerFactory.getStorage().get(ResourceType.MEAT_PATTY) < 10);
    }

    @Test
    void testUpdateStopsWhenOneInputRunsOut() {
        Industry burgerFactory = new Industry("Burger", IndustryType.BURGER_FACTORY, 0, 0, 3, 3);
        burgerFactory.deliverToStorage(ResourceType.BREAD, 2);
        burgerFactory.deliverToStorage(ResourceType.MEAT_PATTY, 10);

        burgerFactory.update(100.0);

        // Should produce 2 hamburgers, consuming all bread
        assertEquals(2, burgerFactory.getStorage().get(ResourceType.HAMBURGER));
        assertEquals(0, burgerFactory.getStorage().get(ResourceType.BREAD));
        assertEquals(8, burgerFactory.getStorage().get(ResourceType.MEAT_PATTY));
    }

    @Test
    void testProductivityChangesOverTime() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);
        double initialProductivity = farm.getProductivity();

        // Update multiple times
        for (int i = 0; i < 100; i++) {
            farm.update(0.1);
        }

        // Productivity should have changed from initial value
        assertNotEquals(initialProductivity, farm.getProductivity());
    }

    @Test
    void testProductivityStaysBetweenBounds() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);

        // Update many times with various deltas
        for (int i = 0; i < 1000; i++) {
            farm.update(0.1);
            double productivity = farm.getProductivity();
            assertTrue(productivity >= 0.4 && productivity <= 1.6,
                    "Productivity " + productivity + " out of bounds");
        }
    }

    @Test
    void testMultipleUpdatesAccumulateProduction() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);

        farm.update(1.0);
        int firstAmount = farm.getStorage().get(ResourceType.WHEAT);

        farm.update(1.0);
        int secondAmount = farm.getStorage().get(ResourceType.WHEAT);

        assertTrue(secondAmount >= firstAmount);
    }

    @Test
    void testSmallDeltaTimesEventuallyProduceUnits() {
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);

        // Update many times with very small delta
        for (int i = 0; i < 1000; i++) {
            farm.update(0.01);
        }

        assertTrue(farm.getStorage().get(ResourceType.WHEAT) > 0);
    }

    @Test
    void testDifferentIndustriesHaveDifferentRandomSeeds() {
        Industry farm1 = new Industry("Farm1", IndustryType.FARM, 0, 0, 3, 3);
        Industry farm2 = new Industry("Farm2", IndustryType.FARM, 100, 100, 3, 3);

        // Update both for a while
        for (int i = 0; i < 100; i++) {
            farm1.update(0.1);
            farm2.update(0.1);
        }

        // They should have different productivity due to different random seeds
        assertNotEquals(farm1.getProductivity(), farm2.getProductivity());
    }

    @Test
    void testSameCoordinatesProduceSameRandomness() {
        Industry farm1 = new Industry("Farm1", IndustryType.FARM, 50, 50, 3, 3);
        Industry farm2 = new Industry("Farm1", IndustryType.FARM, 50, 50, 3, 3);

        // Update both identically
        for (int i = 0; i < 100; i++) {
            farm1.update(0.1);
            farm2.update(0.1);
        }

        // Should have same productivity due to same seed
        assertEquals(farm1.getProductivity(), farm2.getProductivity());
    }

    @Test
    void testCompleteProductionChain() {
        // Create a farm and bakery
        Industry farm = new Industry("Farm", IndustryType.FARM, 0, 0, 3, 3);
        Industry bakery = new Industry("Bakery", IndustryType.BAKERY, 10, 10, 3, 3);

        // Farm produces wheat
        farm.update(10.0);
        int wheat = farm.getStorage().get(ResourceType.WHEAT);
        assertTrue(wheat > 0);

        // Transfer wheat to bakery
        int transferred = farm.takeFromStorage(ResourceType.WHEAT, wheat);
        bakery.deliverToStorage(ResourceType.WHEAT, transferred);

        // Bakery produces bread
        bakery.update(10.0);
        assertTrue(bakery.getStorage().get(ResourceType.BREAD) > 0);
    }

    @Test
    void testNegativeCoordinates() {
        Industry industry = new Industry("Test", IndustryType.FARM, -10, -20, 3, 3);
        assertEquals(-10, industry.getOriginX());
        assertEquals(-20, industry.getOriginY());
        assertTrue(industry.occupies(-10, -20));
        assertFalse(industry.occupies(-11, -20));
    }

    @Test
    void testLargeCoordinates() {
        Industry industry = new Industry("Test", IndustryType.FARM, 10000, 20000, 3, 3);
        assertEquals(10000, industry.getOriginX());
        assertEquals(20000, industry.getOriginY());
        assertTrue(industry.occupies(10000, 20000));
    }

    @Test
    void testLargeDimensions() {
        Industry industry = new Industry("Large", IndustryType.FARM, 0, 0, 100, 200);
        assertEquals(100, industry.getWidth());
        assertEquals(200, industry.getHeight());
        assertTrue(industry.occupies(50, 100));
        assertTrue(industry.occupies(99, 199));
        assertFalse(industry.occupies(100, 200));
    }
}
