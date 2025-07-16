package com.iso27001planner.service;

import com.iso27001planner.entity.AuditLog;
import com.iso27001planner.entity.AuditPlan;
import com.iso27001planner.entity.NonConformity;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.repository.AuditEventRepository;
import com.iso27001planner.repository.AuditPlanRepository;
import com.iso27001planner.repository.NonConformityRepository;
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
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AuditExportService {

    private final AuditEventRepository auditLogRepo;
    private final AuditPlanRepository auditPlanRepo;
    private final NonConformityRepository nonConformityRepo;
    private final ApplicationEventPublisher eventPublisher;

    public void exportCsv(OutputStream out) throws IOException {
        List<AuditLog> logs = auditLogRepo.findAll();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                "Timestamp", "User", "Action", "Entity Type", "Entity ID", "Description", "IP"
        ));
        for (AuditLog log : logs) {
            printer.printRecord(
                    log.getTimestamp(), log.getActorEmail(), log.getActionType(),
                    log.getEntityType(), log.getEntityId(), log.getDescription(), log.getIpAddress()
            );
        }
        printer.flush();
    }

    public void exportPdf(OutputStream out) throws Exception {
        List<AuditLog> logs = auditLogRepo.findAll();
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("Audit Log Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        doc.add(Chunk.NEWLINE);

        for (AuditLog log : logs) {
            doc.add(new Paragraph(log.getTimestamp() + " - " + log.getActorEmail() + " → " + log.getActionType()));
            doc.add(new Paragraph("Entity: " + log.getEntityType() + " [" + log.getEntityId() + "]"));
            doc.add(new Paragraph("Description: " + log.getDescription()));
            doc.add(new Paragraph("IP: " + log.getIpAddress()));
            doc.add(new Paragraph("---------------------------------------------"));
        }

        doc.close();
    }

    public void exportPdfTable(OutputStream out) throws Exception {
        List<AuditLog> logs = auditLogRepo.findAll();
        Document doc = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(doc, out);
        doc.open();

        addLogo(doc);
        addTitle(doc, "Audit Logs Table Report");
        doc.add(Chunk.NEWLINE);

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        Stream.of("Timestamp", "User", "Action", "Entity Type", "Entity ID", "Description", "IP")
                .forEach(header -> {
                    PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                    cell.setBackgroundColor(Color.LIGHT_GRAY);
                    table.addCell(cell);
                });

        for (AuditLog log : logs) {
            table.addCell(log.getTimestamp().toString());
            table.addCell(log.getActorEmail());
            table.addCell(log.getActionType());
            table.addCell(log.getEntityType());
            table.addCell(log.getEntityId());
            table.addCell(log.getDescription());
            table.addCell(log.getIpAddress());
        }

        doc.add(table);
        addFooter(doc);
        doc.close();
    }

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

    public void writeAuditPdfWithSections(List<String> sections, OutputStream out) throws Exception {
        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, out);
        doc.open();

        addHeader(doc);

        if (sections.contains("summary")) addAuditSummary(doc);
        if (sections.contains("findings")) addAuditFindings(doc);
        if (sections.contains("nonconformities")) addNonConformities(doc);
        if (sections.contains("audit logs")) addAuditLogs(doc);
        if (sections.contains("followup")) addFollowUpActions(doc);

        addFooter(doc);
        doc.close();

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "Audit Report generated ",
                 getCurrentUserEmail(),
                "Report",
                 "Audit report",
                "Audit Report generated "
        ));
    }

    private void addHeader(Document doc) throws Exception {
        try {
            Image logo = Image.getInstance(
                    Objects.requireNonNull(getClass().getClassLoader().getResource("static/LOGO.png"))
            );
            logo.scaleToFit(90, 90);
            logo.setAlignment(Element.ALIGN_CENTER);
            doc.add(logo);
        } catch (Exception e) {
            System.err.println("⚠️ Logo not found.");
        }

        Paragraph title = new Paragraph("ISO 27001 – Audit Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        doc.add(Chunk.NEWLINE);
    }

    private void addAuditSummary(Document doc) throws DocumentException {
        List<AuditPlan> plans = auditPlanRepo.findAll(); // or filter by company
        doc.add(new Paragraph("🔍 Audit Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        for (AuditPlan plan : plans) {
            doc.add(new Paragraph("• " + plan.getTitle() + " (" + plan.getStatus() + ") – " + plan.getStartDate() + " to " + plan.getEndDate()));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addAuditFindings(Document doc) throws DocumentException {
        List<AuditPlan> plans = auditPlanRepo.findAll();
        doc.add(new Paragraph("📝 Findings", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        for (AuditPlan plan : plans) {
            doc.add(new Paragraph("• " + plan.getTitle() + ": " + plan.getFindings()));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addNonConformities(Document doc) throws DocumentException {
        List<NonConformity> nonConformities = nonConformityRepo.findAll();
        doc.add(new Paragraph("⚠️ Non-Conformities", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        for (NonConformity nc : nonConformities) {
            doc.add(new Paragraph("• " + nc.getTitle() + " [" + nc.getSeverity() + "] - " + nc.getStatus()));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addAuditLogs(Document doc) throws DocumentException {
        List<AuditLog> logs = auditLogRepo.findTop50ByOrderByTimestampDesc();
        doc.add(new Paragraph("📜 Audit Logs", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        for (AuditLog log : logs) {
            doc.add(new Paragraph(log.getTimestamp() + " – " + log.getActorEmail() + " → " + log.getActionType()));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addFollowUpActions(Document doc) throws DocumentException {
        List<AuditPlan> plans = auditPlanRepo.findAll();
        doc.add(new Paragraph("📌 Follow-Up Actions", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        for (AuditPlan plan : plans) {
            doc.add(new Paragraph("• Recommendations: " + plan.getRecommendations()));
        }
        doc.add(Chunk.NEWLINE);
    }

    private void addFooter(Document doc) throws DocumentException {
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
