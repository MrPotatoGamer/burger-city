package game.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CameraTest {

    @BeforeAll
    static void headless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    void move_clampsToWorldBounds() {
        Camera cam = new Camera(100, 100, 200, 200);

        cam.move(10_000, 10_000);
        assertEquals(100.0, cam.getX(), 1e-9);
        assertEquals(100.0, cam.getY(), 1e-9);

        cam.move(-10_000, -10_000);
        assertEquals(0.0, cam.getX(), 1e-9);
        assertEquals(0.0, cam.getY(), 1e-9);
    }

    @Test
    void setZoom_clampsAndKeepsWorldPointUnderCursor() {
        Camera cam = new Camera(120, 80, 500, 400);

        cam.move(50, 30);

        double mouseX = 33;
        double mouseY = 17;

        double xBefore = cam.getX();
        double yBefore = cam.getY();
        double zoomBefore = cam.getZoom();

        double worldXBefore = (mouseX + xBefore) / zoomBefore;
        double worldYBefore = (mouseY + yBefore) / zoomBefore;

        cam.setZoom(2.0, mouseX, mouseY);

        double worldXAfter = (mouseX + cam.getX()) / cam.getZoom();
        double worldYAfter = (mouseY + cam.getY()) / cam.getZoom();

        assertEquals(worldXBefore, worldXAfter, 1e-9);
        assertEquals(worldYBefore, worldYAfter, 1e-9);

        cam.setZoom(999.0, mouseX, mouseY);
        assertEquals(3.0, cam.getZoom(), 1e-9);

        cam.setZoom(0.0001, mouseX, mouseY);
        assertEquals(0.5, cam.getZoom(), 1e-9);
    }

    @Test
    void centerOnWorld_clampsToWorldBounds() {
        Camera cam = new Camera(100, 100, 200, 200);

        cam.centerOnWorld(-1_000, -1_000);
        assertEquals(0.0, cam.getX(), 1e-9);
        assertEquals(0.0, cam.getY(), 1e-9);

        cam.centerOnWorld(1_000, 1_000);
        assertEquals(100.0, cam.getX(), 1e-9);
        assertEquals(100.0, cam.getY(), 1e-9);
    }
}
