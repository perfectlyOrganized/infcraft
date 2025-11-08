package com.perfectlyorganized.infcraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Util {
    /**
     * Gets all item entities currently loaded in the world
     * @param level The current world level
     * @return List of all loaded item entities with their data
     */
    public static List<ItemData> getAllLoadedItems() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        List<ItemData> items = new ArrayList<>();
        if (server == null) {
            InfCraft.LOGGER.info("Server is null");
            return items;
        } 
        
        for (ServerLevel level : server.getAllLevels()) {
            getItemsInLevel(items, level);
        }
        return items;
    }
    
    private static void getItemsInLevel(List<ItemData> items,ServerLevel level) {
        Iterable<Entity> entities = level.getAllEntities();
        for (Entity entity : entities) {
            if (entity instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity) entity;
                if (itemEntity.isAlive()) {
                    ItemData data = new ItemData(itemEntity);
                    
                    items.add(data);
                }
            }
        }
    }
   
    public static String getAllItems() {
        Collection<Item> allItems = BuiltInRegistries.ITEM.stream().toList();
        
        StringBuilder output = new StringBuilder();
        
        for (Item item : allItems) {
            @SuppressWarnings("null")
            ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(item);
            if (registryName != null) {
                String namespace = registryName.getNamespace();
                String itemId = registryName.getPath();
                String displayName = getItemDisplayName(item);
                
                output.append(String.format("%s:%s, %s\n", namespace, itemId, displayName));
            }
        }
        return output.toString();
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
   
    public static class ItemData {
        public final ItemEntity entity;
        public final BlockPos blockPos;
        public final ItemStack itemStack;
        public final double x;
        public final double y;
        public final double z;
        public final String itemName;
        public final int count;
        public final UUID uuid;
        public final String itemId;
        public final ItemEnchantments enchants;
        
        public ItemData(ItemEntity entity) {
            this.entity = entity;
            this.blockPos = entity.blockPosition();
            this.itemStack = entity.getItem();

            this.x = Math.round(entity.getX() * 2) / 2.0;
            this.y = Math.round(entity.getY() * 2) / 2.0;
            this.z = Math.round(entity.getZ() * 2) / 2.0;
            this.uuid = entity.getUUID();
            ResourceLocation registryName = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            this.itemId = registryName.getNamespace() + ":" + registryName.getPath();
            this.itemName = itemStack.getHoverName().getString();
            this.count = itemStack.getCount();
            this.enchants = itemStack.getEnchantments();
        }
                      
        @Override
        public String toString() {
            return String.format("{\"x\": %.1f, \"y\": %.1f, \"z\": %.1f, \"name\": \"%s\", \"id\": \"%s\", \"count\": %d, \"uuid\": \"%s\", \"enchants\": \"%s\"}",
                x, y, z, itemName, itemId, count, uuid.toString(), enchants.keySet().toString());
        }
        public String getCombinationString() {
            return String.format("{\"name\": \"%s\", \"id\": \"%s\", \"count\": %d,\"enchants\": \"%s\"}",
                itemName, itemId, count, enchants.keySet().toString());
        }
    }


    public static Holder<Item> getItemById(String id) {
        return BuiltInRegistries.ITEM.getHolder(ResourceLocation.parse(id)).orElse(null);
    }
    
}