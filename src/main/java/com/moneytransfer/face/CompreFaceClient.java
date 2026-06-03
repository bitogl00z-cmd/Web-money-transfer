package com.moneytransfer.face;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class CompreFaceClient {
    private static final Logger log = LoggerFactory.getLogger(CompreFaceClient.class);
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;
    private final double similarityThreshold;

    public CompreFaceClient(RestTemplate restTemplate,
                            @Value("${app.compreface.url}") String baseUrl,
                            @Value("${app.compreface.api-key}") String apiKey,
                            @Value("${app.compreface.similarity-threshold:0.7}") double similarityThreshold) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.similarityThreshold = similarityThreshold;
    }

    public void registerFace(Long userId, byte[] imageBytes) {
        String url = baseUrl + "/api/v1/recognition/faces";
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override public String getFilename() { return "face.jpg"; }
        });
        body.add("subject", userId.toString());

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        log.info("CompreFace register response: {}", response.getBody());
    }

    public boolean verifyFace(Long userId, byte[] imageBytes) {
        String url = baseUrl + "/api/v1/recognition/recognize";
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override public String getFilename() { return "face.jpg"; }
        });

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
        log.info("CompreFace recognize response: {}", response.getBody());

        Map<String, Object> respBody = response.getBody();
        if (respBody == null) return false;

        Object resultObj = respBody.get("result");
        if (!(resultObj instanceof List<?> results) || results.isEmpty()) return false;

        Object first = results.get(0);
        if (!(first instanceof Map<?, ?> match)) return false;

        String subject = (String) match.get("subject");
        Number similarity = (Number) match.get("similarity");

        return userId.toString().equals(subject) && similarity != null
                && similarity.doubleValue() >= similarityThreshold;
    }
}
