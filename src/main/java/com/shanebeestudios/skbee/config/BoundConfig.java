package com.shanebeestudios.skbee.config;

import ch.njol.skript.Skript;
import com.shanebeestudios.skbee.SkBee;
import com.shanebeestudios.skbee.api.bound.Bound;
import com.shanebeestudios.skbee.api.util.Util;
import com.shanebeestudios.skbee.api.util.WorldUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class BoundConfig {

    private static final String UPDATED_18_HEIGHTS = "updated_18_heights";

    private final SkBee plugin;
    private File boundFile;
    private FileConfiguration boundConfig;
    private final Map<World, Map<String, Bound>> worldBoundMap = new HashMap<>();

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
    }

    private void loadBounds() {
        ConfigurationSection section = boundConfig.getConfigurationSection("bounds");
        if (section == null) return;
        for (String key : section.getKeys(true)) {
            Object bound = section.get(key);
            if (bound instanceof Bound b) {
                Map<String, Bound> boundMap = getBoundsMap(b.getWorld());
                boundMap.put(b.getId(), b);
                worldBoundMap.put(b.getWorld(), boundMap);
            }
        }
    }

    private void update18Heights() {
        Util.log("Updating bounds:");
        for (World world : worldBoundMap.keySet()) {
            for (Bound bound : getBoundsMap(world).values()) {
                int lesserY = bound.getLesserY();
                int greaterY = bound.getGreaterY();
                if (lesserY == 0 && world != null) {
                    int minHeight = WorldUtils.getMinHeight(world);
                    int maxHeight = WorldUtils.getMaxHeight(world);
                    if (greaterY == 255 || greaterY == maxHeight) {
                        bound.setGreaterY(maxHeight);
                        bound.setLesserY(minHeight);
                        Util.log("Updating bound with id '%s'", bound.getId());
                    }
                }
            }
        }
        boundConfig.set(UPDATED_18_HEIGHTS, true);
        saveAllBounds();
    }

    public void saveBound(Bound bound) {
        Map<String, Bound> boundMap = worldBoundMap.get(bound.getWorld());
        if (boundMap == null)
            boundMap = new HashMap<>();
        boundMap.put(bound.getId(), bound);
        worldBoundMap.put(bound.getWorld(), boundMap);
        if (!bound.isTemporary()) {
            boundConfig.set("bounds." + bound.getId(), bound);
            saveConfig();
        }
    }

    public void removeBound(Bound bound) {
        getBoundsMap(bound.getWorld()).remove(bound.getId());
        if (!bound.isTemporary()) {
            boundConfig.set("bounds." + bound.getId(), null);
            saveConfig();
        }
    }

    public boolean boundExists(String id, World world) {
        return getBoundsMap(world).containsKey(id);
    }

    public Collection<Bound> getBoundsFromID(String id) {
        return worldBoundMap.values().stream()
                .flatMap(boundMap -> boundMap.values().stream())
                .filter(bound -> bound.getId().equals(id))
                .toList();
    }

    public Bound getBoundFromID(String id, World world) {
        return getBoundsMap(world).get(id);
    }

    public void saveAllBounds() {
        for (World world : worldBoundMap.keySet()) {
            for (Bound bound : getBoundsMap(world).values()) {
                if (bound.isTemporary()) continue;
                boundConfig.set("bounds." + bound.getId(), bound);
                getBoundsMap(bound.getWorld()).put(bound.getId(), bound);
            }
        }
        saveConfig();
    }

    private void saveConfig() {
        try {
            boundConfig.save(boundFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param world the world
     * @return the associated bounds map, otherwise an empty HashMap
     */
    public Map<String, Bound> getBoundsMap(World world) {
        worldBoundMap.computeIfAbsent(world, w -> new HashMap<>());
        return worldBoundMap.get(world);
    }

    public Collection<Bound> getBounds() {
        return worldBoundMap.values().stream()
                .flatMap(boundMap -> boundMap.values().stream())
                .toList();
    }

    public Collection<Bound> getBoundsIn(World world) {
        return getBoundsMap(world).values().stream().filter(bound ->
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
