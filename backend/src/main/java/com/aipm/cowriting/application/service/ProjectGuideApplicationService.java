package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.guide.ProjectGuideRequest;
import com.aipm.cowriting.application.dto.guide.ProjectGuideResponse;
import com.aipm.cowriting.application.dto.guide.ProjectGuideTaskResponse;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.MaterialSufficiencyResultEntity;
import com.aipm.cowriting.domain.entity.ProjectGuideEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.AcademicDocumentStatus;
import com.aipm.cowriting.domain.model.GuideMode;
import com.aipm.cowriting.domain.model.GuideProgress;
import com.aipm.cowriting.domain.model.GuideTaskStatus;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AcademicDocumentRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.MaterialSufficiencyResultRepository;
import com.aipm.cowriting.domain.repository.ProjectGuideRepository;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectGuideApplicationService {

    private static final String GUIDE_VERSION = "v1";

    private final ProjectGuideRepository guideRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MaterialRepository materialRepository;
    private final AcademicDocumentRepository documentRepository;
    private final DocumentSectionRepository sectionRepository;
    private final KnowledgeChunkRepository knowledgeRepository;
    private final MaterialSufficiencyResultRepository sufficiencyRepository;
    private final ReviewItemRepository reviewRepository;
    private final ResourceOwnershipService ownership;

    public ProjectGuideApplicationService(
            ProjectGuideRepository guideRepository,
            WorkspaceRepository workspaceRepository,
            MaterialRepository materialRepository,
            AcademicDocumentRepository documentRepository,
            DocumentSectionRepository sectionRepository,
            KnowledgeChunkRepository knowledgeRepository,
            MaterialSufficiencyResultRepository sufficiencyRepository,
            ReviewItemRepository reviewRepository,
            ResourceOwnershipService ownership
    ) {
        this.guideRepository = guideRepository;
        this.workspaceRepository = workspaceRepository;
        this.materialRepository = materialRepository;
        this.documentRepository = documentRepository;
        this.sectionRepository = sectionRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.sufficiencyRepository = sufficiencyRepository;
        this.reviewRepository = reviewRepository;
        this.ownership = ownership;
    }

    @Transactional
    public ProjectGuideResponse createForWorkspace(UUID workspaceId, ProjectGuideRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        ProjectGuideEntity entity = new ProjectGuideEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setGuideVersion(GUIDE_VERSION);
        apply(entity, request == null ? defaultRequest() : request);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        guideRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional
    public ProjectGuideResponse get(UUID workspaceId) {
        ownership.requireWorkspace(workspaceId);
        ProjectGuideEntity entity = guideRepository.findById(workspaceId)
                .orElseGet(() -> createEntity(workspaceId, defaultRequest()));
        return toResponse(entity);
    }

    @Transactional
    public ProjectGuideResponse update(UUID workspaceId, ProjectGuideRequest request) {
        ownership.requireWorkspace(workspaceId);
        ProjectGuideEntity entity = guideRepository.findById(workspaceId)
                .orElseGet(() -> createEntity(workspaceId, defaultRequest()));
        apply(entity, request);
        entity.setUpdatedAt(OffsetDateTime.now());
        guideRepository.save(entity);
        return toResponse(entity);
    }

    private ProjectGuideEntity createEntity(UUID workspaceId, ProjectGuideRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        ProjectGuideEntity entity = new ProjectGuideEntity();
        entity.setWorkspaceId(workspaceId);
        entity.setGuideVersion(GUIDE_VERSION);
        apply(entity, request);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return guideRepository.save(entity);
    }

    private void apply(ProjectGuideEntity entity, ProjectGuideRequest request) {
        entity.setCurrentProgress(request.currentProgress());
        entity.setAvailableMaterials(request.availableMaterials() == null
                ? List.of()
                : request.availableMaterials().stream().distinct().toList());
        entity.setTargetDeadline(request.targetDeadline());
        entity.setPreferredMode(request.preferredMode());
    }

    private ProjectGuideResponse toResponse(ProjectGuideEntity entity) {
        GuideState state = deriveState(entity.getWorkspaceId());
        List<ProjectGuideTaskResponse> tasks = buildTasks(entity.getWorkspaceId(), entity, state);
        String currentTaskId = tasks.stream()
                .filter(task -> task.status() == GuideTaskStatus.NEEDS_ATTENTION)
                .findFirst()
                .or(() -> tasks.stream().filter(task -> task.status() == GuideTaskStatus.CURRENT).findFirst())
                .or(() -> tasks.stream().filter(task -> task.status() == GuideTaskStatus.IN_PROGRESS).findFirst())
                .or(() -> tasks.stream().filter(task -> task.status() == GuideTaskStatus.OPTIONAL).findFirst())
                .map(ProjectGuideTaskResponse::id)
                .orElse("review_delivery");
        long completedRequired = tasks.stream()
                .filter(task -> !"knowledge".equals(task.id()))
                .filter(task -> task.status() == GuideTaskStatus.COMPLETED)
                .count();
        int progress = (int) Math.round((completedRequired / 6.0) * 100);
        return new ProjectGuideResponse(
                entity.getWorkspaceId(), entity.getGuideVersion(), entity.getCurrentProgress(),
                entity.getAvailableMaterials(), entity.getTargetDeadline(), entity.getPreferredMode(),
                progress, currentTaskId, tasks, entity.getUpdatedAt()
        );
    }

    private GuideState deriveState(UUID workspaceId) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<MaterialEntity> keyMaterials = materials.stream().filter(MaterialEntity::isKeyMaterial).toList();
        List<MaterialEntity> gateMaterials = keyMaterials.isEmpty() ? materials : keyMaterials;
        boolean parseComplete = !gateMaterials.isEmpty()
                && gateMaterials.stream().allMatch(item -> item.getParseStage() == ParseStage.AI_PARSED);
        boolean parseNeedsAttention = gateMaterials.stream().anyMatch(item ->
                item.getParseStage() == ParseStage.AI_FAILED || item.getParseStage() == ParseStage.AI_PARTIAL);

        List<AcademicDocumentEntity> documents = documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        AcademicDocumentEntity activeDocument = documents.stream()
                .filter(item -> item.getId().equals(workspace.getActiveDocumentId()))
                .findFirst()
                .or(() -> documents.stream().filter(AcademicDocumentEntity::isPrimaryDocument).findFirst())
                .orElse(documents.isEmpty() ? null : documents.get(0));
        List<DocumentSectionEntity> sections = activeDocument == null
                ? List.of()
                : sectionRepository.findByDocumentIdOrderBySortOrderAsc(activeDocument.getId());
        boolean hasSectionContent = sections.stream().anyMatch(item -> !item.getContent().isBlank());
        boolean allSectionsHaveContent = !sections.isEmpty()
                && sections.stream().allMatch(item -> !item.getContent().isBlank());
        long openReviews = activeDocument == null ? 0 : reviewRepository
                .findByDocumentIdOrderByCreatedAtAsc(activeDocument.getId()).stream()
                .filter(item -> "OPEN".equals(item.getReviewStatus())
                        || "MODIFIED_PENDING_RECHECK".equals(item.getReviewStatus()))
                .count();
        Optional<MaterialSufficiencyResultEntity> sufficiency = sufficiencyRepository
                .findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return new GuideState(
                materials.size(), parseComplete, parseNeedsAttention, sufficiency,
                knowledgeRepository.countByWorkspaceId(workspaceId), sections.size(), hasSectionContent,
                allSectionsHaveContent, openReviews,
                activeDocument != null && activeDocument.getStatus() == AcademicDocumentStatus.READY
        );
    }

    private List<ProjectGuideTaskResponse> buildTasks(
            UUID workspaceId,
            ProjectGuideEntity guide,
            GuideState state
    ) {
        String root = "/app/projects/" + workspaceId;
        List<ProjectGuideTaskResponse> tasks = new ArrayList<>();
        tasks.add(task("project_setup", "研究准备", "建立研究项目", "确认研究目标、文档类型与研究范式。",
                "这些信息决定章节模板和材料门槛。", "形成可调整的学术项目画像。",
                GuideTaskStatus.COMPLETED, root, false, "项目画像已建立"));

        boolean reportedWriting = guide.getCurrentProgress() == GuideProgress.WRITING
                || guide.getCurrentProgress() == GuideProgress.REVISING;
        boolean reportedRevising = guide.getCurrentProgress() == GuideProgress.REVISING;
        GuideTaskStatus materialStatus = state.materialCount() > 0
                ? GuideTaskStatus.COMPLETED
                : reportedWriting ? GuideTaskStatus.NEEDS_ATTENTION : GuideTaskStatus.CURRENT;
        String materialReason = state.materialCount() > 0
                ? "项目已经有真实写作输入。"
                : reportedWriting
                    ? "你已说明正在写作或修改，但平台中还没有可追溯的研究材料。"
                : guide.getAvailableMaterials().isEmpty()
                    ? "当前项目还没有可供系统理解的材料。"
                    : "你已说明手头有材料，请把它们上传到项目中。";
        tasks.add(task("materials", "研究准备", "添加研究材料", "上传要求、文献、数据、笔记或已有草稿。",
                materialReason, "材料进入解析队列并保留原始来源。", materialStatus,
                root + "/upload", materialStatus == GuideTaskStatus.NEEDS_ATTENTION,
                state.materialCount() + " 份材料"));

        GuideTaskStatus parseStatus = state.materialCount() == 0
                ? GuideTaskStatus.UPCOMING
                : state.parseNeedsAttention()
                    ? GuideTaskStatus.NEEDS_ATTENTION
                    : state.parseComplete() ? GuideTaskStatus.COMPLETED : GuideTaskStatus.CURRENT;
        tasks.add(task("parsing", "研究准备", "确认材料解析", "检查 AI 是否准确理解材料，并补齐缺失信息。",
                state.parseNeedsAttention() ? "部分关键材料解析不完整或失败。" : "可靠解析是后续写作和证据链的基础。",
                "获得可确认的摘要、分类、主张与证据信息。", parseStatus,
                root + "/parsing", state.parseNeedsAttention(), state.parseComplete() ? "解析已确认" : "等待确认"));

        GuideTaskStatus readinessStatus = !state.parseComplete()
                ? GuideTaskStatus.UPCOMING
                : state.sufficiency().map(result -> result.isGenerationEligible()
                        ? GuideTaskStatus.COMPLETED : GuideTaskStatus.NEEDS_ATTENTION)
                    .orElse(GuideTaskStatus.CURRENT);
        tasks.add(task("readiness", "研究准备", "检查写作准备度", "按文档类型判断材料是否足以支撑章节写作。",
                readinessStatus == GuideTaskStatus.NEEDS_ATTENTION
                        ? "最近一次检查仍发现材料缺口。" : "先确认材料范围，可以减少后续返工。",
                "得到可写章节与待补材料清单。", readinessStatus,
                root + "/materials", readinessStatus == GuideTaskStatus.NEEDS_ATTENTION,
                readinessStatus == GuideTaskStatus.COMPLETED ? "材料准备度已通过" : "等待检查"));

        GuideTaskStatus knowledgeStatus = state.knowledgeCount() > 0
                ? GuideTaskStatus.COMPLETED
                : state.parseComplete() ? GuideTaskStatus.OPTIONAL : GuideTaskStatus.UPCOMING;
        tasks.add(task("knowledge", "研究资产", "构建项目知识库", "把已解析材料整理为可检索证据片段。",
                "知识库有助于定位证据，但不是进入写作的强制门槛。", "形成项目内可检索的证据资产。",
                knowledgeStatus, root + "/knowledge", false, state.knowledgeCount() + " 个片段"));

        boolean readinessPassed = state.sufficiency().map(MaterialSufficiencyResultEntity::isGenerationEligible).orElse(false);
        GuideTaskStatus writingStatus = state.documentReady()
                ? GuideTaskStatus.COMPLETED
                : state.hasSectionContent() || reportedWriting ? GuideTaskStatus.IN_PROGRESS
                : readinessPassed ? GuideTaskStatus.CURRENT : GuideTaskStatus.UPCOMING;
        tasks.add(task("writing", "写作交付", "推进章节写作", "在章节中生成、编辑、共写并保存版本。",
                state.hasSectionContent() ? "已有章节正文，可以继续补全或修改。" : "章节是正文的唯一可编辑来源。",
                "形成可组装、可追溯的章节正文。", writingStatus,
                root + "/documents", false, state.hasSectionContent() ? "章节写作中" : state.sectionCount() + " 个章节待写"));

        GuideTaskStatus reviewStatus = state.documentReady()
                ? GuideTaskStatus.COMPLETED
                : state.openReviews() > 0 ? GuideTaskStatus.NEEDS_ATTENTION
                : state.allSectionsHaveContent() || reportedRevising
                    ? GuideTaskStatus.CURRENT : GuideTaskStatus.UPCOMING;
        tasks.add(task("review_delivery", "写作交付", "审查并交付", "检查可信链、原创实证、引用和未解决问题。",
                state.openReviews() > 0 ? "当前文档仍有待处理或待复查问题。" : "交付前需要确认整篇质量与引用一致性。",
                "得到可确认并导出的完整文档。", reviewStatus,
                root + "/documents", state.openReviews() > 0,
                state.openReviews() > 0 ? state.openReviews() + " 项待处理" : "等待整篇检查"));
        return List.copyOf(tasks);
    }

    private ProjectGuideTaskResponse task(
            String id,
            String phase,
            String title,
            String description,
            String reason,
            String expectedOutcome,
            GuideTaskStatus status,
            String targetPath,
            boolean blocking,
            String progressLabel
    ) {
        return new ProjectGuideTaskResponse(
                id, phase, title, description, reason, expectedOutcome,
                status, targetPath, blocking, progressLabel
        );
    }

    private ProjectGuideRequest defaultRequest() {
        return new ProjectGuideRequest(GuideProgress.IDEA_ONLY, List.of(), null, GuideMode.FLEXIBLE);
    }

    private record GuideState(
            int materialCount,
            boolean parseComplete,
            boolean parseNeedsAttention,
            Optional<MaterialSufficiencyResultEntity> sufficiency,
            long knowledgeCount,
            int sectionCount,
            boolean hasSectionContent,
            boolean allSectionsHaveContent,
            long openReviews,
            boolean documentReady
    ) {
    }
}
