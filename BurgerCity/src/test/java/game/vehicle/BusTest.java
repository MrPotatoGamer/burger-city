package game.vehicle;

import game.resource.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusTest {

    private Bus bus;

    @BeforeEach
    void setUp() {
        bus = new Bus();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testConstructorSetsSpeed() {
        assertEquals(2, bus.speed);
    }

    @Test
    void testConstructorSetsCapacity() {
        assertEquals(30, bus.capacity);
    }

    @Test
    void testConstructorSetsMaintenanceCost() {
        assertEquals(2, bus.maintenanceCost);
    }

    @Test
    void testConstructorInheritsFromVehicle() {
        assertNotNull(bus);
        assertTrue(bus instanceof Vehicle);
    }

    // ==================== Can Carry Tests ====================

    @Test
    void testCanCarryPassengers() {
        assertTrue(bus.canCarry(ResourceType.PASSENGERS));
    }

    @Test
    void testCannotCarryWheat() {
        assertFalse(bus.canCarry(ResourceType.WHEAT));
    }

    @Test
    void testCannotCarryMeat() {
        assertFalse(bus.canCarry(ResourceType.MEAT));
    }

    @Test
    void testCannotCarryBread() {
        assertFalse(bus.canCarry(ResourceType.BREAD));
    }

    @Test
    void testCannotCarryMeatPatty() {
        assertFalse(bus.canCarry(ResourceType.MEAT_PATTY));
    }

    @Test
    void testCannotCarryHamburger() {
        assertFalse(bus.canCarry(ResourceType.HAMBURGER));
    }

    @Test
    void testCanCarryHandlesNullResourceType() {
        // Should handle null without crashing
        try {
            boolean result = bus.canCarry(null);
            // Bus should only carry passengers, so null should be false
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
                assertTrue(bus.canCarry(type),
                    "Bus should carry " + type);
            } else {
                assertFalse(bus.canCarry(type),
                    "Bus should not carry " + type);
            }
        }
    }

    // ==================== Capacity Tests ====================

    @Test
    void testBusHasCorrectCapacity() {
        assertEquals(30, bus.capacity);
    }

    @Test
    void testBusCapacityIsHigherThanTruck() {
        Truck truck = new Truck();
        assertTrue(bus.capacity > truck.capacity);
    }

    @Test
    void testBusCapacityIsLowerThanAdvancedBus() {
        AdvancedBus advancedBus = new AdvancedBus();
        assertTrue(bus.capacity < advancedBus.capacity);
    }

    // ==================== Speed Tests ====================

    @Test
    void testBusSpeedIsFasterThanTruck() {
        Truck truck = new Truck();
        assertTrue(bus.speed > truck.speed);
    }

    @Test
    void testBusSpeedIsSlowerThanAdvancedBus() {
        AdvancedBus advancedBus = new AdvancedBus();
        assertTrue(bus.speed < advancedBus.speed);
    }

    @Test
    void testBusSpeedMatchesDefaultVehicleSpeed() {
        Vehicle vehicle = new Vehicle();
        assertEquals(vehicle.speed, bus.speed);
    }

    // ==================== Maintenance Cost Tests ====================

    @Test
    void testMaintenanceCostIsPositive() {
        assertTrue(bus.maintenanceCost > 0);
    }

    @Test
    void testMaintenanceCostIsLowerThanTruck() {
        Truck truck = new Truck();
        assertTrue(bus.maintenanceCost < truck.maintenanceCost);
    }

    @Test
    void testMaintenanceCostIsLowerThanAdvancedBus() {
        AdvancedBus advancedBus = new AdvancedBus();
        assertTrue(bus.maintenanceCost < advancedBus.maintenanceCost);
    }

    // ==================== Type Specific Tests ====================

    @Test
    void testBusIsDistinctFromTruck() {
        Truck truck = new Truck();
        // Buses and trucks have opposite cargo restrictions
        assertTrue(bus.canCarry(ResourceType.PASSENGERS));
        assertFalse(bus.canCarry(ResourceType.WHEAT));
        assertFalse(truck.canCarry(ResourceType.PASSENGERS));
        assertTrue(truck.canCarry(ResourceType.WHEAT));
    }

    @Test
    void testBusEffectiveSpeedEqualsSpeed() {
        assertEquals(bus.speed, bus.effectiveSpeed);
    }

    @Test
    void testBusInheritsVehicleMethods() {
        // Test that inherited methods work
        bus.setPurchasePrice(2000);
        assertEquals(2000, bus.getPurchasePrice());
        assertEquals(1000, bus.getSellValue());
    }

    @Test
    void testBusCanBeSpawned() {
        bus.spawnAt(10, 15);
        assertTrue(bus.isSpawned());
        assertEquals(10, bus.getCurrentTileX());
        assertEquals(15, bus.getCurrentTileY());
    }

    @Test
    void testBusCanHavePath() {
        java.util.List<int[]> path = java.util.List.of(
            new int[]{0, 0},
            new int[]{1, 0},
            new int[]{2, 0}
        );
        bus.setPath(path);
        assertTrue(bus.hasPath());
    }

    @Test
    void testBusAges() {
        bus.spawnAt(0, 0);
        double initialAge = bus.getAgeSeconds();
        // Simulate some aging
        bus.ageSeconds = 75.0;
        assertEquals(75.0, bus.getAgeSeconds());
        assertTrue(bus.getAgeSeconds() > initialAge);
    }

    @Test
    void testBusNeedsMaintenance() {
        bus.secondsSinceMaintenance = 200.0;
        assertTrue(bus.needsMaintenance());
    }

    @Test
    void testBusCanBecomeOld() {
        bus.ageSeconds = 700.0;
        assertTrue(bus.isTooOld());
    }

    // ==================== Comparison Tests ====================

    @Test
    void testBusVsAdvancedBusComparison() {
        AdvancedBus advanced = new AdvancedBus();

        // Advanced bus should be better in speed and capacity
        assertTrue(advanced.speed > bus.speed,
            "Advanced bus should be faster");
        assertTrue(advanced.capacity > bus.capacity,
            "Advanced bus should have more capacity");

        // But both carry only passengers
        assertEquals(bus.canCarry(ResourceType.PASSENGERS),
            advanced.canCarry(ResourceType.PASSENGERS));
        assertEquals(bus.canCarry(ResourceType.WHEAT),
            advanced.canCarry(ResourceType.WHEAT));
    }

    @Test
    void testBusIsPassengerSpecialist() {
        // Bus should only transport passengers
        assertTrue(bus.canCarry(ResourceType.PASSENGERS));

        // Verify it rejects all goods
        assertFalse(bus.canCarry(ResourceType.WHEAT));
        assertFalse(bus.canCarry(ResourceType.MEAT));
        assertFalse(bus.canCarry(ResourceType.BREAD));
        assertFalse(bus.canCarry(ResourceType.MEAT_PATTY));
        assertFalse(bus.canCarry(ResourceType.HAMBURGER));
    }

    // ==================== Performance Tests ====================

    @Test
    void testBusIsEconomicalMaintenance() {
        // Bus has lower maintenance cost than most vehicles
        Truck truck = new Truck();
        AdvancedTruck advancedTruck = new AdvancedTruck();

        assertTrue(bus.maintenanceCost <= truck.maintenanceCost);
        assertTrue(bus.maintenanceCost <= advancedTruck.maintenanceCost);
    }

    @Test
    void testBusHasBalancedStats() {
        // Bus should be a balanced vehicle
        assertTrue(bus.speed > 0);
        assertTrue(bus.capacity > 0);
        assertTrue(bus.maintenanceCost > 0);

        // Speed and capacity should be reasonable
        assertTrue(bus.speed <= 5);
        assertTrue(bus.capacity <= 100);
        assertTrue(bus.maintenanceCost <= 10);
    }
}
