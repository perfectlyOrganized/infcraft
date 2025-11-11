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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    private static Optional<Tuple<ItemEntity, ItemEntity>> getClosestItems(List<ItemEntity> items) {
        List<ItemEntity> shiftedList = new ArrayList<>(items);
        Collections.rotate(shiftedList, 1);
        for (int i = 0; i < items.size(); i++) {
            ItemEntity item = items.get(i);
            ItemEntity nextItem = shiftedList.get(i);
            if (Util.getItemId(item).equals(Util.getItemId(nextItem))) {
                continue;
            }
            double dx = item.getX() - nextItem.getX();
            if (Math.abs(dx) >= 1.0) continue;
            double dy = item.getY() - nextItem.getY();
            if (Math.abs(dy) >= 1.0) continue;
            double dz = item.getZ() - nextItem.getZ();
            if (Math.abs(dz) >= 1.0) continue;

            double distance_squared = (dx * dx) + (dy * dy) + (dz * dz);
            if (distance_squared < 1) {
                List<ItemEntity> match = new ArrayList<>();
                match.add(item);
                match.add(nextItem);
                match.sort((a, b) -> Util.getItemId(a).compareTo(Util.getItemId(b)));

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
               
                List<ItemEntity> items = Util.getAllLoadedItems(Optional.ofNullable(Config.inputBlacklist));
                if (items.isEmpty()) {
                    processing = false;
                    return;
                }
                Optional<Tuple<ItemEntity, ItemEntity>> combinationOrOptional = getClosestItems(items);
                if (!combinationOrOptional.isPresent()) {
                    processing = false;
                    return;
                }

                Tuple<ItemEntity, ItemEntity> combination = combinationOrOptional.get();
                
                String saveString = Util.getCombinationString(combination.getA()) + ", " + Util.getCombinationString(combination.getB());
                Optional<AIResponse> itemResult = Optional.empty();
                try {
                    Path path = Config.infcraftDir.resolve("combinations.txt");
                    List<String> combinations = Files.readAllLines(path);
                    if (combinations.contains(saveString)) {
                        int index = combinations.indexOf(saveString);
                        String result = combinations.get(index + 1);
                        itemResult = Optional.ofNullable(builder.fromJson(result, AIResponse.class));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (!itemResult.isPresent()) {
                    if (Math.random() < Config.chanceOfRandom) {
                        itemResult = Optional.ofNullable(Util.getRandomItemResult());
                    } else {
                        Ai.callAPI(saveString).thenAccept(result -> {
                            if (result.isPresent()) {
                                replaceItems(result.get(), combination, saveString, false);
                            }
                            processing = false;
                        });
                        return;
                    }
                }

                if (!itemResult.isPresent()) {
                    processing = false;
                    return;
                }
                AIResponse result = itemResult.get();

                replaceItems(result, combination, saveString, true);
                processing = false;
            }
        }
    }

    public static void replaceItems(AIResponse result, Tuple<ItemEntity, ItemEntity> combination, String saveString, Boolean saved) {
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

        ServerLevel level = (ServerLevel) combination.getA().level();
        ResourceLocation location = ResourceLocation.parse(result.id);
        Item item = ForgeRegistries.ITEMS.getValue(location);
        double x = combination.getA().getX();
        double y = combination.getA().getY();
        double z = combination.getA().getZ();
        if (item != null) {
            ItemStack stack = new ItemStack(item, result.count);
            ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
            combination.getA().discard();
            combination.getB().discard();
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