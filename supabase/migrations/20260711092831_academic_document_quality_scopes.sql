alter table public.evidence_bindings
    add column if not exists scope_type text not null default 'LEGACY_DRAFT',
    add column if not exists document_id uuid references public.academic_documents(id) on delete cascade,
    add column if not exists section_id uuid references public.document_sections(id) on delete cascade,
    add column if not exists section_version_no integer,
    add column if not exists paragraph_fingerprint text;

alter table public.evidence_bindings
    alter column draft_version_id drop not null;

alter table public.evidence_bindings
    drop constraint if exists evidence_bindings_scope_check;

alter table public.evidence_bindings
    add constraint evidence_bindings_scope_check check (
        (scope_type = 'LEGACY_DRAFT'
            and draft_version_id is not null
            and document_id is null
            and section_id is null
            and section_version_no is null)
        or
        (scope_type = 'SECTION'
            and draft_version_id is null
            and document_id is not null
            and section_id is not null
            and section_version_no is not null)
    );

create index if not exists idx_evidence_bindings_section_version_paragraph
    on public.evidence_bindings(section_id, section_version_no, paragraph_id)
    where scope_type = 'SECTION';
create index if not exists idx_evidence_bindings_document_section
    on public.evidence_bindings(document_id, section_id)
    where scope_type = 'SECTION';
create index if not exists idx_evidence_bindings_paragraph_fingerprint
    on public.evidence_bindings(section_id, paragraph_fingerprint, material_id)
    where scope_type = 'SECTION' and paragraph_fingerprint is not null;

alter table public.review_items
    add column if not exists scope_type text not null default 'LEGACY_DRAFT',
    add column if not exists document_id uuid references public.academic_documents(id) on delete cascade,
    add column if not exists section_id uuid references public.document_sections(id) on delete cascade,
    add column if not exists section_version_no integer,
    add column if not exists issue_fingerprint text;

alter table public.review_items
    alter column draft_version_id drop not null;

alter table public.review_items
    drop constraint if exists review_items_scope_check;

alter table public.review_items
    add constraint review_items_scope_check check (
        (scope_type = 'LEGACY_DRAFT'
            and draft_version_id is not null
            and document_id is null
            and section_id is null)
        or
        (scope_type = 'SECTION'
            and draft_version_id is null
            and document_id is not null
            and section_id is not null
            and section_version_no is not null)
        or
        (scope_type = 'DOCUMENT'
            and draft_version_id is null
            and document_id is not null
            and section_id is null)
    );

alter table public.review_items
    drop constraint if exists review_items_status_check;

alter table public.review_items
    add constraint review_items_status_check check (
        review_status in ('OPEN', 'MODIFIED_PENDING_RECHECK', 'RESOLVED', 'IGNORED', 'SUPERSEDED')
    );

create index if not exists idx_review_items_document_status_created
    on public.review_items(document_id, review_status, created_at desc)
    where document_id is not null;
create index if not exists idx_review_items_section_status_created
    on public.review_items(section_id, review_status, created_at desc)
    where section_id is not null;
create index if not exists idx_review_items_scope_fingerprint
    on public.review_items(scope_type, document_id, section_id, issue_fingerprint)
    where issue_fingerprint is not null;

alter table public.review_recheck_logs
    add column if not exists scope_type text not null default 'LEGACY_DRAFT',
    add column if not exists document_id uuid references public.academic_documents(id) on delete cascade,
    add column if not exists section_id uuid references public.document_sections(id) on delete cascade,
    add column if not exists section_version_no integer;

alter table public.review_recheck_logs
    alter column draft_version_id drop not null;

create index if not exists idx_review_recheck_logs_section_version
    on public.review_recheck_logs(section_id, section_version_no, created_at desc)
    where section_id is not null;

alter table public.section_cowrite_previews
    add column if not exists base_content text not null default '',
    add column if not exists target_range_json jsonb not null default '{}'::jsonb,
    add column if not exists diff_rows_json jsonb not null default '[]'::jsonb,
    add column if not exists paragraph_diff_rows_json jsonb not null default '[]'::jsonb;

create table if not exists public.section_cowrite_preview_review_links (
    id uuid primary key default gen_random_uuid(),
    section_cowrite_preview_id uuid not null references public.section_cowrite_previews(id) on delete cascade,
    review_item_id uuid not null references public.review_items(id) on delete cascade,
    relation_type text not null default 'OVERLAPS',
    relation_reason text,
    recheck_prompted boolean not null default false,
    created_at timestamptz not null default now(),
    constraint uk_section_cowrite_preview_review_link unique (section_cowrite_preview_id, review_item_id)
);

create index if not exists idx_section_cowrite_preview_review_links_preview
    on public.section_cowrite_preview_review_links(section_cowrite_preview_id);
create index if not exists idx_section_cowrite_preview_review_links_review
    on public.section_cowrite_preview_review_links(review_item_id);

alter table public.section_cowrite_preview_review_links enable row level security;
revoke all on table public.section_cowrite_preview_review_links from anon, authenticated;
