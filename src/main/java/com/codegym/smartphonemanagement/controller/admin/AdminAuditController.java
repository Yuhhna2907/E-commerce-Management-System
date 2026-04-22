package com.codegym.smartphonemanagement.controller.admin;

import com.codegym.smartphonemanagement.model.TransactionLogType;
import com.codegym.smartphonemanagement.repository.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Controller
@RequestMapping("/admin/audit")
@RequiredArgsConstructor
public class AdminAuditController {

    private final TransactionLogRepository transactionLogRepository;

    @GetMapping
    public String listLogs(Model model,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String search,
                         @RequestParam(required = false) TransactionLogType type) {
        
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        if (search != null && !search.isEmpty() && type != null) {
            model.addAttribute("logs", transactionLogRepository.findByTransactionReferenceContainingIgnoreCaseAndType(search, type, pageable));
        } else if (search != null && !search.isEmpty()) {
            model.addAttribute("logs", transactionLogRepository.findByTransactionReferenceContainingIgnoreCase(search, pageable));
        } else if (type != null) {
            model.addAttribute("logs", transactionLogRepository.findByType(type, pageable));
        } else {
            model.addAttribute("logs", transactionLogRepository.findAll(pageable));
        }

        model.addAttribute("pageTitle", "audit");
        model.addAttribute("search", search);
        model.addAttribute("type", type);
        model.addAttribute("types", TransactionLogType.values());
        
        return "admin/audit/list";
    }

    @GetMapping("/export")
    public void exportCsv(HttpServletResponse response,
                          @RequestParam(required = false) String search,
                          @RequestParam(required = false) TransactionLogType type) throws IOException {
        
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit_logs.csv\"");
        response.setCharacterEncoding("UTF-8");

        PrintWriter writer = response.getWriter();
        writer.write('\uFEFF'); // UTF-8 BOM
        writer.println("Timestamp,Loại,Mã Tham Chiếu,Số Tiền,User ID,IP Address,Trạng Thái");

        var pageable = PageRequest.of(0, 10000, Sort.by("createdAt").descending());
        List<com.codegym.smartphonemanagement.model.TransactionLog> logs;
        
        if (search != null && !search.isEmpty() && type != null) {
            logs = transactionLogRepository.findByTransactionReferenceContainingIgnoreCaseAndType(search, type, pageable).getContent();
        } else if (search != null && !search.isEmpty()) {
            logs = transactionLogRepository.findByTransactionReferenceContainingIgnoreCase(search, pageable).getContent();
        } else if (type != null) {
            logs = transactionLogRepository.findByType(type, pageable).getContent();
        } else {
            logs = transactionLogRepository.findAll(pageable).getContent();
        }

        for (var log : logs) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s\n",
                    log.getCreatedAt(),
                    log.getType(),
                    log.getTransactionReference(),
                    log.getAmount(),
                    log.getUserId() != null ? log.getUserId() : "",
                    log.getIpAddress() != null ? log.getIpAddress() : "",
                    log.getStatus()
            );
        }
        writer.flush();
    }
}
