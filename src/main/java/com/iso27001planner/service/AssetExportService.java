package com.iso27001planner.service;

import com.iso27001planner.entity.Asset;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.repository.AssetRepository;
import com.lowagie.text.*;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AssetExportService {

    private final AssetRepository assetRepository;
    private final ApplicationEventPublisher eventPublisher;

    // === CSV EXPORT ===
    public void exportCsv(Long companyId, OutputStream out) throws IOException {
        List<Asset> assets = assetRepository.findByCompany_Id(companyId);
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "ID", "Name", "Type", "Classification", "Location", "Status", "Value", "Owner", "Last Review"
        ));

        for (Asset asset : assets) {
            printer.printRecord(
                    asset.getId(), asset.getName(), asset.getType(),
                    asset.getClassification(), asset.getLocation(),
                    asset.getStatus(), asset.getValue(),
                    asset.getOwner() != null ? asset.getOwner().getEmail() : "-",
                    asset.getLastReview()
            );
        }

        printer.flush();
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "EXPORT_ASSET_CSV",
                 getCurrentUserEmail(),
                "Asset",
                 companyId.toString(),
                "Exported asset report as CSV for company " + companyId
        ));
    }

    // === PDF SUMMARY EXPORT ===
    public void exportPdf(Long companyId, OutputStream out) throws Exception {
        List<Asset> assets = assetRepository.findByCompany_Id(companyId);
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addLogo(doc);
        addTitle(doc, "Asset Report Summary");
        doc.add(Chunk.NEWLINE);

        for (Asset a : assets) {
            doc.add(new Paragraph("Name: " + a.getName()));
            doc.add(new Paragraph("Type: " + a.getType()));
            doc.add(new Paragraph("Classification: " + a.getClassification()));
            doc.add(new Paragraph("Location: " + a.getLocation()));
            doc.add(new Paragraph("Owner: " + (a.getOwner() != null ? a.getOwner().getEmail() : "-")));
            doc.add(new Paragraph("Value: " + a.getValue()));
            doc.add(new Paragraph("Status: " + a.getStatus()));
            doc.add(new Paragraph("Last Review: " + a.getLastReview()));
            doc.add(new Paragraph("---------------------------------------------"));
        }

        addFooter(doc);
        doc.close();
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "EXPORT_ASSET_PDF",
                 getCurrentUserEmail(),
                "Asset",
                 companyId.toString(),
                "Exported asset report as PDF for company " + companyId
        ));
    }

    // === PDF TABLE EXPORT ===
    public void exportPdfTable(Long companyId, OutputStream out) throws Exception {
        List<Asset> assets = assetRepository.findByCompany_Id(companyId);
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        addLogo(doc);
        addTitle(doc, "Asset Inventory Table");
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 3, 2, 3, 3, 2, 2, 3});

        Stream.of("Name", "Type", "Class", "Location", "Owner", "Status", "Value", "Last Review")
                .forEach(col -> {
                    PdfPCell cell = new PdfPCell(new Phrase(col, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
                    cell.setBackgroundColor(Color.LIGHT_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                });

        for (Asset a : assets) {
            table.addCell(a.getName());
            table.addCell(a.getType());
            table.addCell(a.getClassification());
            table.addCell(a.getLocation());
            table.addCell(a.getOwner() != null ? a.getOwner().getEmail() : "-");
            table.addCell(a.getStatus());
            table.addCell(a.getValue());
            table.addCell(a.getLastReview());
        }

        doc.add(table);
        addFooter(doc);
        doc.close();
        eventPublisher.publishEvent(new AuditEvent(
                this,
                "EXPORT_ASSET_TABLE",
                 getCurrentUserEmail(),
                "Asset",
                 companyId.toString(),
                "Exported asset inventory table for company " + companyId
        ));
    }

    // === BRANDING HELPERS ===
    private void addLogo(Document doc) {
        try {
            Image logo = Image.getInstance(getClass().getClassLoader().getResource("static/LOGO.png"));
            logo.scaleToFit(90, 90);
            logo.setAlignment(Element.ALIGN_CENTER);
            doc.add(logo);
        } catch (Exception e) {
            System.err.println("Logo not found");
        }
    }

    private void addTitle(Document doc, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
    }

    private void addFooter(Document doc) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph("Generated by Protected Consulting ISO 27001 Planner – " + LocalDateTime.now(),
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
