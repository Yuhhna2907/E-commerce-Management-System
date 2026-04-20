package com.codegym.smartphonemanagement.service.profile;

import com.codegym.smartphonemanagement.service.product.DTO.ProductResponseDTO;

import java.util.List;

public interface IRecentlyViewedService {

    /**
     * Ghi nhận lượt xem sản phẩm.
     * Nếu đã xem trước đó → cập nhật timestamp.
     * Nếu vượt quá limit → xóa bản ghi cũ nhất.
     */
    void trackView(Long userId, Long productId);

    /**
     * Lấy danh sách sản phẩm đã xem gần đây.
     * @param limit Số lượng tối đa (thường 20)
     */
    List<ProductResponseDTO> getRecentProducts(Long userId, int limit);
}
