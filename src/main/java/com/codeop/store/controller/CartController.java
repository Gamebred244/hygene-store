package com.codeop.store.controller;

import com.codeop.store.dto.CartItemRequest;
import com.codeop.store.dto.CartItemResponse;
import com.codeop.store.dto.CartResponse;
import com.codeop.store.model.Cart;
import com.codeop.store.model.CartItem;
import com.codeop.store.model.Product;
import com.codeop.store.repository.CartRepository;
import com.codeop.store.repository.ProductRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartController(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse createCart() {
        Cart cart = new Cart();
        return toResponse(cartRepository.save(cart));
    }

    @GetMapping("/{id}")
    public CartResponse getCart(@PathVariable Long id) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        return toResponse(cart);
    }

    @PostMapping("/{id}/items")
    public CartResponse addItem(@PathVariable Long id, @Valid @RequestBody CartItemRequest request) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            item.setUnitPrice(product.getPrice());
            cart.addItem(item);
        }
        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @PutMapping("/{id}/items/{itemId}")
    public CartResponse updateItem(@PathVariable Long id,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody CartItemRequest request) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        CartItem item = cart.getItems().stream()
                .filter(entry -> entry.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        item.setQuantity(request.getQuantity());
        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        CartItem item = cart.getItems().stream()
                .filter(entry -> entry.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        cart.removeItem(item);
        cartRepository.save(cart);
    }

    private CartResponse toResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        List<CartItemResponse> items = cart.getItems().stream().map(this::toItemResponse).toList();
        response.setItems(items);
        response.setTotal(items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setCurrency(cart.getItems().stream()
                .map(item -> item.getProduct().getCurrency())
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("USD"));
        return response;
    }

    private CartItemResponse toItemResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setImageUrl(item.getProduct().getImageUrl());
        response.setQuantity(item.getQuantity());
        response.setUnitPrice(item.getUnitPrice());
        response.setLineTotal(item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity())));
        return response;
    }
}
