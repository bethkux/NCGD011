package cz.cuni.gamedev.nail123.mcworldgeneration;

import cz.cuni.gamedev.nail123.mcworldgeneration.chunking.IChunk;
import org.bukkit.Material;
import org.bukkit.util.noise.OctaveGenerator;
import org.bukkit.util.noise.SimplexOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class CustomChunkGeneratorJava implements IChunkGenerator {
    public long seed;
    OctaveGenerator generator;

    public CustomChunkGeneratorJava(long seed) {
        this.seed = seed;
        this.generator = new SimplexOctaveGenerator(new Random(this.seed), 8);
        this.generator.setScale(0.001);
    }
    public CustomChunkGeneratorJava() {
        this((new Random()).nextLong());
    }

    public void generateChunk(int chunkX, int chunkZ, @NotNull IChunk chunk) {
        for (int X = 0; X <= 15; ++X) {
            for (int Z = 0; Z <= 15; ++Z) {
                // X and Z are the geographic coordinates in Minecraft

                // Noise normalized to -1 .. 1
                double noise = generator.noise(chunkX * 16.0 + X, chunkZ * 16.0 + Z, 2.0, 0.5, true);

                int currentHeight = (int) (Math.round((noise + 1) * 50.0 + 30.0));

                // If lower than sea level (62), add water
                for (int i = currentHeight + 1; i <= 62; ++i) {
                    chunk.set(X, i, Z, Material.WATER);
                }

                // Set top layer of material based on height
                if (currentHeight <= 70) {
                    chunk.set(X, currentHeight, Z, Material.SAND);
                } else if (currentHeight <= 95) {
                    chunk.set(X, currentHeight, Z, Material.GRASS_BLOCK);
                } else {
                    chunk.set(X, currentHeight, Z, Material.SNOW);
                }

                // Place dirt one block underneath
                chunk.set(X, currentHeight - 1, Z, Material.DIRT);

                // Place stone all the way to the bottom, except for the last layer
                for (int i = currentHeight - 2; i >= 1; --i) {
                    chunk.set(X, i, Z, Material.STONE);
                }

                // ... which is bedrock
                chunk.set(X, 0, Z, Material.BEDROCK);
            }
        }
    }
}
