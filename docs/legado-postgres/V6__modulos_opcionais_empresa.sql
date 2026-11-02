ALTER TABLE empresas ADD COLUMN modulo_candidatos_habilitado boolean NOT NULL DEFAULT false;
ALTER TABLE empresas ADD COLUMN modulo_agenda_habilitado boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN empresas.modulo_candidatos_habilitado IS 'Libera o banco de candidatos para todos os usuários autorizados da empresa.';
COMMENT ON COLUMN empresas.modulo_agenda_habilitado IS 'Libera a agenda de atendimentos para todos os usuários autorizados da empresa.';

CREATE TABLE candidatos (
 id uuid PRIMARY KEY,
 empresa_id uuid NOT NULL REFERENCES empresas(id),
 nome_completo varchar(160) NOT NULL,
 email varchar(254) NOT NULL DEFAULT '',
 telefone varchar(40) NOT NULL DEFAULT '',
 cargo_pretendido varchar(160) NOT NULL DEFAULT '',
 url_curriculo varchar(2000) NOT NULL DEFAULT '',
 resumo_profissional varchar(10000) NOT NULL DEFAULT '',
 observacoes varchar(5000) NOT NULL DEFAULT '',
 situacao varchar(20) NOT NULL DEFAULT 'novo' CHECK (situacao IN ('novo','em_analise','aprovado','reprovado')),
 arquivado boolean NOT NULL DEFAULT false,
 revisao bigint NOT NULL DEFAULT 0,
 recebido_em timestamptz NOT NULL DEFAULT now(),
 atualizado_em timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX candidatos_empresa_data ON candidatos(empresa_id, recebido_em DESC);
COMMENT ON TABLE candidatos IS 'Dados e referências aos currículos recebidos por cada empresa, preservados ao desabilitar o módulo.';

CREATE TABLE agendamentos_atendimento (
 id uuid PRIMARY KEY,
 empresa_id uuid NOT NULL REFERENCES empresas(id),
 nome_pessoa varchar(160) NOT NULL,
 email varchar(254) NOT NULL DEFAULT '',
 telefone varchar(40) NOT NULL DEFAULT '',
 assunto varchar(200) NOT NULL,
 responsavel varchar(160) NOT NULL DEFAULT '',
 inicio timestamptz NOT NULL,
 fim timestamptz NOT NULL,
 situacao varchar(20) NOT NULL DEFAULT 'agendado' CHECK (situacao IN ('agendado','confirmado','concluido','cancelado')),
 observacoes varchar(5000) NOT NULL DEFAULT '',
 arquivado boolean NOT NULL DEFAULT false,
 revisao bigint NOT NULL DEFAULT 0,
 criado_em timestamptz NOT NULL DEFAULT now(),
 atualizado_em timestamptz NOT NULL DEFAULT now(),
 CONSTRAINT agendamento_intervalo_valido CHECK (fim > inicio)
);
CREATE INDEX agendamentos_empresa_inicio ON agendamentos_atendimento(empresa_id, inicio);
COMMENT ON TABLE agendamentos_atendimento IS 'Agendamentos por empresa, preservados ao desabilitar o módulo de agenda.';
