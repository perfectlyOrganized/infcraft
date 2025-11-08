package com.perfectlyorganized.infcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perfectlyorganized.infcraft.Util.ItemData;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;


@Mod.EventBusSubscriber
public class ItemWatcher {
    private static Gson builder = new GsonBuilder().disableHtmlEscaping().serializeNulls().create();
    private static int tickCount = 0;
    private static boolean processing = false;
  
    @SuppressWarnings("null")
    private static Optional<Tuple<ItemData, ItemData>> getClosestItems(List<ItemData> items) {
        List<ItemData> shiftedList = new ArrayList<>(items);
        Collections.rotate(shiftedList, 1);
        for (int i = 0; i < items.size(); i++) {
            ItemData item = items.get(i);
            ItemData nextItem = shiftedList.get(i);
            if (item.itemId.equals(nextItem.itemId)) {
                continue;
            }
            double dx = item.x - nextItem.x;
            if (Math.abs(dx) >= 1.0) continue;
            double dy = item.y - nextItem.y;
            if (Math.abs(dy) >= 1.0) continue;
            double dz = item.z - nextItem.z;
            if (Math.abs(dz) >= 1.0) continue;

            double distance_squared = (dx * dx) + (dy * dy) + (dz * dz);
            if (distance_squared < 1) {
                List<ItemData> match = new ArrayList<>();
                match.add(item);
                match.add(nextItem);
                match.sort((a, b) -> a.itemId.compareTo(b.itemId));

                return Optional.ofNullable(new Tuple<>(match.get(0), match.get(1)));
            }
        }
        return Optional.empty();
    }
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            
            tickCount++;
            if (tickCount % 5 == 0) { 
                if (Config.geminiAPIToken == "") {
                    return;
                }
                if (processing) {
                    return;
                }
                processing = true;
               
                List<ItemData> items = Util.getAllLoadedItems();
                if (items.isEmpty()) {
                    processing = false;
                    return;
                }
                Optional<Tuple<ItemData, ItemData>> combinationOrOptional = getClosestItems(items);
                if (!combinationOrOptional.isPresent()) {
                    processing = false;
                    return;
                }

                Tuple<ItemData, ItemData> combination = combinationOrOptional.get();
                
                String saveString = combination.getA().getCombinationString() + ", " + combination.getB().getCombinationString();
                boolean saved = false;
                Optional<AIResponse> itemResult = Optional.empty();
                try {
                    Path path = Config.infcraftDir.resolve("combinations.txt");
                    List<String> combinations = Files.readAllLines(path);
                    if (combinations.contains(saveString)) {
                        int index = combinations.indexOf(saveString);
                        String result = combinations.get(index + 1);
                        itemResult = Optional.ofNullable(builder.fromJson(result, AIResponse.class));
                        saved = true;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!itemResult.isPresent()) {
                    if (Math.random() < 0.01) {
                        itemResult = Optional.ofNullable(Util.getRandomItemResult());
                    } else {
                        try {
                            itemResult = Ai.callAPI(saveString).get();
                        } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                        }
                    }
                }

                if (!itemResult.isPresent()) {
                    processing = false;
                    return;
                }
                AIResponse result = itemResult.get();
                try {
                    Path path = Config.infcraftDir.resolve("combinations.txt");
                    List<String> combinations = Files.readAllLines(path);
                    if (!combinations.contains(saveString)) {
                        combinations.add(saveString);
                        combinations.add(builder.toJson(result));
                        Files.write(path, combinations);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ServerLevel level = (ServerLevel) combination.getA().entity.level();
                ResourceLocation location = ResourceLocation.parse(result.id);
                Item item = ForgeRegistries.ITEMS.getValue(location);
                double x = combination.getA().x;
                double y = combination.getA().y;
                double z = combination.getA().z;
                if (item != null) {
                    ItemStack stack = new ItemStack(item, result.count);
                    ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
                    combination.getA().entity.discard();
                    combination.getB().entity.discard();
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                    level.sendParticles(ParticleTypes.POOF, x, y, z, 4, 0.5, 0.5, 0.5, 0.02);
                    Random rand = new Random();
                    float pitch = ((rand.nextInt(10))/10) + 1;
                    itemEntity.playSound(SoundEvents.ENDER_EYE_DEATH, 0.8f, pitch);

                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null && !saved && result.reason != null) {
                        server.getPlayerList().broadcastSystemMessage(Component.literal(result.reason), false);
                    }
                }
                processing = false;
            }
        }
    }

  
}

class AIResponse {
    AIResponse(int count, String id) {
        this.count = count;
        this.id = id;
    }
    AIResponse(int count, String id, String reason) {
        this.count = count;
        this.id = id;
        this.reason = reason;
    }
    int count;
    String id;
    String reason;
}

class InputItem {
    int x;
    int y;
    int z;
    String name;
    String id;
    int count;
    UUID uuid;
}
class ItemCombination {
    AIResponse result;
    InputItem firstItem;
    InputItem lastItem;
}