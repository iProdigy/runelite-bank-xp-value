package com.bankxpvalue;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.components.ImageComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
public class ItemDataCache {

    @Value
    static class ItemData {
        int id;
        double xp;
        String skill;
    }

    @Data
    static class SkillContents {
        double total = 0;
        List<ImageComponent> images = new ArrayList<>();
    }

    @Value
    static class ItemDataContainer {
        List<ItemData> items;
    }

    private static final Map<String, Integer> skills;
    private final Map<Integer, ItemData> cache = new HashMap<>();
    private final ItemManager itemManager;
    private final Gson gson;

    @Inject
    public ItemDataCache(ItemManager itemManager, Gson gson) {
        this.itemManager = itemManager;
        this.gson = gson;
        populateCache();
    }

    // Stores json data in hashmap
    private void populateCache() {
        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream("/item_xp_data.json"), StandardCharsets.UTF_8)) {
            ItemDataContainer data = gson.fromJson(reader, ItemDataContainer.class);
            for (ItemData item : data.items) {
                cache.put(item.id, item);
            }
        } catch (IOException e) {
            log.warn("Failed to read item xp data", e);
        }
    }

    // Computes the total xp for each skill
    public SkillContents[] getTotals(Item[] items) {
        final int n = skills.size() + 1;
        SkillContents[] skillContents = new SkillContents[n];

        for (int i = 0; i < n; i++) {
            skillContents[i] = new SkillContents();
        }

        SkillContents total = skillContents[n - 1];

        for (Item item : items) {
            getItem(item.getId()).ifPresent(data -> {
                SkillContents skillContent = skillContents[skills.get(data.skill)];

                // Add the XP to the skill's total
                skillContent.total += data.xp * item.getQuantity();
                total.total += data.xp * item.getQuantity();

                // Add the image to the skill's tooltip
                final BufferedImage image = itemManager.getImage(item.getId(), item.getQuantity(), true);
                skillContent.images.add(new ImageComponent(image));
            });
        }
        return skillContents;
    }

    // Outside classes use to search hash table
    public Optional<ItemData> getItem(int id) {
        return Optional.ofNullable(cache.get(id));
    }

    static {
        skills = ImmutableMap.<String, Integer>builder()
            .put("construction", 0)
            .put("cooking", 1)
            .put("crafting", 2)
            .put("farming", 3)
            .put("firemaking", 4)
            .put("fletching", 5)
            .put("herblore", 6)
            .put("prayer", 7)
            .put("smithing", 8)
            .build();
    }
}
