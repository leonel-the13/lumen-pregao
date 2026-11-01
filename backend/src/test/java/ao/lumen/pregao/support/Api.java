package ao.lumen.pregao.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Cliente HTTP de teste com cookies geridos à mão (o cookie de sessão é Secure mesmo em http local). */
public class Api {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private final String base;
    private final Map<String, String> cookies = new LinkedHashMap<>();
    public List<String> lastSetCookies = List.of();

    public Api(int port) {
        this.base = "http://localhost:" + port + "/api/v1";
    }

    public record Res(int status, JsonNode body) {
        public String code() { return body == null ? null : body.path("code").asText(null); }
    }

    public Res get(String path) { return send("GET", path, null, Map.of(), true); }
    public Res post(String path, Object body) { return send("POST", path, body, Map.of(), true); }
    public Res post(String path, Object body, Map<String, String> headers) { return send("POST", path, body, headers, true); }
    public Res put(String path, Object body) { return send("PUT", path, body, Map.of(), true); }
    public Res patch(String path, Object body) { return send("PATCH", path, body, Map.of(), true); }
    public Res postWithoutCsrf(String path, Object body) { return send("POST", path, body, Map.of(), false); }

    public Res login(String email, String password) {
        get("/auth/csrf");
        return post("/auth/login", Map.of("email", email, "password", password));
    }

    public String cookie(String name) { return cookies.get(name); }

    public synchronized Res send(String method, String path, Object body, Map<String, String> headers, boolean csrf) {
        try {
            if (csrf && !"GET".equals(method) && !cookies.containsKey("XSRF-TOKEN")) get("/auth/csrf");
            var b = HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type", "application/json");
            if (!cookies.isEmpty()) {
                b.header("Cookie", String.join("; ", cookies.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList()));
            }
            if (csrf && !"GET".equals(method)) b.header("X-CSRF-Token", cookies.get("XSRF-TOKEN"));
            headers.forEach(b::header);
            b.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body instanceof String s ? s : JSON.writeValueAsString(body)));
            HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            lastSetCookies = r.headers().allValues("Set-Cookie");
            for (String sc : lastSetCookies) {
                String pair = sc.split(";", 2)[0];
                int i = pair.indexOf('=');
                String name = pair.substring(0, i), value = pair.substring(i + 1);
                if (value.isEmpty() || sc.contains("Max-Age=0")) cookies.remove(name); else cookies.put(name, value);
            }
            JsonNode node = r.body() == null || r.body().isBlank() ? null : JSON.readTree(r.body());
            return new Res(r.statusCode(), node);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
