package game.vehicle;

import game.route.Route;
import game.core.Player;
import game.core.ResourcePrices;
import game.resource.Resource;
import game.resource.ResourceType;
import game.building.Garage;
import game.map.City;
import game.map.Industry;
import game.map.Map;
import game.map.Tile;
import game.map.TileType;
import game.save.GameSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Vehicle {

    public static final int TILE_SIZE_PX = 32;

    protected int speed;
    protected int capacity;
    protected int maintenanceCost;
    protected double ageSeconds;
    protected double secondsSinceMaintenance;

    // Maintenance state
    protected boolean goingToMaintenance = false;
    protected boolean inMaintenance = false;
    protected double maintenanceSecondsRemaining = 0;
    protected Garage maintenanceGarage;
    protected Integer maintenanceDestRoadX;
    protected Integer maintenanceDestRoadY;

    // Route management
    protected List<int[]> routePathTiles = List.of();
    protected boolean rejoiningRoute = false;
    protected Integer rejoinRouteAtX;
    protected Integer rejoinRouteAtY;

    protected int purchasePrice = 0;

    protected Route route;
    protected Resource currentCargo;
    protected Garage garage;

    // Store the buildings this vehicle serves (for cleanup when building is destroyed)
    protected Integer startBuildingOriginX;
    protected Integer startBuildingOriginY;
    protected Integer endBuildingOriginX;
    protected Integer endBuildingOriginY;

    // World-position (pixel) coordinates, centered on tiles
    protected double worldX;
    protected double worldY;

    protected int currentTileX;
    protected int currentTileY;

    protected Integer targetTileX;
    protected Integer targetTileY;

    protected Integer previousTileX;
    protected Integer previousTileY;

    protected int lastMoveDx;
    protected int lastMoveDy;

    // If set, vehicle will follow this path in order.
    protected List<int[]> pathTiles = List.of();
    protected int pathIndex = 0;
    protected boolean pathForward = true;

    private boolean arrivedThisUpdate = false;
    private boolean maintenanceRequested = false;

    // Traffic management: direction on current tile (0=none, 1=N, 2=E, 3=S, 4=W)
    protected int currentDirection = 0;
    // Effective speed considering traffic ahead
    protected double effectiveSpeed;
    // Track when this vehicle started approaching an intersection
    protected Integer intersectionClaimX = null;
    protected Integer intersectionClaimY = null;

    public Vehicle() {
        // Interpreted as tiles per second (converted internally to pixels/sec)
        this.speed = 2;
        this.effectiveSpeed = this.speed;
    }

    public void setPurchasePrice(int purchasePrice) {
        this.purchasePrice = Math.max(0, purchasePrice);
    }

    public int getPurchasePrice() {
        return purchasePrice;
    }

    public void setHomeGarage(Garage garage) {
        this.garage = garage;
    }

    public Garage getHomeGarage() {
        return garage;
    }

    public Garage getMaintenanceGarage() {
        return maintenanceGarage;
    }

    public double getAgeSeconds() {
        return ageSeconds;
    }

    public double getMaintenanceIntervalSeconds() {
        // Older vehicles need maintenance more often.
        // Starts at ~120s and drops linearly until a minimum.
        double base = 120.0;
        double min = 30.0;
        double interval = base - (ageSeconds * 0.20);
        return Math.max(min, interval);
    }

    public double getSecondsUntilMaintenanceDue() {
        return Math.max(0.0, getMaintenanceIntervalSeconds() - secondsSinceMaintenance);
    }

    public boolean isGoingToMaintenance() {
        return goingToMaintenance;
    }

    public boolean isInMaintenance() {
        return inMaintenance;
    }

    public double getMaintenanceSecondsRemaining() {
        return maintenanceSecondsRemaining;
    }

    public boolean isTooOld() {
        return ageSeconds >= 600.0; // 10 minutes of game-time by default
    }

    public int getSellValue() {
        if (purchasePrice <= 0) return 0;
        // Simple rule: half price.
        return purchasePrice / 2;
    }

    /**
     * Store the main working route (between the selected buildings).
     */
    public void setRoutePath(List<int[]> routePathTiles) {
        this.routePathTiles = (routePathTiles == null) ? List.of() : routePathTiles;
    }

    /**
     * After reaching (x,y) on the current path, the vehicle will switch back to its stored route.
     */
    public void setRejoinRouteAt(int x, int y) {
        this.rejoiningRoute = true;
        this.rejoinRouteAtX = x;
        this.rejoinRouteAtY = y;
    }

    private static List<GameSnapshot.IntPair> toIntPairs(List<int[]> tiles) {
        List<GameSnapshot.IntPair> pts = new ArrayList<>();
        if (tiles == null) return pts;
        for (int[] p : tiles) {
            if (p == null || p.length < 2) continue;
            pts.add(new GameSnapshot.IntPair(p[0], p[1]));
        }
        return pts;
    }

    public GameSnapshot.VehicleData exportSaveData() {
        List<GameSnapshot.IntPair> pts = toIntPairs(pathTiles);
        List<GameSnapshot.IntPair> routePts = toIntPairs(routePathTiles);

        GameSnapshot.CargoData cargoData = null;
        if (currentCargo != null && !currentCargo.isEmpty()) {
            cargoData = new GameSnapshot.CargoData(currentCargo.getType(), currentCargo.getAmount());
        }

        GameSnapshot.RouteBuildingsData rb = null;
        if (startBuildingOriginX != null && startBuildingOriginY != null && endBuildingOriginX != null && endBuildingOriginY != null) {
            rb = new GameSnapshot.RouteBuildingsData(startBuildingOriginX, startBuildingOriginY, endBuildingOriginX, endBuildingOriginY);
        }

        Integer homeGarageX = (garage == null) ? null : garage.getX();
        Integer homeGarageY = (garage == null) ? null : garage.getY();
        Integer maintenanceGarageX = (maintenanceGarage == null) ? null : maintenanceGarage.getX();
        Integer maintenanceGarageY = (maintenanceGarage == null) ? null : maintenanceGarage.getY();

        return new GameSnapshot.VehicleData(
                getClass().getSimpleName(),
                worldX,
                worldY,
                currentTileX,
                currentTileY,
                targetTileX,
                targetTileY,
                previousTileX,
                previousTileY,
                lastMoveDx,
                lastMoveDy,
                currentDirection,
                pts,
                pathIndex,
                pathForward,
                cargoData,
                rb,
                routePts,
                rejoiningRoute,
                rejoinRouteAtX,
                rejoinRouteAtY,
                ageSeconds,
                secondsSinceMaintenance,
                goingToMaintenance,
                inMaintenance,
                maintenanceSecondsRemaining,
                maintenanceDestRoadX,
                maintenanceDestRoadY,
                homeGarageX,
                homeGarageY,
                maintenanceGarageX,
                maintenanceGarageY,
                purchasePrice
        );
    }

    public void importSaveData(GameSnapshot.VehicleData data) {
        importSaveData(data, null);
    }

    public void importSaveData(GameSnapshot.VehicleData data, Map map) {
        if (data == null) return;

        this.worldX = data.worldX();
        this.worldY = data.worldY();
        this.currentTileX = data.currentTileX();
        this.currentTileY = data.currentTileY();
        this.targetTileX = data.targetTileX();
        this.targetTileY = data.targetTileY();
        this.previousTileX = data.previousTileX();
        this.previousTileY = data.previousTileY();
        this.lastMoveDx = data.lastMoveDx();
        this.lastMoveDy = data.lastMoveDy();
        this.currentDirection = data.currentDirection();

        // Path
        List<int[]> newPath = new ArrayList<>();
        if (data.pathTiles() != null) {
            for (GameSnapshot.IntPair p : data.pathTiles()) {
                if (p == null) continue;
                newPath.add(new int[]{p.x(), p.y()});
            }
        }
        this.pathTiles = newPath;
        this.pathIndex = Math.max(0, data.pathIndex());
        this.pathForward = data.pathForward();

        // Route path
        List<int[]> newRoute = new ArrayList<>();
        if (data.routePathTiles() != null) {
            for (GameSnapshot.IntPair p : data.routePathTiles()) {
                if (p == null) continue;
                newRoute.add(new int[]{p.x(), p.y()});
            }
        }
        this.routePathTiles = newRoute;

        // Route rejoin state
        this.rejoiningRoute = data.rejoiningRoute();
        this.rejoinRouteAtX = data.rejoinRouteAtX();
        this.rejoinRouteAtY = data.rejoinRouteAtY();

        // Economy/maintenance state
        this.purchasePrice = Math.max(0, data.purchasePrice());
        this.ageSeconds = Math.max(0.0, data.ageSeconds());
        this.secondsSinceMaintenance = Math.max(0.0, data.secondsSinceMaintenance());
        this.goingToMaintenance = data.goingToMaintenance();
        this.inMaintenance = data.inMaintenance();
        this.maintenanceSecondsRemaining = Math.max(0.0, data.maintenanceSecondsRemaining());
        this.maintenanceDestRoadX = data.maintenanceDestRoadX();
        this.maintenanceDestRoadY = data.maintenanceDestRoadY();

        // Cargo
        if (data.cargo() != null && data.cargo().type() != null && data.cargo().amount() > 0) {
            this.currentCargo = new Resource(data.cargo().type(), data.cargo().amount());
        } else {
            this.currentCargo = null;
        }

        // Route building origins
        if (data.routeBuildings() != null) {
            this.startBuildingOriginX = data.routeBuildings().startOriginX();
            this.startBuildingOriginY = data.routeBuildings().startOriginY();
            this.endBuildingOriginX = data.routeBuildings().endOriginX();
            this.endBuildingOriginY = data.routeBuildings().endOriginY();
        } else {
            this.startBuildingOriginX = null;
            this.startBuildingOriginY = null;
            this.endBuildingOriginX = null;
            this.endBuildingOriginY = null;
        }

        // Resolve garages (object references do not survive serialization)
        this.garage = null;
        this.maintenanceGarage = null;
        if (map != null) {
            if (data.homeGarageX() != null && data.homeGarageY() != null) {
                Tile t = map.getTile(data.homeGarageX(), data.homeGarageY());
                if (t != null && t.getPlacedBuilding() instanceof Garage g) {
                    this.garage = g;
                }
            }
            if (data.maintenanceGarageX() != null && data.maintenanceGarageY() != null) {
                Tile t = map.getTile(data.maintenanceGarageX(), data.maintenanceGarageY());
                if (t != null && t.getPlacedBuilding() instanceof Garage g) {
                    this.maintenanceGarage = g;
                }
            }
        }

        // Reset transient fields
        this.arrivedThisUpdate = false;
        this.effectiveSpeed = this.speed;
        this.intersectionClaimX = null;
        this.intersectionClaimY = null;
    }

    public double getWorldX() {
        return worldX;
    }

    public double getWorldY() {
        return worldY;
    }

    public int getCurrentTileX() {
        return currentTileX;
    }

    public int getCurrentTileY() {
        return currentTileY;
    }

    public boolean isSpawned() {
        return targetTileX != null || (worldX != 0 || worldY != 0);
    }

    public void spawnAt(int tileX, int tileY) {
        this.currentTileX = tileX;
        this.currentTileY = tileY;
        this.targetTileX = null;
        this.targetTileY = null;
        this.previousTileX = null;
        this.previousTileY = null;
        this.lastMoveDx = 0;
        this.lastMoveDy = 0;
        this.currentDirection = 0;
        this.intersectionClaimX = null;
        this.intersectionClaimY = null;
        this.worldX = tileCenterX(tileX);
        this.worldY = tileCenterY(tileY);
    }

    /**
     * Assign a ROAD-tile path for the vehicle to follow.
     * If the path is empty/null, the vehicle will remain idle.
     */
    public void setPath(List<int[]> pathTiles) {
        this.pathTiles = (pathTiles == null) ? List.of() : pathTiles;
        this.pathIndex = 0;
        this.pathForward = true;
        this.targetTileX = null;
        this.targetTileY = null;
        this.previousTileX = null;
        this.previousTileY = null;
        this.lastMoveDx = 0;
        this.lastMoveDy = 0;

        if (!this.pathTiles.isEmpty()) {
            int[] first = this.pathTiles.get(0);
            spawnAt(first[0], first[1]);
        }
    }

    public boolean hasPath() {
        return pathTiles != null && !pathTiles.isEmpty();
    }

    /**
     * Set the buildings this vehicle serves (for tracking when buildings are destroyed).
     * @param startOriginX Origin X of start building
     * @param startOriginY Origin Y of start building
     * @param endOriginX Origin X of end building
     * @param endOriginY Origin Y of end building
     */
    public void setRouteBuildings(int startOriginX, int startOriginY, int endOriginX, int endOriginY) {
        this.startBuildingOriginX = startOriginX;
        this.startBuildingOriginY = startOriginY;
        this.endBuildingOriginX = endOriginX;
        this.endBuildingOriginY = endOriginY;
    }

    /**
     * Check if this vehicle serves a building at the given origin coordinates.
     */
    public boolean servesBuilding(int originX, int originY) {
        if (startBuildingOriginX != null && startBuildingOriginY != null) {
            if (startBuildingOriginX == originX && startBuildingOriginY == originY) {
                return true;
            }
        }
        if (endBuildingOriginX != null && endBuildingOriginY != null) {
            if (endBuildingOriginX == originX && endBuildingOriginY == originY) {
                return true;
            }
        }
        return false;
    }

    public Resource getCurrentCargo() {
        return currentCargo;
    }

    /**
     * Move along connected ROAD tiles.
     * @param map The game map.
     * @param deltaSeconds Time elapsed since last update.
     */
    public void update(Map map, double deltaSeconds) {
        update(map, deltaSeconds, null, null);
    }

    /**
     * Move along connected ROAD tiles with traffic awareness.
     * @param map The game map.
     * @param deltaSeconds Time elapsed since last update.
     * @param allVehicles All vehicles in the game for traffic checking (can be null).
     */
    public void update(Map map, double deltaSeconds, List<Vehicle> allVehicles) {
        update(map, deltaSeconds, allVehicles, null);
    }

    /**
     * Move along connected ROAD tiles with traffic awareness and traffic lights.
     * @param map The game map.
     * @param deltaSeconds Time elapsed since last update.
     * @param allVehicles All vehicles in the game for traffic checking (can be null).
     * @param trafficLights All traffic lights in the game (can be null).
     */
    public void update(Map map, double deltaSeconds, List<Vehicle> allVehicles, List<game.building.TrafficLight> trafficLights) {
        Objects.requireNonNull(map, "map");
        if (deltaSeconds <= 0) return;

        // Age and maintenance timers always advance (even if idle).
        ageSeconds += deltaSeconds;
        secondsSinceMaintenance += deltaSeconds;

        // If currently in maintenance, just wait it out.
        if (inMaintenance) {
            maintenanceSecondsRemaining -= deltaSeconds;
            if (maintenanceSecondsRemaining <= 0) {
                inMaintenance = false;
                maintenanceSecondsRemaining = 0;
                secondsSinceMaintenance = 0;
                // After maintenance, try to rejoin the route at its start tile.
                if (routePathTiles != null && !routePathTiles.isEmpty()) {
                    int[] join = routePathTiles.get(0);
                    List<int[]> toJoin = map.findRoadPathBetweenRoadTiles(currentTileX, currentTileY, join[0], join[1]);
                    if (!toJoin.isEmpty()) {
                        rejoiningRoute = true;
                        rejoinRouteAtX = join[0];
                        rejoinRouteAtY = join[1];
                        setPath(toJoin);
                    } else {
                        // Fallback: continue on the stored route (teleport-free switch happens when possible).
                        switchToRouteAtCurrentTile();
                    }
                }
            }
            return;
        }

        // If a previous arrival flagged maintenance, start the garage trip now (next tick).
        if (maintenanceRequested) {
            maintenanceRequested = false;
            startGoingToNearestGarage(map);
            if (goingToMaintenance) return;
        }

        // If we reached the route-join point, switch back to the working route.
        if (rejoiningRoute
                && rejoinRouteAtX != null && rejoinRouteAtY != null
                && currentTileX == rejoinRouteAtX && currentTileY == rejoinRouteAtY) {
            rejoiningRoute = false;
            rejoinRouteAtX = null;
            rejoinRouteAtY = null;
            switchToRouteAtCurrentTile();
        }

        // Vehicle only moves when a valid path is assigned.
        if (!hasPath()) {
            // If maintenance is due and we have garages, we can start going when idle.
            maybeStartMaintenance(map);
            return;
        }

        // If we don't have a target yet, try to acquire one.
        if (targetTileX == null || targetTileY == null) {
            chooseNextTarget(map, allVehicles, trafficLights);
            return;
        }

        // Adjust speed based on traffic ahead
        adjustSpeedForTraffic(allVehicles);

        double targetX = tileCenterX(targetTileX);
        double targetY = tileCenterY(targetTileY);
        double dx = targetX - worldX;
        double dy = targetY - worldY;
        double dist = Math.hypot(dx, dy);

        // Arrived (snap).
        if (dist < 0.01) {
            arriveAtTarget(map, allVehicles, trafficLights);
            return;
        }

        double pixelsPerSecond = effectiveSpeed * (double) TILE_SIZE_PX;
        double step = pixelsPerSecond * deltaSeconds;
        if (step >= dist) {
            worldX = targetX;
            worldY = targetY;
            arriveAtTarget(map, allVehicles, trafficLights);
            return;
        }

        worldX += (dx / dist) * step;
        worldY += (dy / dist) * step;
    }

    private void maybeStartMaintenance(Map map) {
        if (map == null) return;
        if (goingToMaintenance || inMaintenance || rejoiningRoute) return;
        if (getSecondsUntilMaintenanceDue() > 0) return;
        startGoingToNearestGarage(map);
    }

    private void startGoingToNearestGarage(Map map) {
        List<Garage> garages = map.getGarages();
        if (garages == null || garages.isEmpty()) return;

        List<int[]> bestPath = List.of();
        Garage bestGarage = null;
        Integer bestRX = null;
        Integer bestRY = null;

        for (Garage g : garages) {
            if (g == null) continue;
            List<int[]> roads = map.adjacentRoadTilesForArea(g.getX(), g.getY(), 1, 1);
            for (int[] r : roads) {
                if (r == null || r.length < 2) continue;
                List<int[]> p = map.findRoadPathBetweenRoadTiles(currentTileX, currentTileY, r[0], r[1]);
                if (p.isEmpty()) continue;
                if (bestPath.isEmpty() || p.size() < bestPath.size()) {
                    bestPath = p;
                    bestGarage = g;
                    bestRX = r[0];
                    bestRY = r[1];
                }
            }
        }

        if (bestGarage == null || bestPath.isEmpty()) return;

        maintenanceGarage = bestGarage;
        maintenanceDestRoadX = bestRX;
        maintenanceDestRoadY = bestRY;
        goingToMaintenance = true;

        // Use a temporary path to the garage. Starts at current tile so no teleport.
        setPath(bestPath);
    }

    private void switchToRouteAtCurrentTile() {
        if (routePathTiles == null || routePathTiles.isEmpty()) return;

        this.pathTiles = routePathTiles;
        int idx = indexOfTile(routePathTiles, currentTileX, currentTileY);
        this.pathIndex = Math.max(0, idx);
        this.pathForward = true;
        this.targetTileX = null;
        this.targetTileY = null;
    }

    /**
     * Adjust effective speed based on vehicles ahead in the same direction.
     * If another vehicle is on our target tile in the same direction, we MUST STOP (no overtaking).
     */
    protected void adjustSpeedForTraffic(List<Vehicle> allVehicles) {
        effectiveSpeed = speed; // Reset to base speed each tick

        if (allVehicles == null || targetTileX == null || targetTileY == null) return;

        // Calculate our planned direction
        int myPlannedDirection = getPlannedDirection(targetTileX, targetTileY);
        if (myPlannedDirection == 0) myPlannedDirection = currentDirection;

        for (Vehicle other : allVehicles) {
            if (other == this) continue;
            if (!other.isSpawned()) continue;

            // Determine other vehicle's direction
            int otherDirection = other.currentDirection;
            if (otherDirection == 0 && other.targetTileX != null && other.targetTileY != null) {
                otherDirection = other.getPlannedDirection(other.targetTileX, other.targetTileY);
            }

            // If other is on our target tile in the same direction, we MUST STOP
            if (other.currentTileX == targetTileX && other.currentTileY == targetTileY) {
                if (otherDirection == myPlannedDirection && otherDirection != 0) {
                    effectiveSpeed = 0; // STOP - cannot overtake
                    return;
                }
            }
        }
    }

    /**
     * Minimal economy hook: call after {@link #update(Map, double)}.
     * Acts when the vehicle arrived at a tile adjacent to a city or industry.
     */
    public void processArrivalEconomy(Map map, Player player) {
        if (!arrivedThisUpdate) return;
        arrivedThisUpdate = false;
        if (map == null || player == null) return;

        if (!hasPath() || pathTiles.isEmpty()) return;

        // Check if we're adjacent to any city or industry
        City adjacentCity = findAdjacentCity(map, currentTileX, currentTileY);
        Industry adjacentIndustry = findAdjacentIndustry(map, currentTileX, currentTileY);

        // If not adjacent to anything, don't process
        if (adjacentCity == null && adjacentIndustry == null) return;

        // Find all cities and industries along the route for delivery logic
        City nextCity = findNextCityOnRoute(map);
        Industry nextIndustry = findNextIndustryOnRoute(map);

        // Better priority rules:
        // - If empty: prefer industry pickup (bus will skip due to canCarry checks).
        // - If carrying passengers: prefer city dropoff.
        // - If carrying goods: prefer industry if it consumes it, otherwise city if it demands it.
        if (currentCargo == null || currentCargo.isEmpty()) {
            if (adjacentIndustry != null) handleIndustryInteraction(adjacentIndustry, player, nextCity, nextIndustry);
            if (adjacentCity != null) handleCityInteraction(adjacentCity, player);
            return;
        }

        ResourceType cargoType = currentCargo.getType();
        if (cargoType == ResourceType.PASSENGERS) {
            if (adjacentCity != null) handleCityInteraction(adjacentCity, player);
            return;
        }

        if (adjacentIndustry != null && adjacentIndustry.consumes(cargoType)) {
            handleIndustryInteraction(adjacentIndustry, player, nextCity, nextIndustry);
        } else if (adjacentCity != null) {
            handleCityInteraction(adjacentCity, player);
        } else if (adjacentIndustry != null) {
            // Can't unload here, but still allow potential logic in the future.
            handleIndustryInteraction(adjacentIndustry, player, nextCity, nextIndustry);
        }
    }

    protected void arriveAtTarget(Map map) {
        arriveAtTarget(map, null, null);
    }

    protected void arriveAtTarget(Map map, List<Vehicle> allVehicles) {
        arriveAtTarget(map, allVehicles, null);
    }

    protected void arriveAtTarget(Map map, List<Vehicle> allVehicles, List<game.building.TrafficLight> trafficLights) {
        if (targetTileX == null || targetTileY == null) return;
        previousTileX = currentTileX;
        previousTileY = currentTileY;

        int newTileX = targetTileX;
        int newTileY = targetTileY;
        lastMoveDx = Integer.compare(newTileX, currentTileX);
        lastMoveDy = Integer.compare(newTileY, currentTileY);

        currentTileX = newTileX;
        currentTileY = newTileY;
        targetTileX = null;
        targetTileY = null;

        // Update direction based on movement
        updateDirection();

        // Clear intersection claim if we're leaving it
        if (intersectionClaimX != null && intersectionClaimY != null) {
            if (currentTileX != intersectionClaimX || currentTileY != intersectionClaimY) {
                // We've left the intersection
                intersectionClaimX = null;
                intersectionClaimY = null;
            }
        }

        arrivedThisUpdate = true;

        // Maintenance: if we arrived at the garage destination, start maintenance.
        if (goingToMaintenance
                && maintenanceDestRoadX != null && maintenanceDestRoadY != null
                && currentTileX == maintenanceDestRoadX && currentTileY == maintenanceDestRoadY) {
            goingToMaintenance = false;
            inMaintenance = true;
            maintenanceSecondsRemaining = 5.0;

            // Park in the garage (idle).
            this.pathTiles = List.of();
            this.targetTileX = null;
            this.targetTileY = null;
            return;
        }

        // If maintenance is due, request it and start the garage trip on the next tick.
        if (getSecondsUntilMaintenanceDue() <= 0 && !goingToMaintenance && !rejoiningRoute) {
            maintenanceRequested = true;
        }
        chooseNextTarget(map, allVehicles, trafficLights);
    }

    protected void updateDirection() {
        if (lastMoveDx == 0 && lastMoveDy == -1) currentDirection = 1; // North
        else if (lastMoveDx == 1 && lastMoveDy == 0) currentDirection = 2; // East
        else if (lastMoveDx == 0 && lastMoveDy == 1) currentDirection = 3; // South
        else if (lastMoveDx == -1 && lastMoveDy == 0) currentDirection = 4; // West
        else currentDirection = 0; // None/stopped
    }

    protected void chooseNextTarget(Map map) {
        chooseNextTarget(map, null, null);
    }

    protected void chooseNextTarget(Map map, List<Vehicle> allVehicles) {
        chooseNextTarget(map, allVehicles, null);
    }

    protected void chooseNextTarget(Map map, List<Vehicle> allVehicles, List<game.building.TrafficLight> trafficLights) {
        if (!hasPath()) {
            targetTileX = null;
            targetTileY = null;
            return;
        }

        // Ensure current index is consistent.
        if (pathIndex < 0) pathIndex = 0;
        if (pathIndex >= pathTiles.size()) pathIndex = pathTiles.size() - 1;

        // If we're not exactly on the expected tile, try to resync.
        int[] expected = pathTiles.get(pathIndex);
        if (expected[0] != currentTileX || expected[1] != currentTileY) {
            int idx = indexOfTile(pathTiles, currentTileX, currentTileY);
            if (idx >= 0) {
                pathIndex = idx;
            }
        }

        // Always move forward for circular routes
        int nextIndex = pathIndex + 1;
        if (nextIndex >= pathTiles.size()) {
            // Wrap back to the beginning for circular routes
            nextIndex = 0;
        }

        if (nextIndex < 0 || nextIndex >= pathTiles.size()) {
            targetTileX = null;
            targetTileY = null;
            return;
        }

        int[] next = pathTiles.get(nextIndex);
        // Safety: only move onto ROAD.
        if (!isRoad(map, next[0], next[1])) {
            targetTileX = null;
            targetTileY = null;
            return;
        }

        // Calculate what our direction WILL BE when we move to next tile
        int plannedDirection = getPlannedDirection(next[0], next[1]);

        // Check for traffic light at the next tile
        game.building.TrafficLight lightAtNext = findTrafficLightAt(trafficLights, next[0], next[1]);
        if (lightAtNext != null) {
            // Check if light is red for our direction
            if (!lightAtNext.isGreen(plannedDirection)) {
                // MUST STOP at red light
                targetTileX = null;
                targetTileY = null;
                return;
            }
        }

        // Check for intersection conflict before setting target
        if (allVehicles != null && isIntersection(map, next[0], next[1])) {
            // If there's a traffic light, don't use intersection priority rules
            if (lightAtNext == null && hasIntersectionConflict(next[0], next[1], allVehicles, plannedDirection)) {
                // Wait - don't set target yet
                targetTileX = null;
                targetTileY = null;
                return;
            }
            // Claim this intersection as ours (only if no traffic light)
            if (lightAtNext == null) {
                intersectionClaimX = next[0];
                intersectionClaimY = next[1];
            }
        } else {
            // Not an intersection or no conflict, clear claim
            intersectionClaimX = null;
            intersectionClaimY = null;
        }

        targetTileX = next[0];
        targetTileY = next[1];
        pathIndex = nextIndex;
    }

    /**
     * Find a traffic light at the specified coordinates.
     */
    private static game.building.TrafficLight findTrafficLightAt(List<game.building.TrafficLight> trafficLights, int x, int y) {
        if (trafficLights == null) return null;
        for (game.building.TrafficLight light : trafficLights) {
            if (light != null && light.getX() == x && light.getY() == y) {
                return light;
            }
        }
        return null;
    }

    /**
     * Check if a tile is an intersection (has more than 2 road neighbors).
     */
    protected boolean isIntersection(Map map, int x, int y) {
        if (!isRoad(map, x, y)) return false;
        return roadNeighbors(map, x, y).size() > 2;
    }

    /**
     * Check if there's a vehicle in the intersection moving on a crossing path.
     * Returns true if we should wait.
     * We must wait until ANY vehicle with a crossing path has left the intersection.
     * First vehicle to claim the intersection gets priority.
     */
    protected boolean hasIntersectionConflict(int intersectionX, int intersectionY, List<Vehicle> allVehicles, int plannedDirection) {
        if (allVehicles == null) return false;
        if (plannedDirection == 0) return false;

        for (Vehicle other : allVehicles) {
            if (other == this) continue;
            if (!other.isSpawned()) continue;

            // Check if other vehicle is in the intersection
            boolean otherInIntersection = (other.currentTileX == intersectionX && other.currentTileY == intersectionY);

            // Check if other vehicle has claimed this intersection (got there first)
            boolean otherClaimedIntersection = (other.intersectionClaimX != null &&
                                                 other.intersectionClaimX == intersectionX &&
                                                 other.intersectionClaimY == intersectionY);

            if (otherInIntersection || otherClaimedIntersection) {
                // Use the other vehicle's current direction
                int otherDirection = other.currentDirection;

                // If other vehicle doesn't have a direction yet but has a target, calculate it
                if (otherDirection == 0 && other.targetTileX != null && other.targetTileY != null) {
                    otherDirection = other.getPlannedDirection(other.targetTileX, other.targetTileY);
                }

                // Check if paths cross
                if (pathsCross(plannedDirection, otherDirection)) {
                    return true; // Conflict - we must wait until other leaves
                }
            }
        }
        return false;
    }

    /**
     * Determine which direction we plan to move when entering the intersection.
     */
    protected int getPlannedDirection(int nextTileX, int nextTileY) {
        int dx = nextTileX - currentTileX;
        int dy = nextTileY - currentTileY;

        if (dx == 0 && dy == -1) return 1; // North
        if (dx == 1 && dy == 0) return 2; // East
        if (dx == 0 && dy == 1) return 3; // South
        if (dx == -1 && dy == 0) return 4; // West
        return 0;
    }

    /**
     * Check if two directions cross each other (are perpendicular).
     * 1=N, 2=E, 3=S, 4=W
     */
    protected boolean pathsCross(int dir1, int dir2) {
        if (dir1 == 0 || dir2 == 0) return false;
        // North/South (1,3) crosses East/West (2,4)
        return (dir1 == 1 || dir1 == 3) && (dir2 == 2 || dir2 == 4)
            || (dir1 == 2 || dir1 == 4) && (dir2 == 1 || dir2 == 3);
    }

    private static int indexOfTile(List<int[]> tiles, int x, int y) {
        for (int i = 0; i < tiles.size(); i++) {
            int[] t = tiles.get(i);
            if (t != null && t.length >= 2 && t[0] == x && t[1] == y) return i;
        }
        return -1;
    }

    protected boolean isPrevious(int x, int y) {
        return previousTileX != null && previousTileY != null && previousTileX == x && previousTileY == y;
    }

    protected static boolean isRoad(Map map, int x, int y) {
        Tile tile = map.getTile(x, y);
        return tile != null && tile.getType() == TileType.ROAD;
    }

    protected static List<int[]> roadNeighbors(Map map, int x, int y) {
        List<int[]> result = new ArrayList<>(4);
        if (isRoad(map, x + 1, y)) result.add(new int[]{x + 1, y});
        if (isRoad(map, x - 1, y)) result.add(new int[]{x - 1, y});
        if (isRoad(map, x, y + 1)) result.add(new int[]{x, y + 1});
        if (isRoad(map, x, y - 1)) result.add(new int[]{x, y - 1});
        return result;
    }

    protected static double tileCenterX(int tileX) {
        return tileX * (double) TILE_SIZE_PX + (TILE_SIZE_PX / 2.0);
    }

    protected static double tileCenterY(int tileY) {
        return tileY * (double) TILE_SIZE_PX + (TILE_SIZE_PX / 2.0);
    }

    public void transport() {}

    public boolean needsMaintenance() {
        return getSecondsUntilMaintenanceDue() <= 0;
    }

    public void goToGarage() {}

    protected boolean canCarry(ResourceType type) {
        return true;
    }

    private void handleCityInteraction(City city, Player player) {
        if (city == null) return;

        if (currentCargo == null || currentCargo.isEmpty()) {
            // Load passengers only (goods are not produced by cities in this minimal model).
            if (!canCarry(ResourceType.PASSENGERS)) return;
            int taken = city.load(ResourceType.PASSENGERS, Math.max(0, capacity));
            if (taken > 0) {
                currentCargo = new Resource(ResourceType.PASSENGERS, taken);
            }
            return;
        }

        // Unload.
        ResourceType type = currentCargo.getType();
        int amount = currentCargo.getAmount();
        if (amount <= 0) return;

        int accepted = city.deliver(type, amount);
        accepted = Math.max(0, Math.min(accepted, amount));
        if (accepted <= 0) return;

        int revenue = accepted * ResourcePrices.revenuePerUnit(type);
        if (revenue != 0) player.addMoney(revenue);
        currentCargo.removeUpTo(accepted);
        if (currentCargo.isEmpty()) currentCargo = null;
    }

    private void handleIndustryInteraction(Industry industry, Player player, City otherEndpointCity, Industry otherEndpointIndustry) {
        if (industry == null) return;

        if (currentCargo == null || currentCargo.isEmpty()) {
            // Load only produced goods that can be delivered to the other endpoint.
            for (ResourceType type : industry.getProfile().getOutputsPerUnit().keySet()) {
                if (type == null) continue;
                if (type == ResourceType.PASSENGERS) continue;
                if (!canCarry(type)) continue;
                if (!canDeliverToOtherEndpoint(type, otherEndpointCity, otherEndpointIndustry)) continue;

                int taken = industry.takeFromStorage(type, Math.max(0, capacity));
                if (taken > 0) {
                    currentCargo = new Resource(type, taken);
                    return;
                }
            }
            return;
        }

        // Deliver inputs to industries that consume them.
        ResourceType type = currentCargo.getType();
        int amount = currentCargo.getAmount();
        if (amount <= 0) return;

        if (!industry.consumes(type)) return;
        industry.deliverToStorage(type, amount);

        int revenue = amount * ResourcePrices.revenuePerUnit(type);
        if (revenue != 0) player.addMoney(revenue);
        currentCargo = null;
    }

    private static boolean canDeliverToOtherEndpoint(ResourceType type, City otherCity, Industry otherIndustry) {
        if (type == null) return false;

        if (otherIndustry != null && otherIndustry.consumes(type)) return true;

        if (otherCity != null) {
            if (type == ResourceType.PASSENGERS) return true;
            return otherCity.getDemandBacklog().get(type) > 0;
        }

        return false;
    }

    private static City findAdjacentCity(Map map, int roadX, int roadY) {
        for (City c : map.getCities()) {
            if (c == null) continue;
            if (c.occupies(roadX + 1, roadY)
                    || c.occupies(roadX - 1, roadY)
                    || c.occupies(roadX, roadY + 1)
                    || c.occupies(roadX, roadY - 1)) {
                return c;
            }
        }
        return null;
    }

    private static Industry findAdjacentIndustry(Map map, int roadX, int roadY) {
        for (Industry i : map.getIndustries()) {
            if (i == null) continue;
            if (i.occupies(roadX + 1, roadY)
                    || i.occupies(roadX - 1, roadY)
                    || i.occupies(roadX, roadY + 1)
                    || i.occupies(roadX, roadY - 1)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Find the next city along the vehicle's route (looking ahead from current position).
     */
    private City findNextCityOnRoute(Map map) {
        if (!hasPath()) return null;
        int currentIdx = indexOfTile(pathTiles, currentTileX, currentTileY);
        if (currentIdx < 0) return null;

        // Search forward along the path
        for (int i = 1; i < pathTiles.size(); i++) {
            int idx = (currentIdx + i) % pathTiles.size();
            int[] tile = pathTiles.get(idx);
            City city = findAdjacentCity(map, tile[0], tile[1]);
            if (city != null) return city;
        }
        return null;
    }

    /**
     * Find the next industry along the vehicle's route (looking ahead from current position).
     */
    private Industry findNextIndustryOnRoute(Map map) {
        if (!hasPath()) return null;
        int currentIdx = indexOfTile(pathTiles, currentTileX, currentTileY);
        if (currentIdx < 0) return null;

        // Search forward along the path
        for (int i = 1; i < pathTiles.size(); i++) {
            int idx = (currentIdx + i) % pathTiles.size();
            int[] tile = pathTiles.get(idx);
            Industry industry = findAdjacentIndustry(map, tile[0], tile[1]);
            if (industry != null) return industry;
        }
        return null;
    }
}