package com.shanebeestudios.skbee.config;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.shanebeestudios.skbee.SkBee;
import com.shanebeestudios.skbee.api.bound.Bound;
import com.shanebeestudios.skbee.api.util.Util;
import com.shanebeestudios.skbee.api.util.WorldUtils;
import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import ch.njol.skript.Skript;

public class BoundConfig {

    private static final String UPDATED_18_HEIGHTS = "updated_18_heights";
    private static final String UPDATED_PER_WORLD_BOUND_IDS = "updated_per_world_bound_ids";

    private final SkBee plugin;
    private File boundFile;
    private FileConfiguration boundConfig;
    private final Map<String, Map<String, Bound>> worldBoundMap = new HashMap<>();

    public BoundConfig(SkBee plugin) {
        this.plugin = plugin;
        loadBoundConfig();
    }

    private void loadBoundConfig() {
        if (boundFile == null) {
            boundFile = new File(plugin.getDataFolder(), "bounds.yml");
        }
        if (!boundFile.exists()) {
            plugin.saveResource("bounds.yml", false);
        }
        boundConfig = YamlConfiguration.loadConfiguration(boundFile);
        loadBounds();

        // Update heights for 1.18 worlds
        if (Skript.isRunningMinecraft(1, 18) && !boundConfig.getBoolean(UPDATED_18_HEIGHTS)) {
            update18Heights();
        }

        // Update bound config to support per world bounds
        if (!boundConfig.getBoolean(UPDATED_PER_WORLD_BOUND_IDS)) {
            updatePerWorldBoundIDs();
        }

    }

    private void loadBounds() {
        ConfigurationSection section = boundConfig.getConfigurationSection("bounds");
        if (section == null) return;
        for (String key : section.getKeys(true)) {
            Object bound = section.get(key);
            if (bound instanceof Bound b) {
                Map<String, Bound> boundMap = getBoundsMap(b.getWorldName());
                boundMap.put(b.getId(), b);
            }
        }
    }

    private void update18Heights() {
        Util.log("Updating bounds to support Minecraft 1.18:");
        for (String worldName : worldBoundMap.keySet()) {
            for (Bound bound : getBoundsMap(worldName).values()) {
                int lesserY = bound.getLesserY();
                int greaterY = bound.getGreaterY();
                World world = bound.getWorld();
                if (lesserY == 0 && world != null) {
                    int minHeight = WorldUtils.getMinHeight(world);
                    int maxHeight = WorldUtils.getMaxHeight(world);
                    if (greaterY == 255 || greaterY == maxHeight) {
                        bound.setGreaterY(maxHeight);
                        bound.setLesserY(minHeight);
                        Util.log("  Updated bound with id '%s'", bound.getId());
                    }
                }
            }
        }
        boundConfig.set(UPDATED_18_HEIGHTS, true);
        saveAllBounds();
    }

    private void updatePerWorldBoundIDs() {
        try {
            Util.log("Updating bounds.yml to support per world bound IDs...");
            File backupBoundFile = new File(plugin.getDataFolder(), new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + "-bounds.yml.bak");
            FileUtils.copyFile(boundFile, backupBoundFile);
            Util.log("A backup of your bounds.yml has been saved to: " + backupBoundFile.getCanonicalPath());
            boundConfig.set("bounds", null);
            boundConfig.set(UPDATED_PER_WORLD_BOUND_IDS, true);
            saveAllBounds();
        } catch (IOException ex) {
            Util.log("&cUnable to update your bounds.yml! If you believe this is an SkBee issue, please report this to SkBee's issue tracker.");
            ex.printStackTrace(System.err);
        }
    }

    public void saveBound(Bound bound) {
        Map<String, Bound> boundMap = getBoundsMap(bound.getWorldName());
        boundMap.put(bound.getId(), bound);
        if (!bound.isTemporary()) {
            boundConfig.set("bounds." + bound.getWorldName() + "." + bound.getId(), bound);
            saveConfig();
        }
    }

    public void removeBound(Bound bound) {
        getBoundsMap(bound.getWorldName()).remove(bound.getId());
        if (!bound.isTemporary()) {
            boundConfig.set("bounds." + bound.getId(), null);
            saveConfig();
        }
    }

    public boolean boundExists(String id, String world) {
        return getBoundsMap(world).containsKey(id);
    }

    public Collection<Bound> getBoundsFromID(String id) {
        return worldBoundMap.values().stream()
                .flatMap(boundMap -> boundMap.values().stream())
                .filter(bound -> bound.getId().equals(id))
                .toList();
    }

    public Bound getBoundFromID(String id, World world) {
        return getBoundsMap(world.getName()).get(id);
    }

    public void saveAllBounds() {
        for (String world : worldBoundMap.keySet()) {
            for (Bound bound : getBoundsMap(world).values()) {
                if (bound.isTemporary()) continue;
                boundConfig.set("bounds." + bound.getWorldName() + "." + bound.getId(), bound);
                getBoundsMap(bound.getWorldName()).put(bound.getId(), bound);
            }
        }
        saveConfig();
    }

    private void saveConfig() {
        try {
            boundConfig.save(boundFile);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * @param world the world name
     * @return Bounds of the provided world
     */
    public Map<String, Bound> getBoundsMap(String world) {
        worldBoundMap.computeIfAbsent(world, w -> new HashMap<>());
        return worldBoundMap.get(world);
    }

    public Collection<Bound> getBounds() {
        return worldBoundMap.values().stream()
                .flatMap(boundMap -> boundMap.values().stream())
                .toList();
    }

    public Collection<Bound> getBoundsIn(World world) {
        return getBoundsMap(world.getName()).values().stream().filter(bound ->
                bound.getWorld() != null && bound.getWorld().equals(world))
                .toList();
    }

    public Collection<Bound> getBoundsAt(Location location) {
        return worldBoundMap.values().stream()
                .flatMap(boundMap -> boundMap.values().stream())
                .filter(bound -> bound.isInRegion(location))
                .toList();
    }

}
