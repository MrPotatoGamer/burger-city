package game.building;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoadTest {

    @Test
    void testRoadConstructor() {
        Road road = new Road(5, 10);
        assertNotNull(road);
    }

    @Test
    void testRoadName() {
        Road road = new Road(0, 0);
        assertEquals("Út", road.getName());
    }

    @Test
    void testRoadCost() {
        Road road = new Road(0, 0);
        assertEquals(60, road.getCost());
    }

    @Test
    void testRoadCostConstant() {
        assertEquals(60, Road.COST);
    }

    @Test
    void testRoadXCoordinate() {
        Road road = new Road(7, 3);
        assertEquals(7, road.getX());
    }

    @Test
    void testRoadYCoordinate() {
        Road road = new Road(7, 3);
        assertEquals(3, road.getY());
    }

    @Test
    void testRoadWithNegativeCoordinates() {
        Road road = new Road(-5, -10);
        assertEquals(-5, road.getX());
        assertEquals(-10, road.getY());
    }

    @Test
    void testRoadWithZeroCoordinates() {
        Road road = new Road(0, 0);
        assertEquals(0, road.getX());
        assertEquals(0, road.getY());
    }

    @Test
    void testRoadWithLargeCoordinates() {
        Road road = new Road(1000, 2000);
        assertEquals(1000, road.getX());
        assertEquals(2000, road.getY());
    }

    @Test
    void testMultipleRoadInstances() {
        Road road1 = new Road(1, 2);
        Road road2 = new Road(3, 4);

        assertNotSame(road1, road2);
        assertEquals(1, road1.getX());
        assertEquals(2, road1.getY());
        assertEquals(3, road2.getX());
        assertEquals(4, road2.getY());
    }

    @Test
    void testRoadInheritsFromBuilding() {
        Road road = new Road(5, 5);
        assertTrue(road instanceof Building);
    }

    @Test
    void testRoadCostMatchesBuildingCost() {
        Road road = new Road(0, 0);
        assertEquals(Road.COST, road.getCost());
    }
}
