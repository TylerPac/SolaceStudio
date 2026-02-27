package dev.tylerpac.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;

import dev.tylerpac.backend.dto.CreateCheckoutSessionRequest;
import dev.tylerpac.backend.dto.CreateCheckoutSessionResponse;
import dev.tylerpac.backend.dto.ShopOrderResponse;
import dev.tylerpac.backend.dto.ShopProductResponse;
import dev.tylerpac.backend.model.User;
import dev.tylerpac.backend.repo.UserRepository;
import dev.tylerpac.backend.service.StripeShopService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/shop")
public class ShopController {

    private final StripeShopService stripeShopService;
    private final UserRepository userRepository;

    public ShopController(StripeShopService stripeShopService, UserRepository userRepository) {
        this.stripeShopService = stripeShopService;
        this.userRepository = userRepository;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ShopProductResponse>> products() {
        return ResponseEntity.ok(stripeShopService.getProducts());
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<?> createCheckoutSession(
        @Valid @RequestBody CreateCheckoutSessionRequest request,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        Principal principal
    ) {
        try {
            User user = requireUser(principal);
            CreateCheckoutSessionResponse response = stripeShopService.createCheckoutSession(user, request.getProductId(), idempotencyKey);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (StripeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ex.getMessage());
        }
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(Principal principal) {
        try {
            User user = requireUser(principal);
            List<ShopOrderResponse> orders = stripeShopService.getOrders(user);
            return ResponseEntity.ok(orders);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
        @RequestBody String payload,
        @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature
    ) {
        if (!StringUtils.hasText(stripeSignature)) {
            return ResponseEntity.badRequest().body("missing_stripe_signature");
        }

        try {
            stripeShopService.handleWebhook(payload, stripeSignature);
            return ResponseEntity.ok("received");
        } catch (SignatureVerificationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid_signature");
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
        }
    }

    private User requireUser(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new IllegalArgumentException("unauthorized");
        }
        return userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new IllegalArgumentException("unauthorized"));
    }
}
