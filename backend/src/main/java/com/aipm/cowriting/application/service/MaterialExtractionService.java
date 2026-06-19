package com.aipm.cowriting.application.service;

import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MaterialExtractionService {

    private final OpenAiImageOcrService openAiImageOcrService;

    public MaterialExtractionService(OpenAiImageOcrService openAiImageOcrService) {
        this.openAiImageOcrService = openAiImageOcrService;
    }

    public String extract(Path path, String fileType) {
        if (path == null || fileType == null) {
            return "";
        }
        try {
            return switch (fileType.toLowerCase()) {
                case "txt", "md", "csv" -> Files.readString(path, StandardCharsets.UTF_8);
                case "pdf" -> extractPdf(path);
                case "docx" -> extractDocx(path);
                case "xlsx" -> extractXlsx(path);
                case "pptx" -> extractPptx(path);
                case "zip" -> extractZip(path);
                case "ppt" -> throw new BusinessException(
                        ErrorCode.MATERIAL_PARSE_FAILED,
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "当前版本暂不支持老版 .ppt，请优先上传 .pptx"
                );
                case "png", "jpg", "jpeg", "webp", "gif", "heic", "tif", "tiff" ->
                        openAiImageOcrService.extractText(path, fileType);
                default -> "";
            };
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.MATERIAL_PARSE_FAILED,
                    HttpStatus.BAD_GATEWAY.value(),
                    "文件文本提取失败"
            );
        }
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(Path path) throws IOException {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(path));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractXlsx(Path path) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(path))) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sheet = workbook.getSheetAt(i);
                builder.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(cell.toString());
                    }
                    if (!cells.isEmpty()) {
                        builder.append(String.join(" | ", cells)).append("\n");
                    }
                }
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private String extractPptx(Path path) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (XMLSlideShow slideShow = new XMLSlideShow(Files.newInputStream(path))) {
            int index = 1;
            for (XSLFSlide slide : slideShow.getSlides()) {
                builder.append("Slide ").append(index++).append(":\n");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String extracted = textShape.getText();
                        if (extracted != null && !extracted.isBlank()) {
                            builder.append(extracted).append("\n");
                            continue;
                        }
                    }
                    String name = shape.getShapeName();
                    if (name != null && !name.isBlank()) {
                        builder.append(name).append("\n");
                    }
                }
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private String extractZip(Path path) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (InputStream inputStream = Files.newInputStream(path);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String entryName = entry.getName();
                String extension = extensionOf(entryName);
                builder.append("Entry: ").append(entryName).append("\n");
                if (List.of("txt", "md", "csv").contains(extension)) {
                    byte[] bytes = zipInputStream.readAllBytes();
                    builder.append(new String(bytes, StandardCharsets.UTF_8)).append("\n\n");
                } else {
                    builder.append("[暂不在 zip 内直接解析该格式: ").append(extension.isBlank() ? "unknown" : extension).append("]\n\n");
                }
            }
        }
        return builder.toString();
    }

    private String extensionOf(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
