package com.react.spring.meta.metasource.connect.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.react.spring.meta.metasource.connect.rest.dto.ApiAuthConfigDTO;
import com.react.spring.meta.metasource.connect.rest.dto.ApiBodyConfigDTO;
import com.react.spring.meta.metasource.connect.rest.dto.ApiConfigDTO;
import com.react.spring.meta.metasource.connect.rest.dto.ApiFormDataFieldDTO;
import com.react.spring.meta.metasource.connect.rest.dto.ApiQueryParamDTO;
import com.react.spring.meta.metasource.connect.rest.dto.ApiUrlEncodedFieldDTO;
import com.react.spring.meta.metasource.connect.rest.dto.RestProxyHeaderDto;
import com.react.spring.meta.metasource.connect.rest.dto.RestProxyRequest;
import com.react.spring.meta.metasource.connect.rest.dto.RestProxyResultDto;
import com.react.spring.meta.metasource.entity.MetaSource;
import com.react.spring.common.enums.ApiAuthType;
import com.react.spring.common.enums.ApiBodyType;
import com.react.spring.common.enums.ApiKeyPlacement;
import com.react.spring.common.enums.ConnectorType;
import com.react.spring.common.exception.NotFoundException;
import com.react.spring.meta.metasource.repository.MetaSourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Service
public class MetaSourceRestService {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final MetaSourceRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    public MetaSourceRestService(MetaSourceRepository repo) {
        this.repo = repo;
    }

    public RestProxyResultDto proxy(UUID metaSourceId, RestProxyRequest req) {
        MetaSource ms = repo.findById(metaSourceId)
                .orElseThrow(() -> new NotFoundException("MetaSource not found: " + metaSourceId));

        if (ms.getConnectorType() != ConnectorType.REST) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "REST proxy chỉ hỗ trợ REST connector, got: " + ms.getConnectorType());
        }

        Map<String, Object> connectorCfg = parseConnectorConfig(ms.getConnectorConfig());
        String baseUrl = strOrThrow(connectorCfg, "baseUrl").replaceAll("/+$", "");
        String sourceAuthToken = str(connectorCfg, "authToken");

        ApiConfigDTO cfg = req.config();
        if (cfg == null) {
            throw new ResponseStatusException(BAD_REQUEST, "config không được để trống");
        }

        String method = cfg.getMethod() != null ? cfg.getMethod().toUpperCase() : "GET";
        String url = buildUrl(baseUrl, cfg.getEndpointPath(), cfg.getQueryParams());

        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(BAD_REQUEST, "URL không hợp lệ: " + url);
        }

        HttpRequest.BodyPublisher bodyPublisher = buildBodyPublisher(cfg.getBody());

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(TIMEOUT)
                .method(method, bodyPublisher);

        // Auth: connector-level token is overridden by per-request auth config
        applyAuth(builder, cfg.getAuth(), sourceAuthToken);

        // Default headers
        builder.header("Accept", "application/json");
        applyContentType(builder, cfg.getBody());

        // Custom headers (canEdit=false headers are included as-is)
        if (cfg.getHeaders() != null) {
            cfg.getHeaders().stream()
                    .filter(h -> h != null && h.getKey() != null && !h.getKey().isBlank())
                    .forEach(h -> builder.header(h.getKey(), h.getValue() != null ? h.getValue() : ""));
        }

        long t0 = System.currentTimeMillis();
        try {
            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - t0;

            List<RestProxyHeaderDto> responseHeaders = response.headers().map().entrySet().stream()
                    .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                    .flatMap(entry -> entry.getValue().stream()
                            .map(value -> new RestProxyHeaderDto(entry.getKey(), value)))
                    .toList();

            return new RestProxyResultDto(response.statusCode(), responseHeaders, response.body(), duration);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY,
                    "Request timeout sau " + TIMEOUT.getSeconds() + "s");
        } catch (Exception e) {
            throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Request thất bại: " + e.getMessage());
        }
    }

    private String buildUrl(String baseUrl, String endpointPath, List<ApiQueryParamDTO> queryParams) {
        String path = endpointPath == null ? "" : endpointPath;
        if (!path.startsWith("/")) path = "/" + path;

        String base = baseUrl + path;

        if (queryParams == null || queryParams.isEmpty()) return base;

        String qs = queryParams.stream()
                .filter(p -> p != null && p.getKey() != null && !p.getKey().isBlank())
                .filter(p -> !Boolean.FALSE.equals(p.getCanEdit()) || Boolean.TRUE.equals(p.getRequired()))
                .map(p -> encodeParam(p.getKey(), p.getValue()))
                .collect(Collectors.joining("&"));

        return qs.isEmpty() ? base : base + "?" + qs;
    }

    private String encodeParam(String key, String value) {
        String k = key == null ? "" : key.trim();
        String v = value == null ? "" : value;
        try {
            return URLEncoder.encode(k, StandardCharsets.UTF_8)
                    + "=" + URLEncoder.encode(v, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return k + "=" + v;
        }
    }

    private void applyAuth(HttpRequest.Builder builder, ApiAuthConfigDTO auth, String sourceToken) {
        if (auth != null && auth.getAuthType() != null && auth.getAuthType() != ApiAuthType.NONE) {
            switch (auth.getAuthType()) {
                case BEARER -> {
                    if (auth.getBearerToken() != null && !auth.getBearerToken().isBlank()) {
                        builder.header("Authorization", "Bearer " + auth.getBearerToken());
                        return;
                    }
                }
                case BASIC -> {
                    if (auth.getUsername() != null) {
                        String creds = auth.getUsername() + ":" + (auth.getPassword() != null ? auth.getPassword() : "");
                        String encoded = java.util.Base64.getEncoder().encodeToString(
                                creds.getBytes(StandardCharsets.UTF_8));
                        builder.header("Authorization", "Basic " + encoded);
                        return;
                    }
                }
                case API_KEY -> {
                    if (auth.getApiKeyName() != null && !auth.getApiKeyName().isBlank()) {
                        String val = auth.getApiKeyValue() != null ? auth.getApiKeyValue() : "";
                        if (auth.getApiKeyPlacement() == ApiKeyPlacement.QUERY) {
                            // query param — handled at URL level; skip header injection
                        } else {
                            builder.header(auth.getApiKeyName(), val);
                        }
                        return;
                    }
                }
                default -> {}
            }
        }
        // Fall back to connector-level bearer token
        if (sourceToken != null && !sourceToken.isBlank()) {
            builder.header("Authorization", "Bearer " + sourceToken);
        }
    }

    private HttpRequest.BodyPublisher buildBodyPublisher(ApiBodyConfigDTO body) {
        if (body == null || body.getBodyType() == null || body.getBodyType() == ApiBodyType.NONE) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return switch (body.getBodyType()) {
            case JSON, RAW -> {
                String content = body.getRawContent() != null ? body.getRawContent() : "";
                yield HttpRequest.BodyPublishers.ofString(content);
            }
            case FORM_DATA, URL_ENCODED -> {
                String encoded = buildFormBody(body);
                yield HttpRequest.BodyPublishers.ofString(encoded);
            }
            default -> HttpRequest.BodyPublishers.noBody();
        };
    }

    private String buildFormBody(ApiBodyConfigDTO body) {
        if (body.getBodyType() == ApiBodyType.FORM_DATA && body.getFormDataFields() != null) {
            return body.getFormDataFields().stream()
                    .filter(f -> f != null && f.getKey() != null && !f.getKey().isBlank())
                    .map(f -> encodeParam(f.getKey(), f.getValue()))
                    .collect(Collectors.joining("&"));
        }
        if (body.getBodyType() == ApiBodyType.URL_ENCODED && body.getUrlEncodedFields() != null) {
            return body.getUrlEncodedFields().stream()
                    .filter(f -> f != null && f.getKey() != null && !f.getKey().isBlank())
                    .map(f -> encodeParam(f.getKey(), f.getValue()))
                    .collect(Collectors.joining("&"));
        }
        return "";
    }

    private void applyContentType(HttpRequest.Builder builder, ApiBodyConfigDTO body) {
        if (body == null || body.getBodyType() == null || body.getBodyType() == ApiBodyType.NONE) return;
        switch (body.getBodyType()) {
            case JSON -> builder.header("Content-Type", "application/json");
            case RAW -> builder.header("Content-Type", "text/plain");
            case FORM_DATA -> builder.header("Content-Type", "application/x-www-form-urlencoded");
            case URL_ENCODED -> builder.header("Content-Type", "application/x-www-form-urlencoded");
            default -> {}
        }
    }

    private Map<String, Object> parseConnectorConfig(String json) {
        if (json == null || json.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "connectorConfig trống");
        }
        try {
            return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "connectorConfig không phải JSON hợp lệ");
        }
    }

    private String str(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v == null ? null : v.toString();
    }

    private String strOrThrow(Map<String, Object> m, String k) {
        String s = str(m, k);
        if (s == null || s.isBlank())
            throw new ResponseStatusException(BAD_REQUEST, "Thiếu field connectorConfig." + k);
        return s;
    }
}
