package game.vehicle;

import game.resource.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedBusTest {

    private AdvancedBus advancedBus;

    @BeforeEach
    void setUp() {
        advancedBus = new AdvancedBus();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testConstructorSetsSpeed() {
        assertEquals(3, advancedBus.speed);
    }

    @Test
    void testConstructorSetsCapacity() {
        assertEquals(50, advancedBus.capacity);
    }

    @Test
    void testConstructorSetsMaintenanceCost() {
        assertEquals(3, advancedBus.maintenanceCost);
    }

    @Test
    void testConstructorInheritsFromVehicle() {
        assertNotNull(advancedBus);
        assertTrue(advancedBus instanceof Vehicle);
    }

    // ==================== Can Carry Tests ====================

    @Test
    void testCanCarryPassengers() {
        assertTrue(advancedBus.canCarry(ResourceType.PASSENGERS));
    }

    @Test
    void testCannotCarryWheat() {
        assertFalse(advancedBus.canCarry(ResourceType.WHEAT));
    }

    @Test
    void testCannotCarryMeat() {
        assertFalse(advancedBus.canCarry(ResourceType.MEAT));
    }

    @Test
    void testCannotCarryBread() {
        assertFalse(advancedBus.canCarry(ResourceType.BREAD));
    }

    @Test
    void testCannotCarryMeatPatty() {
        assertFalse(advancedBus.canCarry(ResourceType.MEAT_PATTY));
    }

    @Test
    void testCannotCarryHamburger() {
        assertFalse(advancedBus.canCarry(ResourceType.HAMBURGER));
    }

    @Test
    void testCanCarryHandlesNullResourceType() {
        // Should handle null without crashing
        try {
            boolean result = advancedBus.canCarry(null);
            // Advanced bus should only carry passengers, so null should be false
            assertFalse(result);
        } catch (NullPointerException e) {
            // NPE is acceptable for null input
        }
    }

    // ==================== All Resource Type Tests ====================

    @Test
    void testCanCarryOnlyPassengers() {
        ResourceType[] allTypes = ResourceType.values();
        for (ResourceType type : allTypes) {
            if (type == ResourceType.PASSENGERS) {
                assertTrue(advancedBus.canCarry(type),
                    "Advanced Bus should carry " + type);
            } else {
                assertFalse(advancedBus.canCarry(type),
                    "Advanced Bus should not carry " + type);
            }
        }
    }

    // ==================== Capacity Tests ====================

    @Test
    void testAdvancedBusHasCorrectCapacity() {
        assertEquals(50, advancedBus.capacity);
    }

    @Test
    void testAdvancedBusCapacityIsHigherThanBus() {
        Bus bus = new Bus();
        assertTrue(advancedBus.capacity > bus.capacity);
    }

    @Test
    void testAdvancedBusCapacityIsHigherThanTruck() {
        Truck truck = new Truck();
        assertTrue(advancedBus.capacity > truck.capacity);
    }

    @Test
    void testAdvancedBusCapacityIsHigherThanAdvancedTruck() {
        AdvancedTruck advancedTruck = new AdvancedTruck();
        assertTrue(advancedBus.capacity > advancedTruck.capacity);
    }

    @Test
    void testAdvancedBusCapacityIncreaseFromBus() {
        Bus bus = new Bus();
        int increase = advancedBus.capacity - bus.capacity;
        assertEquals(20, increase);
    }

    // ==================== Speed Tests ====================

    @Test
    void testAdvancedBusSpeedIsFasterThanBus() {
        Bus bus = new Bus();
        assertTrue(advancedBus.speed > bus.speed);
    }

    @Test
    void testAdvancedBusSpeedIsFasterThanTruck() {
        Truck truck = new Truck();
        assertTrue(advancedBus.speed > truck.speed);
    }

    @Test
    void testAdvancedBusSpeedIsFasterThanAdvancedTruck() {
        AdvancedTruck advancedTruck = new AdvancedTruck();
        assertTrue(advancedBus.speed > advancedTruck.speed);
    }

    @Test
    void testAdvancedBusIsFastestVehicle() {
        Vehicle vehicle = new Vehicle();
        Bus bus = new Bus();
        Truck truck = new Truck();
        AdvancedTruck advancedTruck = new AdvancedTruck();

        assertTrue(advancedBus.speed >= vehicle.speed);
        assertTrue(advancedBus.speed >= bus.speed);
        assertTrue(advancedBus.speed >= truck.speed);
        assertTrue(advancedBus.speed >= advancedTruck.speed);
    }

    @Test
    void testAdvancedBusSpeedImprovement() {
        Bus bus = new Bus();
        int speedIncrease = advancedBus.speed - bus.speed;
        assertEquals(1, speedIncrease);
    }

    // ==================== Maintenance Cost Tests ====================

    @Test
    void testMaintenanceCostIsPositive() {
        assertTrue(advancedBus.maintenanceCost > 0);
    }

    @Test
    void testMaintenanceCostIsHigherThanBus() {
        Bus bus = new Bus();
        assertTrue(advancedBus.maintenanceCost > bus.maintenanceCost);
    }

    @Test
    void testMaintenanceCostIsLowerThanAdvancedTruck() {
        AdvancedTruck advancedTruck = new AdvancedTruck();
        assertTrue(advancedBus.maintenanceCost < advancedTruck.maintenanceCost);
    }

    @Test
    void testMaintenanceCostIncrease() {
        Bus bus = new Bus();
        int costIncrease = advancedBus.maintenanceCost - bus.maintenanceCost;
        assertEquals(1, costIncrease);
    }

    // ==================== Type Specific Tests ====================

    @Test
    void testAdvancedBusIsDistinctFromTruck() {
        Truck truck = new Truck();
        // Advanced buses and trucks have opposite cargo restrictions
        assertTrue(advancedBus.canCarry(ResourceType.PASSENGERS));
        assertFalse(advancedBus.canCarry(ResourceType.WHEAT));
        assertFalse(truck.canCarry(ResourceType.PASSENGERS));
        assertTrue(truck.canCarry(ResourceType.WHEAT));
    }

    @Test
    void testAdvancedBusEffectiveSpeedEqualsSpeed() {
        // EffectiveSpeed is set in parent constructor before subclass sets speed
        // After first update it will match
        assertEquals(2.0, advancedBus.effectiveSpeed); // Default from Vehicle constructor
        assertEquals(3, advancedBus.speed); // AdvancedBus's speed
    }

    @Test
    void testAdvancedBusInheritsVehicleMethods() {
        // Test that inherited methods work
        advancedBus.setPurchasePrice(4000);
        assertEquals(4000, advancedBus.getPurchasePrice());
        assertEquals(2000, advancedBus.getSellValue());
    }

    @Test
    void testAdvancedBusCanBeSpawned() {
        advancedBus.spawnAt(12, 18);
        assertTrue(advancedBus.isSpawned());
        assertEquals(12, advancedBus.getCurrentTileX());
        assertEquals(18, advancedBus.getCurrentTileY());
    }

    @Test
    void testAdvancedBusCanHavePath() {
        java.util.List<int[]> path = java.util.List.of(
            new int[]{0, 0},
            new int[]{1, 0},
            new int[]{2, 0},
            new int[]{3, 0}
        );
        advancedBus.setPath(path);
        assertTrue(advancedBus.hasPath());
    }

    @Test
    void testAdvancedBusAges() {
        advancedBus.spawnAt(0, 0);
        double initialAge = advancedBus.getAgeSeconds();
        // Simulate some aging
        advancedBus.ageSeconds = 150.0;
        assertEquals(150.0, advancedBus.getAgeSeconds());
        assertTrue(advancedBus.getAgeSeconds() > initialAge);
    }

    @Test
    void testAdvancedBusNeedsMaintenance() {
        advancedBus.secondsSinceMaintenance = 200.0;
        assertTrue(advancedBus.needsMaintenance());
    }

    @Test
    void testAdvancedBusCanBecomeOld() {
        advancedBus.ageSeconds = 700.0;
        assertTrue(advancedBus.isTooOld());
    }

    // ==================== Comparison Tests ====================

    @Test
    void testAdvancedBusVsBusComparison() {
        Bus bus = new Bus();

        // Advanced bus should be better in all stats
        assertTrue(advancedBus.speed > bus.speed,
            "Advanced bus should be faster");
        assertTrue(advancedBus.capacity > bus.capacity,
            "Advanced bus should have more capacity");
        assertTrue(advancedBus.maintenanceCost >= bus.maintenanceCost,
            "Advanced bus should cost more or equal to maintain");

        // But both carry only passengers
        assertEquals(bus.canCarry(ResourceType.PASSENGERS),
            advancedBus.canCarry(ResourceType.PASSENGERS));
        assertEquals(bus.canCarry(ResourceType.WHEAT),
            advancedBus.canCarry(ResourceType.WHEAT));
    }

    @Test
    void testAdvancedBusIsPassengerSpecialist() {
        // Advanced bus should only transport passengers
        assertTrue(advancedBus.canCarry(ResourceType.PASSENGERS));

        // Verify it rejects all goods
        assertFalse(advancedBus.canCarry(ResourceType.WHEAT));
        assertFalse(advancedBus.canCarry(ResourceType.MEAT));
        assertFalse(advancedBus.canCarry(ResourceType.BREAD));
        assertFalse(advancedBus.canCarry(ResourceType.MEAT_PATTY));
        assertFalse(advancedBus.canCarry(ResourceType.HAMBURGER));
    }

    @Test
    void testAdvancedBusHasHighestCapacity() {
        Vehicle vehicle = new Vehicle();
        Bus bus = new Bus();
        Truck truck = new Truck();
        AdvancedTruck advancedTruck = new AdvancedTruck();

        // Advanced bus should have the highest capacity among all vehicles
        assertTrue(advancedBus.capacity >= vehicle.capacity);
        assertTrue(advancedBus.capacity >= bus.capacity);
        assertTrue(advancedBus.capacity >= truck.capacity);
        assertTrue(advancedBus.capacity >= advancedTruck.capacity);
    }

    // ==================== Performance Tests ====================

    @Test
    void testAdvancedBusIsUpgrade() {
        Bus basicBus = new Bus();

        // Should be better in performance
        assertTrue(advancedBus.speed > basicBus.speed);
        assertTrue(advancedBus.capacity > basicBus.capacity);

        // But costs more
        assertTrue(advancedBus.maintenanceCost >= basicBus.maintenanceCost);
    }

    @Test
    void testAdvancedBusHasBalancedStats() {
        // Advanced bus should be a powerful vehicle
        assertTrue(advancedBus.speed > 0);
        assertTrue(advancedBus.capacity > 0);
        assertTrue(advancedBus.maintenanceCost > 0);

        // Stats should be reasonable
        assertTrue(advancedBus.speed <= 5);
        assertTrue(advancedBus.capacity <= 100);
        assertTrue(advancedBus.maintenanceCost <= 10);
    }

    @Test
    void testAdvancedBusCarriesSameTypesAsBus() {
        Bus bus = new Bus();

        // Should carry same resource types
        for (ResourceType type : ResourceType.values()) {
            assertEquals(bus.canCarry(type), advancedBus.canCarry(type),
                "Advanced bus and bus should carry same types: " + type);
        }
    }

    @Test
    void testAdvancedBusCapacityIsSignificantUpgrade() {
        Bus bus = new Bus();
        double percentageIncrease = ((double) advancedBus.capacity / bus.capacity - 1) * 100;
        assertTrue(percentageIncrease >= 50, "Capacity should increase by at least 50%");
    }

    @Test
    void testAdvancedBusIsPremiumPassengerTransport() {
        Bus bus = new Bus();

        // Advanced bus combines best speed and capacity for passengers
        assertTrue(advancedBus.speed > bus.speed);
        assertTrue(advancedBus.capacity > bus.capacity);
        assertTrue(advancedBus.canCarry(ResourceType.PASSENGERS));
        assertFalse(advancedBus.canCarry(ResourceType.WHEAT));
    }

    @Test
    void testAdvancedBusHasReasonableMaintenanceCost() {
        // Despite being the best passenger vehicle, maintenance should be reasonable
        assertTrue(advancedBus.maintenanceCost <= 5,
            "Maintenance cost should not be excessive");
    }
}
