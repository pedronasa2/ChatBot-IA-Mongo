CREATE TABLE users (
 id uuid PRIMARY KEY, email varchar(254) NOT NULL UNIQUE,
 password_hash varchar(100) NOT NULL, enabled boolean NOT NULL DEFAULT true,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE companies (
 id uuid PRIMARY KEY, name varchar(160) NOT NULL,
 description text NOT NULL DEFAULT '', address varchar(500) NOT NULL DEFAULT '',
 contact varchar(300) NOT NULL DEFAULT '', website varchar(500) NOT NULL DEFAULT '',
 services text NOT NULL DEFAULT '', timezone varchar(80) NOT NULL DEFAULT 'America/Sao_Paulo',
 bot_enabled boolean NOT NULL DEFAULT false, revision bigint NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE company_memberships (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id), company_id uuid NOT NULL REFERENCES companies(id),
 role varchar(10) NOT NULL CHECK (role IN ('owner','editor')),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE (user_id,company_id)
);
CREATE TABLE bot_configuration_versions (
 id uuid PRIMARY KEY, company_id uuid NOT NULL REFERENCES companies(id), version integer NOT NULL,
 state varchar(12) NOT NULL CHECK (state IN ('draft','published')),
 bot_name varchar(80) NOT NULL DEFAULT 'Assistente', tone varchar(30) NOT NULL DEFAULT 'cordial',
 emojis boolean NOT NULL DEFAULT false, instructions text NOT NULL DEFAULT '', handoff_instructions text NOT NULL DEFAULT '',
 author_id uuid NOT NULL REFERENCES users(id), published_at timestamptz,
 revision bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now(),
 UNIQUE(company_id,version)
);
CREATE UNIQUE INDEX one_draft_per_company ON bot_configuration_versions(company_id) WHERE state='draft';
CREATE TABLE audit_events (
 id uuid PRIMARY KEY, company_id uuid NOT NULL REFERENCES companies(id), actor_id uuid NOT NULL REFERENCES users(id),
 configuration_id uuid REFERENCES bot_configuration_versions(id), action varchar(60) NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
