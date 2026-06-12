package com.moneytransfer.export;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.transaction.TransactionRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ExportService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public ExportService(AccountRepository accountRepository,
                         TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public byte[] exportPdf(Long accountId, LocalDate from, LocalDate to) throws DocumentException {
        Account account = accountRepository.findById(accountId).orElseThrow();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);
        List<Transaction> transactions = transactionRepository
                .findByAccountIdAndCreatedAtBetween(accountId, fromDt, toDt);

        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("Account Statement"));
        document.add(new Paragraph("Account: " + account.getAccountNumber()));
        document.add(new Paragraph("Period: " + from + " to " + to));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(5);
        table.addCell("Date");
        table.addCell("Code");
        table.addCell("Type");
        table.addCell("Amount");
        table.addCell("Balance");

        for (Transaction tx : transactions) {
            table.addCell(tx.getCreatedAt().toLocalDate().toString());
            table.addCell(tx.getTransactionCode());
            table.addCell(tx.getType().name());
            table.addCell(tx.getAmount().toString());
            BigDecimal balance = tx.getFromBalanceAfter() != null ? tx.getFromBalanceAfter() :
                    (tx.getToBalanceAfter() != null ? tx.getToBalanceAfter() : BigDecimal.ZERO);
            table.addCell(balance.toString());
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public byte[] exportExcel(Long accountId, LocalDate from, LocalDate to) throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);
        List<Transaction> transactions = transactionRepository
                .findByAccountIdAndCreatedAtBetween(accountId, fromDt, toDt);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Statement");

        Row header = sheet.createRow(0);
        String[] cols = {"Date", "Code", "Type", "Amount", "Balance"};
        for (int i = 0; i < cols.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(cols[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        int rowNum = 1;
        for (Transaction tx : transactions) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(tx.getCreatedAt().toLocalDate().toString());
            row.createCell(1).setCellValue(tx.getTransactionCode());
            row.createCell(2).setCellValue(tx.getType().name());
            row.createCell(3).setCellValue(tx.getAmount().doubleValue());
            BigDecimal balance = tx.getFromBalanceAfter() != null ? tx.getFromBalanceAfter() :
                    (tx.getToBalanceAfter() != null ? tx.getToBalanceAfter() : BigDecimal.ZERO);
            row.createCell(4).setCellValue(balance.doubleValue());
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }
}
