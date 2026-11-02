ALTER TABLE usuarios ADD COLUMN superusuario boolean NOT NULL DEFAULT false;
COMMENT ON COLUMN usuarios.superusuario IS 'Permite administrar o prompt global e consultar métricas de todas as empresas.';
CREATE UNIQUE INDEX um_superusuario_do_sistema ON usuarios(superusuario) WHERE superusuario;

CREATE TABLE configuracoes_globais (
 id smallint PRIMARY KEY CHECK (id = 1),
 prompt_sistema text NOT NULL DEFAULT '',
 complementar_configuracoes_empresa boolean NOT NULL DEFAULT true,
 revisao bigint NOT NULL DEFAULT 0,
 atualizado_por uuid REFERENCES usuarios(id),
 atualizado_em timestamptz NOT NULL DEFAULT now()
);
INSERT INTO configuracoes_globais(id) VALUES (1);
COMMENT ON TABLE configuracoes_globais IS 'Prompt de sistema e regras gerais compartilhadas pelos assistentes.';

CREATE TABLE historico_prompts_globais (
 id uuid PRIMARY KEY,
 prompt_sistema text NOT NULL,
 complementar_configuracoes_empresa boolean NOT NULL,
 revisao bigint NOT NULL UNIQUE,
 usuario_responsavel_id uuid NOT NULL REFERENCES usuarios(id),
 criado_em timestamptz NOT NULL DEFAULT now()
);
COMMENT ON TABLE historico_prompts_globais IS 'Versões e autoria das alterações no prompt global.';

CREATE TABLE eventos_mensagens_atendimento (
 id uuid PRIMARY KEY,
 empresa_id uuid NOT NULL REFERENCES empresas(id),
 origem varchar(80) NOT NULL,
 id_evento_origem varchar(160) NOT NULL,
 direcao varchar(10) NOT NULL CHECK (direcao IN ('recebida', 'enviada')),
 resultado varchar(10) NOT NULL CHECK (resultado IN ('sucesso', 'erro')),
 ocorrido_em timestamptz NOT NULL,
 registrado_em timestamptz NOT NULL DEFAULT now(),
 UNIQUE (origem, id_evento_origem)
);
CREATE INDEX eventos_mensagens_atendimento_ocorrido_em ON eventos_mensagens_atendimento(ocorrido_em);
CREATE INDEX eventos_mensagens_atendimento_empresa_data ON eventos_mensagens_atendimento(empresa_id, ocorrido_em);
COMMENT ON TABLE eventos_mensagens_atendimento IS 'Eventos de mensagens para métricas, sem armazenar o conteúdo das conversas.';
