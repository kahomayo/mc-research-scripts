package kahomayo;

import org.jnbt.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.IntStream;
import static kahomayo.Main.arrIdx;

public class BigBlock {

    private static CompoundTag makeChunk(int amountGarbage, Random r) {
        var blockArray = new byte[32768];
        var blockLightArray = new byte[16384];
        var dataArray = new byte[16384];
        var skyLightArray = new byte[16384];
        var heightMapArray = new byte[256];
        List<Tag> tileEntities = new ArrayList<>();

        Arrays.fill(skyLightArray, (byte) 0xFF);
        Arrays.fill(blockArray, (byte) 7);

        Map<String, Tag> levelMap = new HashMap<>();
        levelMap.put("xPos", new IntTag("xPos", 2));
        levelMap.put("zPos", new IntTag("zPos", 2));
        levelMap.put("LastUpdate", new LongTag("LastUpdate", 500));
        levelMap.put("Blocks", new ByteArrayTag("Blocks", blockArray));
        levelMap.put("Data", new ByteArrayTag("Data", dataArray));
        levelMap.put("SkyLight", new ByteArrayTag("SkyLight", skyLightArray));
        levelMap.put("BlockLight", new ByteArrayTag("BlockLight", blockLightArray));
        levelMap.put("HeightMap", new ByteArrayTag("HeightMap", heightMapArray));
        levelMap.put("TerrainPopulated", new ByteTag("TerrainPopulated", (byte) 1));
        levelMap.put("Entities", new ListTag("Entities", CompoundTag.class, Collections.emptyList()));
        levelMap.put("TileEntities", new ListTag("TileEntities", CompoundTag.class, tileEntities));

        var levelTag = new CompoundTag("Level", levelMap);
        return new CompoundTag("", Map.of("Level", levelTag));
    }

    public static void main(String[] args) throws IOException {
        Path outputPath = Paths.get("World1","2","2","c.2.2.dat");
        Files.createDirectories(outputPath.getParent());
        try(NBTOutputStream os = new NBTOutputStream(Files.newOutputStream(outputPath))) {
            os.writeTag(makeChunk(0, new Random(69420)));
        }
    }
}
