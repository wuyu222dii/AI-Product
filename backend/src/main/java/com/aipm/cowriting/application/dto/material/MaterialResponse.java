package com.aipm.cowriting.application.dto.material;

import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.MaterialRole;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.model.ResearchArtifactType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MaterialResponse(
        UUID id,
        String filename,
        String fileType,
        String sourceType,
        boolean isKeyMaterial,
        ParseStage parseStage,
        BigDecimal confidenceScore,
        OffsetDateTime createdAt,
        MaterialCategory aiMaterialCategory,
        MaterialCategory effectiveMaterialCategory,
        boolean categoryOverridden,
        MaterialRole materialRole,
        ResearchArtifactType researchArtifactType,
        List<String> materialTags,
        String summary,
        String topicRelation,
        List<String> detectedClaims,
        List<String> detectedEvidence,
        List<String> detectedRequirements,
        BibliographicMetadata bibliographicMetadata,
        ParseQualityReport parseQuality
) {
    public MaterialResponse(
            UUID id,
            String filename,
            String fileType,
            String sourceType,
            boolean isKeyMaterial,
            ParseStage parseStage,
            BigDecimal confidenceScore,
            OffsetDateTime createdAt,
            MaterialCategory aiMaterialCategory,
            MaterialCategory effectiveMaterialCategory,
            boolean categoryOverridden,
            String summary,
            String topicRelation,
            List<String> detectedClaims,
            List<String> detectedEvidence,
            List<String> detectedRequirements,
            BibliographicMetadata bibliographicMetadata,
            ParseQualityReport parseQuality
    ) {
        this(
                id, filename, fileType, sourceType, isKeyMaterial, parseStage, confidenceScore, createdAt,
                aiMaterialCategory, effectiveMaterialCategory, categoryOverridden,
                MaterialRole.UNKNOWN, ResearchArtifactType.NONE, List.of(),
                summary, topicRelation, detectedClaims, detectedEvidence, detectedRequirements,
                bibliographicMetadata, parseQuality
        );
    }
}
