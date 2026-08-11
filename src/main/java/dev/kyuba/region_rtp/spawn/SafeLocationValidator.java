package dev.kyuba.region_rtp.spawn;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import dev.kyuba.region_rtp.worldguard.ResolvedRegion;

/**
 * Centralized safety validation for a candidate teleport location.
 *
 * <p>Treats the teleport destination as the player's feet position. The
 * "ground" block is the block below the feet. The "head" block is one above
 * the feet.
 *
 * <p>All dangerous-material checks are centralized here rather than scattered
 * through the codebase.
 */
public final class SafeLocationValidator {

    /**
     * Options controlling per-region safety behaviour.
     */
    public record Options(boolean requireSkyExposure, boolean allowWater) {
        public static final Options DEFAULT = new Options(true, false);
    }

    /**
     * Result of a safety check.
     */
    public enum Result {
        /** Safe to stand at. */
        OK,
        /** Region manager / containment issue. */
        NOT_IN_REGION,
        /** Outside the world's valid build height. */
        OUT_OF_HEIGHT,
        /** The supporting (ground) block is missing or unsafe. */
        UNSAFE_GROUND,
        /** The feet block is solid or dangerous. */
        UNSAFE_FEET,
        /** The head block is solid or dangerous. */
        UNSAFE_HEAD,
        /** Sky exposure required but blocked above. */
        NO_SKY_EXPOSURE
    }

    /**
     * Validate a candidate feet location.
     *
     * @param feet    the block the player would stand in (feet position)
     * @param region  the resolved WorldGuard region, used for final containment
     * @param options per-region safety options
     * @return a {@link Result}
     */
    public @NotNull Result check(@NotNull Block feet, @NotNull ResolvedRegion region,
                                 @NotNull Options options) {
        World world = feet.getWorld();
        int x = feet.getX();
        int y = feet.getY();
        int z = feet.getZ();

        // Height check
        if (y < world.getMinHeight() || y > world.getMaxHeight()) {
            return Result.OUT_OF_HEIGHT;
        }
        int headY = y + 1;
        if (headY > world.getMaxHeight()) {
            return Result.OUT_OF_HEIGHT;
        }

        // WorldGuard vertical containment for feet and head
        if (!region.contains(x, y, z)) {
            return Result.NOT_IN_REGION;
        }
        if (!region.contains(x, headY, z)) {
            return Result.NOT_IN_REGION;
        }

        Block ground = feet.getRelative(BlockFace.DOWN);
        Block head = feet.getRelative(BlockFace.UP);

        // Ground: must be a solid, safe-to-stand-on block
        Material groundMat = ground.getType();
        if (!isSafeGround(groundMat)) {
            return Result.UNSAFE_GROUND;
        }

        // Feet: must be non-solid and non-dangerous
        Material feetMat = feet.getType();
        if (isSolid(feetMat) || isDangerous(feetMat, options)) {
            return Result.UNSAFE_FEET;
        }

        // Head: must be non-solid and non-dangerous
        Material headMat = head.getType();
        if (isSolid(headMat) || isDangerous(headMat, options)) {
            return Result.UNSAFE_HEAD;
        }

        // Sky exposure: no solid/occluding blocks above the head up to the sky
        if (options.requireSkyExposure() && !hasSkyExposure(world, x, headY, z)) {
            return Result.NO_SKY_EXPOSURE;
        }

        return Result.OK;
    }

    /**
     * Check whether there is a clear column from the head block up to the top
     * of the world — i.e. no occluding blocks above the player.
     *
     * <p>Uses the block's {@code isSolid()} as the occlusion test, which covers
     * typical ceilings, roofs and overhangs without relying on NMS light data.
     */
    private boolean hasSkyExposure(@NotNull World world, int x, int headY, int z) {
        for (int checkY = headY + 1; checkY <= world.getMaxHeight(); checkY++) {
            Block above = world.getBlockAt(x, checkY, z);
            if (above.getType().isSolid()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Build a Bukkit {@link Location} at the center of the given feet block.
     */
    public static @NotNull Location toLocation(@NotNull Block feet) {
        return new Location(feet.getWorld(), feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
    }

    /**
     * A block is a safe ground if it is solid and not a known hazard.
     */
    public static boolean isSafeGround(@NotNull Material material) {
        if (!material.isSolid()) {
            return false;
        }
        return !isDangerous(material, Options.DEFAULT);
    }

    /**
     * Whether the material is solid (would collide with a player body).
     */
    public static boolean isSolid(@NotNull Material material) {
        return material.isSolid();
    }

    /**
     * Whether the material can hurt, slow, or trap the player.
     *
     * <p>Only blocks that actively harm the player on contact are listed here.
     * Blocks that merely get destroyed (crops, farmland, flowers, tall grass)
     * are allowed — inside WorldGuard-protected regions they are
     * typically protected from block-breaking anyway, and they do not harm
     * the player standing in them.
     *
     * <p>Water is considered dangerous unless the region explicitly allows it
     * via {@link Options#allowWater()}.
     */
    public static boolean isDangerous(@NotNull Material material, @NotNull Options options) {
        if (material.isAir()) {
            return false;
        }
        return switch (material) {
            // Burns / fire
            case LAVA, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE -> true;
            // Contact damage
            case CACTUS, MAGMA_BLOCK, WITHER_ROSE, POINTED_DRIPSTONE -> true;
            // Suffocate / freeze / sink
            case POWDER_SNOW, BUBBLE_COLUMN -> true;
            // Slows and traps
            case COBWEB, SWEET_BERRY_BUSH -> true;
            // Unreliable / disappears
            case DRAGON_EGG -> true;
            // Fluids
            case WATER -> !options.allowWater();
            default -> false;
        };
    }
}