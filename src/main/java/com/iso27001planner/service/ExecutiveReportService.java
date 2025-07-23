package com.iso27001planner.service;

import com.iso27001planner.entity.*;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ExecutiveReportService {
    private final CompanyRepository companyRepository;
    private final AssetRepository assetRepository;
    private final ControlRepository controlRepository;
    private final RiskRepository riskRepository;
    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;


    public void exportExecutiveSummary(Long companyId, List<String> sections, OutputStream outputStream) throws Exception {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        List<Asset> assets = assetRepository.findByCompany_Id(companyId);
        List<Risk> risks = riskRepository.findByCompany_Id(companyId);
        List<Control> controls = controlRepository.findByCompany_Id(companyId);
        List<Task> tasks = taskRepository.findByCompany_Id(companyId); // if applicable

        Document doc = new Document(PageSize.A4);
        PdfWriter.getInstance(doc, outputStream);
        doc.open();

        addLogo(doc);
        addTitle(doc, "Executive Summary Report – ISO 27001");

        if (sections.contains("summary")) {
            addExecutiveSummary(doc, company, assets, risks, controls);
        }
        if (sections.contains("highlights")) {
            addKeyHighlights(doc, assets, risks);
        }
        if (sections.contains("risks")) {
            addKeyRisks(doc, risks);
        }
        if (sections.contains("progress")) {
            addImplementationProgress(doc, controls, tasks);
        }

        addFooter(doc);
        doc.close();

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "EXECUTIVE_REPORT_GENERATED",
                 getCurrentUserEmail(),
                "Report",
                "executive-summary-report",
                "Generated executive summary report"
        ));
    }

    private void addExecutiveSummary(Document doc, Company company, List<Asset> assets, List<Risk> risks, List<Control> controls) throws DocumentException {
        doc.add(new Paragraph("🏢 Company Overview", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        doc.add(new Paragraph("Name: " + company.getName()));
        doc.add(new Paragraph("Assets: " + assets.size()));
        doc.add(new Paragraph("Risks: " + risks.size()));
        doc.add(new Paragraph("Controls: " + controls.size()));
        doc.add(Chunk.NEWLINE);
    }

    private void addKeyHighlights(Document doc, List<Asset> assets, List<Risk> risks) throws DocumentException {
        doc.add(new Paragraph("🌟 Key Highlights", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));

        long highImpactRisks = risks.stream()
                .filter(r -> "high".equalsIgnoreCase(r.getImpact()) || "critical".equalsIgnoreCase(r.getImpact()))
                .count();

        doc.add(new Paragraph("- Assets in inventory: " + assets.size()));
        doc.add(new Paragraph("- High/Critical Risks: " + highImpactRisks));
        doc.add(new Paragraph("- Risk Treatment in Progress: " +
                risks.stream().filter(r -> !"implemented".equalsIgnoreCase(r.getTreatment())).count()));
        doc.add(Chunk.NEWLINE);
    }

    private void addKeyRisks(Document doc, List<Risk> risks) throws DocumentException {
        doc.add(new Paragraph("⚠️ Key Risks", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));

        List<Risk> topRisks = risks.stream()
                .filter(r -> r.getSeverity().equalsIgnoreCase("high") || r.getSeverity().equalsIgnoreCase("critical"))
                .limit(5)
                .toList();

        for (Risk r : topRisks) {
            doc.add(new Paragraph("• " + r.getTitle() + " – Severity: " + r.getSeverity()));
        }

        if (topRisks.isEmpty()) {
            doc.add(new Paragraph("✅ No high or critical risks at this time."));
        }

        doc.add(Chunk.NEWLINE);
    }

    private void addImplementationProgress(Document doc, List<Control> controls, List<Task> tasks) throws DocumentException {
        doc.add(new Paragraph("📈 Implementation Progress", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));

        long total = controls.size();
        long implemented = controls.stream().filter(c -> "implemented".equalsIgnoreCase(c.getStatus())).count();
        long planned = controls.stream().filter(c -> "planned".equalsIgnoreCase(c.getStatus())).count();
        long partial = controls.stream().filter(c -> "partially_implemented".equalsIgnoreCase(c.getStatus())).count();

        doc.add(new Paragraph("Implemented: " + implemented + " / " + total));
        doc.add(new Paragraph("Planned: " + planned));
        doc.add(new Paragraph("Partially Implemented: " + partial));

        long openTasks = tasks.stream().filter(t -> !"done".equalsIgnoreCase(t.getStatus())).count();
        doc.add(new Paragraph("📝 Open Tasks: " + openTasks));

        doc.add(Chunk.NEWLINE);
    }

    private void addLogo(Document doc) {
        try {
            Image logo = Image.getInstance(Objects.requireNonNull(getClass().getClassLoader().getResource("static/LOGO.png")));
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
