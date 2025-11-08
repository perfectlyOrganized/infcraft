package com.perfectlyorganized.infcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(modid = InfCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<String> GEMINI_API_TOKEN;
    public static final ForgeConfigSpec.ConfigValue<String> AI_PROMPT;
    static {
        GEMINI_API_TOKEN = BUILDER
            .comment("Gemini API Token: https://aistudio.google.com/api-keys" )
            .define("gemini_api_token", "");
            
            AI_PROMPT = BUILDER
                .comment("Prompt for AI" )
                .define("ai_prompt",  """
                You are a game mechanic for a Minecraft-like crafting system. Your sole function is to combine two items into a new, creative result based on specific rules.

                Input Format:
                You will receive a JSON array of two items. Each item is an object with the following structure:
                [
                  {
                    "count": <number>,
                    "id": "<namespace>:<item_id>",
                    "name": "<string>"
                  },
                  {
                    "count": <number>,
                    "id": "<namespace>:<item_id>",
                    "name": "<string>"
                  }
                ]

                Output Format:
                You MUST respond only with a valid JSON object. No other text, explanations, or formatting. The JSON must have this exact structure:
                {
                    "count": <number>,
                    "id": "<namespace>:<item_id>"
                    "reason": "<a brief, one-sentence explanation for the combination>"
                }

                Crafting Rules:
                1. The new item must be a creative combination of the two inputs. Inform your decision using:
                   Minecraft crafting logic (e.g., materials, tool parts).
                   Real-world logic (e.g., colors, concepts).
                   The item's in-game appearance and color.
                   The ratio of the input count values.
                2. The output id MUST be different from both input ids, unless the combination of two identical items logically results in more of the same (e.g., 2 minecraft:iron_ingot -> 1 minecraft:iron_block).
                3. The output id must be a valid, existing Minecraft item ID. Do not invent new IDs.
                4. The output count should be logically derived from the input counts (e.g., it could be the sum, average, or a new value based on the recipe).

                Example: 
                Input:
                [
                  { "count": 1, "id": "minecraft:stone_axe" },
                  { "count": 1, "id": "minecraft:iron_ingot" }
                ]

                Output:
                {
                  "result": {
                    "count": 1,
                    "id": "minecraft:iron_axe"
                  },
                  "reasoning": "Combining a stone axe head with iron reinforces it, creating a more durable iron axe."
                }

                Now, process the following input according to these rules. Respond with ONLY the JSON object.

                The list of IDs and name is:
                """);
        SPEC = BUILDER.build();
    }
    static final ForgeConfigSpec SPEC;

    public static String geminiAPIToken = "";
    public static String prompt = "";

    public static final Path configDir = FMLPaths.CONFIGDIR.get();
    public static final Path infcraftDir = configDir.resolve(InfCraft.MODID);
    private static void setupConfigFiles() {
        try {
            Files.createDirectories(infcraftDir);
            Path combinationsPath = infcraftDir.resolve("combinations.txt");
            Files.createFile(combinationsPath);
        } catch (IOException e) {
            InfCraft.LOGGER.error("Failed to create config directory: {}", infcraftDir, e);
        }
        
    }
    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event)
    {
        geminiAPIToken = GEMINI_API_TOKEN.get();
        prompt = AI_PROMPT.get();
        
        setupConfigFiles();
    }
    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event)
    {
        geminiAPIToken = GEMINI_API_TOKEN.get();
        prompt = AI_PROMPT.get();
    }
}
