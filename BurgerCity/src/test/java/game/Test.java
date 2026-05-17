package game;

import game.resource.ResourceType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExampleTest {

    @Test
    void testResourceTypeDisplayName() {
        assertEquals("Búza", ResourceType.WHEAT.getDisplayName());
        assertEquals("Hamburger", ResourceType.HAMBURGER.getDisplayName());
    }
}