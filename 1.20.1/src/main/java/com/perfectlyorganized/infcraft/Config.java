package com.perfectlyorganized.infcraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> OUTPUT_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> INPUT_BLACKLIST;
    public static final ForgeConfigSpec.ConfigValue<Float> AI_TEMPERATURE;
    public static final ForgeConfigSpec.ConfigValue<Float> CHANCE_OF_RANDOM; 
    public static final ForgeConfigSpec.ConfigValue<String> AI_MODEL;
    static {
        GEMINI_API_TOKEN = BUILDER
            .comment("Gemini API Token: https://aistudio.google.com/api-keys" )
            .define("gemini_api_token", "");
            
            AI_PROMPT = BUILDER
                .comment("Prompt for AI" )
                .define("ai_prompt",  """
                You are a creative crafting AI for a modded Minecraft environment. Your sole function is to combine two items into a new, creative result with emphasis on modded content when appropriate.

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
                  "id": "<namespace>:<item_id>",
                  "reason": "<a brief, one-sentence explanation for the combination>"
                }

                CRAFTING PREFERENCES:
                **Primary Focus**: Creative and logical combinations that make sense
                **Modded Emphasis**: When multiple valid options exist, prefer modded items over vanilla
                **Namespace Awareness**: Pay attention to item namespaces and create combinations that respect mod themes

                Decision Process:
                1. First, consider what makes the most logical and creative sense for the combination
                2. If multiple equally valid results exist, choose modded items over vanilla
                3. Respect mod boundaries and themes (tech mods with tech, magic with magic when appropriate)
                4. Create interesting cross-mod interactions when it makes conceptual sense
                5. Vanilla results are acceptable when they are the most logical choice

                SPECIFIC GUIDELINES:
                - The output should always make conceptual sense based on the input items
                - Consider item properties, colors, materials, and in-game functions
                - Output count should be logically derived from input counts
                - The result ID must be different from both input IDs (except for stacking identical items)
                - All item IDs must be valid and exist in the game

                EXAMPLES:
                - "create:cogwheel" + "minecraft:redstone" → "create:redstone_contact" (modded preferred)
                - "minecraft:iron_ingot" + "minecraft:stick" → "minecraft:iron_sword" (vanilla acceptable when logical)
                - "botania:mana_pearl" + "minecraft:ender_pearl" → "botania:ender_hand" (modded + vanilla → modded)
                - "thermal:signalum_ingot" + "minecraft:bow" → "thermal:signalum_quiver" (creative modded combination)

                Now, process the following input according to these rules. Respond with ONLY the JSON object.

                Available items include both modded and vanilla options. Choose the most creative and logical result.

                The list of IDs and name is:
                """);
        INPUT_BLACKLIST = BUILDER
            .comment("Input words blacklist" ) 
            .define("input_blacklist", List.of("backpack", "bag", "shulker box", "bundle"));
        OUTPUT_BLACKLIST = BUILDER
            .comment("Output words blacklist" )
            .define("output_blacklist", List.of("stairs", "slab", "dye", "enchanted book", "uncraftable", "waxed", "block of", "air"));
        AI_TEMPERATURE = BUILDER
            .comment("AI Temperature" )
            .define("ai_temperature", 0.7f);
        CHANCE_OF_RANDOM = BUILDER
            .comment("Chance of random item" )
            .define("chance_of_random", 0.05f);
        AI_MODEL = BUILDER
            .comment("AI Model" )
            .define("ai_model", "gemini-2.0-flash");
        SPEC = BUILDER.build();
    }
    static final ForgeConfigSpec SPEC;

    public static String geminiAPIToken = "";
    public static String prompt = "";
    public static List<? extends String> inputBlacklist = List.of();
    public static List<? extends String> outputBlacklist = List.of();
    public static float aiTemperature = 0.7f;
    public static float chanceOfRandom = 0.05f;
    public static String aiModel = "gemini-2.0-flash";

    public static final Path configDir = FMLPaths.CONFIGDIR.get();
    public static final Path infcraftDir = configDir.resolve(InfCraft.MODID);
    private static void setupConfigFiles() {
        try {
            if (!Files.exists(infcraftDir)) {
              Files.createDirectories(infcraftDir);
            }
            Path combinationsPath = infcraftDir.resolve("combinations.txt");
            if (!Files.exists(combinationsPath)) {
              Files.createFile(combinationsPath);
            }
        } catch (IOException e) {
            InfCraft.LOGGER.error("Failed to create config directory: {}", infcraftDir, e);
        }
        
    }
    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event)
    {
        geminiAPIToken = GEMINI_API_TOKEN.get();
        prompt = AI_PROMPT.get();
        inputBlacklist = INPUT_BLACKLIST.get();
        outputBlacklist = OUTPUT_BLACKLIST.get();
        aiTemperature = AI_TEMPERATURE.get();
        chanceOfRandom = CHANCE_OF_RANDOM.get();
        aiModel = AI_MODEL.get();
        
        setupConfigFiles();
    }
    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event)
    {
        geminiAPIToken = GEMINI_API_TOKEN.get();
        prompt = AI_PROMPT.get();
        inputBlacklist = INPUT_BLACKLIST.get();
        outputBlacklist = OUTPUT_BLACKLIST.get();
        aiTemperature = AI_TEMPERATURE.get();
        chanceOfRandom = CHANCE_OF_RANDOM.get();
        aiModel = AI_MODEL.get();
    }
}
