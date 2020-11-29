package kahomayo;

import org.jnbt.*;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.IntPredicate;

/**
 * Ok, ok, I guess I'll not copy-paste the same code 20 times.
 */
public class Utils {
    static final int CHUNK_X = 2;
    static final int CHUNK_Z = 2;
    static final Path OUTPUT_PATH = Paths.get("World1", "2", "2", "c.2.2.dat");

    static int arrIdx(int x, int y, int z) {
        return y  + x * 128 + z * 16 * 128;
    }

    static String makeLongRandomString(Random r, int len) {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    static int firstWhere(int min, int max, IntPredicate p) {
        while (min < max) {
            int mid  = (min + max) / 2;
            if (p.test(mid)) {
                max = mid;
            } else {
                min = mid + 1;
            }
        }
        return min;
    }

    static int idxToY(int i) {
        return (i % (16 * 128)) % 128;
    }

    static int idxToZ(int i) {
        return (i % (16 * 128)) / 128;
    }

    static int idxToX(int i) {
        return i / (16 * 128);
    }

    static CompoundTag makeChunkCompound(byte[] blockArray, byte[] blockLightArray, byte[] dataArray, byte[] skyLightArray, byte[] heightMapArray, List<Tag> tileEntities) {
        Map<String, Tag> levelMap = new HashMap<>();
        levelMap.put("xPos", new IntTag("xPos", CHUNK_X));
        levelMap.put("zPos", new IntTag("zPos", CHUNK_Z));
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

    static boolean overflows(ByteArrayOutputStream baos, int fudge) {
        return ((baos.size() + 5) / 4096 + 1) >= 256 + fudge;
    }
}
