package game.ui;

public class Camera {

    private double x;
    private double y;
    private double zoom;
    private int viewportWidth;
    private int viewportHeight;
    private int worldWidth;
    private int worldHeight;

    public Camera(int viewportWidth, int viewportHeight, int worldWidth, int worldHeight) {
        this.x = 0;
        this.y = 0;
        this.zoom = 1.0;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
    }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;
        clampPosition();
    }

    public void setZoom(double newZoom, double mouseX, double mouseY) {
        // Világkoordináta a kurzor alatt zoom előtt
        double worldXBeforeZoom = (mouseX + x) / zoom;
        double worldYBeforeZoom = (mouseY + y) / zoom;

        // Zoom frissítése határokkal
        zoom = Math.max(0.5, Math.min(3.0, newZoom));

        // Kamera pozíció újraszámolása, hogy a kurzor alatt ugyanaz a pont maradjon
        x = worldXBeforeZoom * zoom - mouseX;
        y = worldYBeforeZoom * zoom - mouseY;

        clampPosition();
    }

    public void zoom(double factor, double mouseX, double mouseY) {
        setZoom(zoom * factor, mouseX, mouseY);
    }

    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
        clampPosition();
    }

    private void clampPosition() {
        double maxX = Math.max(0, worldWidth * zoom - viewportWidth);
        double maxY = Math.max(0, worldHeight * zoom - viewportHeight);

        x = Math.max(0, Math.min(x, maxX));
        y = Math.max(0, Math.min(y, maxY));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZoom() {
        return zoom;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    // Képernyő koordinátából világ koordinátába
    public double screenToWorldX(double screenX) {
        return (screenX + x) / zoom;
    }

    public double screenToWorldY(double screenY) {
        return (screenY + y) / zoom;
    }
}