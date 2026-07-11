package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.DocumentReadinessResponse;
import com.aipm.cowriting.application.dto.academic.ReadinessIssueResponse;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.AcademicProjectProfileEntity;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.model.AcademicDocumentType;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.MaterialRole;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.model.ResearchArtifactType;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AcademicReadinessApplicationService {

    private final AcademicDocumentApplicationService documentService;
    private final AcademicProfileApplicationService profileService;
    private final AcademicRuleCatalog ruleCatalog;
    private final AiSemanticParseResultRepository parseResultRepository;
    private final DocumentSectionRepository sectionRepository;

    public AcademicReadinessApplicationService(
            AcademicDocumentApplicationService documentService,
            AcademicProfileApplicationService profileService,
            AcademicRuleCatalog ruleCatalog,
            AiSemanticParseResultRepository parseResultRepository,
            DocumentSectionRepository sectionRepository
    ) {
        this.documentService = documentService;
        this.profileService = profileService;
        this.ruleCatalog = ruleCatalog;
        this.parseResultRepository = parseResultRepository;
        this.sectionRepository = sectionRepository;
    }

    public DocumentReadinessResponse check(UUID documentId) {
        AcademicDocumentEntity document = documentService.getDocument(documentId);
        AcademicProjectProfileEntity profile = profileService.getEntity(document.getWorkspaceId());
        List<MaterialEntity> materials = documentService.resolveMaterials(document);
        List<AiSemanticParseResultEntity> parseResults = parseResults(materials);
        List<ReadinessIssueResponse> issues = new ArrayList<>();
        Map<String, Boolean> coverage = coverage(materials, parseResults);

        if (!coverage.get("parsedKeyMaterial")) {
            issues.add(issue("KEY_MATERIAL_NOT_PARSED", "BLOCKING", "缺少已解析的核心材料",
                    "当前文档没有可供 AI 可靠使用的关键材料。",
                    "请标记并完成至少 1 份核心材料的 AI 解析。", null));
        }
        if (!coverage.get("literature")) {
            issues.add(issue("LITERATURE_MISSING", "BLOCKING", "缺少可引用文献",
                    "当前文档没有已解析的文献材料，无法形成可靠的学术论证。",
                    "请检索并上传与研究问题直接相关的真实文献。", null));
        }
        if (!coverage.get("submissionRequirement") && documentRequirements(document).isEmpty()) {
            issues.add(issue("REQUIREMENT_UNCONFIRMED", "CONFIRMATION", "写作与提交要求尚未确认",
                    "系统没有找到学校、导师、课程、期刊或用户确认的明确要求。",
                    "请补充文档类型、目标长度、引用格式和结构要求。", null));
        }
        if (ruleCatalog.requiresResearchArtifacts(document.getDocumentType(), profile.getResearchParadigm())
                && !coverage.get("researchArtifact")) {
            issues.add(issue("RESEARCH_ARTIFACT_MISSING", "BLOCKING", "缺少研究数据或分析依据",
                    artifactMessage(profile.getResearchParadigm()),
                    artifactAction(profile.getResearchParadigm()), null));
        }
        if (document.getDocumentType() == AcademicDocumentType.DOCTORAL_DISSERTATION
                && !hasOriginalContribution(document.getRequirementProfileJson())) {
            issues.add(issue("ORIGINAL_CONTRIBUTION_UNCONFIRMED", "CONFIRMATION", "原创贡献尚未确认",
                    "博士论文需要明确拟解决的研究缺口和原创贡献，但 AI 不能替代作者判断创新性。",
                    "请用自己的语言补充原创贡献、适用边界和验证方式。", null));
        }

        long emptySections = sectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId).stream()
                .filter(section -> section.getContent() == null || section.getContent().isBlank())
                .count();
        if (emptySections > 0) {
            issues.add(issue("SECTIONS_INCOMPLETE", "ADVISORY", "仍有未完成章节",
                    "当前有 " + emptySections + " 个章节尚未形成正文。",
                    "可按章节逐步生成或写作，不需要等待整篇材料一次齐全。", null));
        }

        int score = Math.max(0, 100
                - (int) issues.stream().filter(item -> "BLOCKING".equals(item.level())).count() * 30
                - (int) issues.stream().filter(item -> "CONFIRMATION".equals(item.level())).count() * 10
                - (int) issues.stream().filter(item -> "ADVISORY".equals(item.level())).count() * 3);
        boolean eligible = issues.stream().noneMatch(item -> "BLOCKING".equals(item.level()));
        String status = eligible
                ? issues.stream().anyMatch(item -> "CONFIRMATION".equals(item.level())) ? "NEEDS_CONFIRMATION" : "READY"
                : "NEEDS_SUPPLEMENT";
        String nextAction = eligible
                ? "可以按章节继续写作；应用 AI 内容前仍需人工核对证据、引用和学术要求。"
                : "请先补齐阻断项；不受影响的章节仍可手动编辑。";
        return new DocumentReadinessResponse(documentId, status, score, eligible, issues, coverage, nextAction);
    }

    public DocumentReadinessResponse checkSection(UUID sectionId) {
        DocumentSectionEntity section = documentService.getSectionEntity(sectionId);
        DocumentReadinessResponse documentReport = check(section.getDocumentId());
        AcademicDocumentEntity document = documentService.getDocument(section.getDocumentId());
        AcademicProjectProfileEntity profile = profileService.getEntity(document.getWorkspaceId());
        List<ReadinessIssueResponse> issues = new ArrayList<>();
        for (ReadinessIssueResponse issue : documentReport.issues()) {
            if (!"RESEARCH_ARTIFACT_MISSING".equals(issue.code())
                    || ruleCatalog.sectionRequiresResearchArtifacts(section.getSectionType(), profile.getResearchParadigm())) {
                issues.add(new ReadinessIssueResponse(
                        issue.code(), issue.level(), issue.label(), issue.message(), issue.suggestedAction(), sectionId
                ));
            }
        }
        boolean eligible = issues.stream().noneMatch(item -> "BLOCKING".equals(item.level()));
        int score = Math.max(0, 100
                - (int) issues.stream().filter(item -> "BLOCKING".equals(item.level())).count() * 30
                - (int) issues.stream().filter(item -> "CONFIRMATION".equals(item.level())).count() * 10);
        String status = eligible
                ? issues.stream().anyMatch(item -> "CONFIRMATION".equals(item.level())) ? "NEEDS_CONFIRMATION" : "READY"
                : "NEEDS_SUPPLEMENT";
        return new DocumentReadinessResponse(
                document.getId(), status, score, eligible, issues, documentReport.artifactCoverage(),
                eligible ? "当前章节可以生成或共写。" : "请先补齐当前章节所需的真实材料。"
        );
    }

    private List<AiSemanticParseResultEntity> parseResults(List<MaterialEntity> materials) {
        List<UUID> ids = materials.stream().map(MaterialEntity::getId).toList();
        return ids.isEmpty() ? List.of() : parseResultRepository.findByMaterialIdIn(ids);
    }

    private Map<String, Boolean> coverage(List<MaterialEntity> materials, List<AiSemanticParseResultEntity> parseResults) {
        Set<UUID> parsedMaterialIds = materials.stream()
                .filter(material -> material.getParseStage() == ParseStage.AI_PARSED)
                .map(MaterialEntity::getId)
                .collect(Collectors.toSet());
        Map<UUID, AiSemanticParseResultEntity> byMaterial = parseResults.stream()
                .collect(Collectors.toMap(AiSemanticParseResultEntity::getMaterialId, item -> item, (left, right) -> left));
        boolean parsedKey = materials.stream().anyMatch(material -> material.isKeyMaterial() && parsedMaterialIds.contains(material.getId()));
        boolean literature = matches(parseResults, item -> effectiveRole(item) == MaterialRole.LITERATURE
                || effectiveCategory(item) == MaterialCategory.REFERENCE_MATERIAL);
        boolean requirement = matches(parseResults, item -> effectiveRole(item) == MaterialRole.SUBMISSION_REQUIREMENT
                || effectiveCategory(item) == MaterialCategory.ASSIGNMENT_REQUIREMENT);
        boolean researchArtifact = matches(parseResults, item -> effectiveRole(item) == MaterialRole.RESEARCH_ARTIFACT
                || item.getResearchArtifactType() != null && item.getResearchArtifactType() != ResearchArtifactType.NONE
                || effectiveCategory(item) == MaterialCategory.RESEARCH_RESULT
                || effectiveCategory(item) == MaterialCategory.CHART_OR_DATA);
        boolean authorDraft = materials.stream().anyMatch(material -> {
            AiSemanticParseResultEntity parse = byMaterial.get(material.getId());
            return parse != null && (effectiveRole(parse) == MaterialRole.AUTHOR_DRAFT
                    || effectiveCategory(parse) == MaterialCategory.USER_DRAFT);
        });
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put("parsedKeyMaterial", parsedKey);
        result.put("literature", literature);
        result.put("submissionRequirement", requirement);
        result.put("researchArtifact", researchArtifact);
        result.put("authorDraft", authorDraft);
        return result;
    }

    private boolean matches(List<AiSemanticParseResultEntity> items, Predicate<AiSemanticParseResultEntity> predicate) {
        return items.stream().anyMatch(predicate);
    }

    private MaterialRole effectiveRole(AiSemanticParseResultEntity item) {
        return item.getMaterialRole() == null ? MaterialRole.UNKNOWN : item.getMaterialRole();
    }

    private MaterialCategory effectiveCategory(AiSemanticParseResultEntity item) {
        return item.getManualMaterialCategory() == null ? item.getMaterialCategory() : item.getManualMaterialCategory();
    }

    private boolean hasOriginalContribution(Map<String, Object> profile) {
        if (profile == null) return false;
        return nonBlank(profile.get("originalContribution")) || nonBlank(profile.get("researchContribution"));
    }

    private Map<String, Object> documentRequirements(AcademicDocumentEntity document) {
        return document.getRequirementProfileJson() == null ? Map.of() : document.getRequirementProfileJson();
    }

    private boolean nonBlank(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private String artifactMessage(ResearchParadigm paradigm) {
        return switch (paradigm) {
            case QUANTITATIVE, EXPERIMENTAL -> "当前研究范式需要真实数据、样本说明或分析结果，系统不会编造实验和统计结果。";
            case QUALITATIVE -> "当前研究范式需要访谈、观察、田野记录或编码分析依据。";
            case MIXED_METHODS -> "混合研究需要定量与定性两类依据，至少应先上传当前章节使用的研究材料。";
            case COMPUTATIONAL, DESIGN_SCIENCE -> "当前研究需要代码、实验日志、系统测试、模型输出或设计验证材料。";
            default -> "当前文档缺少能支撑结果与讨论章节的原创研究材料。";
        };
    }

    private String artifactAction(ResearchParadigm paradigm) {
        return switch (paradigm) {
            case QUANTITATIVE, EXPERIMENTAL -> "请上传数据表、统计输出、实验记录、样本信息或图表说明。";
            case QUALITATIVE -> "请上传访谈文本、观察记录、编码结果或研究备忘录。";
            case MIXED_METHODS -> "请补充定量数据和定性材料，并标明它们分别支撑哪些章节。";
            case COMPUTATIONAL, DESIGN_SCIENCE -> "请上传代码说明、模型输出、测试记录、原型评估或技术实验结果。";
            default -> "请补充真实研究成果、分析记录或数据依据。";
        };
    }

    private ReadinessIssueResponse issue(String code, String level, String label, String message, String action, UUID sectionId) {
        return new ReadinessIssueResponse(code, level, label, message, action, sectionId);
    }

}
