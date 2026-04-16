package com.codegym.smartphonemanagement.controller.api;

import com.codegym.smartphonemanagement.service.cart.DTO.CartResponseDTO;
import com.codegym.smartphonemanagement.service.cart.user.ICartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CartApiController
 * 
 * Tests the API endpoints used by mobile components for cart operations
 */
@WebMvcTest(CartApiController.class)
class CartApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCartCount_ShouldReturnCorrectCount_WhenCartHasItems() throws Exception {
        // Arrange
        CartResponseDTO mockCart = CartResponseDTO.builder()
                .cartId(1L)
                .items(new ArrayList<>()) // 2 items
                .totalPrice(BigDecimal.valueOf(100000))
                .build();
        
        // Add 2 mock items to the list
        mockCart.getItems().add(null); // Placeholder items
        mockCart.getItems().add(null);
        
        when(cartService.getCart(anyLong())).thenReturn(mockCart);

        // Act & Assert
        mockMvc.perform(get("/api/cart/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void getCartCount_ShouldReturnZero_WhenCartIsEmpty() throws Exception {
        // Arrange
        CartResponseDTO mockCart = CartResponseDTO.builder()
                .cartId(1L)
                .items(new ArrayList<>()) // Empty cart
                .totalPrice(BigDecimal.ZERO)
                .build();
        
        when(cartService.getCart(anyLong())).thenReturn(mockCart);

        // Act & Assert
        mockMvc.perform(get("/api/cart/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void getCartCount_ShouldReturnError_WhenServiceThrowsException() throws Exception {
        // Arrange
        when(cartService.getCart(anyLong())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get("/api/cart/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.message").value("Database error"));
    }
}