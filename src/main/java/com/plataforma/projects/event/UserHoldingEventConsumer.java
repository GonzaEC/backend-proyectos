package com.plataforma.projects.event;

import com.plataforma.projects.service.UserHoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserHoldingEventConsumer {

    private final UserHoldingService userHoldingService;

    /**
     * Consume users.token_purchased: el usuario compró tokens de un proyecto.
     * Payload esperado: { userId, projectId, tokensAmount }
     */
    @KafkaListener(topics = "users.token_purchased", groupId = "service-projects")
    public void onTokenPurchased(Map<String, Object> payload) {
        try {
            Long userId    = toLong(payload.get("userId"));
            Long projectId = toLong(payload.get("projectId"));
            BigDecimal amount = new BigDecimal(payload.get("tokensAmount").toString());

            userHoldingService.updateHolding(userId, projectId, amount);
            log.info("Holding actualizado por compra: userId={} projectId={} +{}", userId, projectId, amount);
        } catch (Exception e) {
            log.error("Error procesando users.token_purchased: {}", payload, e);
        }
    }

    /**
     * Consume marketplace.order_matched: se completó una venta P2P.
     * Payload esperado: { sellerId, buyerId, projectId, tokensAmount }
     */
    @KafkaListener(topics = "marketplace.order_matched", groupId = "service-projects")
    public void onOrderMatched(Map<String, Object> payload) {
        try {
            Long sellerId  = toLong(payload.get("sellerId"));
            Long buyerId   = toLong(payload.get("buyerId"));
            Long projectId = toLong(payload.get("projectId"));
            BigDecimal amount = new BigDecimal(payload.get("tokensAmount").toString());

            userHoldingService.updateHolding(sellerId, projectId, amount.negate());
            userHoldingService.updateHolding(buyerId,  projectId, amount);
            log.info("Holdings actualizados por orden: seller={} buyer={} projectId={} amount={}", sellerId, buyerId, projectId, amount);
        } catch (Exception e) {
            log.error("Error procesando marketplace.order_matched: {}", payload, e);
        }
    }

    private Long toLong(Object val) {
        if (val instanceof Number n) return n.longValue();
        return Long.parseLong(val.toString());
    }
}
