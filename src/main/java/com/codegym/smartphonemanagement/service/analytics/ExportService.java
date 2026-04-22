package com.codegym.smartphonemanagement.service.analytics;

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting analytics data to CSV and Excel formats
 * Supports Vietnamese characters with proper encoding
 */
@Service
@Slf4j
public class ExportService {
    
    private static final DateTimeFormatter FILENAME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss");
    
    // UTF-8 BOM for Excel to recognize Vietnamese characters
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    
    /**
     * Export data to CSV format with UTF-8 BOM for Vietnamese character support
     * 
     * @param headers Column headers
     * @param rows Data rows
     * @param filename Base filename (without extension)
     * @return ByteArrayResource containing CSV file
     */
    public ByteArrayResource exportToCsv(
            List<String> headers,
            List<List<String>> rows,
            String filename) {
        
        log.info("Exporting {} rows to CSV: {}", rows.size(), filename);
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {
            
            // Write UTF-8 BOM for Excel compatibility
            baos.write(UTF8_BOM);
            
            // Write headers
            writer.writeNext(headers.toArray(new String[0]));
            
            // Write data rows
            for (List<String> row : rows) {
                writer.writeNext(row.toArray(new String[0]));
            }
            
            writer.flush();
            osw.flush();
            
            // Generate filename with timestamp
            String timestampedFilename = generateFilename(filename, "csv");
            
            log.info("Successfully exported CSV: {} ({} bytes)", 
                     timestampedFilename, baos.size());
            
            return new ByteArrayResource(baos.toByteArray());
            
        } catch (Exception e) {
            log.error("Failed to export CSV: {}", filename, e);
            throw new RuntimeException("Không thể xuất file CSV: " + e.getMessage(), e);
        }
    }
    
    /**
     * Export data to Excel format with formatting
     * 
     * @param headers Column headers
     * @param rows Data rows
     * @param filename Base filename (without extension)
     * @param sheetName Name of the Excel sheet
     * @return ByteArrayResource containing Excel file
     */
    public ByteArrayResource exportToExcel(
            List<String> headers,
            List<List<String>> rows,
            String filename,
            String sheetName) {
        
        log.info("Exporting {} rows to Excel: {}", rows.size(), filename);
        
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Create sheet
            Sheet sheet = workbook.createSheet(sheetName);
            
            // Create header style (bold)
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
            
            // Freeze top row
            sheet.createFreezePane(0, 1);
            
            // Create data rows
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row dataRow = sheet.createRow(rowIndex + 1);
                List<String> rowData = rows.get(rowIndex);
                
                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    Cell cell = dataRow.createCell(colIndex);
                    String value = rowData.get(colIndex);
                    
                    // Try to parse as number for better formatting
                    try {
                        double numValue = Double.parseDouble(value);
                        cell.setCellValue(numValue);
                    } catch (NumberFormatException e) {
                        // Not a number, set as string
                        cell.setCellValue(value);
                    }
                }
            }
            
            // Auto-fit column widths
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                // Add some padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, currentWidth + 1000);
            }
            
            // Write to output stream
            workbook.write(baos);
            
            // Generate filename with timestamp
            String timestampedFilename = generateFilename(filename, "xlsx");
            
            log.info("Successfully exported Excel: {} ({} bytes)", 
                     timestampedFilename, baos.size());
            
            return new ByteArrayResource(baos.toByteArray());
            
        } catch (Exception e) {
            log.error("Failed to export Excel: {}", filename, e);
            throw new RuntimeException("Không thể xuất file Excel: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create header cell style (bold font)
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Bold font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        
        // Background color (light gray)
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Borders
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }
    
    /**
     * Generate filename with timestamp
     * Format: {name}-{yyyy-MM-dd-HHmmss}.{ext}
     * 
     * @param baseName Base filename
     * @param extension File extension (without dot)
     * @return Timestamped filename
     */
    public String generateFilename(String baseName, String extension) {
        String timestamp = LocalDateTime.now().format(FILENAME_FORMATTER);
        return String.format("%s-%s.%s", baseName, timestamp, extension);
    }
}
