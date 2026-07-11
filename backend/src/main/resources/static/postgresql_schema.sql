-- AI 论文共写工作台 v1.x legacy base schema
-- v2.0 current schema changes are managed by:
-- supabase/migrations/20260711031857_v2_academic_workspace.sql
-- Runtime Hibernate DDL is disabled; do not use this legacy draft instead of migrations.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'workspace_status') THEN
        CREATE TYPE workspace_status AS ENUM (
            'draft',
            'processing',
            'ready',
            'blocked',
            'archived'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'parse_stage') THEN
        CREATE TYPE parse_stage AS ENUM (
            'preprocessed',
            'ai_parsed',
            'ai_partial',
            'ai_failed'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'material_category') THEN
        CREATE TYPE material_category AS ENUM (
            'assignment_requirement',
            'reference_material',
            'user_draft',
            'research_result',
            'chart_or_data',
            'supplement_note',
            'unknown'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'generation_status') THEN
        CREATE TYPE generation_status AS ENUM (
            'success',
            'partial',
            'failed',
            'blocked_insufficient_material'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'review_impact_level') THEN
        CREATE TYPE review_impact_level AS ENUM (
            'notice',
            'local_fix',
            'must_confirm'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_status') THEN
        CREATE TYPE job_status AS ENUM (
            'queued',
            'running',
            'success',
            'partial',
            'failed'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_type') THEN
        CREATE TYPE job_type AS ENUM (
            'semantic_parse',
            'requirement_extract',
            'material_sufficiency_eval',
            'draft_generate',
            'outline_refine',
            'evidence_bind',
            'review_pass',
            'appeal_recheck',
            'export_docx',
            'export_pdf'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_action_type') THEN
        CREATE TYPE user_action_type AS ENUM (
            'accept',
            'ignore',
            'defer',
            'appeal',
            'proceed_with_risk',
            'retry',
            'upload_supplement'
        );
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title TEXT NOT NULL,
    status workspace_status NOT NULL DEFAULT 'draft',
    current_draft_version_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    filename TEXT NOT NULL,
    file_type TEXT NOT NULL,
    source_type TEXT NOT NULL,
    raw_file_url TEXT NOT NULL,
    is_key_material BOOLEAN NOT NULL DEFAULT FALSE,
    parse_stage parse_stage NOT NULL DEFAULT 'preprocessed',
    confidence_score NUMERIC(5,4) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS preprocessed_contents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL UNIQUE REFERENCES materials(id) ON DELETE CASCADE,
    plain_text TEXT NULL,
    ocr_text TEXT NULL,
    page_map_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    image_slices_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ai_semantic_parse_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL UNIQUE REFERENCES materials(id) ON DELETE CASCADE,
    material_category material_category NOT NULL DEFAULT 'unknown',
    summary TEXT NULL,
    topic_relation TEXT NULL,
    detected_claims_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    detected_evidence_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    detected_requirements_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    bibliographic_metadata_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    confidence_score NUMERIC(5,4) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    material_id UUID NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    source_excerpt TEXT NULL,
    keywords_json TEXT NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS requirement_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    topic TEXT NULL,
    word_count INTEGER NULL,
    deadline TIMESTAMPTZ NULL,
    citation_style TEXT NULL,
    special_requirements_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    version INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, version)
);

CREATE TABLE IF NOT EXISTS material_sufficiency_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    is_generation_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    missing_items_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    recommended_supplements_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    reason TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS literature_candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    provider TEXT NOT NULL,
    title TEXT NOT NULL,
    authors_json TEXT NOT NULL DEFAULT '[]',
    year TEXT NULL,
    source_title TEXT NULL,
    publisher TEXT NULL,
    doi TEXT NULL,
    url TEXT NULL,
    abstract_snippet TEXT NULL,
    citation_preview TEXT NULL,
    quality_score INTEGER NULL,
    quality_label TEXT NULL,
    matched_reasons_json TEXT NOT NULL DEFAULT '[]',
    missing_metadata_json TEXT NOT NULL DEFAULT '[]',
    duplicate_group_key TEXT NOT NULL,
    recommended_use TEXT NULL,
    status TEXT NOT NULL DEFAULT 'TO_DOWNLOAD',
    material_id UUID NULL REFERENCES materials(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE literature_candidates ENABLE ROW LEVEL SECURITY;

CREATE TABLE IF NOT EXISTS draft_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    version_no INTEGER NOT NULL,
    title_suggestion TEXT NULL,
    outline_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    paragraph_skeletons_json JSONB NOT NULL DEFAULT '[]'::JSONB,
    draft_text TEXT NOT NULL DEFAULT '',
    source_trace_map_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    generation_status generation_status NOT NULL DEFAULT 'success',
    created_by TEXT NOT NULL DEFAULT 'system',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (workspace_id, version_no)
);

CREATE TABLE IF NOT EXISTS evidence_bindings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_version_id UUID NOT NULL REFERENCES draft_versions(id) ON DELETE CASCADE,
    paragraph_id TEXT NOT NULL DEFAULT 'p1',
    knowledge_chunk_id UUID NULL REFERENCES knowledge_chunks(id) ON DELETE SET NULL,
    claim_text TEXT NOT NULL,
    material_id UUID NULL REFERENCES materials(id) ON DELETE CASCADE,
    source_excerpt TEXT NULL,
    target_range_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    confidence_score NUMERIC(5,4) NULL,
    support_type TEXT NOT NULL DEFAULT 'SOURCE_TRACE',
    binding_status TEXT NOT NULL DEFAULT 'WEAK',
    citation_text TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS cowrite_previews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    draft_version_id UUID NOT NULL REFERENCES draft_versions(id) ON DELETE CASCADE,
    action TEXT NOT NULL,
    target_range_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    instruction TEXT NULL,
    controls_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    candidate_title_suggestion TEXT NULL,
    candidate_draft_text TEXT NOT NULL,
    candidate_source_trace_map_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    diff_summary_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    status TEXT NOT NULL DEFAULT 'READY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    applied_at TIMESTAMPTZ NULL
);

CREATE TABLE IF NOT EXISTS review_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    draft_version_id UUID NOT NULL REFERENCES draft_versions(id) ON DELETE CASCADE,
    review_type TEXT NOT NULL,
    review_impact_level review_impact_level NOT NULL,
    target_range_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    message TEXT NOT NULL,
    suggested_fix TEXT NULL,
    can_bypass BOOLEAN NOT NULL DEFAULT TRUE,
    review_status TEXT NOT NULL DEFAULT 'OPEN',
    resolution_note TEXT NULL,
    resolved_at TIMESTAMPTZ NULL,
    last_rechecked_at TIMESTAMPTZ NULL,
    recheck_note TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS review_recheck_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_item_id UUID NOT NULL REFERENCES review_items(id) ON DELETE CASCADE,
    draft_version_id UUID NOT NULL REFERENCES draft_versions(id) ON DELETE CASCADE,
    outcome TEXT NOT NULL,
    previous_status TEXT NULL,
    next_status TEXT NULL,
    previous_impact_level TEXT NULL,
    next_impact_level TEXT NULL,
    note TEXT NULL,
    basis_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS appeal_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_item_id UUID NOT NULL REFERENCES review_items(id) ON DELETE CASCADE,
    user_reason TEXT NOT NULL,
    evidence_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    review_outcome TEXT NULL,
    resolved_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS generation_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    job_type job_type NOT NULL,
    status job_status NOT NULL DEFAULT 'queued',
    input_ref_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    output_ref_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    error_message TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    draft_version_id UUID NULL REFERENCES draft_versions(id) ON DELETE SET NULL,
    action_type user_action_type NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE workspaces
    ADD CONSTRAINT fk_workspaces_current_draft
    FOREIGN KEY (current_draft_version_id)
    REFERENCES draft_versions(id)
    ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_materials_workspace_parse_stage
    ON materials(workspace_id, parse_stage);

CREATE INDEX IF NOT EXISTS idx_materials_workspace_created_at
    ON materials(workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_draft_versions_workspace_version
    ON draft_versions(workspace_id, version_no DESC);

CREATE INDEX IF NOT EXISTS idx_review_items_draft_impact
    ON review_items(draft_version_id, review_impact_level);

CREATE INDEX IF NOT EXISTS idx_review_recheck_logs_item_created
    ON review_recheck_logs(review_item_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_evidence_bindings_draft_paragraph
    ON evidence_bindings(draft_version_id, paragraph_id);

CREATE INDEX IF NOT EXISTS idx_evidence_bindings_material
    ON evidence_bindings(material_id);

CREATE INDEX IF NOT EXISTS idx_cowrite_previews_workspace_created
    ON cowrite_previews(workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cowrite_previews_draft
    ON cowrite_previews(draft_version_id);

CREATE INDEX IF NOT EXISTS idx_generation_jobs_workspace_status
    ON generation_jobs(workspace_id, status);

CREATE INDEX IF NOT EXISTS idx_requirement_snapshots_workspace_version
    ON requirement_snapshots(workspace_id, version DESC);

CREATE INDEX IF NOT EXISTS idx_material_sufficiency_workspace_created
    ON material_sufficiency_results(workspace_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_literature_candidates_workspace_duplicate
    ON literature_candidates(workspace_id, duplicate_group_key);

CREATE INDEX IF NOT EXISTS idx_literature_candidates_workspace_status
    ON literature_candidates(workspace_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_literature_candidates_material
    ON literature_candidates(material_id);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_workspace_created
    ON knowledge_chunks(workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_material
    ON knowledge_chunks(material_id);
