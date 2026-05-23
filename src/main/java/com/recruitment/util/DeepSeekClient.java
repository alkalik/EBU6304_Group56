package com.recruitment.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * Lightweight client for the DeepSeek Chat API.
 * Uses Java 11's built-in HttpClient – no extra dependencies required.
 *
 * <p>Supports both blocking (non-streaming) and SSE streaming modes.
 * Falls back gracefully: if the API call fails the caller receives {@code null}
 * and should fall back to the rule-based result.
 */
public class DeepSeekClient {

    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public DeepSeekClient() {
        this.apiKey = AppConfig.getDeepSeekApiKey();
        this.apiUrl  = AppConfig.getDeepSeekUrl();
        this.model   = AppConfig.getDeepSeekModel();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Non-streaming (blocking) call
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Send a blocking chat request and return the full assistant reply.
     *
     * @return assistant reply text, or {@code null} on any error
     */
    public String chat(String systemPrompt, String userMessage, int maxTokens) {
        if (apiKey == null || apiKey.isEmpty()) return null;
        try {
            String body = buildRequestBody(systemPrompt, userMessage, maxTokens, false);
            HttpRequest req = buildRequest(body);
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                System.err.println("[DeepSeek] HTTP " + resp.statusCode() + ": " + resp.body());
                return null;
            }
            JsonObject json = GSON.fromJson(resp.body(), JsonObject.class);
            return json.getAsJsonArray("choices")
                       .get(0).getAsJsonObject()
                       .getAsJsonObject("message")
                       .get("content").getAsString();
        } catch (Exception e) {
            System.err.println("[DeepSeek] Request failed: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Streaming call (SSE)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stream a chat response via SSE. Each token chunk is delivered to
     * {@code onToken} on a background thread as it arrives.
     *
     * <p>Must NOT be called on the Swing EDT; run it in a separate thread.
     *
     * @param onToken   called with each incremental text fragment (never null)
     * @param onDone    called once when the stream ends successfully
     * @param onError   called with an error message if the request fails
     */
    public void chatStreaming(String systemPrompt, String userMessage, int maxTokens,
                              Consumer<String> onToken, Runnable onDone,
                              Consumer<String> onError) {
        if (apiKey == null || apiKey.isEmpty()) {
            if (onError != null) onError.accept("API Key not configured.");
            return;
        }
        try {
            String body = buildRequestBody(systemPrompt, userMessage, maxTokens, true);
            HttpRequest req = buildRequest(body);

            // Use ofLines() to receive the SSE stream line-by-line
            HttpResponse<java.util.stream.Stream<String>> resp =
                    HTTP.send(req, HttpResponse.BodyHandlers.ofLines());

            if (resp.statusCode() != 200) {
                if (onError != null) onError.accept("HTTP " + resp.statusCode());
                return;
            }

            resp.body().forEach(line -> {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) return;
                    try {
                        JsonObject chunk = GSON.fromJson(data, JsonObject.class);
                        JsonArray choices = chunk.getAsJsonArray("choices");
                        if (choices == null || choices.size() == 0) return;
                        JsonObject delta = choices.get(0).getAsJsonObject()
                                                  .getAsJsonObject("delta");
                        if (delta != null && delta.has("content")
                                && !delta.get("content").isJsonNull()) {
                            String token = delta.get("content").getAsString();
                            if (!token.isEmpty()) onToken.accept(token);
                        }
                    } catch (Exception ignored) {
                        // Malformed chunk – skip silently
                    }
                }
            });

            if (onDone != null) onDone.run();

        } catch (Exception e) {
            System.err.println("[DeepSeek] Stream failed: " + e.getMessage());
            if (onError != null) onError.accept(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Domain-specific convenience methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stream a skill-gap analysis. Tokens are delivered to {@code onToken} as
     * they arrive; {@code onDone} is called when the stream completes.
     */
    public void streamSkillGapAnalysis(String jobTitle, String requiredSkills,
                                        String candidateName, String candidateSkills,
                                        double matchPercent, String missingSkills,
                                        Consumer<String> onToken, Runnable onDone,
                                        Consumer<String> onError) {
        String system = "You are an expert recruitment analyst. "
                + "Provide concise, actionable skill-gap analysis in 3-5 bullet points. "
                + "Use plain language. Do NOT repeat the numbers already shown in the table. "
                + "Focus on WHY each missing skill matters and HOW the candidate could bridge the gap.";

        String user = String.format(
                "Job: %s\nRequired skills: %s\n"
                + "Candidate: %s\nCandidate's skills: %s\n"
                + "Match rate: %.0f%%\nMissing skills: %s\n\n"
                + "Please give a brief skill-gap analysis and recommendation.",
                jobTitle, requiredSkills, candidateName, candidateSkills,
                matchPercent, missingSkills);

        chatStreaming(system, user, 400, onToken, onDone, onError);
    }

    /**
     * Stream a workload balance analysis. Tokens are delivered to {@code onToken}.
     */
    public void streamWorkloadBalance(String workloadSummary,
                                       Consumer<String> onToken, Runnable onDone,
                                       Consumer<String> onError) {
        String system = "You are an academic department administrator assistant. "
                + "Analyse TA workload data and provide 3-5 specific, practical rebalancing suggestions. "
                + "Be concise. Use bullet points. Mention fairness and student support quality.";

        String user = "Here is the current TA workload data:\n\n"
                + workloadSummary
                + "\n\nPlease provide targeted workload balancing recommendations.";

        chatStreaming(system, user, 500, onToken, onDone, onError);
    }

    // Non-streaming convenience (kept for export/text use)
    public String analyzeSkillGap(String jobTitle, String requiredSkills,
                                   String candidateName, String candidateSkills,
                                   double matchPercent, String missingSkills) {
        String system = "You are an expert recruitment analyst. "
                + "Provide concise, actionable skill-gap analysis in 3-5 bullet points.";
        String user = String.format(
                "Job: %s\nRequired skills: %s\nCandidate: %s\nCandidate's skills: %s\n"
                + "Match rate: %.0f%%\nMissing skills: %s\n\nBrief skill-gap analysis:",
                jobTitle, requiredSkills, candidateName, candidateSkills, matchPercent, missingSkills);
        return chat(system, user, 400);
    }

    public String analyzeWorkloadBalance(String workloadSummary) {
        String system = "You are an academic department administrator assistant. "
                + "Provide 3-5 specific workload rebalancing suggestions in bullet points.";
        String user = "TA workload data:\n\n" + workloadSummary
                + "\n\nTargeted workload balancing recommendations:";
        return chat(system, user, 500);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String buildRequestBody(String systemPrompt, String userMessage,
                                     int maxTokens, boolean stream) {
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", 0.3);
        body.addProperty("stream", stream);

        JsonArray messages = new JsonArray();
        JsonObject sys = new JsonObject();
        sys.addProperty("role", "system");
        sys.addProperty("content", systemPrompt);
        messages.add(sys);
        JsonObject usr = new JsonObject();
        usr.addProperty("role", "user");
        usr.addProperty("content", userMessage);
        messages.add(usr);
        body.add("messages", messages);

        return GSON.toJson(body);
    }

    private HttpRequest buildRequest(String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }
}
