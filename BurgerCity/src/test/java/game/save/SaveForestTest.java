package game.save;

import game.core.Player;
import game.core.TimeManager;
import game.map.Map;
import game.map.Tile;
import game.map.TileType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SaveForestTest {

    @Test
    void saveLoad_preservesForestTilesAndTreeCount() throws Exception {
        Path tmp = Files.createTempDirectory("burgercity-saves-");
        try {
            SaveManager sm = new SaveManager(tmp);

            Map map = new Map(12, 9);
            map.initGrassForLoad();

            assertTrue(map.setForestForLoad(2, 3, 4));
            assertTrue(map.setForestForLoad(10, 1, 2));
            assertTrue(map.setForestForLoad(0, 8, 1));

            Player player = new Player(1234);
            TimeManager tm = new TimeManager();

            SaveGame save = sm.createSave("test", map, player, tm, List.of(), List.of());
            GameSnapshot snap = sm.loadSnapshot(save);

            assertNotNull(snap.map());
            assertNotNull(snap.map().forests());

            assertTrue(
                    snap.map().forests().stream().anyMatch(f -> f.x() == 2 && f.y() == 3 && f.trees() == 4),
                    "Expected forest (2,3) with 4 trees in snapshot"
            );
            assertTrue(
                    snap.map().forests().stream().anyMatch(f -> f.x() == 10 && f.y() == 1 && f.trees() == 2),
                    "Expected forest (10,1) with 2 trees in snapshot"
            );
            assertTrue(
                    snap.map().forests().stream().anyMatch(f -> f.x() == 0 && f.y() == 8 && f.trees() == 1),
                    "Expected forest (0,8) with 1 tree in snapshot"
            );

            SaveManager.LoadedState loaded = sm.instantiate(snap);
            Tile t1 = loaded.map().getTile(2, 3);
            Tile t2 = loaded.map().getTile(10, 1);
            Tile t3 = loaded.map().getTile(0, 8);

            assertNotNull(t1);
            assertNotNull(t2);
            assertNotNull(t3);

            assertEquals(TileType.FOREST, t1.getType());
            assertEquals(4, t1.getForestTrees());

            assertEquals(TileType.FOREST, t2.getType());
            assertEquals(2, t2.getForestTrees());

            assertEquals(TileType.FOREST, t3.getType());
            assertEquals(1, t3.getForestTrees());

            Tile nonForest = loaded.map().getTile(5, 5);
            assertNotNull(nonForest);
            assertNotEquals(TileType.FOREST, nonForest.getType());
            assertEquals(0, nonForest.getForestTrees());
        } finally {
            // Best-effort cleanup
            try (var stream = Files.list(tmp)) {
                stream.forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {}
                });
            } catch (Exception ignored) {}
            try {
                Files.deleteIfExists(tmp);
            } catch (Exception ignored) {}
        }
    }
}
