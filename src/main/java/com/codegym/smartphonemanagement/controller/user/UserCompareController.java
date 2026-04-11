package com.codegym.smartphonemanagement.controller.user;

import com.codegym.smartphonemanagement.service.product.DTO.ComparisonItemDTO;
import com.codegym.smartphonemanagement.service.product.user.IUserProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserCompareController {

    private final IUserProductService userProductService;

    @GetMapping("/user/compare")
    public String compareProducts(@RequestParam("ids") List<Long> productIds, Model model) {
        if (productIds == null || productIds.isEmpty()) {
            return "redirect:/user/products";
        }
        
        List<ComparisonItemDTO> comparisonList = userProductService.compareProducts(productIds);
        model.addAttribute("comparisonList", comparisonList);
        
        return "user/product/compare";
    }
}
