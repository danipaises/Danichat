-- Run only in the disposable database created by tools/danichat/test_profile_rls.py.
-- Every row is synthetic; the transaction rolls back even on success.
begin;

insert into auth.users (id) values
    ('00000000-0000-4000-8000-000000000001'),
    ('00000000-0000-4000-8000-000000000002');

set local role anon;
do $$ begin
    perform * from public.danichat_profiles;
    raise exception 'anonymous SELECT must be denied';
exception when insufficient_privilege then null;
end $$;
reset role;

set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000001"}', true);
insert into public.danichat_profiles (user_id, display_name, bio)
values ('00000000-0000-4000-8000-000000000001', 'Perfil de teste A', 'Texto sintético');

-- The actual Android API uses a PostgREST upsert.
insert into public.danichat_profiles (user_id, display_name, bio)
values ('00000000-0000-4000-8000-000000000001', 'Perfil A atualizado', '')
on conflict (user_id) do update
set user_id = excluded.user_id, display_name = excluded.display_name, bio = excluded.bio;

do $$ begin
    if (select display_name from public.danichat_profiles) <> 'Perfil A atualizado' then
        raise exception 'owner upsert must succeed';
    end if;
    begin
        insert into public.danichat_profiles (user_id, display_name)
        values ('00000000-0000-4000-8000-000000000002', 'Proibido');
        raise exception 'foreign INSERT must be denied';
    exception when insufficient_privilege then null;
    end;
    begin
        update public.danichat_profiles set user_id = '00000000-0000-4000-8000-000000000002';
        raise exception 'owner reassignment must be denied';
    exception when insufficient_privilege then null;
    end;
    begin
        update public.danichat_profiles set created_at = '2000-01-01';
        raise exception 'client timestamp mutation must be denied';
    exception when insufficient_privilege then null;
    end;
    begin
        update public.danichat_profiles set display_name = repeat('a', 81);
        raise exception 'oversize name must be denied';
    exception when check_violation then null;
    end;
    begin
        update public.danichat_profiles set bio = repeat('a', 501);
        raise exception 'oversize bio must be denied';
    exception when check_violation then null;
    end;
end $$;

select set_config('request.jwt.claims', '{"sub":"00000000-0000-4000-8000-000000000002"}', true);
insert into public.danichat_profiles (user_id, display_name)
values ('00000000-0000-4000-8000-000000000002', 'Perfil de teste B');

do $$ declare changed integer; begin
    if (select count(*) from public.danichat_profiles) <> 1 then
        raise exception 'foreign profile must be invisible';
    end if;
    if exists(select 1 from public.danichat_profiles where user_id = '00000000-0000-4000-8000-000000000001') then
        raise exception 'filter by known foreign ID must not bypass RLS';
    end if;
    update public.danichat_profiles set display_name = 'Proibido'
    where user_id = '00000000-0000-4000-8000-000000000001';
    get diagnostics changed = row_count;
    if changed <> 0 then raise exception 'foreign UPDATE must affect zero rows'; end if;
end $$;

-- An authenticated role without an authenticated subject has no rows.
select set_config('request.jwt.claims', '{}', true);
do $$ begin
    if exists(select 1 from public.danichat_profiles) then raise exception 'subject is mandatory'; end if;
    begin
        insert into public.danichat_profiles (user_id, display_name)
        values ('00000000-0000-4000-8000-000000000001', 'Proibido');
        raise exception 'INSERT without subject must fail';
    exception when insufficient_privilege then null;
    end;
end $$;
reset role;

do $$ begin
    if (select display_name from public.danichat_profiles
        where user_id = '00000000-0000-4000-8000-000000000001') <> 'Perfil A atualizado' then
        raise exception 'original profile must remain intact';
    end if;
    if exists(select 1 from pg_proc p join pg_namespace n on n.oid = p.pronamespace
        where n.nspname = 'public' and p.proname like 'danichat_%' and p.prosecdef) then
        raise exception 'no privileged DaniChat functions are expected';
    end if;
end $$;
rollback;
