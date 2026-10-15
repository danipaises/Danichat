#!/usr/bin/env python3
"""Exercise the real migration in isolated PostgreSQL 17, with synthetic auth claims.

No host ports, external database, credentials, real users, or production data.
This verifies PostgreSQL grants/RLS, not the remote Supabase Auth service.
"""
import pathlib
import secrets
import subprocess
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]
IMAGE = "postgres:17-alpine@sha256:b0f9560a2de083e2cc7382e75f808c7381a32852a7ec49117deedb300e552b24"


def run(*args, input=None):
    result = subprocess.run(args, input=input, text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
    return result.stdout


def main():
    name = "danichat-rls-" + secrets.token_hex(6)
    run("docker", "run", "--detach", "--name", name, "--network", "none",
        "--env", "POSTGRES_HOST_AUTH_METHOD=trust", IMAGE)
    try:
        deadline = time.monotonic() + 40
        while subprocess.run(["docker", "exec", name, "pg_isready", "-U", "postgres"],
                             capture_output=True).returncode:
            if time.monotonic() >= deadline:
                raise RuntimeError("Disposable PostgreSQL did not become ready")
            time.sleep(0.25)

        # Supabase's auth schema is represented only by auth.users and auth.uid().
        # The grants, trigger, RLS and constraints under test are the real migration.
        bootstrap = """
create role anon nologin;
create role authenticated nologin;
create schema auth;
create table auth.users (id uuid primary key);
create function auth.uid() returns uuid language sql stable as $$
    select (nullif(current_setting('request.jwt.claims', true), '')::jsonb ->> 'sub')::uuid
$$;
grant usage on schema public, auth to anon, authenticated;
grant execute on function auth.uid() to anon, authenticated;
"""
        migrations = "\n".join(p.read_text() for p in sorted((ROOT / "supabase/migrations").glob("*.sql")))
        checks = (ROOT / "supabase/tests/profile_rls.sql").read_text()
        run("docker", "exec", "--interactive", name, "psql", "-U", "postgres",
            "--no-psqlrc", "--set", "ON_ERROR_STOP=1", input=bootstrap + migrations + checks)
        print("PASS: migration, own-profile upsert, anonymous denial, cross-user isolation, "
              "reassignment denial, immutable timestamps, limits and missing-subject denial")
    finally:
        run("docker", "rm", "--force", "--volumes", name)


if __name__ == "__main__":
    main()
