package com.codegym.smartphonemanagement.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveForLaterResponse {
    private boolean success;
    private String message;
    private int cartItemCount;
    private int wishlistItemCount;
}