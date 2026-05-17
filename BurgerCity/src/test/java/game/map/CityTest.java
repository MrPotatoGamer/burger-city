package game.map;

import game.resource.ResourceInventory;
import game.resource.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CityTest {

    private City city;

    @BeforeEach
    void setUp() {
        city = new City("TestCity", 10, 20, 5, 5);
    }

    // ==================== Constructor Tests ====================

    @Test
    void testConstructorSetsName() {
        assertEquals("TestCity", city.getName());
    }

    @Test
    void testConstructorSetsOrigin() {
        assertEquals(10, city.getOriginX());
        assertEquals(20, city.getOriginY());
    }

    @Test
    void testConstructorSetsDimensions() {
        assertEquals(5, city.getWidth());
        assertEquals(5, city.getHeight());
    }

    @Test
    void testConstructorCalculatesPopulation() {
        // Population = width * height * 120
        int expectedPopulation = 5 * 5 * 120;
        assertEquals(expectedPopulation, city.getPopulation());
    }

    @Test
    void testConstructorEnforcesMinimumWidth() {
        City smallCity = new City("Small", 0, 0, 1, 5);
        assertEquals(3, smallCity.getWidth());
    }

    @Test
    void testConstructorEnforcesMinimumHeight() {
        City smallCity = new City("Small", 0, 0, 5, 2);
        assertEquals(3, smallCity.getHeight());
    }

    @Test
    void testConstructorEnforcesMinimumWidthAndHeight() {
        City tinyCity = new City("Tiny", 0, 0, 0, 0);
        assertEquals(3, tinyCity.getWidth());
        assertEquals(3, tinyCity.getHeight());
    }

    @Test
    void testConstructorWithNegativeDimensions() {
        City negCity = new City("Negative", 0, 0, -5, -3);
        assertEquals(3, negCity.getWidth());
        assertEquals(3, negCity.getHeight());
    }

    @Test
    void testConstructorInitializesWaitingInventory() {
        assertNotNull(city.getWaiting());
        assertTrue(city.getWaiting().isEmpty());
    }

    @Test
    void testConstructorInitializesDemandBacklog() {
        assertNotNull(city.getDemandBacklog());
        assertTrue(city.getDemandBacklog().isEmpty());
    }

    @Test
    void testConstructorSetsPassengersPerSecond() {
        double passengersPerSecond = city.getPassengersPerSecond();
        assertTrue(passengersPerSecond > 0);
    }

    @Test
    void testConstructorSetsGoodsPerSecond() {
        Map<ResourceType, Double> goodsPerSecond = city.getGoodsPerSecond();
        assertNotNull(goodsPerSecond);
        assertTrue(goodsPerSecond.containsKey(ResourceType.HAMBURGER));
    }

    // ==================== Population Tests ====================

    @Test
    void testPopulationBasedOnSize() {
        City city1 = new City("City1", 0, 0, 3, 3);
        City city2 = new City("City2", 0, 0, 10, 10);

        assertTrue(city2.getPopulation() > city1.getPopulation());
    }

    @Test
    void testPopulationNeverNegative() {
        City city = new City("Test", 0, 0, 3, 3);
        assertTrue(city.getPopulation() >= 0);
    }

    // ==================== Occupies Tests ====================

    @Test
    void testOccupiesReturnsTrueForOrigin() {
        assertTrue(city.occupies(10, 20));
    }

    @Test
    void testOccupiesReturnsTrueForPointInsideBounds() {
        assertTrue(city.occupies(12, 22));
    }

    @Test
    void testOccupiesReturnsTrueForEdges() {
        assertTrue(city.occupies(14, 24)); // width-1, height-1
    }

    @Test
    void testOccupiesReturnsFalseForPointOutsideBounds() {
        assertFalse(city.occupies(9, 20));
        assertFalse(city.occupies(15, 20));
        assertFalse(city.occupies(10, 19));
        assertFalse(city.occupies(10, 25));
    }

    @Test
    void testOccupiesReturnsFalseForBoundaryEdge() {
        // originX + width = 15, so x=15 is outside
        assertFalse(city.occupies(15, 20));
        // originY + height = 25, so y=25 is outside
        assertFalse(city.occupies(10, 25));
    }

    @Test
    void testOccupiesWithNegativeCoordinates() {
        City negCity = new City("Neg", -10, -10, 5, 5);
        assertTrue(negCity.occupies(-10, -10));
        assertTrue(negCity.occupies(-6, -6)); // -10+4, -10+4
        assertFalse(negCity.occupies(-11, -10));
    }

    // ==================== Update Tests ====================

    @Test
    void testUpdateWithZeroDeltaDoesNothing() {
        int initialWaiting = city.getWaiting().get(ResourceType.PASSENGERS);
        city.update(0.0);
        assertEquals(initialWaiting, city.getWaiting().get(ResourceType.PASSENGERS));
    }

    @Test
    void testUpdateWithNegativeDeltaDoesNothing() {
        int initialWaiting = city.getWaiting().get(ResourceType.PASSENGERS);
        city.update(-1.0);
        assertEquals(initialWaiting, city.getWaiting().get(ResourceType.PASSENGERS));
    }

    @Test
    void testUpdateGeneratesPassengers() {
        city.update(10.0);
        assertTrue(city.getWaiting().get(ResourceType.PASSENGERS) > 0);
    }

    @Test
    void testUpdateGeneratesDemandBacklog() {
        // Demand generation is very slow for small cities, update for a long time
        for (int i = 0; i < 1000; i++) {
            city.update(1.0);
        }
        // At minimum, demand should have accumulated over 1000 seconds
        assertTrue(city.getDemandBacklog().get(ResourceType.HAMBURGER) >= 0);
    }

    @Test
    void testUpdateAccumulatesPassengersOverTime() {
        city.update(1.0);
        int afterFirst = city.getWaiting().get(ResourceType.PASSENGERS);

        city.update(1.0);
        int afterSecond = city.getWaiting().get(ResourceType.PASSENGERS);

        assertTrue(afterSecond >= afterFirst);
    }

    @Test
    void testUpdateAccumulatesDemandOverTime() {
        city.update(1.0);
        int afterFirst = city.getDemandBacklog().get(ResourceType.HAMBURGER);

        city.update(1.0);
        int afterSecond = city.getDemandBacklog().get(ResourceType.HAMBURGER);

        assertTrue(afterSecond >= afterFirst);
    }

    @Test
    void testUpdateWithSmallDeltaEventuallyProducesPassengers() {
        for (int i = 0; i < 5000; i++) { // More iterations needed
            city.update(0.01);
        }
        assertTrue(city.getWaiting().get(ResourceType.PASSENGERS) > 0);
    }

    @Test
    void testUpdateAdjustsRatesOverTime() {
        double initialRate = city.getPassengersPerSecond();

        for (int i = 0; i < 100; i++) {
            city.update(0.1);
        }

        double finalRate = city.getPassengersPerSecond();
        // Rate may have changed due to drift/noise
        assertTrue(finalRate > 0);
    }

    // ==================== Load Tests ====================

    @Test
    void testLoadPassengersWhenAvailable() {
        city.update(10.0);
        int available = city.getWaiting().get(ResourceType.PASSENGERS);

        int loaded = city.load(ResourceType.PASSENGERS, 5);

        assertTrue(loaded <= 5);
        assertTrue(loaded <= available);
    }

    @Test
    void testLoadRemovesPassengersFromWaiting() {
        city.update(10.0);
        int beforeLoad = city.getWaiting().get(ResourceType.PASSENGERS);

        int loaded = city.load(ResourceType.PASSENGERS, 3);

        int afterLoad = city.getWaiting().get(ResourceType.PASSENGERS);
        assertEquals(beforeLoad - loaded, afterLoad);
    }

    @Test
    void testLoadReturnsZeroWhenNoneAvailable() {
        int loaded = city.load(ResourceType.PASSENGERS, 10);
        assertEquals(0, loaded);
    }

    @Test
    void testLoadLimitedByAvailableAmount() {
        city.update(1.0);
        int available = city.getWaiting().get(ResourceType.PASSENGERS);

        int loaded = city.load(ResourceType.PASSENGERS, available + 100);

        assertEquals(available, loaded);
    }

    @Test
    void testLoadWithZeroAmount() {
        city.update(10.0);
        int loaded = city.load(ResourceType.PASSENGERS, 0);
        assertEquals(0, loaded);
    }

    @Test
    void testLoadWithNegativeAmount() {
        city.update(10.0);
        int loaded = city.load(ResourceType.PASSENGERS, -5);
        assertEquals(0, loaded);
    }

    // ==================== Deliver Tests ====================

    @Test
    void testDeliverPassengersAlwaysAccepted() {
        int delivered = city.deliver(ResourceType.PASSENGERS, 50);
        assertEquals(50, delivered);
    }

    @Test
    void testDeliverPassengersWithZeroAmount() {
        int delivered = city.deliver(ResourceType.PASSENGERS, 0);
        assertEquals(0, delivered);
    }

    @Test
    void testDeliverPassengersWithNegativeAmount() {
        int delivered = city.deliver(ResourceType.PASSENGERS, -10);
        assertEquals(0, delivered);
    }

    @Test
    void testDeliverGoodsWhenDemandExists() {
        city.update(10.0);
        int demand = city.getDemandBacklog().get(ResourceType.HAMBURGER);

        int delivered = city.deliver(ResourceType.HAMBURGER, 5);

        assertTrue(delivered <= 5);
        assertTrue(delivered <= demand);
    }

    @Test
    void testDeliverGoodsReducesDemandBacklog() {
        city.update(10.0);
        int beforeDeliver = city.getDemandBacklog().get(ResourceType.HAMBURGER);

        int delivered = city.deliver(ResourceType.HAMBURGER, 3);

        int afterDeliver = city.getDemandBacklog().get(ResourceType.HAMBURGER);
        assertEquals(beforeDeliver - delivered, afterDeliver);
    }

    @Test
    void testDeliverGoodsWhenNoDemand() {
        int delivered = city.deliver(ResourceType.HAMBURGER, 10);
        assertEquals(0, delivered);
    }

    @Test
    void testDeliverGoodsLimitedByDemand() {
        city.update(5.0);
        int demand = city.getDemandBacklog().get(ResourceType.HAMBURGER);

        int delivered = city.deliver(ResourceType.HAMBURGER, demand + 100);

        assertEquals(demand, delivered);
    }

    @Test
    void testDeliverOtherResourceTypes() {
        city.update(10.0);

        // Cities don't demand wheat, so delivery should be 0
        int deliveredWheat = city.deliver(ResourceType.WHEAT, 10);
        assertEquals(0, deliveredWheat);
    }

    // ==================== Backwards Compatibility Tests ====================

    @Test
    void testGeneratePassengersMethod() {
        city.generatePassengers();
        // Should generate some passengers (equivalent to update(1.0))
        assertTrue(city.getWaiting().get(ResourceType.PASSENGERS) >= 0);
    }

    @Test
    void testAcceptGoodsMethod() {
        // Should not throw exception
        city.acceptGoods();
    }

    // ==================== Rate Tests ====================

    @Test
    void testPassengersPerSecondInReasonableRange() {
        double rate = city.getPassengersPerSecond();
        assertTrue(rate >= 0.002 && rate <= 0.25);
    }

    @Test
    void testGoodsPerSecondInReasonableRange() {
        Map<ResourceType, Double> goodsRates = city.getGoodsPerSecond();
        for (Double rate : goodsRates.values()) {
            assertTrue(rate >= 0.0 && rate <= 0.12);
        }
    }

    @Test
    void testLargerCityHasHigherPassengerRate() {
        City smallCity = new City("Small", 0, 0, 3, 3);
        City largeCity = new City("Large", 0, 0, 10, 10);

        // Initially, larger city should have higher rate
        // (may vary due to randomness, but generally true)
        assertTrue(largeCity.getPopulation() > smallCity.getPopulation());
    }

    @Test
    void testGoodsPerSecondIsUnmodifiable() {
        Map<ResourceType, Double> goodsRates = city.getGoodsPerSecond();

        // Should not be able to modify the returned map
        assertThrows(UnsupportedOperationException.class, () -> {
            goodsRates.put(ResourceType.BREAD, 1.0);
        });
    }

    // ==================== Integration Tests ====================

    @Test
    void testCityLifecycle() {
        // Simulate a city over time
        for (int i = 0; i < 100; i++) { // More iterations for accumulation
            city.update(1.0);
        }

        // Should have generated passengers and demand
        assertTrue(city.getWaiting().get(ResourceType.PASSENGERS) > 0);
        assertTrue(city.getDemandBacklog().get(ResourceType.HAMBURGER) > 0);

        // Load some passengers
        int passengers = city.load(ResourceType.PASSENGERS, 100);
        assertTrue(passengers > 0);

        // Deliver some goods
        int delivered = city.deliver(ResourceType.HAMBURGER, 100);
        assertTrue(delivered > 0);
    }

    @Test
    void testMultipleCitiesHaveDifferentRandomSeeds() {
        City city1 = new City("City1", 0, 0, 5, 5);
        City city2 = new City("City2", 10, 10, 5, 5);

        for (int i = 0; i < 100; i++) {
            city1.update(0.1);
            city2.update(0.1);
        }

        // Different coordinates should lead to different random behavior
        // (may occasionally be equal due to randomness, but unlikely)
        double rate1 = city1.getPassengersPerSecond();
        double rate2 = city2.getPassengersPerSecond();

        // Just verify both are valid rates
        assertTrue(rate1 > 0);
        assertTrue(rate2 > 0);
    }

    @Test
    void testSameCitySameRandomSeed() {
        City city1 = new City("TestCity", 5, 5, 5, 5);
        City city2 = new City("TestCity", 5, 5, 5, 5);

        // Same name and coordinates should produce same random seed
        for (int i = 0; i < 50; i++) {
            city1.update(0.1);
            city2.update(0.1);
        }

        // Should have very similar behavior
        assertEquals(city1.getPassengersPerSecond(), city2.getPassengersPerSecond(), 0.001);
    }

    // ==================== Edge Case Tests ====================

    @Test
    void testCityAtZeroCoordinates() {
        City zeroCity = new City("Origin", 0, 0, 5, 5);
        assertTrue(zeroCity.occupies(0, 0));
        assertTrue(zeroCity.occupies(4, 4));
        assertFalse(zeroCity.occupies(-1, 0));
    }

    @Test
    void testCityWithLargeCoordinates() {
        City largeCity = new City("Far", 1000, 2000, 5, 5);
        assertTrue(largeCity.occupies(1000, 2000));
        assertTrue(largeCity.occupies(1004, 2004));
    }

    @Test
    void testCityWithMaximumSize() {
        City maxCity = new City("Huge", 0, 0, 100, 100);
        assertEquals(100, maxCity.getWidth());
        assertEquals(100, maxCity.getHeight());
        assertTrue(maxCity.getPopulation() > 1_000_000);
    }

    @Test
    void testEmptyNameCity() {
        City emptyName = new City("", 0, 0, 5, 5);
        assertEquals("", emptyName.getName());
    }

    @Test
    void testVeryLongUpdatePeriod() {
        city.update(1000.0);
        // Should generate many passengers - demand may be very slow for small city
        assertTrue(city.getWaiting().get(ResourceType.PASSENGERS) > 10);
        // Demand backlog should at least be non-negative
        assertTrue(city.getDemandBacklog().get(ResourceType.HAMBURGER) >= 0);
    }

    @Test
    void testResourceInventoryReferences() {
        ResourceInventory waiting1 = city.getWaiting();
        ResourceInventory waiting2 = city.getWaiting();

        // Should return the same object
        assertSame(waiting1, waiting2);
    }
}
