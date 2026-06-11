package com.moneytransfer.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AiServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private AiService aiService;

    @Test
    void parseTransferIntent() {
        Map<String, Object> mockResponse = Map.of(
            "intent", "transfer",
            "confidence", 0.95,
            "entities", Map.of("amount", 500000, "target", "user2")
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(mockResponse));

        var result = aiService.parse("chuyển 500k cho user2");
        assertEquals("transfer", result.get("intent"));
        assertEquals(0.95, (double) result.get("confidence"));
    }
}
