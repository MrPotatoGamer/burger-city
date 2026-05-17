package game.building;

import game.vehicle.Vehicle;

public class Garage extends Building {

    public static final int COST = 1800;

    public Garage(int x, int y) {
        super("Garázs", COST, x, y);
    }

    public Vehicle buyVehicle(String type) {
        return null;
    }

    public void sellVehicle(Vehicle v) {}

    public void maintainVehicle(Vehicle v) {}
}