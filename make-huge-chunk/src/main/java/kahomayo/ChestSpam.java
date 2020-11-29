package kahomayo;

import org.jnbt.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;

import static java.util.Map.entry;
import static kahomayo.Utils.*;

public class ChestSpam {
    final static int[] POSSIBLE_ITEMS = Stream.of(
            IntStream.range(256, 302).boxed(),
            IntStream.range(310, 349).boxed(),
            IntStream.range(1, 7).boxed(),
            IntStream.range(12, 16).boxed(),
            IntStream.range(22, 26).boxed(),
            IntStream.range(35, 43).boxed(),
            IntStream.of(17, 20, 44, 45, 46, 48, 49, 50, 52, 53, 54, 57, 58, 61, 65, 66, 67, 69, 70, 72, 76, 77, 80, 81, 82, 84, 85, 86, 87, 88, 89, 91).boxed()
    ).flatMap(Function.identity()).mapToInt(i -> i).toArray();
    final static int WOOD_T = 59;
    final static int STONE_T = 131;
    final static int IRON_T = 250;
    final static int DIAMOND_T = 1561;
    final static int GOLD_T = 32;
    final static Map<Integer, Integer> DAMAGE_VALUES = Map.ofEntries(
            entry(17, 3),
            entry(35, 15),
            entry(256, IRON_T),
            entry(257, IRON_T),
            entry(258, IRON_T),
            entry(259, 64),
            entry(263, 2),
            entry(267, IRON_T),
            entry(268, WOOD_T),
            entry(269, WOOD_T),
            entry(270, WOOD_T),
            entry(271, WOOD_T),
            entry(272, STONE_T),
            entry(273, STONE_T),
            entry(274, STONE_T),
            entry(275, STONE_T),
            entry(276, DIAMOND_T),
            entry(277, DIAMOND_T),
            entry(278, DIAMOND_T),
            entry(279, DIAMOND_T),
            entry(283, GOLD_T),
            entry(284, GOLD_T),
            entry(285, GOLD_T),
            entry(286, GOLD_T),
            entry(290, WOOD_T),
            entry(291, STONE_T),
            entry(292, IRON_T),
            entry(293, DIAMOND_T),
            entry(294, 513),
            entry(298, armor(0, 0)),
            entry(299, armor(1, 0)),
            entry(300, armor(2, 0)),
            entry(301, armor(3, 0)),
            entry(306, armor(0, 2)),
            entry(307, armor(1, 2)),
            entry(308, armor(2, 2)),
            entry(309, armor(3, 2)),
            entry(310, armor(0, 3)),
            entry(311, armor(1, 3)),
            entry(312, armor(2, 3)),
            entry(313, armor(3, 3)),
            entry(314, armor(0, 4)),
            entry(315, armor(1, 4)),
            entry(316, armor(2, 4)),
            entry(317, armor(3, 4)),
            entry(351, 15)
    );
    final static int[] ITEM_CHOICES = Arrays.stream(POSSIBLE_ITEMS).boxed().flatMap(i ->
            DAMAGE_VALUES.containsKey(i) ?
                    IntStream.range(0, DAMAGE_VALUES.get(i)).map(d -> (i << 16) | d).boxed() :
                    Stream.of(i << 16)
    ).mapToInt(i -> i).toArray();
    public static final int SEED = 2185;

    static int armor(int type, int mat) {
        int[] arr = {11, 16, 15, 13};
        return arr[type] * 3 << mat;
    }

    public static void main(String[] args) throws IOException {
        Path outputPath = OUTPUT_PATH;
        int amountGarbage = Utils.firstWhere(0, 27 * 16 * 16 * 128, amount -> {
            CompoundTag rootTag = makeChunk(amount, new Random(SEED));
            var baos = new ByteArrayOutputStream();
            try (NBTOutputStream os = new NBTOutputStream(new DeflaterOutputStream(baos), false)) {
                os.writeTag(rootTag);
            } catch (IOException ignored) {
            }
            System.out.printf("g  %d, s  %d\n", amount, baos.size());
            return overflows(baos, 0);
        });

        final int SAFETY_BUFFER = 1;
        System.out.println(amountGarbage);

        try (NBTOutputStream os = new NBTOutputStream(Files.newOutputStream(outputPath))) {
            os.writeTag(makeChunk(amountGarbage - SAFETY_BUFFER, new Random(SEED)));
        }
    }

    private static CompoundTag makeChunk(int nItems, Random r) {
        var blockArray = new byte[32768];
        var blockLightArray = new byte[16384];
        var dataArray = new byte[16384];
        var skyLightArray = new byte[16384];
        var heightMapArray = new byte[256];
        List<Tag> tileEntities = new ArrayList<>();

        int i = 0;
        int remItems = nItems;
        while (remItems > 0) {
            while (idxToX(i) % 2 == idxToZ(i) % 2) {
                blockArray[i] = (byte) 5;
                ++i;
            }
            int amount = Integer.min(remItems, 27);
            blockArray[i] = 54;
            List<Tag> items = new ArrayList<>();
            for (byte j = 0; j < amount; ++j) {
                int itemChoice = ITEM_CHOICES[r.nextInt(ITEM_CHOICES.length)];
                short id = (short) (itemChoice >> 16);
                short damage = (short) (itemChoice & 0xffff);

                Map<String, Tag> item = new HashMap<>();
                item.put("Count", new ByteTag("Count", (byte) r.nextInt(256)));
                item.put("Damage", new ShortTag("Damage", damage));
                item.put("id", new ShortTag("id", id));
                item.put("Slot", new ByteTag("Slot", j));
                items.add(new CompoundTag("Item", item));
            }

            Map<String, Tag> chestTe = new HashMap<>();
            chestTe.put("x", new IntTag("x", idxToX(i) + CHUNK_X * 16));
            chestTe.put("y", new IntTag("y", idxToY(i)));
            chestTe.put("z", new IntTag("z", idxToZ(i) + CHUNK_Z * 16));
            chestTe.put("Items", new ListTag("Items", CompoundTag.class, items));
            chestTe.put("id", new StringTag("id", "Chest"));

            tileEntities.add(new CompoundTag("ChestEntity", chestTe));

            remItems -= 27;
            ++i;
        }

        var chunkComp = Utils.makeChunkCompound(blockArray, blockLightArray, dataArray, skyLightArray, heightMapArray, tileEntities);
        return chunkComp;
    }
}
