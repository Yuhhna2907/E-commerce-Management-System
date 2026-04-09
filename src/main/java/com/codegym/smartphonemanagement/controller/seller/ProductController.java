package com.codegym.smartphonemanagement.controller.seller;

import com.codegym.smartphonemanagement.repository.user.CategoryRepository;
import com.codegym.smartphonemanagement.service.product.DTO.ProductRequestDTO;
import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;
import com.codegym.smartphonemanagement.service.product.seller.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final CategoryRepository categoryRepository;

    // ===============================
    // 1. LIST + SEARCH + FILTER + PAGINATION
    // ===============================
    @GetMapping
    public String listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model
    ) {
        // Tạo Sort object dựa trên sort + direction
        Sort sortObj = Sort.by("id"); // mặc định
        if (sort != null && direction != null) {
            if (direction.equalsIgnoreCase("asc")) {
                sortObj = Sort.by(sort).ascending();
            } else {
                sortObj = Sort.by(sort).descending();
            }
        }

        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<ProductResponseDTO> productPage =
                productService.search(keyword, categoryId, page, size, sort, direction);

        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("brand", brand);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("productRequestDTO", new ProductRequestDTO());

        return "admin/product/list"; // thymeleaf template
    }

    // ===============================
    // 3. SAVE PRODUCT (Nâng cấp để nhận File)
    // ===============================
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> saveProduct(
            @ModelAttribute ProductRequestDTO dto,
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                // 1. Xác định đường dẫn thư mục lưu ảnh
                // Nó sẽ lưu vào: [Thư mục dự án]/src/main/resources/static/uploads/
                String uploadRoot = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
                java.io.File uploadDir = new java.io.File(uploadRoot);

                // 2. Nếu thư mục chưa tồn tại thì tạo mới
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                // 3. Tạo tên file duy nhất (Ví dụ: a1b2c3..._fox.jpg) để không bị trùng
                String fileName = java.util.UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

                // 4. Lưu file vật lý vào ổ cứng
                java.io.File fileToSave = new java.io.File(uploadRoot + fileName);
                imageFile.transferTo(fileToSave);

                // 5. QUAN TRỌNG: Lưu đường dẫn ảo vào DTO để lưu xuống Database
                // Trình duyệt sẽ gọi ảnh qua cái này
                dto.setImageUrl("/uploads/" + fileName);
            }

            // 6. Gọi service lưu vào DB
            ProductResponseDTO saved = productService.create(dto);

            response.put("status", "success");
            response.put("message", "Thêm sản phẩm thành công!");
            response.put("product", saved);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "Lỗi lưu file: " + e.getMessage());
        }
        return response;
    }

    // ===============================
    // 4. UPDATE PRODUCT (Dành cho nút Sửa)
    // ===============================
    @PostMapping("/update/{id}")
    @ResponseBody
    public Map<String, Object> updateProduct(
            @PathVariable Long id,
            @ModelAttribute ProductRequestDTO dto,
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Tương tự, xử lý ảnh nếu Duy chọn file mới
            if (imageFile != null && !imageFile.isEmpty()) {
                // Xử lý lưu file...
            }

            productService.update(id, dto); // Đảm bảo productService của Duy có hàm update này
            response.put("status", "success");
            response.put("message", "Cập nhật sản phẩm thành công!");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }


    // ===============================
    // 5. DELETE
    // ===============================
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return "redirect:/admin/products";
    }
}

