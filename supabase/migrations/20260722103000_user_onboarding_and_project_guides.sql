alter table public.user_profiles
    add column if not exists onboarding_status text,
    add column if not exists onboarding_version text,
    add column if not exists onboarding_completed_at timestamptz;

-- Profiles are normally created by the auth trigger. Backfill older auth accounts that never
-- reached the application before this migration so they are not mistaken for new registrations.
insert into public.user_profiles (
    id,
    display_name,
    avatar_url,
    onboarding_status,
    onboarding_version,
    onboarding_completed_at,
    created_at,
    updated_at
)
select
    auth_user.id,
    nullif(coalesce(auth_user.raw_user_meta_data ->> 'full_name', auth_user.raw_user_meta_data ->> 'name'), ''),
    nullif(coalesce(auth_user.raw_user_meta_data ->> 'avatar_url', auth_user.raw_user_meta_data ->> 'picture'), ''),
    'COMPLETED',
    'v1',
    now(),
    coalesce(auth_user.created_at, now()),
    now()
from auth.users auth_user
on conflict (id) do nothing;

-- Existing accounts keep their current experience. New accounts use the NOT_STARTED default.
update public.user_profiles
set onboarding_status = 'COMPLETED',
    onboarding_version = coalesce(onboarding_version, 'v1'),
    onboarding_completed_at = coalesce(onboarding_completed_at, now())
where onboarding_status is null;

alter table public.user_profiles
    alter column onboarding_status set default 'NOT_STARTED',
    alter column onboarding_status set not null;

alter table public.user_profiles
    drop constraint if exists user_profiles_onboarding_status_check;

alter table public.user_profiles
    add constraint user_profiles_onboarding_status_check check (
        onboarding_status in ('NOT_STARTED', 'COMPLETED', 'SKIPPED')
    );

create table if not exists public.project_guides (
    workspace_id uuid primary key references public.workspaces(id) on delete cascade,
    guide_version text not null default 'v1',
    current_progress text not null default 'IDEA_ONLY',
    available_materials jsonb not null default '[]'::jsonb,
    target_deadline date,
    preferred_mode text not null default 'FLEXIBLE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint project_guides_current_progress_check check (
        current_progress in ('IDEA_ONLY', 'TOPIC_DEFINED', 'MATERIALS_COLLECTING', 'WRITING', 'REVISING')
    ),
    constraint project_guides_preferred_mode_check check (
        preferred_mode in ('GUIDED', 'FLEXIBLE')
    ),
    constraint project_guides_available_materials_array_check check (
        jsonb_typeof(available_materials) = 'array'
    )
);

insert into public.project_guides (
    workspace_id,
    guide_version,
    current_progress,
    available_materials,
    preferred_mode
)
select
    workspace.id,
    'v1',
    'IDEA_ONLY',
    '[]'::jsonb,
    'FLEXIBLE'
from public.workspaces workspace
on conflict (workspace_id) do nothing;

alter table public.project_guides enable row level security;
revoke all privileges on table public.project_guides from anon, authenticated;
