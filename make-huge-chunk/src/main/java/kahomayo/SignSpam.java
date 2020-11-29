package kahomayo;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import java.util.zip.DeflaterOutputStream;

import org.jnbt.*;

import static kahomayo.Utils.*;

public class SignSpam {

    public static void main(String[] args) throws IOException {
        Path outputPath = OUTPUT_PATH;

        int amountGarbage = Utils.firstWhere(0, 15 * 4 * 27900, amount -> {
            CompoundTag rootTag = makeChunk(amount, new Random(69420));
            var baos = new ByteArrayOutputStream();
            try (NBTOutputStream os = new NBTOutputStream(new DeflaterOutputStream(baos), false)) {
                os.writeTag(rootTag);
            } catch (IOException ignored) {
            }
            System.out.printf("g  %d, s  %d\n", amount, baos.size());
            return overflows(baos,  44);
        });
        final int SAFETY_BUFFER = -16;
        System.out.println(amountGarbage);

        Files.createDirectories(outputPath.getParent());
        try(NBTOutputStream os = new NBTOutputStream(Files.newOutputStream(outputPath))) {
            os.writeTag(makeChunk(amountGarbage - SAFETY_BUFFER, new Random(69420)));
        }
        Random finalR = new Random();
        System.out.printf("Done, write %s, %s, %s, %s on a sign to overflow", Utils.makeLongRandomString(finalR, 15), Utils.makeLongRandomString(finalR, 15), Utils.makeLongRandomString(finalR, 15), Utils.makeLongRandomString(finalR, 15));
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
            maxY = Utils.idxToY(positions[i]);
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


        return Utils.makeChunkCompound(blockArray, blockLightArray, dataArray, skyLightArray, heightMapArray, tileEntities);
    }

    private static void makeSign(Random r, byte[] blockArray, byte[] dataArray, List<Tag> tileEntities, int i, int amountData) {
        blockArray[i]=(byte) 68;
        dataArray[i /2] |= (byte) 3 << ((i %2) * 4);
        Map<String, Tag> te = new HashMap<>();
        te.put("id", new StringTag("id", "Sign"));
        int globalX = Utils.idxToX(i) + CHUNK_X * 16;
        int globalZ = Utils.idxToZ(i) + CHUNK_Z * 16;
        int globalY = Utils.idxToY(i);
        te.put("x", new IntTag("x", globalX));
        te.put("y", new IntTag("y", globalY));
        te.put("z", new IntTag("z", globalZ));
        for (int line = 1; line <= 4; ++line) {
            int dataOnLine = Integer.min(amountData, 15);
            amountData -= dataOnLine;
            te.put("Text" + line, new StringTag("Text" + line, Utils.makeLongRandomString(r, dataOnLine)));
        }
        tileEntities.add(new CompoundTag("SignEntity", te));
    }

}
