package net.serverplugins.anticheat;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;

final class MovementEnvironment {
    private MovementEnvironment() {
    }

    static boolean exempt(Player player) {
        return player.getAllowFlight() || player.isFlying() || player.isGliding()
                || player.isInsideVehicle() || player.isSwimming() || player.isRiptiding()
                || player.isInWater() || player.isClimbing()
                || player.hasPotionEffect(PotionEffectType.LEVITATION)
                || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)
                || player.hasPotionEffect(PotionEffectType.JUMP_BOOST)
                || player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)
                || player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.LUNGE);
    }

    static boolean specialBlocks(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!world.isChunkLoaded((x + dx) >> 4, (z + dz) >> 4)) {
                    return true;
                }
                for (int dy = -1; dy <= 2; dy++) {
                    Block block = world.getBlockAt(x + dx, y + dy, z + dz);
                    Material type = block.getType();
                    if (block.isLiquid() || type == Material.ICE || type == Material.PACKED_ICE
                            || type == Material.BLUE_ICE || type == Material.FROSTED_ICE
                            || type == Material.SLIME_BLOCK || type == Material.HONEY_BLOCK
                            || type == Material.SOUL_SAND || type == Material.SOUL_SOIL
                            || type == Material.COBWEB || type == Material.POWDER_SNOW
                            || type == Material.LADDER || type == Material.VINE
                            || type == Material.SCAFFOLDING || type == Material.BUBBLE_COLUMN
                            || type == Material.TWISTING_VINES || type == Material.TWISTING_VINES_PLANT
                            || type == Material.WEEPING_VINES || type == Material.WEEPING_VINES_PLANT
                            || type == Material.PISTON_HEAD || type == Material.MOVING_PISTON) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean supported(Player player) {
        BoundingBox body = player.getBoundingBox();
        BoundingBox feet = new BoundingBox(body.getMinX() - 0.15, body.getMinY() - 0.65, body.getMinZ() - 0.15,
                body.getMaxX() + 0.15, body.getMinY() + 0.1, body.getMaxZ() + 0.15);
        World world = player.getWorld();
        for (int x = (int) Math.floor(feet.getMinX()); x <= Math.floor(feet.getMaxX()); x++) {
            for (int z = (int) Math.floor(feet.getMinZ()); z <= Math.floor(feet.getMaxZ()); z++) {
                if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                    return true;
                }
                for (int y = (int) Math.floor(feet.getMinY()) - 1; y <= Math.floor(feet.getMaxY()); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    for (BoundingBox shape : block.getCollisionShape().getBoundingBoxes()) {
                        if (feet.overlaps(shape.clone().shift(x, y, z))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
