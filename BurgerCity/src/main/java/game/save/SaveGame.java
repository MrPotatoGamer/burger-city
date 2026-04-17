package game.save;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class SaveGame {

    private final String id;
    private final String saveName;
    private final long savedAtEpochMillis;
    private final String fileName;

    public SaveGame(String id, String saveName, long savedAtEpochMillis, String fileName) {
        this.id = Objects.requireNonNull(id, "id");
        this.saveName = Objects.requireNonNull(saveName, "saveName");
        this.savedAtEpochMillis = savedAtEpochMillis;
        this.fileName = Objects.requireNonNull(fileName, "fileName");
    }

    public String getId() {
        return id;
    }

    public String getSaveName() {
        return saveName;
    }

    public long getSavedAtEpochMillis() {
        return savedAtEpochMillis;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        String ts = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(savedAtEpochMillis));
        return saveName + " (" + ts + ")";
    }

    public void delete() {}
}