package com.codeop.store.controller;

import com.codeop.store.dto.PaymentRequest;
import com.codeop.store.dto.PaymentResponse;
import com.codeop.store.model.CustomerOrder;
import com.codeop.store.model.OrderStatus;
import com.codeop.store.model.Payment;
import com.codeop.store.model.PaymentStatus;
import com.codeop.store.repository.OrderRepository;
import com.codeop.store.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentController(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<PaymentResponse> listPayments() {
        return paymentRepository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PaymentResponse getPayment(@PathVariable Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        return toResponse(payment);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@Valid @RequestBody PaymentRequest request) {
        CustomerOrder order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        BigDecimal amount = request.getAmount();
        String currency = request.getCurrency();

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(request.getProvider());
        payment.setProviderReference(request.getProviderReference());
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentStatus.SUCCEEDED);

        Payment saved = paymentRepository.save(payment);
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        return toResponse(saved);
    }

    private PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setOrderId(payment.getOrder().getId());
        response.setProvider(payment.getProvider());
        response.setProviderReference(payment.getProviderReference());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
