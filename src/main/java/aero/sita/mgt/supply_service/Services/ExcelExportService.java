package aero.sita.mgt.supply_service.Services;

import aero.sita.mgt.supply_service.Schemas.DTO.TransactionResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

@Service
public class ExcelExportService {

    public byte[] exportHistoryToExcel(List<TransactionResponse> data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Histórico");

            // Cabeçalho
            Row header = sheet.createRow(0);
            String[] columns = {"Supply,Quantity,Type Change,Region,Date,Price Total"};

            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Dados
            int rowIdx = 1;
            for (TransactionResponse h : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(h.getSupplyName());
                row.createCell(1).setCellValue(h.getQuantityAmended());
                row.createCell(2).setCellValue(h.getTypeEntry());
                row.createCell(3).setCellValue(h.getRegionCode());
                row.createCell(4).setCellValue(h.getCreated().toString());
                row.createCell(5).setCellValue(h.getTotalPrice());
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao exportar Excel", e);
        }
    }

    public File exportToFile(List<TransactionResponse> data, String filename) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Histórico");

            Row header = sheet.createRow(0);
            String[] columns = {"Supply,Quantity,Type Change,Region,Date,Price Total"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (TransactionResponse h : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(h.getSupplyName());
                row.createCell(1).setCellValue(h.getQuantityAmended());
                row.createCell(2).setCellValue(h.getTypeEntry());
                row.createCell(3).setCellValue(h.getRegionCode());
                row.createCell(4).setCellValue(h.getCreated().toString());
                row.createCell(5).setCellValue(h.getTotalPrice());
            }

            File file = new File("exports/" + filename);
            file.getParentFile().mkdirs(); // cria pasta se não existir

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }

            return file;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar arquivo XLSX", e);
        }
    }
}
