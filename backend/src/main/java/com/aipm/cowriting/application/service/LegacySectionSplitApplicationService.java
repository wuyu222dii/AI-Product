package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.ApplySectionSplitRequest;
import com.aipm.cowriting.application.dto.academic.CreateDocumentSectionRequest;
import com.aipm.cowriting.application.dto.academic.DocumentSectionResponse;
import com.aipm.cowriting.application.dto.academic.SectionSplitItem;
import com.aipm.cowriting.application.dto.academic.SectionSplitPreviewResponse;
import com.aipm.cowriting.application.dto.academic.UpdateDocumentSectionRequest;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacySectionSplitApplicationService {

    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "(?m)^(?:#{1,6}\\s+(.+)|((?:第[一二三四五六七八九十百0-9]+[章节])(?:\\s+.*)?))\\s*$"
    );

    private final AcademicDocumentApplicationService documentService;

    public LegacySectionSplitApplicationService(AcademicDocumentApplicationService documentService) {
        this.documentService = documentService;
    }

    public SectionSplitPreviewResponse preview(UUID sectionId) {
        DocumentSectionEntity section = documentService.getSectionEntity(sectionId);
        assertLegacy(section);
        List<SectionSplitItem> items = detect(section.getContent());
        boolean canApply = items.size() > 1;
        String message = canApply
                ? "已识别 " + items.size() + " 个章节。确认前不会改动正文。"
                : "没有识别到足够明确的章节标题，请先在正文中补充 Markdown 标题或“第X章”标题。";
        return new SectionSplitPreviewResponse(sectionId, documentService.currentSectionVersion(section), canApply, items, message);
    }

    @Transactional
    public List<DocumentSectionResponse> apply(UUID sectionId, ApplySectionSplitRequest request) {
        DocumentSectionEntity section = documentService.getSectionEntity(sectionId);
        assertLegacy(section);
        int currentVersion = documentService.currentSectionVersion(section);
        if (currentVersion != request.baseVersionNo()) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATUS_CONFLICT, HttpStatus.CONFLICT.value(), "正文在拆分预览后已被修改，请重新预览");
        }
        if (request.sections().size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_BODY, HttpStatus.BAD_REQUEST.value(), "至少需要两个章节才能应用拆分");
        }

        List<DocumentSectionResponse> result = new ArrayList<>();
        SectionSplitItem first = request.sections().get(0);
        result.add(documentService.updateSection(sectionId, new UpdateDocumentSectionRequest(
                first.title(), first.content(), section.getSortOrder(), section.getTargetLength(), null,
                section.getSourceTraceMapJson(), "按用户确认的标题拆分旧版整篇正文"
        )));
        for (int index = 1; index < request.sections().size(); index++) {
            SectionSplitItem item = request.sections().get(index);
            result.add(documentService.createSection(section.getDocumentId(), new CreateDocumentSectionRequest(
                    null,
                    section.getSortOrder() + index,
                    item.sectionType(),
                    item.title(),
                    item.content(),
                    section.getTargetLength() == null ? null : Math.max(100, section.getTargetLength() / request.sections().size())
            )));
        }
        return result;
    }

    private List<SectionSplitItem> detect(String content) {
        String text = content == null ? "" : content;
        Matcher matcher = HEADING_PATTERN.matcher(text);
        List<Heading> headings = new ArrayList<>();
        while (matcher.find()) {
            String title = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            headings.add(new Heading(matcher.start(), matcher.end(), title.trim()));
        }
        if (headings.isEmpty()) return List.of(new SectionSplitItem("正文", "LEGACY_FULL_TEXT", text));

        List<SectionSplitItem> result = new ArrayList<>();
        if (headings.get(0).start() > 0 && !text.substring(0, headings.get(0).start()).isBlank()) {
            result.add(new SectionSplitItem("正文导言", "INTRODUCTION", text.substring(0, headings.get(0).start()).trim()));
        }
        for (int index = 0; index < headings.size(); index++) {
            Heading heading = headings.get(index);
            int end = index + 1 < headings.size() ? headings.get(index + 1).start() : text.length();
            String body = text.substring(heading.end(), end).trim();
            if (!body.isBlank()) result.add(new SectionSplitItem(heading.title(), "CUSTOM", body));
        }
        return result.isEmpty() ? List.of(new SectionSplitItem("正文", "LEGACY_FULL_TEXT", text)) : result;
    }

    private void assertLegacy(DocumentSectionEntity section) {
        if (!"LEGACY_FULL_TEXT".equalsIgnoreCase(section.getSectionType())) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATUS_CONFLICT, HttpStatus.CONFLICT.value(), "只有旧版整篇正文可以使用自动拆分");
        }
    }

    private record Heading(int start, int end, String title) { }
}
