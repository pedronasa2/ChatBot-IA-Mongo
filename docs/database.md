# Tabelas do banco

Banco da aplicação: `postgres` no servidor `189.126.111.15:5432`.

| Tabela | Finalidade |
| --- | --- |
| `usuarios` | Contas autorizadas a acessar o painel |
| `empresas` | Informações dos negócios atendidos |
| `vinculos_usuarios_empresas` | Acesso e papel de cada usuário por empresa |
| `versoes_configuracoes_bot` | Rascunhos e versões publicadas das configurações do bot |
| `eventos_auditoria` | Histórico de ações nas configurações |
| `spring_session` | Sessões persistentes gerenciadas pelo Spring Session |
| `spring_session_attributes` | Atributos das sessões persistentes |
| `flyway_schema_history` | Histórico técnico das migrações do Flyway |

A migração V3 renomeia as cinco tabelas de negócio e adiciona descrições no catálogo PostgreSQL. Colunas, dados e relacionamentos são preservados. As tabelas técnicas mantêm os nomes padrão de suas ferramentas. Migrações V1 e V2 permanecem intactas para preservar seus checksums.

Reinicie o backend com o código atualizado após aplicar V3. Backup anterior à alteração: `infra/backups/2026-10-06-transfer/before-table-renames.dump`.

## Atributos e colunas de negócio

As entidades Java são `Empresa` e `ConfiguracaoBot`, nos pacotes `empresas` e `configuracoes`. Controladores, repositórios, classes de autenticação e formulários Angular usam nomes em português. Os atributos JSON seguem camelCase; as colunas PostgreSQL seguem snake_case.

| Entidade/tabela | Atributos ou colunas |
| --- | --- |
| `Empresa` / `empresas` | `nome`, `descricao`, `endereco`, `contato`, `site`, `servicos`, `fusoHorario` → `fuso_horario`, `botAtivo` → `bot_ativo`, `revisao` |
| `ConfiguracaoBot` / `versoes_configuracoes_bot` | `empresaId` → `empresa_id`, `versao`, `estado`, `nomeBot` → `nome_bot`, `tomVoz` → `tom_voz`, `usarEmojis` → `usar_emojis`, `instrucoes`, `instrucoesTransferencia` → `instrucoes_transferencia`, `autorId` → `autor_id`, `publicadoEm` → `publicado_em`, `revisao` |
| `usuarios` | `id`, `email`, `hash_senha`, `ativo`, `criado_em`, `atualizado_em` |
| `vinculos_usuarios_empresas` | `id`, `usuario_id`, `empresa_id`, `papel`, `criado_em`, `atualizado_em` |
| `eventos_auditoria` | `id`, `empresa_id`, `usuario_responsavel_id`, `configuracao_bot_id`, `acao`, `criado_em` |

Datas comuns às entidades usam `criadoEm`/`criado_em` e `atualizadoEm`/`atualizado_em`. Estados: `rascunho`, `publicado`. Papéis: `proprietario`, `editor`. A migração V4 renomeia colunas e converte esses valores sem recriar registros. A senha continua armazenada como hash BCrypt.

Rotas do negócio: `/api/empresas` e `/api/empresas/{id}/rascunho`. Login recebe `email` e `senha`. Nomes exigidos por bibliotecas (JPA, Spring, Angular, Spring Session e Flyway) mantêm seus contratos técnicos. A classe principal para execução pela IDE agora é `br.com.painel.Aplicacao`.

## Administração e métricas

A migração V5 adiciona `usuarios.superusuario`, com limite de um superusuário, e as tabelas `configuracoes_globais`, `historico_prompts_globais` e `eventos_mensagens_atendimento`. A configuração global é única; cada alteração gera uma versão com autoria. Eventos guardam empresa, origem, ID estável, direção, resultado e datas; `origem` + `id_evento_origem` impedem duplicação. Conteúdo das mensagens não é armazenado.
