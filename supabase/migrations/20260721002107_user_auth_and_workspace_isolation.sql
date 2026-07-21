create table if not exists public.user_profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    display_name text,
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint user_profiles_display_name_length check (
        display_name is null or char_length(display_name) between 1 and 80
    )
);

alter table public.workspaces
    add column if not exists legacy_unowned boolean not null default false;

alter table public.workspaces
    alter column user_id drop not null;

-- Preserve pre-v2.1 demo data for local regression without exposing it to users.
update public.workspaces
set user_id = null,
    legacy_unowned = true
where user_id = '00000000-0000-0000-0000-000000000001'::uuid;

alter table public.workspaces
    drop constraint if exists workspaces_user_id_fkey;

alter table public.workspaces
    add constraint workspaces_user_id_fkey
    foreign key (user_id) references auth.users(id) on delete cascade;

alter table public.workspaces
    drop constraint if exists workspaces_owner_state_check;

alter table public.workspaces
    add constraint workspaces_owner_state_check check (
        (legacy_unowned = true and user_id is null)
        or
        (legacy_unowned = false and user_id is not null)
    );

create index if not exists idx_workspaces_user_updated_at
    on public.workspaces(user_id, updated_at desc)
    where user_id is not null;

create or replace function public.handle_auth_user_profile()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.user_profiles (id, display_name, avatar_url)
    values (
        new.id,
        nullif(coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name'), ''),
        nullif(coalesce(new.raw_user_meta_data ->> 'avatar_url', new.raw_user_meta_data ->> 'picture'), '')
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created_profile on auth.users;
create trigger on_auth_user_created_profile
    after insert on auth.users
    for each row execute function public.handle_auth_user_profile();

alter table public.user_profiles enable row level security;

-- Auth stays browser-facing, but every business table is accessed through Spring.
do $$
declare
    table_record record;
begin
    for table_record in
        select schemaname, tablename
        from pg_tables
        where schemaname = 'public'
    loop
        execute format(
            'revoke all privileges on table %I.%I from anon, authenticated',
            table_record.schemaname,
            table_record.tablename
        );
    end loop;
end
$$;

revoke all on function public.handle_auth_user_profile() from public, anon, authenticated;
