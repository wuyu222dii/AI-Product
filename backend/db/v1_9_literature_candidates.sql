-- v1.9 文献补充增强：候选文献待下载清单

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

CREATE UNIQUE INDEX IF NOT EXISTS idx_literature_candidates_workspace_duplicate
    ON literature_candidates(workspace_id, duplicate_group_key);

CREATE INDEX IF NOT EXISTS idx_literature_candidates_workspace_status
    ON literature_candidates(workspace_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_literature_candidates_material
    ON literature_candidates(material_id);

-- Supabase public schema defense-in-depth: backend JPA uses the database connection,
-- while direct Data API access remains blocked unless explicit policies are added later.
ALTER TABLE literature_candidates ENABLE ROW LEVEL SECURITY;
