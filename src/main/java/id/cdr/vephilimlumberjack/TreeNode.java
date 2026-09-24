package id.cdr.vephilimlumberjack;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

public record TreeNode(String id, String worldName, int x, int y, int z) {
    public Block block() {
        World world = Bukkit.getWorld(worldName);
        return world == null ? null : world.getBlockAt(x, y, z);
    }
}
