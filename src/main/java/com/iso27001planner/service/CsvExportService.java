package com.iso27001planner.service;

import com.iso27001planner.entity.Risk;
import com.iso27001planner.repository.RiskRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final RiskRepository riskRepository;

    public void writeRisksToCsv(Long companyId, OutputStream outputStream) throws IOException {
        List<Risk> risks = riskRepository.findByCompany_Id(companyId);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "ID", "Title", "Asset", "Threat", "Likelihood", "Impact", "Severity", "Due Date"
        ));

        for (Risk r : risks) {
            printer.printRecord(
                    r.getId(), r.getTitle(),
                    r.getAsset() != null ? r.getAsset().getName() : "",
                    r.getThreat(), r.getLikelihood(), r.getImpact(),
                    r.getSeverity(), r.getDueDate()
            );
        }

        printer.flush();
    }
}
