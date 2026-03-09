package game.core;

import game.map.Map;
import game.save.SaveManager;
import game.vehicle.Vehicle;
import game.route.Route;
import game.building.Building;
import game.objective.Objective;
import java.util.List;

public class Game {

    private String name;
    private int money;
    private boolean running;
    private int timeSpeed;

    private Player player;
    private SaveManager saveManager;
    private Map map;

    private List<Vehicle> vehicles;
    private List<Route> routes;
    private List<Building> buildings;
    private List<Objective> objectives;

    public void start() {}

    public void pause() {}

    public void setSpeed(int multiplier) {}

    public void exit() {}
}