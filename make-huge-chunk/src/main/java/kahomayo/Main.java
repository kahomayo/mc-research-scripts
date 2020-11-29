package kahomayo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.zip.DeflaterOutputStream;

import org.jnbt.*;

public class Main {
    public static int arrIdx(int x, int y, int z) {
        return y  + x * 128 + z * 16 * 128;
    }

    private static String makeLongRandomString(Random r, int len) {
        String alphabet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append(alphabet.charAt(r.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    private static int firstWhere(int min, int max, IntPredicate p) {
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

    public static void main(String[] args) throws IOException {
        Path outputPath = Paths.get("World1","2","2","c.2.2.dat");

        int amountGarbage = firstWhere(0, 15 * 4 * 27900, amount -> {
            CompoundTag rootTag = makeChunk(amount, new Random(69420));
            var baos = new ByteArrayOutputStream();
            try (NBTOutputStream os = new NBTOutputStream(new DeflaterOutputStream(baos), false)) {
                os.writeTag(rootTag);
            } catch (IOException ignored) {
            }
            System.out.printf("g  %d, s  %d\n", amount, baos.size());
            return ((baos.size() + 5) / 4096 + 1) >= /*256*/ 350;
        });
        final int SAFETY_BUFFER = -16;
        System.out.println(amountGarbage);

        Files.createDirectories(outputPath.getParent());
        try(NBTOutputStream os = new NBTOutputStream(Files.newOutputStream(outputPath))) {
            os.writeTag(makeChunk(amountGarbage - SAFETY_BUFFER, new Random(69420)));
        }
        Random finalR = new Random();
        System.out.printf("Done, write %s, %s, %s, %s on a sign to overflow", makeLongRandomString(finalR, 15), makeLongRandomString(finalR, 15),makeLongRandomString(finalR, 15),makeLongRandomString(finalR, 15));
    }

    private static CompoundTag makeChunk(int amountGarbage, Random r) {
        var blockArray = new byte[32768];
        var blockLightArray = new byte[16384];
        var dataArray = new byte[16384];
        var skyLightArray = new byte[16384];
        var heightMapArray = new byte[256];
        List<Tag> tileEntities = new ArrayList<>();

        Arrays.fill(skyLightArray, (byte) 0xFF);
        // floor
        for (int x = 0; x < 16 ; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < 4; ++y) {
                    blockArray[arrIdx(x, y, z)] = (byte) 20;
                }
            }
        }

        IntStream pos = IntStream.range(4, 128).flatMap(y -> IntStream.range(1, 16).flatMap(x -> IntStream.range(1, 16).map(z -> arrIdx(x, y, z))));
        int[] positions = pos.limit((long) Math.ceil((double) amountGarbage / (15 * 4))).toArray();
        int i = 0;
        int maxY = 0;
        while (amountGarbage > 0) {
            int amount = Integer.min(amountGarbage, 15 * 4);
            makeSign(r, blockArray, dataArray, tileEntities, positions[i], amount);
            maxY = (positions[i] % (16 * 128)) % 128;
            ++i;
            amountGarbage -= amount;
        }

        for (int x = 0; x < 16 ; ++x) {
            for (int z = 0; z < 16; ++z) {
                blockArray[arrIdx(x, maxY + 1, z)] = (byte) 20;
            }
        }

        // wall
        for (int y = 0; y < maxY + 1; ++y) {
            for (int z = 0; z < 16; ++z) {
                blockArray[arrIdx(z, y, 0)] = (byte) 7;
                blockArray[arrIdx(z, y, 15)]= (byte) 7;
                blockArray[arrIdx(0, y, z)]= (byte) 7;
                blockArray[arrIdx(15, y, z)]= (byte) 7;
            }
        }



        Map<String, Tag> levelMap = new HashMap<>();
        levelMap.put("xPos", new IntTag("xPos", 2));
        levelMap.put("zPos", new IntTag("zPos", 2));
        levelMap.put("LastUpdate", new LongTag("LastUpdate", 500));
        levelMap.put("Blocks", new ByteArrayTag("Blocks", blockArray));
        levelMap.put("Data", new ByteArrayTag("Data", dataArray));
        levelMap.put("SkyLight", new ByteArrayTag("SkyLight", skyLightArray));
        levelMap.put("BlockLight", new ByteArrayTag("BlockLight", blockLightArray));
        levelMap.put("HeightMap", new ByteArrayTag("HeightMap", heightMapArray));
        levelMap.put("TerrainPopulated", new ByteTag("TerrainPopulated", (byte)1));
        levelMap.put("Entities", new ListTag("Entities", CompoundTag.class, Collections.emptyList()));
        levelMap.put("TileEntities", new ListTag("TileEntities", CompoundTag.class, tileEntities));

        var levelTag = new CompoundTag("Level", levelMap);
        return new CompoundTag("", Map.of("Level", levelTag));
    }

    private static void makeSign(Random r, byte[] blockArray, byte[] dataArray, List<Tag> tileEntities, int i, int amountData) {
        blockArray[i]=(byte) 68;
        dataArray[i /2] |= (byte) 3 << ((i %2) * 4);
        Map<String, Tag> te = new HashMap<>();
        te.put("id", new StringTag("id", "Sign"));
        int globalX = (i / (16 * 128)) + 2 * 16;
        int globalZ = ((i % (16 * 128)) / 128) + 2 * 16;
        int globalY = (i % (16 * 128)) % 128;
        te.put("x", new IntTag("x", globalX));
        te.put("y", new IntTag("y", globalY));
        te.put("z", new IntTag("z", globalZ));
        for (int line = 1; line <= 4; ++line) {
            int dataOnLine = Integer.min(amountData, 15);
            amountData -= dataOnLine;
            te.put("Text" + line, new StringTag("Text" + line, makeLongRandomString(r, dataOnLine)));
        }
        tileEntities.add(new CompoundTag("SignEntity", te));
    }

}
