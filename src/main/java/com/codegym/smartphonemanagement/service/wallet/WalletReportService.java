package com.codegym.smartphonemanagement.service.wallet;

import com.codegym.smartphonemanagement.model.SzWalletTransaction;
import com.codegym.smartphonemanagement.repository.SzWalletTransactionRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service xử lý xuất báo cáo tài chính cho ví (Excel/PDF).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletReportService {

    private final SzWalletTransactionRepository txRepo;

    /**
     * Xuất báo cáo giao dịch ví dưới dạng Excel.
     */
    public byte[] generateExcelReport(LocalDateTime start, LocalDateTime end) throws IOException {
        log.info("Generating Excel report for wallet transactions from {} to {}", start, end);
        List<SzWalletTransaction> transactions = txRepo.findAllByCreatedAtBetweenOrderByCreatedAtDesc(start, end);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Wallet Transactions");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            XSSFFont font = ((XSSFWorkbook) workbook).createFont();
            font.setFontName("Arial");
            font.setBold(true);
            headerStyle.setFont(font);

            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Thời gian", "Người dùng", "Loại giao dịch", "Số tiền (VNĐ)", "Số dư sau", "Mô tả", "Mã đơn hàng"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            
            // Format cho số tiền
            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0"));

            for (SzWalletTransaction tx : transactions) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getId());
                row.createCell(1).setCellValue(tx.getCreatedAt().format(formatter));
                
                String userName = (tx.getWallet().getUser().getFullName() != null && !tx.getWallet().getUser().getFullName().isEmpty()) 
                        ? tx.getWallet().getUser().getFullName() 
                        : tx.getWallet().getUser().getUsername();
                row.createCell(2).setCellValue(userName);
                
                row.createCell(3).setCellValue(tx.getType().name());
                
                Cell amountCell = row.createCell(4);
                amountCell.setCellValue(tx.getAmount().doubleValue());
                amountCell.setCellStyle(currencyStyle);
                
                Cell balanceCell = row.createCell(5);
                balanceCell.setCellValue(tx.getBalanceAfter() != null ? tx.getBalanceAfter().doubleValue() : 0);
                balanceCell.setCellStyle(currencyStyle);
                
                row.createCell(6).setCellValue(tx.getDescription() != null ? tx.getDescription() : "");
                row.createCell(7).setCellValue(tx.getRelatedOrderId() != null ? tx.getRelatedOrderId().toString() : "-");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Xuất báo cáo giao dịch ví dưới dạng PDF.
     * Lưu ý: Do hạn chế về font mặc định trong OpenPDF với tiếng Việt có dấu, 
     * báo cáo PDF sẽ sử dụng tiếng Việt không dấu để đảm bảo hiển thị đúng trên mọi hệ thống.
     */
    public byte[] generatePdfReport(LocalDateTime start, LocalDateTime end) throws IOException {
        log.info("Generating PDF report for wallet transactions from {} to {}", start, end);
        List<SzWalletTransaction> transactions = txRepo.findAllByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Tiêu đề (Không dấu để tránh lỗi font)
            com.lowagie.text.Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("BAO CAO GIAO DICH VI SMARTZONE XU", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph period = new Paragraph("Khoang thoi gian: " + start.format(formatter) + " - " + end.format(formatter));
            period.setAlignment(Element.ALIGN_CENTER);
            document.add(period);
            document.add(new Paragraph(" "));

            // Bảng dữ liệu
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100f);
            table.setWidths(new float[]{1f, 2.5f, 3f, 2f, 2f, 2f, 3.5f, 1.5f});

            // Headers (Không dấu)
            String[] headers = {"ID", "Thoi gian", "Nguoi dung", "Loai", "So tien", "So du", "Mo ta", "Don hang"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Rows
            com.lowagie.text.Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (SzWalletTransaction tx : transactions) {
                table.addCell(new Phrase(String.valueOf(tx.getId()), dataFont));
                table.addCell(new Phrase(tx.getCreatedAt().format(formatter), dataFont));
                
                String userName = (tx.getWallet().getUser().getFullName() != null && !tx.getWallet().getUser().getFullName().isEmpty()) 
                        ? tx.getWallet().getUser().getFullName() 
                        : tx.getWallet().getUser().getUsername();
                // Loại bỏ dấu thủ công hoặc dùng thư viện nếu cần, ở đây giả định tên có thể có dấu nhưng các trường khác thì fix cứng
                table.addCell(new Phrase(removeAccents(userName), dataFont));
                
                table.addCell(new Phrase(tx.getType().name(), dataFont));
                table.addCell(new Phrase(String.format("%,.0f", tx.getAmount()), dataFont));
                table.addCell(new Phrase(String.format("%,.0f", tx.getBalanceAfter()), dataFont));
                table.addCell(new Phrase(removeAccents(tx.getDescription()), dataFont));
                table.addCell(new Phrase(tx.getRelatedOrderId() != null ? String.valueOf(tx.getRelatedOrderId()) : "-", dataFont));
            }

            document.add(table);
            document.close();
            return out.toByteArray();
        }
    }

    /**
     * Helper cực đơn giản để loại bỏ dấu tiếng Việt (tránh lỗi hiển thị PDF khi không nạp font Unicode)
     */
    private String removeAccents(String str) {
        if (str == null) return "";
        return str.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
                  .replaceAll("[èéẹẻẽêềếệểễ]", "e")
                  .replaceAll("[ìíịỉĩ]", "i")
                  .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
                  .replaceAll("[ùúụủũưừứựửữ]", "u")
                  .replaceAll("[ỳýỵỷỹ]", "y")
                  .replaceAll("[đ]", "d")
                  .replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A")
                  .replaceAll("[ÈÉẸẺẼÊỀẾỆỂễ]", "E")
                  .replaceAll("[ÌÍỊỈĨ]", "I")
                  .replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O")
                  .replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U")
                  .replaceAll("[ỲÝỴỶỸ]", "Y")
                  .replaceAll("[Đ]", "D");
    }
}
