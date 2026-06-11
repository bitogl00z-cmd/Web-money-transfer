package com.moneytransfer.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final String pythonUrl;

    public AiService(RestTemplate restTemplate, @Value("${ai.python.url:http://localhost:5000}") String pythonUrl) {
        this.restTemplate = restTemplate;
        this.pythonUrl = pythonUrl;
    }

    public Map<String, Object> parse(String text) {
        String url = pythonUrl + "/api/ai/parse";
        ResponseEntity<Map> response = restTemplate.postForEntity(url, Map.of("text", text), Map.class);
        return response.getBody();
    }
}
