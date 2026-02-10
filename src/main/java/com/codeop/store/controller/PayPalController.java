package com.codeop.store.controller;

import com.codeop.store.dto.PayPalOrderRequest;
import com.codeop.store.dto.PayPalOrderResponse;
import com.codeop.store.model.Cart;
import com.codeop.store.model.CartItem;
import com.codeop.store.model.CustomerOrder;
import com.codeop.store.model.OrderItem;
import com.codeop.store.model.OrderStatus;
import com.codeop.store.model.Payment;
import com.codeop.store.model.PaymentStatus;
import com.codeop.store.repository.CartRepository;
import com.codeop.store.repository.OrderRepository;
import com.codeop.store.repository.PaymentRepository;
import com.codeop.store.repository.UserRepository;
import com.codeop.store.service.PayPalService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments/paypal")
public class PayPalController {

    private final PayPalService payPalService;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public PayPalController(PayPalService payPalService,
                            CartRepository cartRepository,
                            OrderRepository orderRepository,
                            PaymentRepository paymentRepository,
                            UserRepository userRepository) {
        this.payPalService = payPalService;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/create")
    public PayPalOrderResponse createOrder(@Valid @RequestBody PayPalOrderRequest request) {
        Cart cart = cartRepository.findById(request.getCartId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        Map<String, Object> response = payPalService.createOrder(request.getAmount(), request.getCurrency(), cart.getId());
        if (response == null || response.get("id") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create PayPal order");
        }
        return new PayPalOrderResponse(String.valueOf(response.get("id")), String.valueOf(response.get("status")));
    }

    @PostMapping("/capture/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public PayPalOrderResponse captureOrder(@PathVariable String orderId, Authentication authentication) {
        Map<String, Object> response = payPalService.captureOrder(orderId);
        if (response == null || response.get("id") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to capture PayPal order");
        }
        PayPalService.PayPalOrderContext context = payPalService.getContext(orderId);
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing payment context");
        }
        Cart cart = cartRepository.findById(context.cartId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        String username = authentication != null ? authentication.getName() : "guest";

        CustomerOrder order = new CustomerOrder();
        order.setCustomerName(username);
        order.setCustomerEmail(userRepository.findByUsername(username)
                .map(user -> user.getEmail())
                .orElse(username + "@hygiene-store.local"));
        order.setStatus(OrderStatus.PAID);
        order.setCurrency(context.currency());
        cart.getItems().forEach(item -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(item.getProduct());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setUnitPrice(item.getUnitPrice());
            order.addItem(orderItem);
        });
        order.setTotalAmount(context.amount());
        CustomerOrder savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setProvider("PAYPAL");
        payment.setProviderReference(orderId);
        payment.setAmount(context.amount());
        payment.setCurrency(context.currency());
        payment.setStatus(PaymentStatus.SUCCEEDED);
        paymentRepository.save(payment);

        cartRepository.delete(cart);
        payPalService.clearContext(orderId);

        return new PayPalOrderResponse(String.valueOf(response.get("id")), String.valueOf(response.get("status")));
    }
}
