package com.perfectlyorganized.infcraft;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.genai.Client;
import com.google.genai.errors.ClientException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Ai {
    private static Gson builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static GenerateContentConfig config =
        GenerateContentConfig.builder()
            .thinkingConfig(ThinkingConfig.builder().thinkingBudget(0))
            .candidateCount(1)
            .systemInstruction(Content.fromParts(Part.fromText(Config.prompt + "\n" + Util.getAllItems())))
            .responseMimeType("application/json")
            .responseSchema(Schema.builder().type(Type.Known.OBJECT)
                .properties(
                    Map.of(
                    "id", Schema.builder().type(Type.Known.STRING).description("Namespace:ItemID of the item.").build(),
                    "count", Schema.builder().type(Type.Known.INTEGER).description("Count of the item from 1 to 64.").build(), 
                    "reason", Schema.builder().type(Type.Known.STRING).description("The explanation why this is the right item.").build()
                )).required(List.of("id", "count"))
            )
            .build();
    static Client createClient() {
        if (Config.geminiAPIToken == null || Config.geminiAPIToken.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured! Check your config file.");
        }
        HttpOptions httpOptions = HttpOptions.builder()
        .retryOptions(
            HttpRetryOptions.builder()
                .attempts(3)
                .httpStatusCodes(408, 429)
            ).build();
        return Client.builder().apiKey(Config.geminiAPIToken).httpOptions(httpOptions).build();
    }

    public static CompletableFuture<Optional<AIResponse>> callAPI(Client client, String saveString) {
        return CompletableFuture.supplyAsync(() -> {
            GenerateContentResponse response;
            try {
                response = client.models.generateContent(
                    "gemini-2.0-flash", 
                    saveString, 
                    config
                    );
            } catch (ClientException e) {
                e.printStackTrace();
                return Optional.empty();
            }
            String jsonString = response.text();
            try {
                return Optional.of(builder.fromJson(jsonString, AIResponse.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }
}
