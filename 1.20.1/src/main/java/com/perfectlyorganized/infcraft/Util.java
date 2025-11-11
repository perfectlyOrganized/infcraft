package com.perfectlyorganized.infcraft;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Util {
    /**
     * Gets all item entities currently loaded in the world
     * @param level The current world level
     * @return List of all loaded item entities with their data
     */
    public static List<ItemEntity> getAllLoadedItems(Optional<List<? extends String>> wordBlacklist) {
        List<Pattern> blacklistPatterns = wordBlacklist.orElseGet(Collections::emptyList).stream()
        .map(String::toLowerCase)
        .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b"))
        .collect(Collectors.toList());
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        List<ItemEntity> items = new ArrayList<>();
        if (server == null) {
            InfCraft.LOGGER.info("Server is null");
            return items;
        } 
        
        for (ServerLevel level : server.getAllLevels()) {
            getItemsInLevel(items, level);
        }

        items = items.stream()
        .filter(item -> {
            String displayName = getItemDisplayName(item.getItem().getItem()).toLowerCase();
            return blacklistPatterns.stream()
                .noneMatch(pattern -> pattern.matcher(displayName).find());
        })
        .collect(Collectors.toList());
        return items;
    }
    
    private static void getItemsInLevel(List<ItemEntity> items,ServerLevel level) {
        Iterable<Entity> entities = level.getAllEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity) entity;
                if (itemEntity.isAlive()) {
                    items.add(itemEntity);
                }
            }
        }
    }
   
    public static Collection<MobEffect> getAllPotionEffects() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        return server.registryAccess()
            .registryOrThrow(Registries.MOB_EFFECT)
            .stream()
            .collect(Collectors.toList());
    }
    public static String getAllItems(Optional<List<? extends String>> wordBlacklist) {
        List<Pattern> blacklistPatterns = wordBlacklist.orElseGet(Collections::emptyList).stream()
        .map(String::toLowerCase)
        .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b"))
        .collect(Collectors.toList());
        
        Collection<Item> allItems = ForgeRegistries.ITEMS.getValues().stream()
        .filter(Objects::nonNull) // Filter out null items
        .filter(item -> {
            String displayName = getItemDisplayName(item).toLowerCase();
            return blacklistPatterns.stream()
                .noneMatch(pattern -> pattern.matcher(displayName).find());
        })
        .collect(Collectors.toList());
        
        List<Item> shuffledItems = new ArrayList<>(allItems);
        Collections.shuffle(shuffledItems);
        StringBuilder output = new StringBuilder();
        for (Item item : shuffledItems) {
            ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
            if (registryName != null) {
                String namespace = registryName.getNamespace();
                String itemId = registryName.getPath();
                String displayName = getItemDisplayName(item);
                
                output.append(String.format("%s:%s, %s\n", namespace, itemId, displayName));
            }
        }

        return output.toString();
    }

    public static AIResponse getRandomItemResult() {
        List<Item> allItems = ForgeRegistries.ITEMS.getValues().stream()
        .filter(Objects::nonNull) // Filter out null items
        .collect(Collectors.toList());
        
        int randomIndex = (int) (Math.random() * allItems.size());
        Item randomItem = allItems.get(randomIndex);
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(randomItem);

        String namespace = registryName.getNamespace();
        String itemId = registryName.getPath();
        return new AIResponse(1, namespace + ":" + itemId);
    }

    public static String getItemDisplayName(Item item) {
        try {
            @SuppressWarnings("null")
            ItemStack stack = new ItemStack(item);
            return stack.getHoverName().getString();
        } catch (Exception e) {
            return "Unknown Name";
        }
    }
    public static String getItemId(ItemEntity entity) {
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(entity.getItem().getItem());
        return registryName.getNamespace() + ":" + registryName.getPath();
    }
    public static String getItemId(Item item) {
        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(item);
        return registryName.getNamespace() + ":" + registryName.getPath();
    }

    public static String getCombinationString(ItemEntity itemEntity) {
        return String.format("{\"name\": \"%s\", \"id\": \"%s\", \"count\": %d}",
            itemEntity.getItem().getHoverName().getString(),
            getItemId(itemEntity.getItem().getItem()),
            itemEntity.getItem().getCount());
    }
    public static Holder<Item> getItemById(String id) {
        return ForgeRegistries.ITEMS.getHolder(ResourceLocation.parse(id)).orElse(null);
    }
    
}