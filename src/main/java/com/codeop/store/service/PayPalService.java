package com.codeop.store.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(name = "app.paypal.client-id")
public class PayPalService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String clientId;
    private final String secret;
    private final String baseUrl;
    private final Map<String, PayPalOrderContext> orderContexts = new ConcurrentHashMap<>();

    public PayPalService(@Value("${app.paypal.client-id}") String clientId,
                         @Value("${app.paypal.secret}") String secret,
                         @Value("${app.paypal.base-url}") String baseUrl) {
        this.clientId = clientId;
        this.secret = secret;
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> createOrder(BigDecimal amount, String currency, Long cartId) {
        String token = fetchAccessToken();

        Map<String, Object> amountMap = new HashMap<>();
        amountMap.put("currency_code", currency);
        amountMap.put("value", amount.setScale(2).toPlainString());

        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("amount", amountMap);

        Map<String, Object> body = new HashMap<>();
        body.put("intent", "CAPTURE");
        body.put("purchase_units", List.of(purchaseUnit));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = restTemplate.postForObject(baseUrl + "/v2/checkout/orders", request, Map.class);
        if (response != null && response.get("id") != null) {
            orderContexts.put(String.valueOf(response.get("id")),
                    new PayPalOrderContext(cartId, amount, currency));
        }
        return response;
    }

    public Map<String, Object> captureOrder(String orderId) {
        String token = fetchAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        return restTemplate.postForObject(baseUrl + "/v2/checkout/orders/" + orderId + "/capture", request, Map.class);
    }

    public PayPalOrderContext getContext(String orderId) {
        return orderContexts.get(orderId);
    }

    public void clearContext(String orderId) {
        orderContexts.remove(orderId);
    }

    public record PayPalOrderContext(Long cartId, BigDecimal amount, String currency) {}

    private String fetchAccessToken() {
        String auth = Base64.getEncoder()
                .encodeToString((clientId + ":" + secret).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.AUTHORIZATION, "Basic " + auth);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        Map<String, Object> response = restTemplate.postForObject(baseUrl + "/v1/oauth2/token", request, Map.class);
        return response != null ? String.valueOf(response.get("access_token")) : "";
    }
}
