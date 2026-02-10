package com.codeop.store.controller;

import com.codeop.store.dto.CartItemRequest;
import com.codeop.store.dto.CartItemResponse;
import com.codeop.store.dto.CartResponse;
import com.codeop.store.model.AppUser;
import com.codeop.store.model.Cart;
import com.codeop.store.model.CartItem;
import com.codeop.store.model.Product;
import com.codeop.store.repository.CartRepository;
import com.codeop.store.repository.ProductRepository;
import com.codeop.store.repository.UserRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.Authentication;
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
    private final UserRepository userRepository;

    public CartController(CartRepository cartRepository,
                          ProductRepository productRepository,
                          UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public CartResponse getMyCart(Authentication authentication) {
        Cart cart = getOrCreateCart(authentication);
        return toResponse(cart);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse createCart() {
        Cart cart = new Cart();
        return toResponse(cartRepository.save(cart));
    }

    @PostMapping("/me/items")
    public CartResponse addItemForMe(Authentication authentication,
                                    @Valid @RequestBody CartItemRequest request) {
        Cart cart = getOrCreateCart(authentication);
        return addItemToCart(cart, request);
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
        return addItemToCart(cart, request);
    }

    @PutMapping("/me/items/{itemId}")
    public CartResponse updateItemForMe(Authentication authentication,
                                        @PathVariable Long itemId,
                                        @Valid @RequestBody CartItemRequest request) {
        Cart cart = getOrCreateCart(authentication);
        return updateCartItem(cart, itemId, request);
    }

    @PutMapping("/{id}/items/{itemId}")
    public CartResponse updateItem(@PathVariable Long id,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody CartItemRequest request) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        return updateCartItem(cart, itemId, request);
    }

    @DeleteMapping("/me/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItemForMe(Authentication authentication, @PathVariable Long itemId) {
        Cart cart = getOrCreateCart(authentication);
        removeCartItem(cart, itemId);
        cartRepository.save(cart);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        Cart cart = cartRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found"));
        removeCartItem(cart, itemId);
        cartRepository.save(cart);
    }

    private CartResponse addItemToCart(Cart cart, CartItemRequest request) {
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

    private CartResponse updateCartItem(Cart cart, Long itemId, CartItemRequest request) {
        CartItem item = cart.getItems().stream()
                .filter(entry -> entry.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        item.setQuantity(request.getQuantity());
        Cart saved = cartRepository.save(cart);
        return toResponse(saved);
    }

    private void removeCartItem(Cart cart, Long itemId) {
        CartItem item = cart.getItems().stream()
                .filter(entry -> entry.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        cart.removeItem(item);
    }

    private Cart getOrCreateCart(Authentication authentication) {
        AppUser user = getCurrentUser(authentication);
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }

    private AppUser getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
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
