-- ============================================
-- SQL Script: Tạo Dữ Liệu Test Cho Q&A System
-- ============================================
-- Sử dụng script này để tạo dữ liệu mẫu test Q&A section
-- Yêu cầu: User ID = 1 và Product ID = 1 phải tồn tại

-- ============================================
-- 1. Kiểm tra User ID = 1 có tồn tại không
-- ============================================
SELECT 'Checking if User ID = 1 exists...' as status;
SELECT * FROM users WHERE id = 1;

-- Nếu không có, tạo user mẫu (uncomment nếu cần)
-- INSERT INTO users (id, username, email, password, role, created_at) 
-- VALUES (1, 'testuser', 'test@example.com', 'password123', 'SELLER', NOW());

-- ============================================
-- 2. Kiểm tra Product ID = 1 có tồn tại không
-- ============================================
SELECT 'Checking if Product ID = 1 exists...' as status;
SELECT * FROM products WHERE id = 1;

-- ============================================
-- 3. Xóa dữ liệu test cũ (nếu có)
-- ============================================
SELECT 'Cleaning old test data...' as status;

DELETE FROM answer_votes WHERE answer_id IN (
    SELECT id FROM product_answers WHERE question_id IN (
        SELECT id FROM product_questions WHERE product_id = 1
    )
);

DELETE FROM product_answers WHERE question_id IN (
    SELECT id FROM product_questions WHERE product_id = 1
);

DELETE FROM product_questions WHERE product_id = 1;

-- ============================================
-- 4. Tạo Câu Hỏi Mẫu
-- ============================================
SELECT 'Creating sample questions...' as status;

INSERT INTO product_questions (product_id, user_id, question_text, created_at) VALUES
(1, 1, 'Sản phẩm này có bảo hành bao lâu?', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 1, 'Có màu sắc nào khác ngoài màu đen không?', DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 1, 'Pin của sản phẩm này có tốt không?', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, 1, 'Sản phẩm có kèm theo phụ kiện gì không?', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 1, 'Có hỗ trợ trả góp không?', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 5. Tạo Câu Trả Lời Mẫu
-- ============================================
SELECT 'Creating sample answers...' as status;

-- Câu trả lời cho câu hỏi 1
INSERT INTO product_answers (question_id, user_id, answer_text, created_at) VALUES
(1, 1, 'Sản phẩm được bảo hành chính hãng 12 tháng. Bảo hành 1 đổi 1 trong 30 ngày đầu nếu có lỗi từ nhà sản xuất.', DATE_SUB(NOW(), INTERVAL 4 DAY));

-- Câu trả lời cho câu hỏi 2
INSERT INTO product_answers (question_id, user_id, answer_text, created_at) VALUES
(2, 1, 'Hiện tại sản phẩm có 4 màu: Đen, Trắng, Xanh dương và Hồng. Bạn có thể chọn màu khi đặt hàng.', DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Câu trả lời cho câu hỏi 3
INSERT INTO product_answers (question_id, user_id, answer_text, created_at) VALUES
(3, 1, 'Pin của sản phẩm rất tốt, dung lượng 5000mAh, sử dụng được cả ngày với mức độ sử dụng trung bình. Hỗ trợ sạc nhanh 65W.', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- Câu trả lời cho câu hỏi 4
INSERT INTO product_answers (question_id, user_id, answer_text, created_at) VALUES
(4, 1, 'Sản phẩm kèm theo: Cáp sạc USB-C, Adapter sạc nhanh 65W, Ốp lưng silicon, Sách hướng dẫn sử dụng và Que lấy sim.', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ============================================
-- 6. Tạo Vote Mẫu
-- ============================================
SELECT 'Creating sample votes...' as status;

-- Vote cho câu trả lời 1 (5 helpful, 1 not helpful)
INSERT INTO answer_votes (answer_id, user_id, is_helpful, created_at) VALUES
(1, 1, true, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- Vote cho câu trả lời 2 (8 helpful, 0 not helpful)
INSERT INTO answer_votes (answer_id, user_id, is_helpful, created_at) VALUES
(2, 1, true, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- Vote cho câu trả lời 3 (12 helpful, 2 not helpful)
INSERT INTO answer_votes (answer_id, user_id, is_helpful, created_at) VALUES
(3, 1, true, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- Vote cho câu trả lời 4 (3 helpful, 0 not helpful)
INSERT INTO answer_votes (answer_id, user_id, is_helpful, created_at) VALUES
(4, 1, true, NOW());

-- ============================================
-- 7. Kiểm tra kết quả
-- ============================================
SELECT 'Verification...' as status;

SELECT 'Total Questions:' as metric, COUNT(*) as count FROM product_questions WHERE product_id = 1
UNION ALL
SELECT 'Total Answers:' as metric, COUNT(*) as count FROM product_answers WHERE question_id IN (SELECT id FROM product_questions WHERE product_id = 1)
UNION ALL
SELECT 'Total Votes:' as metric, COUNT(*) as count FROM answer_votes WHERE answer_id IN (SELECT id FROM product_answers WHERE question_id IN (SELECT id FROM product_questions WHERE product_id = 1));

-- Hiển thị chi tiết
SELECT 
    pq.id as question_id,
    pq.question_text,
    COUNT(DISTINCT pa.id) as answer_count,
    COUNT(DISTINCT av.id) as vote_count
FROM product_questions pq
LEFT JOIN product_answers pa ON pa.question_id = pq.id
LEFT JOIN answer_votes av ON av.answer_id = pa.id
WHERE pq.product_id = 1
GROUP BY pq.id, pq.question_text
ORDER BY pq.created_at DESC;

-- ============================================
-- 8. Tạo thêm câu hỏi chưa có câu trả lời
-- ============================================
SELECT 'Creating unanswered questions...' as status;

INSERT INTO product_questions (product_id, user_id, question_text, created_at) VALUES
(1, 1, 'Sản phẩm có chống nước không?', NOW()),
(1, 1, 'Có thể mua thêm bảo hành mở rộng không?', NOW());

-- ============================================
-- DONE!
-- ============================================
SELECT '✅ Test data created successfully!' as status;
SELECT 'You can now test Q&A section at: http://localhost:8080/test-qa-section.html' as info;
