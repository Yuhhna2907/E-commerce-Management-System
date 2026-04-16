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
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            Model model
    ) {
        // 1. Xử lý phân trang và sắp xếp
        Sort sortObj = direction.equalsIgnoreCase("asc") ?
                Sort.by(sort).ascending() : Sort.by(sort).descending();

        // 2. Gọi service lấy danh sách sản phẩm (đã có tìm kiếm/phân trang)
        Page<ProductResponseDTO> productPage =
                productService.search(keyword, categoryId, minPrice, maxPrice, page, size, sort, direction);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        // 3. LẤY DỮ LIỆU THỐNG KÊ (Phần mới thêm)
        java.util.Map<String, Object> stats = productService.getDashboardStats();

        // 4. Đổ dữ liệu thống kê ra giao diện
        model.addAttribute("totalProducts", stats.get("totalProducts"));
        model.addAttribute("lowStockCount", stats.get("lowStockCount"));
        model.addAttribute("inventoryValue", stats.get("inventoryValue"));

        // 5. Đổ dữ liệu danh sách và các tham số filter
        model.addAttribute("productPage", productPage);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("brand", brand);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("keyword", keyword);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);

        // 6. Dữ liệu bổ trợ cho Form và Sidebar
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("productRequestDTO", new ProductRequestDTO());

        model.addAttribute("pageTitle", "product");

        return "admin/product/list";
    }

    // ===============================
    // 3. SAVE PRODUCT (Nâng cấp để nhận File) - CẢI TIẾN
    // ===============================
    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> saveProduct(
            @ModelAttribute ProductRequestDTO dto,
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile
    ) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("📥 [ProductController.saveProduct] Nhận request - Name: " + dto.getName() + ", Brand: " + dto.getBrand());
        
        try {
            // ✅ VALIDATE DTO
            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Tên sản phẩm không được trống");
            }
            if (dto.getBrand() == null || dto.getBrand().trim().isEmpty()) {
                throw new IllegalArgumentException("Hãng không được trống");
            }
            if (dto.getCategoryId() == null) {
                throw new IllegalArgumentException("Danh mục không được trống");
            }
            if (dto.getPrice() == null || dto.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá phải lớn hơn 0");
            }
            if (dto.getStock() == null || dto.getStock() < 0) {
                throw new IllegalArgumentException("Số lượng không được âm");
            }

            // ✅ XỬ LÝ UPLOAD ẢNH
            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("📸 Đang upload ảnh: " + imageFile.getOriginalFilename() + ", Size: " + imageFile.getSize());
                
                // Validate file size (5MB)
                if (imageFile.getSize() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("File ảnh không được vượt quá 5MB");
                }

                // Validate file type
                String contentType = imageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("File phải là ảnh (JPEG, PNG, GIF, WebP)");
                }

                try {
                    // 1. Xác định đường dẫn thư mục lưu ảnh
                    String uploadRoot = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
                    java.io.File uploadDir = new java.io.File(uploadRoot);

                    // 2. Nếu thư mục chưa tồn tại thì tạo mới
                    if (!uploadDir.exists()) {
                        boolean created = uploadDir.mkdirs();
                        System.out.println("📁 Tạo thư mục uploads: " + created);
                    }

                    // 3. Tạo tên file duy nhất
                    String fileName = java.util.UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

                    // 4. Lưu file vật lý vào ổ cứng
                    java.io.File fileToSave = new java.io.File(uploadRoot + fileName);
                    imageFile.transferTo(fileToSave);
                    System.out.println("✅ Lưu ảnh thành công: " + fileToSave.getAbsolutePath());

                    // 5. Lưu đường dẫn ảo vào DTO
                    dto.setImageUrl("/uploads/" + fileName);
                } catch (java.io.IOException ioException) {
                    System.err.println("❌ Lỗi upload file: " + ioException.getMessage());
                    throw new RuntimeException("Lỗi upload ảnh: " + ioException.getMessage());
                }
            }

            // ✅ GỌI SERVICE LƯU VÀO DB
            System.out.println("💾 Đang lưu sản phẩm vào database...");
            ProductResponseDTO saved = productService.create(dto);
            System.out.println("✅ Sản phẩm lưu thành công! ID: " + saved.getId());

            response.put("status", "success");
            response.put("message", "✅ Thêm sản phẩm thành công!");
            response.put("product", saved);
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Validation error: " + e.getMessage());
            response.put("status", "error");
            response.put("message", "❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi thêm sản phẩm: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "❌ Lỗi: " + e.getMessage());
        }
        return response;
    }

    // ===============================
    // 4. UPDATE PRODUCT (Dành cho nút Sửa) - CẢI TIẾN
    // ===============================
    @PostMapping("/update/{id}")
    @ResponseBody
    public Map<String, Object> updateProduct(
            @PathVariable Long id,
            @ModelAttribute ProductRequestDTO dto,
            @RequestParam(value = "imageFile", required = false) org.springframework.web.multipart.MultipartFile imageFile
    ) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("📥 [ProductController.updateProduct] Cập nhật sản phẩm ID: " + id);
        
        try {
            // ✅ VALIDATE DTO
            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Tên sản phẩm không được trống");
            }
            if (dto.getBrand() == null || dto.getBrand().trim().isEmpty()) {
                throw new IllegalArgumentException("Hãng không được trống");
            }
            if (dto.getCategoryId() == null) {
                throw new IllegalArgumentException("Danh mục không được trống");
            }
            if (dto.getPrice() == null || dto.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá phải lớn hơn 0");
            }
            if (dto.getStock() == null || dto.getStock() < 0) {
                throw new IllegalArgumentException("Số lượng không được âm");
            }

            // ✅ XỬ LÝ UPLOAD ẢNH MỚI NẾU CÓ
            if (imageFile != null && !imageFile.isEmpty()) {
                System.out.println("📸 Đang upload ảnh mới: " + imageFile.getOriginalFilename());
                
                // Validate file size (5MB)
                if (imageFile.getSize() > 5 * 1024 * 1024) {
                    throw new IllegalArgumentException("File ảnh không được vượt quá 5MB");
                }

                // Validate file type
                String contentType = imageFile.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IllegalArgumentException("File phải là ảnh (JPEG, PNG, GIF, WebP)");
                }

                try {
                    // 1. Xác định đường dẫn thư mục lưu ảnh
                    String uploadRoot = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";
                    java.io.File uploadDir = new java.io.File(uploadRoot);

                    // 2. Nếu thư mục chưa tồn tại thì tạo mới
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    // 3. Tạo tên file duy nhất
                    String fileName = java.util.UUID.randomUUID().toString() + "_" + imageFile.getOriginalFilename();

                    // 4. Lưu file vật lý vào ổ cứng
                    java.io.File fileToSave = new java.io.File(uploadRoot + fileName);
                    imageFile.transferTo(fileToSave);
                    System.out.println("✅ Lưu ảnh mới thành công: " + fileToSave.getAbsolutePath());

                    // 5. Lưu đường dẫn ảo vào DTO
                    dto.setImageUrl("/uploads/" + fileName);
                } catch (java.io.IOException ioException) {
                    System.err.println("❌ Lỗi upload file: " + ioException.getMessage());
                    throw new RuntimeException("Lỗi upload ảnh: " + ioException.getMessage());
                }
            }

            // ✅ GỌI SERVICE CẬP NHẬT
            System.out.println("💾 Đang cập nhật sản phẩm...");
            ProductResponseDTO updated = productService.update(id, dto);
            System.out.println("✅ Sản phẩm cập nhật thành công!");

            response.put("status", "success");
            response.put("message", "✅ Cập nhật sản phẩm thành công!");
            response.put("product", updated);
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Validation error: " + e.getMessage());
            response.put("status", "error");
            response.put("message", "❌ " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi cập nhật sản phẩm: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            response.put("status", "error");
            response.put("message", "❌ Lỗi: " + e.getMessage());
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

    @PostMapping("/toggle-status/{id}")
    @ResponseBody
    public Map<String, Object> toggleProductStatus(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean newStatus = productService.toggleStatus(id);
            response.put("status", "success");
            response.put("newStatus", newStatus);
            response.put("message", "Đã cập nhật trạng thái!");
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }
}

