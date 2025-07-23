package com.iso27001planner.service;

import com.iso27001planner.entity.Company;
import com.iso27001planner.entity.Control;
import com.iso27001planner.event.AuditEvent;
import com.iso27001planner.exception.BusinessException;
import com.iso27001planner.repository.CompanyRepository;
import com.iso27001planner.repository.ControlRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplianceReportService {
    private final CompanyRepository companyRepo;
    private final ControlRepository controlRepo;
    private final ApplicationEventPublisher eventPublisher;

    public void exportComplianceReport(Long companyId, List<String> sections, OutputStream outputStream) throws Exception {
        Company company = companyRepo.findById(companyId)
                .orElseThrow(() -> new BusinessException("Company not found", HttpStatus.NOT_FOUND));

        List<Control> controls = controlRepo.findByCompany_Id(companyId);

        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc, PageSize.A4);


        addLogo(doc);
        addTitle(doc, "ISO 27001 – Compliance Report");

        if (sections.contains("summary")) {
            addComplianceSummary(doc, company, controls);
        }
        if (sections.contains("details")) {
            addControlDetails(doc, controls);
        }
        if (sections.contains("charts")) {
            addComplianceCharts(doc, pdfDoc, controls); // iText 7 signature
        }
        if (sections.contains("gaps")) {
            addDetailedGaps(doc, controls);
        }

        addFooter(doc);
        doc.close();

        eventPublisher.publishEvent(new AuditEvent(
                this,
                "COMPLIANCE_REPORT_GENERATED",
                getCurrentUserEmail(),
                "ComplianceReport",
                "compliance",
                "Generated compliance report"
        ));
    }

    private void addComplianceSummary(Document doc, Company company, List<Control> controls) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        doc.add(new Paragraph("📋 Executive Summary").setFont(font).setFontSize(14));

        doc.add(new Paragraph("Company: " + company.getName()));

        long total = controls.size();
        long implemented = controls.stream().filter(c -> "implemented".equalsIgnoreCase(c.getStatus())).count();
        long partial = controls.stream().filter(c -> "partially_implemented".equalsIgnoreCase(c.getStatus())).count();
        long planned = controls.stream().filter(c -> "planned".equalsIgnoreCase(c.getStatus())).count();
        long notImplemented = total - (implemented + partial + planned);

        doc.add(new Paragraph("Total Controls: " + total));
        doc.add(new Paragraph("Implemented: " + implemented));
        doc.add(new Paragraph("Partially Implemented: " + partial));
        doc.add(new Paragraph("Planned: " + planned));
        doc.add(new Paragraph("Not Implemented: " + notImplemented));
        doc.add(new Paragraph("\n"));
    }

    private void addControlDetails(Document doc, List<Control> controls) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        doc.add(new Paragraph("📄 Control Details").setFont(font).setFontSize(14));

        if (controls.isEmpty()) {
            doc.add(new Paragraph("No controls available for this company."));
        } else {
            for (Control control : controls) {
                doc.add(new Paragraph("• " + control.getTitle() + " [" + control.getStatus() + "]"));
            }
        }

        doc.add(new Paragraph("\n"));
    }

    private void addDetailedGaps(Document doc, List<Control> controls) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        doc.add(new Paragraph("⚠️ Detailed Gaps").setFont(font).setFontSize(14));
        boolean found = false;

        for (Control c : controls) {
            if ("not_implemented".equalsIgnoreCase(c.getStatus()) || "planned".equalsIgnoreCase(c.getStatus())) {
                found = true;
                doc.add(new Paragraph("• " + c.getTitle() + " — Status: " + c.getStatus()));
            }
        }

        if (!found) {
            doc.add(new Paragraph("All controls are implemented or partially implemented. No gaps found."));
        }

        doc.add(new Paragraph("\n"));
    }

    public void addComplianceCharts(Document doc, PdfDocument pdfDoc, List<Control> controls) throws IOException {
        // Add title
        doc.add(new Paragraph("📈 Compliance Charts")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(14)
                .setMarginBottom(10));

        // Compute data
        int total = controls.size();
        int imp = (int) controls.stream().filter(c -> "implemented".equalsIgnoreCase(c.getStatus())).count();
        int part = (int) controls.stream().filter(c -> "partially_implemented".equalsIgnoreCase(c.getStatus())).count();
        int plan = (int) controls.stream().filter(c -> "planned".equalsIgnoreCase(c.getStatus())).count();
        int notImp = total - (imp + part + plan);

        // Create chart space
        float chartWidth = 200f;
        float chartHeight = 100f;
        PdfFormXObject chartObject = new PdfFormXObject(new Rectangle(chartWidth, chartHeight));

        // Correct PdfCanvas usage
        PdfCanvas pdfCanvas = new PdfCanvas(chartObject, pdfDoc);

        // Bar positioning
        float startX = 10;
        float chartY = chartHeight - 20;
        float barHeight = 12;
        float gap = 6;

        // Draw bars
        drawBar(pdfCanvas, startX, chartY, chartWidth - 20, total, imp, barHeight, ColorConstants.GREEN, "Implemented");
        drawBar(pdfCanvas, startX, chartY - (barHeight + gap), chartWidth - 20, total, part, barHeight, ColorConstants.ORANGE, "Partial");
        drawBar(pdfCanvas, startX, chartY - 2 * (barHeight + gap), chartWidth - 20, total, plan, barHeight, ColorConstants.BLUE, "Planned");
        drawBar(pdfCanvas, startX, chartY - 3 * (barHeight + gap), chartWidth - 20, total, notImp, barHeight, ColorConstants.RED, "Not Implemented");

        // Embed the drawn object as an image inline
        Image chartImage = new Image(chartObject);
        chartImage.setAutoScale(true);
        doc.add(chartImage);

        // Add spacing after chart
        doc.add(new Paragraph("\n"));
    }

    private void drawBar(PdfCanvas canvas, float x, float y, float totalW, int total, int value,
                         float h, com.itextpdf.kernel.colors.Color color, String label) throws IOException {

        float width = (total > 0) ? (value * totalW / total) : 0;

        // Draw filled rectangle
        canvas.saveState()
                .setFillColor(color)
                .rectangle(x, y, width, h)
                .fill()
                .restoreState();

        // Add text label
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        canvas.beginText()
                .setFontAndSize(font, 10)
                .moveText(x + width + 5, y + 3)
                .showText(label + " (" + value + ")")
                .endText();
    }


    private void addLogo(Document doc) {
        try {
            URL logoUrl = getClass().getClassLoader().getResource("static/LOGO.png");
            if (logoUrl == null) {
                System.err.println("Logo not found");
                return;
            }

            ImageData data = ImageDataFactory.create(logoUrl);
            Image logo = new Image(data)
                    .scaleToFit(90, 90)
                    .setHorizontalAlignment(HorizontalAlignment.CENTER);

            doc.add(logo);
        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
        }
    }

    private void addTitle(Document doc, String text) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        Paragraph title = new Paragraph(text)
                .setFont(font)
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER);
        doc.add(title);
    }

    private void addFooter(Document doc) throws IOException {
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
        Paragraph footer = new Paragraph("Generated by Protected Consulting ISO 27001 Planner – " + LocalDateTime.now())
                .setFont(font)
                .setFontSize(10)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER);

        doc.add(new Paragraph("\n"));
        doc.add(footer);
    }

    private String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }
}
