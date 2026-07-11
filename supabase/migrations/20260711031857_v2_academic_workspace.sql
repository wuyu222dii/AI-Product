create extension if not exists "pgcrypto";

alter table public.workspaces
    add column if not exists active_document_id uuid;

alter table public.ai_semantic_parse_results
    add column if not exists material_role text not null default 'UNKNOWN',
    add column if not exists research_artifact_type text not null default 'NONE',
    add column if not exists material_tags_json text not null default '[]';

create table if not exists public.academic_project_profiles (
    workspace_id uuid primary key references public.workspaces(id) on delete cascade,
    academic_stage text not null default 'UNDERGRADUATE',
    discipline_group text not null default 'INTERDISCIPLINARY',
    research_paradigm text not null default 'OTHER',
    primary_language text not null default 'zh-CN',
    default_citation_style text not null default 'APA',
    institution text,
    ai_usage_policy text not null default 'EVIDENCE_GROUNDED_DRAFTING',
    ai_policy_json jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint academic_project_profiles_stage_check check (academic_stage in ('UNDERGRADUATE', 'MASTER', 'DOCTORAL', 'RESEARCHER')),
    constraint academic_project_profiles_discipline_check check (discipline_group in ('STEM', 'MEDICINE_HEALTH', 'SOCIAL_SCIENCE', 'HUMANITIES', 'BUSINESS_LAW', 'INTERDISCIPLINARY')),
    constraint academic_project_profiles_paradigm_check check (research_paradigm in ('QUANTITATIVE', 'QUALITATIVE', 'MIXED_METHODS', 'EXPERIMENTAL', 'COMPUTATIONAL', 'DESIGN_SCIENCE', 'THEORETICAL', 'SYSTEMATIC_REVIEW', 'OTHER')),
    constraint academic_project_profiles_ai_policy_check check (ai_usage_policy in ('GUIDANCE_ONLY', 'EVIDENCE_GROUNDED_DRAFTING', 'FULL_DRAFTING_ALLOWED'))
);

create table if not exists public.academic_documents (
    id uuid primary key default gen_random_uuid(),
    workspace_id uuid not null references public.workspaces(id) on delete cascade,
    document_type text not null,
    title text not null,
    status text not null default 'PLANNING',
    target_institution text,
    target_venue text,
    target_length integer,
    length_unit text not null default 'WORDS',
    citation_style text not null default 'APA',
    requirement_profile_json jsonb not null default '{}'::jsonb,
    primary_document boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint academic_documents_type_check check (document_type in ('COURSE_PAPER', 'RESEARCH_PROPOSAL', 'UNDERGRADUATE_THESIS', 'MASTER_THESIS', 'DOCTORAL_DISSERTATION', 'JOURNAL_ARTICLE', 'CONFERENCE_PAPER', 'LITERATURE_REVIEW', 'RESEARCH_REPORT')),
    constraint academic_documents_status_check check (status in ('PLANNING', 'WRITING', 'REVIEWING', 'READY', 'ARCHIVED')),
    constraint academic_documents_length_unit_check check (length_unit in ('WORDS', 'CHARACTERS')),
    constraint academic_documents_target_length_check check (target_length is null or target_length between 100 and 300000)
);

create unique index if not exists uk_academic_documents_primary
    on public.academic_documents(workspace_id)
    where primary_document;
create index if not exists idx_academic_documents_workspace_status_updated
    on public.academic_documents(workspace_id, status, updated_at desc);
create index if not exists idx_academic_documents_workspace_type
    on public.academic_documents(workspace_id, document_type);

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'workspaces_active_document_id_fkey'
          and conrelid = 'public.workspaces'::regclass
    ) then
        alter table public.workspaces
            add constraint workspaces_active_document_id_fkey
            foreign key (active_document_id) references public.academic_documents(id) on delete set null;
    end if;
end $$;

create index if not exists idx_workspaces_active_document
    on public.workspaces(active_document_id);

create table if not exists public.document_sections (
    id uuid primary key default gen_random_uuid(),
    document_id uuid not null references public.academic_documents(id) on delete cascade,
    parent_section_id uuid references public.document_sections(id) on delete cascade,
    sort_order integer not null default 0,
    section_type text not null,
    title text not null,
    content text not null default '',
    target_length integer,
    status text not null default 'EMPTY',
    source_trace_map_json jsonb not null default '{}'::jsonb,
    version_no integer not null default 1,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint document_sections_status_check check (status in ('EMPTY', 'DRAFTING', 'REVIEWING', 'READY')),
    constraint document_sections_sort_order_check check (sort_order >= 0),
    constraint document_sections_version_check check (version_no >= 1)
);

create index if not exists idx_document_sections_tree
    on public.document_sections(document_id, parent_section_id, sort_order);
create index if not exists idx_document_sections_status
    on public.document_sections(document_id, status);
create index if not exists idx_document_sections_parent
    on public.document_sections(parent_section_id);

alter table public.document_sections
    add column if not exists search_vector tsvector generated always as (
        to_tsvector('simple', coalesce(title, '') || ' ' || coalesce(content, ''))
    ) stored;

create index if not exists idx_document_sections_search_vector
    on public.document_sections using gin(search_vector);

create table if not exists public.document_section_versions (
    id uuid primary key default gen_random_uuid(),
    section_id uuid not null references public.document_sections(id) on delete cascade,
    version_no integer not null,
    title text not null,
    content text not null default '',
    source_trace_map_json jsonb not null default '{}'::jsonb,
    change_source text not null,
    change_summary text,
    created_at timestamptz not null default now(),
    constraint document_section_versions_version_check check (version_no >= 1),
    constraint uk_document_section_version unique (section_id, version_no)
);

create index if not exists idx_document_section_versions_latest
    on public.document_section_versions(section_id, version_no desc);

create table if not exists public.document_material_links (
    id uuid primary key default gen_random_uuid(),
    document_id uuid not null references public.academic_documents(id) on delete cascade,
    material_id uuid not null references public.materials(id) on delete cascade,
    role text not null default 'SUPPORTING',
    included boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint uk_document_material_link unique (document_id, material_id)
);

create index if not exists idx_document_material_links_document
    on public.document_material_links(document_id, included);
create index if not exists idx_document_material_links_material
    on public.document_material_links(material_id);

create table if not exists public.ai_action_logs (
    id uuid primary key default gen_random_uuid(),
    workspace_id uuid not null references public.workspaces(id) on delete cascade,
    document_id uuid references public.academic_documents(id) on delete cascade,
    section_id uuid references public.document_sections(id) on delete cascade,
    action_type text not null,
    model_name text,
    evidence_material_ids_json jsonb not null default '[]'::jsonb,
    request_summary text,
    output_summary text,
    accepted boolean,
    disclosure_required boolean not null default true,
    created_at timestamptz not null default now()
);

create index if not exists idx_ai_action_logs_document_created
    on public.ai_action_logs(document_id, created_at desc);
create index if not exists idx_ai_action_logs_workspace_created
    on public.ai_action_logs(workspace_id, created_at desc);
create index if not exists idx_ai_action_logs_section
    on public.ai_action_logs(section_id);

create table if not exists public.section_cowrite_previews (
    id uuid primary key default gen_random_uuid(),
    section_id uuid not null references public.document_sections(id) on delete cascade,
    base_version_no integer not null,
    action text not null,
    instruction text,
    controls_json jsonb not null default '{}'::jsonb,
    candidate_content text not null,
    candidate_source_trace_map_json jsonb not null default '{}'::jsonb,
    diff_summary_json jsonb not null default '{}'::jsonb,
    status text not null default 'READY',
    ai_action_log_id uuid references public.ai_action_logs(id) on delete set null,
    created_at timestamptz not null default now(),
    applied_at timestamptz,
    constraint section_cowrite_previews_status_check check (status in ('READY', 'APPLIED', 'DISCARDED'))
);

create index if not exists idx_section_cowrite_previews_section_created
    on public.section_cowrite_previews(section_id, created_at desc);
create index if not exists idx_section_cowrite_previews_ai_log
    on public.section_cowrite_previews(ai_action_log_id);

alter table public.requirement_snapshots
    add column if not exists document_id uuid,
    add column if not exists source_type text not null default 'PROJECT';

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'requirement_snapshots_document_id_fkey'
          and conrelid = 'public.requirement_snapshots'::regclass
    ) then
        alter table public.requirement_snapshots
            add constraint requirement_snapshots_document_id_fkey
            foreign key (document_id) references public.academic_documents(id) on delete cascade;
    end if;
end $$;

create index if not exists idx_requirement_snapshots_document_version
    on public.requirement_snapshots(document_id, version desc);

alter table public.draft_versions
    add column if not exists document_id uuid;

do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'draft_versions_document_id_fkey'
          and conrelid = 'public.draft_versions'::regclass
    ) then
        alter table public.draft_versions
            add constraint draft_versions_document_id_fkey
            foreign key (document_id) references public.academic_documents(id) on delete set null;
    end if;
end $$;

create index if not exists idx_draft_versions_document_version
    on public.draft_versions(document_id, version_no desc);

alter table public.knowledge_chunks
    add column if not exists search_vector tsvector generated always as (
        to_tsvector('simple', coalesce(chunk_text, '') || ' ' || coalesce(source_excerpt, ''))
    ) stored;

create index if not exists idx_knowledge_chunks_search_vector
    on public.knowledge_chunks using gin(search_vector);

insert into public.academic_project_profiles (
    workspace_id,
    academic_stage,
    discipline_group,
    research_paradigm,
    primary_language,
    default_citation_style,
    ai_usage_policy,
    ai_policy_json
)
select id, 'UNDERGRADUATE', 'INTERDISCIPLINARY', 'OTHER', 'zh-CN', 'APA', 'EVIDENCE_GROUNDED_DRAFTING',
       '{"humanReviewRequired":true,"disclosureRequired":true}'::jsonb
from public.workspaces
on conflict (workspace_id) do nothing;

insert into public.academic_documents (
    id,
    workspace_id,
    document_type,
    title,
    status,
    target_length,
    length_unit,
    citation_style,
    requirement_profile_json,
    primary_document
)
select gen_random_uuid(), w.id, 'COURSE_PAPER', w.title,
       case when w.status::text in ('READY', 'ready') then 'WRITING' else 'PLANNING' end,
       3000, 'WORDS', 'APA', '{}'::jsonb, true
from public.workspaces w
where not exists (
    select 1 from public.academic_documents d where d.workspace_id = w.id
);

update public.workspaces w
set active_document_id = d.id
from public.academic_documents d
where d.workspace_id = w.id
  and d.primary_document = true
  and w.active_document_id is null;

insert into public.document_sections (
    id,
    document_id,
    sort_order,
    section_type,
    title,
    content,
    target_length,
    status,
    source_trace_map_json,
    version_no
)
select gen_random_uuid(), d.id, 0, 'LEGACY_FULL_TEXT', '正文', coalesce(v.draft_text, ''), d.target_length,
       case when coalesce(v.draft_text, '') = '' then 'EMPTY' else 'DRAFTING' end,
       coalesce(v.source_trace_map_json::jsonb, '{}'::jsonb), 1
from public.academic_documents d
join public.workspaces w on w.id = d.workspace_id
left join public.draft_versions v on v.id = w.current_draft_version_id
where d.primary_document = true
  and not exists (select 1 from public.document_sections s where s.document_id = d.id);

insert into public.document_section_versions (
    id,
    section_id,
    version_no,
    title,
    content,
    source_trace_map_json,
    change_source,
    change_summary
)
select gen_random_uuid(), s.id, s.version_no, s.title, s.content, s.source_trace_map_json, 'MIGRATION', '从旧版整篇草稿迁移'
from public.document_sections s
where not exists (
    select 1 from public.document_section_versions v
    where v.section_id = s.id and v.version_no = s.version_no
);

alter table public.academic_project_profiles enable row level security;
alter table public.academic_documents enable row level security;
alter table public.document_sections enable row level security;
alter table public.document_section_versions enable row level security;
alter table public.document_material_links enable row level security;
alter table public.ai_action_logs enable row level security;
alter table public.section_cowrite_previews enable row level security;
