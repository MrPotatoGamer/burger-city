package game.save;

import game.building.Building;
import game.building.Garage;
import game.building.Stop;
import game.building.TrafficLight;
import game.core.Player;
import game.core.TimeManager;
import game.map.City;
import game.map.Industry;
import game.map.IndustryType;
import game.map.Tile;
import game.map.TileType;
import game.resource.Resource;
import game.resource.ResourceInventory;
import game.resource.ResourceType;
import game.save.json.Json;
import game.vehicle.AdvancedBus;
import game.vehicle.AdvancedTruck;
import game.vehicle.Bus;
import game.vehicle.Truck;
import game.vehicle.Vehicle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SaveManager {

    private final Path savesDir;

    public SaveManager() {
        this(defaultSavesDir());
    }

    public SaveManager(Path savesDir) {
        this.savesDir = Objects.requireNonNull(savesDir, "savesDir");
    }

    public Path getSavesDir() {
        return savesDir;
    }

    private static Path defaultSavesDir() {
        return Paths.get(System.getProperty("user.home"), ".burgercity", "saves");
    }

    public List<SaveGame> listSaves() {
        ensureDir();
        List<SaveGame> saves = new ArrayList<>();
        try {
            if (!Files.exists(savesDir)) return List.of();
            try (var stream = Files.list(savesDir)) {
                stream
                        .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                        .forEach(p -> {
                            SaveGame sg = tryReadMetadata(p);
                            if (sg != null) saves.add(sg);
                        });
            }
        } catch (IOException e) {
            // Fall back to empty list.
            return List.of();
        }

        saves.sort(Comparator.comparingLong(SaveGame::getSavedAtEpochMillis).reversed());
        return saves;
    }

    public SaveGame createSave(String saveName,
                               game.map.Map map,
                               Player player,
                               TimeManager timeManager,
                               List<Vehicle> vehicles,
                               List<TrafficLight> trafficLights) throws IOException {
        ensureDir();

        String normalizedName = saveName == null ? "" : saveName.trim();
        if (normalizedName.isEmpty()) normalizedName = "save";

        for (SaveGame existing : listSaves()) {
            String existingName = existing.getSaveName();
            if (existingName != null && existingName.trim().equalsIgnoreCase(normalizedName)) {
                throw new IllegalArgumentException("Már létezik ilyen nevű mentés: " + normalizedName);
            }
        }

        long now = System.currentTimeMillis();
        String id = UUID.randomUUID().toString();
        String safeName = sanitizeFileComponent(normalizedName);
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withLocale(Locale.ROOT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(now));
        String fileName = stamp + "_" + safeName + "_" + id.substring(0, 8) + ".json";
        Path path = savesDir.resolve(fileName);

        GameSnapshot snapshot = capture(normalizedName, now, map, player, timeManager, vehicles, trafficLights);
        Map<String, Object> jsonObj = snapshotToJson(id, snapshot);
        Files.writeString(path, Json.stringify(jsonObj), StandardCharsets.UTF_8);

        return new SaveGame(id, normalizedName, now, fileName);
    }

    public GameSnapshot loadSnapshot(SaveGame save) throws IOException {
        Objects.requireNonNull(save, "save");
        ensureDir();
        Path path = savesDir.resolve(save.getFileName());
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Object parsed = Json.parse(content);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IOException("Invalid save file: root is not an object");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> obj = (Map<String, Object>) root;
        return snapshotFromJson(obj);
    }

    public LoadedState instantiate(GameSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        TimeManager tm = new TimeManager();
        if (snapshot.time() != null) {
            TimeManager.TimeSpeed speed = TimeManager.TimeSpeed.NORMAL;
            try {
                speed = TimeManager.TimeSpeed.valueOf(snapshot.time().speed());
            } catch (Exception ignored) {}
            tm.restore(speed, snapshot.time().totalTicks(), snapshot.time().gameTimeSeconds());
        }

        Player player = new Player(snapshot.player() == null ? 0 : snapshot.player().money());

        game.map.Map map = loadMap(snapshot.map());

        List<TrafficLight> trafficLights = new ArrayList<>();
        if (snapshot.trafficLights() != null) {
            for (GameSnapshot.TrafficLightData td : snapshot.trafficLights()) {
                if (td == null) continue;
                TrafficLight tl = new TrafficLight(td.x(), td.y());
                tl.restore(td.state(), td.timeInState(), td.greenDurationMain(), td.greenDurationCross());
                // Ensure it's on the map as a placed building.
                // Do not use gameplay validation during load (it can reject for unrelated reasons).
                if (!map.placeBuildingForLoad(td.x(), td.y(), tl)) {
                    System.err.println("[SaveManager] WARNING: Failed to restore TrafficLight at (" + td.x() + "," + td.y() + ")");
                }
                trafficLights.add(tl);
            }
        }

        List<Vehicle> vehicles = new ArrayList<>();
        if (snapshot.vehicles() != null) {
            for (GameSnapshot.VehicleData vd : snapshot.vehicles()) {
                if (vd == null) continue;
                // `kind` is stored as the vehicle class simple name by Vehicle.exportSaveData().
                // Be permissive for older/hand-edited saves (case/spacing/underscores).
                String rawKind = vd.kind();
                String kind = (rawKind == null) ? "" : rawKind.trim();
                String norm = kind.replaceAll("[\\s_-]+", "").toLowerCase(Locale.ROOT);

                Vehicle v = switch (norm) {
                    case "bus" -> new Bus();
                    case "truck" -> new Truck();
                    case "advancedbus" -> new AdvancedBus();
                    case "advancedtruck" -> new AdvancedTruck();
                    default -> {
                        // Unknown kind fallback to Truck.
                        yield new Truck();
                    }
                };

                v.importSaveData(vd, map);
                vehicles.add(v);
            }
        }

        return new LoadedState(map, player, tm, vehicles, trafficLights);
    }

    public record LoadedState(game.map.Map map, Player player, TimeManager timeManager, List<Vehicle> vehicles, List<TrafficLight> trafficLights) {}

    private game.map.Map loadMap(GameSnapshot.MapData data) {
        int w = (data == null) ? 50 : data.width();
        int h = (data == null) ? 40 : data.height();
        game.map.Map map = new game.map.Map(w, h);
        map.initGrassForLoad();

        if (data == null) {
            map.loadPredefined();
            return map;
        }

        // Cities
        if (data.cities() != null) {
            for (GameSnapshot.CityData cd : data.cities()) {
                if (cd == null) continue;
                City c = new City(cd.name(), cd.originX(), cd.originY(), cd.width(), cd.height());
                map.addCityForLoad(c);
                restoreInventory(c.getWaiting(), cd.waiting());
                restoreInventory(c.getDemandBacklog(), cd.demandBacklog());
            }
        }

        // Industries
        if (data.industries() != null) {
            for (GameSnapshot.IndustryData ind : data.industries()) {
                if (ind == null) continue;
                Industry i = new Industry(ind.name(), ind.type(), ind.originX(), ind.originY(), ind.width(), ind.height());
                map.addIndustryForLoad(i);
                restoreInventory(i.getStorage(), ind.storage());
            }
        }

        // Roads
        if (data.roads() != null) {
            for (GameSnapshot.IntPair p : data.roads()) {
                if (p == null) continue;
                if (!map.buildRoad(p.x(), p.y())) {
                    System.err.println("[SaveManager] WARNING: Failed to restore road at (" + p.x() + "," + p.y() + ")");
                }
            }
        }

        // Buildings
        if (data.buildings() != null) {
            for (GameSnapshot.BuildingData bd : data.buildings()) {
                if (bd == null) continue;
                Building b = switch (bd.type()) {
                    case "Garage" -> new Garage(bd.x(), bd.y());
                    case "Stop" -> new Stop(bd.x(), bd.y());
                    default -> null;
                };
                if (b != null) {
                    if (!map.placeBuildingForLoad(bd.x(), bd.y(), b)) {
                        Tile t = map.getTile(bd.x(), bd.y());
                        String tileType = (t == null || t.getType() == null) ? "?" : t.getType().name();
                        System.err.println("[SaveManager] WARNING: Failed to restore building " + bd.type() + " at (" + bd.x() + "," + bd.y() + ") on tile " + tileType);
                    }
                }
            }
        }

        // Forests (restore after roads/buildings so we don't block building placement)
        if (data.forests() != null) {
            for (GameSnapshot.ForestData f : data.forests()) {
                if (f == null) continue;
                applyForestIfPossible(map, f.x(), f.y(), f.trees());
            }
        }

        return map;
    }

    private static void applyForestIfPossible(game.map.Map map, int x, int y, int trees) {
        if (map == null) return;
        Tile tile = map.getTile(x, y);
        if (tile == null) return;
        if (tile.getPlacedBuilding() != null) return;
        if (tile.isOccupied()) return;
        if (tile.getType() != TileType.GRASS && tile.getType() != TileType.FOREST) return;

        int clamped = Math.max(1, Math.min(4, trees));
        tile.setForestTrees(clamped);
        tile.setWalkable(true);
        tile.setOccupied(false);
        tile.setPlacedBuilding(null);
    }

    private static void restoreInventory(ResourceInventory inv, Map<ResourceType, Integer> items) {
        if (inv == null || items == null) return;
        for (var e : items.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            int amount = Math.max(0, e.getValue());
            if (amount > 0) inv.add(e.getKey(), amount);
        }
    }

    private void ensureDir() {
        try {
            Files.createDirectories(savesDir);
        } catch (IOException ignored) {}
    }

    private static String sanitizeFileComponent(String name) {
        if (name == null) return "save";
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return "save";
        String s = trimmed.replaceAll("[^a-zA-Z0-9._-]+", "_");
        if (s.length() > 60) s = s.substring(0, 60);
        return s;
    }

    private SaveGame tryReadMetadata(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            Object parsed = Json.parse(content);
            if (!(parsed instanceof Map<?, ?> root)) return null;
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) root;
            String name = asString(obj.get("name"));
            long ts = asLong(obj.get("savedAtEpochMillis"), 0L);
            String id = asString(obj.get("id"));
            if (name == null) name = file.getFileName().toString();
            if (id == null) id = file.getFileName().toString();
            return new SaveGame(id, name, ts, file.getFileName().toString());
        } catch (Exception e) {
            return null;
        }
    }

    private GameSnapshot capture(String saveName,
                                 long savedAt,
                                 game.map.Map map,
                                 Player player,
                                 TimeManager timeManager,
                                 List<Vehicle> vehicles,
                                 List<TrafficLight> trafficLights) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(timeManager, "timeManager");

        GameSnapshot.PlayerData pd = new GameSnapshot.PlayerData(player.getMoney());
        GameSnapshot.TimeData td = new GameSnapshot.TimeData(
                timeManager.getCurrentSpeed().name(),
                timeManager.getTotalTicks(),
                timeManager.getGameTimeSeconds()
        );

        // Map contents
        List<GameSnapshot.CityData> cities = new ArrayList<>();
        for (City c : map.getCities()) {
            if (c == null) continue;
            cities.add(new GameSnapshot.CityData(
                    c.getName(), c.getOriginX(), c.getOriginY(), c.getWidth(), c.getHeight(),
                    toIntMap(c.getWaiting()),
                    toIntMap(c.getDemandBacklog())
            ));
        }

        List<GameSnapshot.IndustryData> industries = new ArrayList<>();
        for (Industry i : map.getIndustries()) {
            if (i == null) continue;
            industries.add(new GameSnapshot.IndustryData(
                    i.getName(), i.getIndustryType(), i.getOriginX(), i.getOriginY(), i.getWidth(), i.getHeight(),
                    toIntMap(i.getStorage())
            ));
        }

        List<GameSnapshot.IntPair> roads = new ArrayList<>();
        List<GameSnapshot.BuildingData> buildings = new ArrayList<>();
        List<GameSnapshot.ForestData> forests = new ArrayList<>();
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                Tile tile = map.getTile(x, y);
                if (tile == null) continue;
                if (tile.getType() == TileType.ROAD) {
                    roads.add(new GameSnapshot.IntPair(x, y));
                }
                if (tile.getType() == TileType.BUILDING) {
                    Building b = tile.getPlacedBuilding();
                    if (b instanceof Garage) buildings.add(new GameSnapshot.BuildingData("Garage", x, y));
                    else if (b instanceof Stop) buildings.add(new GameSnapshot.BuildingData("Stop", x, y));
                }
                if (tile.getType() == TileType.FOREST) {
                    int trees = Math.max(1, tile.getForestTrees());
                    forests.add(new GameSnapshot.ForestData(x, y, trees));
                }
            }
        }

        GameSnapshot.MapData md = new GameSnapshot.MapData(map.getWidth(), map.getHeight(), cities, industries, roads, buildings, forests);

        // Vehicles
        List<GameSnapshot.VehicleData> vds = new ArrayList<>();
        if (vehicles != null) {
            for (Vehicle v : vehicles) {
                if (v == null) continue;
                vds.add(v.exportSaveData());
            }
        }

        // Traffic lights
        List<GameSnapshot.TrafficLightData> tls = new ArrayList<>();
        if (trafficLights != null) {
            for (TrafficLight tl : trafficLights) {
                if (tl == null) continue;
                tls.add(tl.exportSaveData());
            }
        }

        return new GameSnapshot(
                GameSnapshot.CURRENT_VERSION,
                saveName,
                savedAt,
                pd,
                td,
                md,
                vds,
                tls
        );
    }

    private static Map<ResourceType, Integer> toIntMap(ResourceInventory inv) {
        if (inv == null) return Map.of();
        return inv.asUnmodifiableMap();
    }

    private static Map<String, Object> snapshotToJson(String id, GameSnapshot s) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("version", s.version());
        root.put("id", id);
        root.put("name", s.name());
        root.put("savedAtEpochMillis", s.savedAtEpochMillis());

        Map<String, Object> player = new LinkedHashMap<>();
        player.put("money", s.player().money());
        root.put("player", player);

        Map<String, Object> time = new LinkedHashMap<>();
        time.put("speed", s.time().speed());
        time.put("totalTicks", s.time().totalTicks());
        time.put("gameTimeSeconds", s.time().gameTimeSeconds());
        root.put("time", time);

        root.put("map", mapToJson(s.map()));

        List<Object> vehicles = new ArrayList<>();
        for (GameSnapshot.VehicleData vd : s.vehicles()) {
            vehicles.add(vehicleToJson(vd));
        }
        root.put("vehicles", vehicles);

        List<Object> lights = new ArrayList<>();
        for (GameSnapshot.TrafficLightData td : s.trafficLights()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("x", td.x());
            o.put("y", td.y());
            o.put("state", td.state());
            o.put("timeInState", td.timeInState());
            o.put("greenDurationMain", td.greenDurationMain());
            o.put("greenDurationCross", td.greenDurationCross());
            lights.add(o);
        }
        root.put("trafficLights", lights);

        return root;
    }

    private static Map<String, Object> mapToJson(GameSnapshot.MapData md) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("width", md.width());
        map.put("height", md.height());

        List<Object> cities = new ArrayList<>();
        for (GameSnapshot.CityData c : md.cities()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", c.name());
            o.put("originX", c.originX());
            o.put("originY", c.originY());
            o.put("width", c.width());
            o.put("height", c.height());
            o.put("waiting", resourceIntMapToJson(c.waiting()));
            o.put("demandBacklog", resourceIntMapToJson(c.demandBacklog()));
            cities.add(o);
        }
        map.put("cities", cities);

        List<Object> industries = new ArrayList<>();
        for (GameSnapshot.IndustryData i : md.industries()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("name", i.name());
            o.put("type", i.type().name());
            o.put("originX", i.originX());
            o.put("originY", i.originY());
            o.put("width", i.width());
            o.put("height", i.height());
            o.put("storage", resourceIntMapToJson(i.storage()));
            industries.add(o);
        }
        map.put("industries", industries);

        List<Object> roads = new ArrayList<>();
        for (GameSnapshot.IntPair p : md.roads()) {
            roads.add(List.of(p.x(), p.y()));
        }
        map.put("roads", roads);

        List<Object> buildings = new ArrayList<>();
        for (GameSnapshot.BuildingData b : md.buildings()) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("type", b.type());
            o.put("x", b.x());
            o.put("y", b.y());
            buildings.add(o);
        }
        map.put("buildings", buildings);

        List<Object> forests = new ArrayList<>();
        for (GameSnapshot.ForestData f : md.forests()) {
            forests.add(List.of(f.x(), f.y(), f.trees()));
        }
        map.put("forests", forests);

        return map;
    }

    private static Map<String, Object> vehicleToJson(GameSnapshot.VehicleData v) {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("kind", v.kind());
        o.put("worldX", v.worldX());
        o.put("worldY", v.worldY());
        o.put("currentTileX", v.currentTileX());
        o.put("currentTileY", v.currentTileY());
        o.put("targetTileX", v.targetTileX());
        o.put("targetTileY", v.targetTileY());
        o.put("previousTileX", v.previousTileX());
        o.put("previousTileY", v.previousTileY());
        o.put("lastMoveDx", v.lastMoveDx());
        o.put("lastMoveDy", v.lastMoveDy());
        o.put("currentDirection", v.currentDirection());
        o.put("pathIndex", v.pathIndex());
        o.put("pathForward", v.pathForward());

        List<Object> tiles = new ArrayList<>();
        for (GameSnapshot.IntPair p : v.pathTiles()) {
            tiles.add(List.of(p.x(), p.y()));
        }
        o.put("pathTiles", tiles);

        List<Object> routeTiles = new ArrayList<>();
        if (v.routePathTiles() != null) {
            for (GameSnapshot.IntPair p : v.routePathTiles()) {
                if (p == null) continue;
                routeTiles.add(List.of(p.x(), p.y()));
            }
        }
        o.put("routePathTiles", routeTiles);

        o.put("rejoiningRoute", v.rejoiningRoute());
        o.put("rejoinRouteAtX", v.rejoinRouteAtX());
        o.put("rejoinRouteAtY", v.rejoinRouteAtY());

        o.put("ageSeconds", v.ageSeconds());
        o.put("secondsSinceMaintenance", v.secondsSinceMaintenance());
        o.put("goingToMaintenance", v.goingToMaintenance());
        o.put("inMaintenance", v.inMaintenance());
        o.put("maintenanceSecondsRemaining", v.maintenanceSecondsRemaining());
        o.put("maintenanceDestRoadX", v.maintenanceDestRoadX());
        o.put("maintenanceDestRoadY", v.maintenanceDestRoadY());

        if (v.homeGarageX() != null && v.homeGarageY() != null) {
            o.put("homeGarage", List.of(v.homeGarageX(), v.homeGarageY()));
        } else {
            o.put("homeGarage", null);
        }

        if (v.maintenanceGarageX() != null && v.maintenanceGarageY() != null) {
            o.put("maintenanceGarage", List.of(v.maintenanceGarageX(), v.maintenanceGarageY()));
        } else {
            o.put("maintenanceGarage", null);
        }

        o.put("purchasePrice", v.purchasePrice());

        if (v.cargo() != null) {
            Map<String, Object> cargo = new LinkedHashMap<>();
            cargo.put("type", v.cargo().type().name());
            cargo.put("amount", v.cargo().amount());
            o.put("cargo", cargo);
        } else {
            o.put("cargo", null);
        }

        if (v.routeBuildings() != null) {
            Map<String, Object> rb = new LinkedHashMap<>();
            rb.put("startOriginX", v.routeBuildings().startOriginX());
            rb.put("startOriginY", v.routeBuildings().startOriginY());
            rb.put("endOriginX", v.routeBuildings().endOriginX());
            rb.put("endOriginY", v.routeBuildings().endOriginY());
            o.put("routeBuildings", rb);
        } else {
            o.put("routeBuildings", null);
        }

        return o;
    }

    private static Map<String, Object> resourceIntMapToJson(Map<ResourceType, Integer> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (map == null) return out;
        for (var e : map.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            out.put(e.getKey().name(), e.getValue());
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static GameSnapshot snapshotFromJson(Map<String, Object> root) {
        int version = (int) asLong(root.get("version"), 1L);
        String name = asString(root.get("name"));
        long ts = asLong(root.get("savedAtEpochMillis"), 0L);

        Map<String, Object> playerObj = (Map<String, Object>) root.get("player");
        int money = (int) asLong(playerObj == null ? null : playerObj.get("money"), 0L);

        Map<String, Object> timeObj = (Map<String, Object>) root.get("time");
        String speed = asString(timeObj == null ? null : timeObj.get("speed"));
        long totalTicks = asLong(timeObj == null ? null : timeObj.get("totalTicks"), 0L);
        double gameTimeSeconds = asDouble(timeObj == null ? null : timeObj.get("gameTimeSeconds"), 0.0);

        Map<String, Object> mapObj = (Map<String, Object>) root.get("map");
        GameSnapshot.MapData mapData = mapFromJson(mapObj);

        List<GameSnapshot.VehicleData> vehicles = new ArrayList<>();
        Object vehiclesObj = root.get("vehicles");
        if (vehiclesObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    vehicles.add(vehicleFromJson((Map<String, Object>) m));
                }
            }
        }

        List<GameSnapshot.TrafficLightData> tls = new ArrayList<>();
        Object tlObj = root.get("trafficLights");
        if (tlObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> o = (Map<String, Object>) m;
                    int x = (int) asLong(o.get("x"), 0L);
                    int y = (int) asLong(o.get("y"), 0L);
                    String state = asString(o.get("state"));
                    double timeInState = asDouble(o.get("timeInState"), 0.0);
                    double main = asDouble(o.get("greenDurationMain"), 5.0);
                    double cross = asDouble(o.get("greenDurationCross"), 5.0);
                    tls.add(new GameSnapshot.TrafficLightData(x, y, state, timeInState, main, cross));
                }
            }
        }

        return new GameSnapshot(
                version,
                name == null ? "save" : name,
                ts,
                new GameSnapshot.PlayerData(money),
                new GameSnapshot.TimeData(speed == null ? TimeManager.TimeSpeed.NORMAL.name() : speed, totalTicks, gameTimeSeconds),
                mapData,
                vehicles,
                tls
        );
    }

    @SuppressWarnings("unchecked")
    private static GameSnapshot.MapData mapFromJson(Map<String, Object> mapObj) {
        if (mapObj == null) {
            return new GameSnapshot.MapData(50, 40, List.of(), List.of(), List.of(), List.of(), List.of());
        }

        int w = (int) asLong(mapObj.get("width"), 50L);
        int h = (int) asLong(mapObj.get("height"), 40L);

        List<GameSnapshot.CityData> cities = new ArrayList<>();
        Object citiesObj = mapObj.get("cities");
        if (citiesObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) continue;
                Map<String, Object> o = (Map<String, Object>) m;
                String name = asString(o.get("name"));
                int ox = (int) asLong(o.get("originX"), 0L);
                int oy = (int) asLong(o.get("originY"), 0L);
                int cw = (int) asLong(o.get("width"), 3L);
                int ch = (int) asLong(o.get("height"), 3L);
                Map<ResourceType, Integer> waiting = resourceIntMapFromJson((Map<String, Object>) o.get("waiting"));
                Map<ResourceType, Integer> demand = resourceIntMapFromJson((Map<String, Object>) o.get("demandBacklog"));
                cities.add(new GameSnapshot.CityData(name, ox, oy, cw, ch, waiting, demand));
            }
        }

        List<GameSnapshot.IndustryData> industries = new ArrayList<>();
        Object indObj = mapObj.get("industries");
        if (indObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) continue;
                Map<String, Object> o = (Map<String, Object>) m;
                String name = asString(o.get("name"));
                String typeStr = asString(o.get("type"));
                IndustryType type;
                try {
                    type = IndustryType.valueOf(typeStr);
                } catch (Exception e) {
                    type = IndustryType.FACTORY;
                }
                int ox = (int) asLong(o.get("originX"), 0L);
                int oy = (int) asLong(o.get("originY"), 0L);
                int iw = (int) asLong(o.get("width"), 2L);
                int ih = (int) asLong(o.get("height"), 2L);
                Map<ResourceType, Integer> storage = resourceIntMapFromJson((Map<String, Object>) o.get("storage"));
                industries.add(new GameSnapshot.IndustryData(name, type, ox, oy, iw, ih, storage));
            }
        }

        List<GameSnapshot.IntPair> roads = new ArrayList<>();
        Object roadsObj = mapObj.get("roads");
        if (roadsObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof List<?> arr) || arr.size() < 2) continue;
                int x = (int) asLong(arr.get(0), 0L);
                int y = (int) asLong(arr.get(1), 0L);
                roads.add(new GameSnapshot.IntPair(x, y));
            }
        }

        List<GameSnapshot.BuildingData> buildings = new ArrayList<>();
        Object bObj = mapObj.get("buildings");
        if (bObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> m)) continue;
                Map<String, Object> o = (Map<String, Object>) m;
                String type = asString(o.get("type"));
                int x = (int) asLong(o.get("x"), 0L);
                int y = (int) asLong(o.get("y"), 0L);
                buildings.add(new GameSnapshot.BuildingData(type, x, y));
            }
        }

        List<GameSnapshot.ForestData> forests = new ArrayList<>();
        Object fObj = mapObj.get("forests");
        if (fObj instanceof List<?> list) {
            for (Object item : list) {
                // Accept either [x,y,trees] (preferred) or {x,y,trees}
                if (item instanceof List<?> arr) {
                    if (arr.size() < 2) continue;
                    int x = (int) asLong(arr.get(0), 0L);
                    int y = (int) asLong(arr.get(1), 0L);
                    int trees = (arr.size() >= 3) ? (int) asLong(arr.get(2), 1L) : 1;
                    if (trees > 0) forests.add(new GameSnapshot.ForestData(x, y, trees));
                } else if (item instanceof Map<?, ?> m) {
                    Map<String, Object> o = (Map<String, Object>) m;
                    int x = (int) asLong(o.get("x"), 0L);
                    int y = (int) asLong(o.get("y"), 0L);
                    int trees = (int) asLong(o.get("trees"), 1L);
                    if (trees > 0) forests.add(new GameSnapshot.ForestData(x, y, trees));
                }
            }
        }

        return new GameSnapshot.MapData(w, h, cities, industries, roads, buildings, forests);
    }

    @SuppressWarnings("unchecked")
    private static GameSnapshot.VehicleData vehicleFromJson(Map<String, Object> o) {
        String kind = asString(o.get("kind"));
        double worldX = asDouble(o.get("worldX"), 0.0);
        double worldY = asDouble(o.get("worldY"), 0.0);
        int cx = (int) asLong(o.get("currentTileX"), 0L);
        int cy = (int) asLong(o.get("currentTileY"), 0L);

        Integer tx = o.get("targetTileX") == null ? null : (int) asLong(o.get("targetTileX"), 0L);
        Integer ty = o.get("targetTileY") == null ? null : (int) asLong(o.get("targetTileY"), 0L);
        Integer px = o.get("previousTileX") == null ? null : (int) asLong(o.get("previousTileX"), 0L);
        Integer py = o.get("previousTileY") == null ? null : (int) asLong(o.get("previousTileY"), 0L);

        int lastMoveDx = (int) asLong(o.get("lastMoveDx"), 0L);
        int lastMoveDy = (int) asLong(o.get("lastMoveDy"), 0L);
        int currentDirection = (int) asLong(o.get("currentDirection"), 0L);

        int pathIndex = (int) asLong(o.get("pathIndex"), 0L);
        boolean pathForward = asBoolean(o.get("pathForward"), true);

        List<GameSnapshot.IntPair> pathTiles = new ArrayList<>();
        Object ptObj = o.get("pathTiles");
        if (ptObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof List<?> arr) || arr.size() < 2) continue;
                int x = (int) asLong(arr.get(0), 0L);
                int y = (int) asLong(arr.get(1), 0L);
                pathTiles.add(new GameSnapshot.IntPair(x, y));
            }
        }

        List<GameSnapshot.IntPair> routePathTiles = new ArrayList<>();
        Object rptObj = o.get("routePathTiles");
        if (rptObj instanceof List<?> list) {
            for (Object item : list) {
                if (!(item instanceof List<?> arr) || arr.size() < 2) continue;
                int x = (int) asLong(arr.get(0), 0L);
                int y = (int) asLong(arr.get(1), 0L);
                routePathTiles.add(new GameSnapshot.IntPair(x, y));
            }
        }

        boolean rejoiningRoute = asBoolean(o.get("rejoiningRoute"), false);
        Integer rejoinRouteAtX = o.get("rejoinRouteAtX") == null ? null : (int) asLong(o.get("rejoinRouteAtX"), 0L);
        Integer rejoinRouteAtY = o.get("rejoinRouteAtY") == null ? null : (int) asLong(o.get("rejoinRouteAtY"), 0L);

        double ageSeconds = asDouble(o.get("ageSeconds"), 0.0);
        double secondsSinceMaintenance = asDouble(o.get("secondsSinceMaintenance"), 0.0);
        boolean goingToMaintenance = asBoolean(o.get("goingToMaintenance"), false);
        boolean inMaintenance = asBoolean(o.get("inMaintenance"), false);
        double maintenanceSecondsRemaining = asDouble(o.get("maintenanceSecondsRemaining"), 0.0);
        Integer maintenanceDestRoadX = o.get("maintenanceDestRoadX") == null ? null : (int) asLong(o.get("maintenanceDestRoadX"), 0L);
        Integer maintenanceDestRoadY = o.get("maintenanceDestRoadY") == null ? null : (int) asLong(o.get("maintenanceDestRoadY"), 0L);

        Integer homeGarageX = null;
        Integer homeGarageY = null;
        Object hgObj = o.get("homeGarage");
        if (hgObj instanceof List<?> arr && arr.size() >= 2) {
            homeGarageX = (int) asLong(arr.get(0), 0L);
            homeGarageY = (int) asLong(arr.get(1), 0L);
        }

        Integer maintenanceGarageX = null;
        Integer maintenanceGarageY = null;
        Object mgObj = o.get("maintenanceGarage");
        if (mgObj instanceof List<?> arr && arr.size() >= 2) {
            maintenanceGarageX = (int) asLong(arr.get(0), 0L);
            maintenanceGarageY = (int) asLong(arr.get(1), 0L);
        }

        int purchasePrice = (int) asLong(o.get("purchasePrice"), 0L);

        GameSnapshot.CargoData cargo = null;
        Object cargoObj = o.get("cargo");
        if (cargoObj instanceof Map<?, ?> m) {
            Map<String, Object> c = (Map<String, Object>) m;
            String typeStr = asString(c.get("type"));
            int amount = (int) asLong(c.get("amount"), 0L);
            try {
                cargo = new GameSnapshot.CargoData(ResourceType.valueOf(typeStr), amount);
            } catch (Exception ignored) {
                cargo = null;
            }
        }

        GameSnapshot.RouteBuildingsData rb = null;
        Object rbObj = o.get("routeBuildings");
        if (rbObj instanceof Map<?, ?> m) {
            Map<String, Object> r = (Map<String, Object>) m;
            rb = new GameSnapshot.RouteBuildingsData(
                    (int) asLong(r.get("startOriginX"), 0L),
                    (int) asLong(r.get("startOriginY"), 0L),
                    (int) asLong(r.get("endOriginX"), 0L),
                    (int) asLong(r.get("endOriginY"), 0L)
            );
        }

        return new GameSnapshot.VehicleData(
                kind == null ? "Truck" : kind,
                worldX,
                worldY,
                cx,
                cy,
                tx,
                ty,
                px,
                py,
                lastMoveDx,
                lastMoveDy,
                currentDirection,
                pathTiles,
                pathIndex,
                pathForward,
                cargo,
            rb,
            routePathTiles,
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

    private static Map<ResourceType, Integer> resourceIntMapFromJson(Map<String, Object> obj) {
        if (obj == null) return Map.of();
        Map<ResourceType, Integer> out = new LinkedHashMap<>();
        for (var e : obj.entrySet()) {
            try {
                ResourceType t = ResourceType.valueOf(e.getKey());
                int amount = (int) asLong(e.getValue(), 0L);
                if (amount > 0) out.put(t, amount);
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static String asString(Object o) {
        return (o instanceof String s) ? s : null;
    }

    private static long asLong(Object o, long def) {
        if (o instanceof Number n) return n.longValue();
        return def;
    }

    private static double asDouble(Object o, double def) {
        if (o instanceof Number n) return n.doubleValue();
        return def;
    }

    private static boolean asBoolean(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        return def;
    }
}