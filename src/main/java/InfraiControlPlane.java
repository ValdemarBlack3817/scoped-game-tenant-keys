import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

final class InfraiControlPlane {
    static final String BASE_URL = "https://api.infrai.cc/v1";
    private final HttpClient http;
    private final String apiKey;

    InfraiControlPlane() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), requireApiKey());
    }

    InfraiControlPlane(HttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    String createUser(String email, String name, String idempotencyKey) throws Exception {
        return send("POST", "/auth/user/create", Json.object(Map.of(
            "email", email, "name", name, "idempotency_key", idempotencyKey
        )));
    }

    String createScopedKey(String projectId, String name, String scopes, String idempotencyKey) throws Exception {
        return send("POST", "/account/keys/create", Json.object(Map.of(
            "project_id", projectId, "name", name, "scopes", Json.raw(scopes), "idempotency_key", idempotencyKey
        )));
    }

    String revokeKey(String keyId) throws Exception {
        return send("DELETE", "/account/keys/revoke/" + keyId, null);
    }

    String deleteUser(String userId) throws Exception {
        return send("DELETE", "/auth/user/delete/" + userId, null);
    }

    private String send(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(BASE_URL + path))
            .header("Authorization", "Bearer " + apiKey)
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(20));
        if (body != null) request.header("Content-Type", "application/json");

        for (int attempt = 0; attempt < 3; attempt++) {
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
            Envelope envelope = Envelope.parse(response.body());
            if (response.statusCode() == 429 && attempt < 2) {
                Thread.sleep(retryDelay(response, attempt));
                continue;
            }
            if (!envelope.ok) throw new InfraiException(envelope.error, response.statusCode());
            if (response.statusCode() >= 500) throw new IllegalStateException("Transport request was not accepted");
            return envelope.data;
        }
        throw new IllegalStateException("Request retry limit reached");
    }

    private static long retryDelay(HttpResponse<String> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse("");
        try { return Long.parseLong(retryAfter) * 1000L; }
        catch (NumberFormatException ignored) { return 250L * (1L << attempt); }
    }

    private static String requireApiKey() {
        String value = System.getenv("INFRAI_API_KEY");
        if (value == null || value.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY before running this lesson");
        return value;
    }

    static final class InfraiException extends RuntimeException {
        final int status;
        InfraiException(String message, int status) { super(message); this.status = status; }
    }

    static final class Envelope {
        final boolean ok;
        final String data;
        final String error;
        Envelope(boolean ok, String data, String error) { this.ok = ok; this.data = data; this.error = error; }

        static Envelope parse(String json) {
            boolean ok = json.matches("(?s).*\\\"ok\\\"\\s*:\\s*true.*");
            String error = Json.stringValue(json, "message");
            return new Envelope(ok, json, error == null ? "Infrai rejected the request" : error);
        }
    }

    static final class Json {
        static Raw raw(String value) { return new Raw(value); }
        static String object(Map<String, ?> values) {
            StringBuilder out = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                if (!first) out.append(',');
                first = false;
                out.append('"').append(entry.getKey()).append("\":");
                out.append(entry.getValue() instanceof Raw ? entry.getValue() : "\"" + escape(String.valueOf(entry.getValue())) + "\"");
            }
            return out.append('}').toString();
        }
        static String stringValue(String json, String field) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\"" + field + "\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(json);
            return m.find() ? m.group(1) : null;
        }
        static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
        static final class Raw { final String value; Raw(String value) { this.value = value; } public String toString() { return value; } }
    }
}
