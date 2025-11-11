package com.perfectlyorganized.infcraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Ai {
    private static Gson builder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static final int MAX_RETRIES = 4;
    private static final Duration RETRY_DELAY = Duration.ofSeconds(2);
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/" + Config.aiModel + ":generateContent?key=";
    public static CompletableFuture<Optional<AIResponse>> callAPI(String saveString) {
        if (Config.geminiAPIToken == null || Config.geminiAPIToken.trim().isEmpty()) {
            InfCraft.LOGGER.warn("Gemini API key is not configured! Check your config file.");
            return CompletableFuture.completedFuture(Optional.empty());
        }
        
        return callAPIWithRetry(saveString, new AtomicInteger(0));
    }
    private static CompletableFuture<Optional<AIResponse>> callAPIWithRetry(String saveString, AtomicInteger retryCount) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
                
        String requestBody = createRequestBody(saveString);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL + Config.geminiAPIToken))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
      
        if (Config.geminiAPIToken == null || Config.geminiAPIToken.trim().isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured! Check your config file.");
        }
      
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose(response -> {
                if (response.statusCode() == 200) {
                    try {
                        JsonObject body = JsonParser.parseString(response.body()).getAsJsonObject();;
                        String innerJsonText = body
                                .getAsJsonArray("candidates")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("content")
                                .getAsJsonArray("parts")
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();
                        AIResponse aiResponse = builder.fromJson(innerJsonText, AIResponse.class);
                        return CompletableFuture.completedFuture(Optional.of(aiResponse));
                    } catch (Exception e) {
                        System.err.println("Error parsing API response: " + e.getMessage());
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                } else if (shouldRetry(response.statusCode()) && retryCount.get() < MAX_RETRIES) {
                      System.out.println("API call failed with status: " + response.statusCode() + 
                                         ". Retrying... (" + (retryCount.get() + 1) + "/" + MAX_RETRIES + ")");
                        
                        return CompletableFuture.supplyAsync(() -> {
                            try {
                                Thread.sleep(RETRY_DELAY.toMillis());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return Optional.empty();
                            }
                            retryCount.incrementAndGet();
                            return callAPIWithRetry(saveString, retryCount).join();
                    });
                } else {
                        // Final failure case
                        System.err.println("API call failed with status: " + response.statusCode() + 
                                         " after " + retryCount.get() + " retries. Response: " + response.body());
                        return CompletableFuture.completedFuture(Optional.empty());
                    }
                });

      
    }
       private static String createRequestBody(String saveString) {
        return """
            {    
            "system_instruction": {
                "parts": [
                    {"text": "%s"}
                ]
            },
            "contents": [{
                "parts": [{
                    "text": "%s"
                }]
            }],
            "generationConfig": {
                "temperature": %s,
                "candidateCount": 1,
                "thinkingConfig": {
                    "thinkingBudget": 0
                },
                "responseMimeType": "application/json",
                "responseJsonSchema": {
                    "type": "object",
                    "properties": {
                        "id": {
                            "type": "string",
                            "description": "Namespace:ItemID of the item."
                        },
                        "count": {
                            "type": "integer",
                            "description": "Count of the item from 1 to 64."
                        },
                        "reason": {
                            "type": "string",
                            "description": "The explanation why this is the right item."
                        }
                    },
                    "required": ["id", "count"]
                }
            }
        }
        """.formatted(escapeJson(Config.prompt + "\n" + Util.getAllItems(Optional.of(Config.outputBlacklist))), escapeJson(saveString), Config.aiTemperature);
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    private static boolean shouldRetry(int statusCode) {
        // Retry on timeout (408), rate limit (429), and server errors (5xx)
        return statusCode == 408 || 
               statusCode == 429 || 
               (statusCode >= 500 && statusCode < 600);
    }
}
