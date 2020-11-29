package kahomayo;

import org.jnbt.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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

        return Utils.makeChunkCompound(blockArray, blockLightArray, dataArray, skyLightArray, heightMapArray, tileEntities);
    }

    public static void main(String[] args) throws IOException {
        Path outputPath = Utils.OUTPUT_PATH;
        Files.createDirectories(outputPath.getParent());
        try(NBTOutputStream os = new NBTOutputStream(Files.newOutputStream(outputPath))) {
            os.writeTag(makeChunk(0, new Random(69420)));
        }
    }
}
