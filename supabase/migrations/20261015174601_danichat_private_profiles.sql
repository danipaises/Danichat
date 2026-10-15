-- Initial online account data only. No phone numbers, contacts, keys or messages.
create table public.danichat_profiles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    display_name text not null check (
        char_length(btrim(display_name)) between 1 and 80
        and char_length(display_name) <= 80
    ),
    bio text not null default '' check (char_length(bio) <= 500),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

alter table public.danichat_profiles enable row level security;
alter table public.danichat_profiles force row level security;

revoke all on table public.danichat_profiles from public, anon, authenticated;
grant select on table public.danichat_profiles to authenticated;
grant insert (user_id, display_name, bio) on public.danichat_profiles to authenticated;
grant update (user_id, display_name, bio) on public.danichat_profiles to authenticated;

create policy danichat_profile_select_own on public.danichat_profiles
    for select to authenticated
    using ((select auth.uid()) = user_id);

create policy danichat_profile_insert_own on public.danichat_profiles
    for insert to authenticated
    with check ((select auth.uid()) = user_id);

create policy danichat_profile_update_own on public.danichat_profiles
    for update to authenticated
    using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

create function public.danichat_touch_profile_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.updated_at = pg_catalog.now();
    return new;
end;
$$;

revoke all on function public.danichat_touch_profile_updated_at() from public, anon, authenticated;

create trigger danichat_profile_updated_at
before update on public.danichat_profiles
for each row execute function public.danichat_touch_profile_updated_at();

comment on table public.danichat_profiles is
    'Opt-in DaniChat account profile. Private to its owner; not a public contact directory.';
