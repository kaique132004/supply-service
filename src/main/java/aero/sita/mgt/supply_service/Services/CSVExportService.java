package aero.sita.mgt.supply_service.Services;

import aero.sita.mgt.supply_service.Schemas.DTO.TransactionResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CSVExportService {

    public byte[] exportHistoryToCsv(List<TransactionResponse> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8);

        writer.write("Supply,Quantity,Type Change,Region,Date,Price Total\n");


        for (TransactionResponse tx : data) {
            writer.write(String.join(",",
                    tx.getSupplyName(),
                    String.valueOf(tx.getQuantityAmended()),
                    tx.getTypeEntry(),
                    tx.getRegionCode(),
                    tx.getCreated().toString(),
                    tx.getTotalPrice().toString()
            ));
            writer.write("\n");
        }

        writer.flush();
        return out.toByteArray();
    }
}
