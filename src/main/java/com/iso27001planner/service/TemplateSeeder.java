package com.iso27001planner.service;

import com.iso27001planner.entity.DocumentTemplate;
import com.iso27001planner.repository.DocumentTemplateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateSeeder {

    private final DocumentTemplateRepository templateRepository;

    @PostConstruct
    public void seedTemplates() {
        try {
            if (!templateRepository.findByCompanyIsNull().isEmpty()) {
                log.info("Templates already seeded. Skipping...");
                return;
            }

            Path folder = Paths.get("src/main/resources/iso/templates");

            try (Stream<Path> paths = Files.walk(folder)) {
                paths.filter(Files::isRegularFile)
                        .forEach(path -> {
                            try {
                                String fileName = path.getFileName().toString();
                                String isoClause = fileName.split("_")[0];
                                String title = fileName.substring(fileName.indexOf("_") + 1)
                                        .replace(".docx", "")
                                        .replace(".pdf", "");

                                String content = parseFileContent(path);

                                templateRepository.save(DocumentTemplate.builder()
                                        .title(title)
                                        .type("policy")
                                        .isoClause(isoClause)
                                        .description("Seeded ISO template")
                                        .fileName(fileName)
                                        .filePath(path.toString())
                                        .fileType(Files.probeContentType(path))
                                        .fileSize(Files.size(path))
                                        .content(content)
                                        .build());

                                log.info("Seeded template: {}", title);
                            } catch (Exception ex) {
                                log.warn("Failed to seed file: {}", path.getFileName(), ex);
                            }
                        });
            }

        } catch (IOException e) {
            log.error("Failed to load templates directory", e);
        }
    }

    private String parseFileContent(Path file) throws IOException {
        String type = Files.probeContentType(file);
        if (type != null && type.endsWith("pdf")) {
            try (PDDocument doc = PDDocument.load(file.toFile())) {
                return new PDFTextStripper().getText(doc);
            }
        } else if (type != null && type.contains("word")) {
            try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file))) {
                return doc.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .collect(Collectors.joining("\n"));
            }
        }
        return "Failed to read format of the Document";
    }
}
