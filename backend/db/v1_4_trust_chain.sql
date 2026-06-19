-- v1.4 可信链优先深挖计划
-- 作用：补齐段落级证据绑定、共写预览、审查手动复查所需表结构。

ALTER TABLE evidence_bindings
    ADD COLUMN IF NOT EXISTS paragraph_id TEXT NOT NULL DEFAULT 'p1',
    ADD COLUMN IF NOT EXISTS knowledge_chunk_id UUID NULL REFERENCES knowledge_chunks(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS support_type TEXT NOT NULL DEFAULT 'SOURCE_TRACE',
    ADD COLUMN IF NOT EXISTS binding_status TEXT NOT NULL DEFAULT 'WEAK',
    ADD COLUMN IF NOT EXISTS citation_text TEXT NULL;

ALTER TABLE evidence_bindings
    ALTER COLUMN material_id DROP NOT NULL;

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

ALTER TABLE review_items
    ADD COLUMN IF NOT EXISTS last_rechecked_at TIMESTAMPTZ NULL,
    ADD COLUMN IF NOT EXISTS recheck_note TEXT NULL;

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

CREATE INDEX IF NOT EXISTS idx_evidence_bindings_draft_paragraph
    ON evidence_bindings(draft_version_id, paragraph_id);

CREATE INDEX IF NOT EXISTS idx_evidence_bindings_material
    ON evidence_bindings(material_id);

CREATE INDEX IF NOT EXISTS idx_cowrite_previews_workspace_created
    ON cowrite_previews(workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_cowrite_previews_draft
    ON cowrite_previews(draft_version_id);

CREATE INDEX IF NOT EXISTS idx_review_recheck_logs_item_created
    ON review_recheck_logs(review_item_id, created_at DESC);
