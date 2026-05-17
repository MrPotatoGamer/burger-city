package game.ui;

import game.map.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MinimapUITest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void mousePressed_andDrag_navigatesCamera_andCallsCallback() {
        Map map = new Map(10, 10);
        map.initGrassForLoad();

        int worldW = map.getWidth() * 32;
        int worldH = map.getHeight() * 32;

        Camera camera = new Camera(100, 100, worldW, worldH);
        AtomicInteger navigations = new AtomicInteger(0);

        MinimapUI minimap = new MinimapUI(map, camera, navigations::incrementAndGet);
        minimap.setSize(200, 100);

        // Click near bottom-right.
        int clickX = 199;
        int clickY = 99;

        MouseEvent press = new MouseEvent(
                minimap,
                MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(),
                0,
                clickX,
                clickY,
                1,
                false
        );
        for (var l : minimap.getMouseListeners()) {
            l.mousePressed(press);
        }

        assertEquals(1, navigations.get());

        double scaleX = minimap.getWidth() / (double) worldW;
        double scaleY = minimap.getHeight() / (double) worldH;
        double worldX = clickX / scaleX;
        double worldY = clickY / scaleY;

        // Camera.centerOnWorld clamps internally; reproduce expected clamped camera position.
        double desiredX = worldX - camera.getViewportWidth() / 2.0;
        double desiredY = worldY - camera.getViewportHeight() / 2.0;
        double maxX = Math.max(0.0, worldW * camera.getZoom() - camera.getViewportWidth());
        double maxY = Math.max(0.0, worldH * camera.getZoom() - camera.getViewportHeight());
        double expectedX = Math.max(0.0, Math.min(desiredX, maxX));
        double expectedY = Math.max(0.0, Math.min(desiredY, maxY));

        assertEquals(expectedX, camera.getX(), 1e-6);
        assertEquals(expectedY, camera.getY(), 1e-6);

        // Drag to top-left should navigate again and clamp to 0,0.
        MouseEvent drag = new MouseEvent(
                minimap,
                MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(),
                0,
                0,
                0,
                0,
                false
        );
        for (var l : minimap.getMouseMotionListeners()) {
            l.mouseDragged(drag);
        }

        assertEquals(2, navigations.get());
        assertEquals(0.0, camera.getX(), 1e-9);
        assertEquals(0.0, camera.getY(), 1e-9);
    }
}
