package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.export.ExportRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ExportApplicationService {

    private static final Set<String> SUPPORTED_FORMATS = Set.of("docx", "pdf");
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

    private final DraftVersionRepository draftVersionRepository;
    private final MaterialRepository materialRepository;
    private final AiSemanticParseResultRepository aiSemanticParseResultRepository;
    private final JobApplicationService jobApplicationService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final Path exportRoot = Paths.get(System.getProperty("user.dir"), "generated-exports")
            .toAbsolutePath().normalize();

    public ExportApplicationService(
            DraftVersionRepository draftVersionRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository aiSemanticParseResultRepository,
            JobApplicationService jobApplicationService,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.draftVersionRepository = draftVersionRepository;
        this.materialRepository = materialRepository;
        this.aiSemanticParseResultRepository = aiSemanticParseResultRepository;
        this.jobApplicationService = jobApplicationService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    public JobResponse export(UUID draftId, ExportRequest request) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        return exportSnapshot(draft, request);
    }

    public JobResponse exportSnapshot(DraftVersionEntity draft, ExportRequest request) {
        if (!SUPPORTED_FORMATS.contains(request.format().toLowerCase())) {
            throw new BusinessException(
                    ErrorCode.EXPORT_FORMAT_UNSUPPORTED,
                    HttpStatus.BAD_REQUEST.value(),
                    "暂不支持该导出格式"
            );
        }
        try {
            String format = request.format().toLowerCase();
            UUID jobId = jobApplicationService.createJob("export_" + format, "success", draft.getWorkspaceId());
            Path relativeFile = Path.of(
                    currentUserService.userId().toString(),
                    draft.getWorkspaceId().toString(),
                    draft.getId() + "-" + jobId + "." + format
            );
            Path outputFile = exportRoot.resolve(relativeFile).normalize();
            requireInsideExportRoot(outputFile);
            Files.createDirectories(outputFile.getParent());
            List<String> referenceTexts = collectReferenceTexts(draft, request.citationStyle());
            if ("docx".equals(format)) {
                writeDocx(draft, referenceTexts, outputFile);
            } else {
                writePdf(draft, referenceTexts, outputFile);
            }
            jobApplicationService.attachOutput(jobId, java.util.Map.of(
                    "downloadUrl", "/api/v1/exports/" + jobId + "/download",
                    "fileName", relativeFile.toString(),
                    "format", format
            ));
            return new JobResponse(jobId.toString(), "success");
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.EXPORT_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "导出文件生成失败"
            );
        }
    }

    public Resource loadExport(UUID jobId) {
        java.util.Map<String, Object> job = jobApplicationService.getJobForCurrentUser(jobId);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> outputRef = (java.util.Map<String, Object>) job.get("outputRef");
        if (outputRef == null || outputRef.get("fileName") == null) {
            throw new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在");
        }
        Path file = exportRoot.resolve(String.valueOf(outputRef.get("fileName"))).normalize();
        requireInsideExportRoot(file);
        if (!Files.exists(file)) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, HttpStatus.NOT_FOUND.value(), "导出文件不存在");
        }
        return new FileSystemResource(file);
    }

    private void requireInsideExportRoot(Path file) {
        if (!file.startsWith(exportRoot)) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, HttpStatus.NOT_FOUND.value(), "导出文件不存在");
        }
    }

    private void writeDocx(DraftVersionEntity draft, List<String> referenceTexts, Path outputFile) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph title = document.createParagraph();
            title.createRun().setText(draft.getTitleSuggestion());
            title.setSpacingAfter(240);

            for (String paragraphText : splitParagraphs(draft.getDraftText())) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText(paragraphText);
                paragraph.setSpacingAfter(180);
            }

            if (!referenceTexts.isEmpty()) {
                XWPFParagraph referenceTitle = document.createParagraph();
                referenceTitle.createRun().setText("参考文献");
                referenceTitle.setSpacingBefore(260);
                referenceTitle.setSpacingAfter(160);

                for (String referenceText : referenceTexts) {
                    XWPFParagraph reference = document.createParagraph();
                    reference.createRun().setText(referenceText);
                    reference.setSpacingAfter(120);
                }
            }

            try (var out = Files.newOutputStream(outputFile)) {
                document.write(out);
            }
        }
    }

    private void writePdf(DraftVersionEntity draft, List<String> referenceTexts, Path outputFile) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.setLeading(18f);
                contentStream.newLineAtOffset(50, 780);
                contentStream.showText(safePdfText(draft.getTitleSuggestion()));
                contentStream.newLine();
                contentStream.newLine();

                for (String paragraph : splitParagraphs(draft.getDraftText())) {
                    for (String line : wrap(paragraph, 70)) {
                        contentStream.showText(safePdfText(line));
                        contentStream.newLine();
                    }
                    contentStream.newLine();
                }

                if (!referenceTexts.isEmpty()) {
                    contentStream.newLine();
                    contentStream.showText(safePdfText("References"));
                    contentStream.newLine();
                    for (String referenceText : referenceTexts) {
                        for (String line : wrap(referenceText, 70)) {
                            contentStream.showText(safePdfText(line));
                            contentStream.newLine();
                        }
                    }
                }
                contentStream.endText();
            }

            document.save(outputFile.toFile());
        }
    }

    private List<String> collectReferenceTexts(DraftVersionEntity draft, String citationStyle) {
        LinkedHashSet<String> materialIds = collectSourceMaterialIds(draft.getSourceTraceMapJson());
        if (materialIds.isEmpty()) {
            return List.of();
        }
        Map<String, MaterialEntity> materialById = materialRepository
                .findByWorkspaceIdOrderByCreatedAtDesc(draft.getWorkspaceId())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        material -> material.getId().toString(),
                        material -> material,
                        (existing, replacement) -> existing
                ));
        List<UUID> parseableMaterialIds = materialIds.stream()
                .map(this::parseUuid)
                .filter(Objects::nonNull)
                .toList();
        Map<String, AiSemanticParseResultEntity> parseResultByMaterialId = parseableMaterialIds.isEmpty()
                ? Map.of()
                : aiSemanticParseResultRepository
                        .findByMaterialIdIn(parseableMaterialIds)
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                                parseResult -> parseResult.getMaterialId().toString(),
                                parseResult -> parseResult,
                                (existing, replacement) -> existing
                        ));

        int[] index = {0};
        return materialIds.stream()
                .map(materialById::get)
                .filter(Objects::nonNull)
                .map(material -> {
                    index[0]++;
                    BibliographicMetadata metadata = readBibliographicMetadata(parseResultByMaterialId.get(material.getId().toString()));
                    return referenceText(material, metadata, citationStyle, index[0]);
                })
                .toList();
    }

    private LinkedHashSet<String> collectSourceMaterialIds(String sourceTraceMapJson) {
        if (sourceTraceMapJson == null || sourceTraceMapJson.isBlank()) {
            return new LinkedHashSet<>();
        }
        try {
            Map<String, Object> sourceTraceMap = objectMapper.readValue(
                    sourceTraceMapJson,
                    new TypeReference<>() {
                    }
            );
            LinkedHashSet<String> materialIds = new LinkedHashSet<>();
            sourceTraceMap.values().forEach(value -> collectMaterialIds(value, materialIds));
            return materialIds;
        } catch (JsonProcessingException e) {
            return new LinkedHashSet<>();
        }
    }

    private void collectMaterialIds(Object value, LinkedHashSet<String> materialIds) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue) {
            materialIds.add(stringValue);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                collectMaterialIds(item, materialIds);
            }
            return;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Object nestedIds = mapValue.get("materialIds");
            if (nestedIds == null) {
                nestedIds = mapValue.get("materials");
            }
            if (nestedIds == null) {
                nestedIds = mapValue.get("sources");
            }
            if (nestedIds != null) {
                collectMaterialIds(nestedIds, materialIds);
            }
        }
    }

    private String referenceText(MaterialEntity material, BibliographicMetadata metadata, String citationStyle, int index) {
        if (isGbtStyle(citationStyle)) {
            return formatGbt7714(material, metadata, index);
        }
        return formatApa(material, metadata);
    }

    private String formatApa(MaterialEntity material, BibliographicMetadata metadata) {
        String title = firstNonBlank(metadata.title(), materialTitle(material));
        String year = firstNonBlank(metadata.year(), materialYear(material));
        String author = formatAuthors(metadata.authors(), ", ");
        String source = firstNonBlank(metadata.sourceTitle(), metadata.publisher(), materialCategory(material));
        String locator = firstNonBlank(metadata.doi(), metadata.url(), "");
        if (author.isBlank()) {
            return joinParts(List.of(
                    "%s. (%s).".formatted(title, year),
                    source,
                    locator
            ));
        }
        return joinParts(List.of(
                "%s. (%s).".formatted(author, year),
                title,
                source,
                locator
        ));
    }

    private String formatGbt7714(MaterialEntity material, BibliographicMetadata metadata, int index) {
        String author = formatAuthors(metadata.authors(), ", ");
        String title = firstNonBlank(metadata.title(), materialTitle(material));
        String type = gbtPublicationType(metadata.publicationType(), metadata.url());
        String source = firstNonBlank(metadata.sourceTitle(), metadata.publisher(), materialCategory(material));
        String year = firstNonBlank(metadata.year(), materialYear(material));
        String locator = firstNonBlank(metadata.doi(), metadata.url(), "");
        String leading = author.isBlank() ? title : "%s. %s".formatted(author, title);
        return joinParts(List.of(
                "[%d] %s%s".formatted(index, leading, type),
                "%s, %s".formatted(source, year),
                locator
        ));
    }

    private BibliographicMetadata readBibliographicMetadata(AiSemanticParseResultEntity parseResult) {
        if (parseResult == null
                || parseResult.getBibliographicMetadataJson() == null
                || parseResult.getBibliographicMetadataJson().isBlank()) {
            return BibliographicMetadata.empty();
        }
        try {
            return objectMapper.readValue(parseResult.getBibliographicMetadataJson(), BibliographicMetadata.class);
        } catch (JsonProcessingException e) {
            return BibliographicMetadata.empty();
        }
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isGbtStyle(String citationStyle) {
        String normalized = citationStyle == null ? "" : citationStyle.toLowerCase();
        return normalized.contains("gb") || normalized.contains("7714");
    }

    private String formatAuthors(List<String> authors, String separator) {
        if (authors == null || authors.isEmpty()) {
            return "";
        }
        return String.join(separator, authors);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String joinParts(List<String> parts) {
        return parts.stream()
                .filter(part -> part != null && !part.isBlank())
                .map(String::trim)
                .map(part -> part.endsWith(".") ? part : part + ".")
                .collect(java.util.stream.Collectors.joining(" "));
    }

    private String gbtPublicationType(String publicationType, String url) {
        String normalized = publicationType == null ? "" : publicationType.toUpperCase();
        if (normalized.contains("JOURNAL")) {
            return "[J]";
        }
        if (normalized.contains("BOOK")) {
            return "[M]";
        }
        if (normalized.contains("THESIS")) {
            return "[D]";
        }
        if (normalized.contains("CONFERENCE")) {
            return "[C]";
        }
        if (normalized.contains("REPORT")) {
            return "[R]";
        }
        if (url != null && !url.isBlank()) {
            return "[EB/OL]";
        }
        return "[Z]";
    }

    private String materialTitle(MaterialEntity material) {
        String filename = material.getFilename() == null || material.getFilename().isBlank()
                ? "未命名材料"
                : material.getFilename();
        String title = filename.replaceFirst("\\.[^.]+$", "").trim();
        return title.isBlank() ? filename : title;
    }

    private String materialYear(MaterialEntity material) {
        Matcher matcher = YEAR_PATTERN.matcher(material.getFilename() == null ? "" : material.getFilename());
        if (matcher.find()) {
            return matcher.group();
        }
        return "n.d.";
    }

    private String materialCategory(MaterialEntity material) {
        if (material.isKeyMaterial()) {
            return "关键材料";
        }
        if (material.getFileType() != null && !material.getFileType().isBlank()) {
            return material.getFileType();
        }
        return "上传材料";
    }

    private java.util.List<String> splitParagraphs(String text) {
        if (text == null || text.isBlank()) {
            return java.util.List.of();
        }
        return java.util.Arrays.stream(text.split("\\R\\R+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private java.util.List<String> wrap(String text, int maxChars) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null) {
            return lines;
        }
        String remaining = text;
        while (remaining.length() > maxChars) {
            lines.add(remaining.substring(0, maxChars));
            remaining = remaining.substring(maxChars);
        }
        if (!remaining.isBlank()) {
            lines.add(remaining);
        }
        return lines;
    }

    private String safePdfText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
