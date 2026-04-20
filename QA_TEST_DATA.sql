-- SQL script để thêm test data cho Q&A system
-- Chạy script này sau khi các bảng đã được tạo

-- Thêm câu hỏi test (giả sử product_id = 24, user_id = 1)
INSERT INTO product_questions (product_id, user_id, question_text, created_at) VALUES
(24, 1, 'Sản phẩm này có bảo hành bao lâu?', NOW()),
(24, 1, 'Có hỗ trợ giao hàng miễn phí không?', NOW()),
(24, 1, 'Pin của máy này dùng được bao lâu?', NOW());

-- Thêm câu trả lời test (giả sử user_id = 1 là SELLER)
INSERT INTO product_answers (question_id, user_id, answer_text, created_at) VALUES
(1, 1, 'Sản phẩm được bảo hành 12 tháng chính hãng. Quý khách có thể mang đến các trung tâm bảo hành của chúng tôi trên toàn quốc.', NOW()),
(2, 1, 'Có ạ, chúng tôi hỗ trợ giao hàng miễn phí cho đơn hàng trên 500.000đ trong nội thành.', NOW()),
(3, 1, 'Pin của máy này có dung lượng 5000mAh, sử dụng bình thường có thể dùng được 1-2 ngày.', NOW());

-- Thêm votes test
INSERT INTO answer_votes (answer_id, user_id, is_helpful, created_at) VALUES
(1, 1, true, NOW()),
(2, 1, true, NOW()),
(3, 1, false, NOW());
