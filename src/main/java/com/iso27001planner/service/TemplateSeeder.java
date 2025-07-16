package com.iso27001planner.service;

import com.iso27001planner.entity.DocumentTemplate;
import com.iso27001planner.repository.DocumentTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateSeeder {

    private final DocumentTemplateRepository templateRepository;

    private final Path templateDir = Paths.get("src/main/resources/templates/iso-27001");

    @PostConstruct
    public void seedTemplates() {
        try {
            if (!Files.exists(templateDir)) {
                log.warn(" Template directory not found: {}", templateDir);
                return;
            }

            try (Stream<Path> paths = Files.walk(templateDir)) {
                List<Path> files = paths.filter(Files::isRegularFile).toList();

                log.info("📁 Found {} template files.", files.size());

                for (Path file : files) {
                    try {
                        String hash = calculateFileHash(file);

                        boolean exists = templateRepository.existsByFileHash(hash);
                        if (exists) {
                            log.debug("🔁 Skipping existing template: {}", file.getFileName());
                            continue;
                        }

                        String fileName = file.getFileName().toString();
                        String title = fileName.substring(fileName.indexOf("_") + 1).replaceAll("\\.(docx|doc|pdf|txt|csv|xlsx)$", "");


                        DocumentTemplate saved = templateRepository.save(DocumentTemplate.builder()
                                .title(title)
                                .type("policy")
                                .description("Auto-seeded template for clause ")
                                .fileName(fileName)
                                .filePath(file.toString())
                                .fileType(Files.probeContentType(file))
                                .fileSize(Files.size(file))
                                .fileHash(hash)
                                .build());


                        log.info(" Seeded template: [{}] {}", saved.getIsoClause(), saved.getTitle());

                    } catch (Exception ex) {
                        log.warn(" Failed to seed file: {}", file.getFileName(), ex);
                    }
                }

            }
        } catch (IOException e) {
            log.error(" Failed to walk template directory", e);
        }
    }

    private String calculateFileHash(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] fileBytes = Files.readAllBytes(file);
        byte[] hash = digest.digest(fileBytes);
        return HexFormat.of().formatHex(hash);
    }

    private String parseFileContent(Path file) throws IOException {
        String type = Files.probeContentType(file);
        String fileName = file.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".pdf")) {
            try (PDDocument doc = PDDocument.load(file.toFile())) {
                return new PDFTextStripper().getText(doc);
            }
        }

        if (fileName.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file))) {
                return doc.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .collect(Collectors.joining("\n"));
            }
        }

        if (fileName.endsWith(".doc")) {
            try (HWPFDocument doc = new HWPFDocument(Files.newInputStream(file))) {
                WordExtractor extractor = new WordExtractor(doc);
                return extractor.getText();
            }
        }

        if (fileName.endsWith(".txt") || (type != null && type.startsWith("text"))) {
            return Files.readString(file);
        }

        if (fileName.endsWith(".csv")) {
            return String.join("\n", Files.readAllLines(file));
        }

        if (fileName.endsWith(".xlsx")) {
            try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(file))) {
                StringBuilder sb = new StringBuilder();
                for (Sheet sheet : workbook) {
                    for (Row row : sheet) {
                        for (Cell cell : row) {
                            sb.append(getCellValue(cell)).append("\t");
                        }
                        sb.append("\n");
                    }
                }
                return sb.toString();
            }
        }

        return "Unsupported file type for parsing";
    }

    private String getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
