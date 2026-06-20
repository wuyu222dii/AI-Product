-- v1.6 可信交付增强版
-- 用于持久化“共写预览 -> 可能受影响审查项”的关联关系。

CREATE TABLE IF NOT EXISTS cowrite_preview_review_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cowrite_preview_id UUID NOT NULL REFERENCES cowrite_previews(id) ON DELETE CASCADE,
    review_item_id UUID NOT NULL REFERENCES review_items(id) ON DELETE CASCADE,
    relation_type TEXT NOT NULL DEFAULT 'MAY_ADDRESS',
    relation_reason TEXT NULL,
    recheck_prompted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cowrite_preview_review_links_preview
    ON cowrite_preview_review_links(cowrite_preview_id);

CREATE INDEX IF NOT EXISTS idx_cowrite_preview_review_links_review
    ON cowrite_preview_review_links(review_item_id);
