package game.ui;

import game.map.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class MapRendererSmokeTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void construct_andPaint_doesNotThrow_andViewportResizeUpdatesCamera() {
        Map map = new Map(8, 6);
        map.initGrassForLoad();

        MapRenderer renderer = new MapRenderer(map);
        renderer.setVehicles(null);
        renderer.setTrafficLights(null);

        renderer.setSize(320, 200);
        for (var l : renderer.getComponentListeners()) {
            l.componentResized(new ComponentEvent(renderer, ComponentEvent.COMPONENT_RESIZED));
        }

        assertNotNull(renderer.getCamera());
        assertEquals(320, renderer.getCamera().getViewportWidth());
        assertEquals(200, renderer.getCamera().getViewportHeight());

        
        assertDoesNotThrow(renderer::rebuildGrassLayerCache);

        BufferedImage img = new BufferedImage(320, 200, BufferedImage.TYPE_INT_ARGB);
        var g2 = img.createGraphics();
        try {
            assertDoesNotThrow(() -> renderer.paint(g2));
        } finally {
            g2.dispose();
        }
    }
}
