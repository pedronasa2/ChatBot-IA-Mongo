# Persistência MongoDB

As quatro coleções de domínio são `empresas`, `usuarios`, `configuracoes_sistema` e `eventos_mensagens`. O diagrama completo está no [README](../README.md). Os campos usam camelCase. Empresa e usuário mantêm UUID binário subtipo 4; mensagens usam ObjectId. Datas são BSON Date em UTC, representadas por `Instant` no Java e ISO-8601 no HTTP.

`Empresa.bot` e `Usuario.convite` são objetos embutidos. Chatwoot usa os campos `chatwootAccountId` e `chatwootInboxId` diretamente em empresa. Cada usuário comum tem uma única `empresaId`; `perfil` é `SUPERUSUARIO` ou `USUARIO`. O superusuário pode ter empresa nula e vê todas as empresas. As políticas consultam os privilégios atuais no banco, inclusive em sessões já abertas.

`configuracoes_sistema` contém o documento `_id: "principal"`. Não há histórico de versões do prompt nem auditoria. Empresa e configuração global têm revisão otimista. Dados do negócio, bot e Chatwoot compartilham a revisão da empresa; gravações obsoletas retornam 409.

Índices: e-mail único; empresa do usuário; hash de convite único quando presente; combinação Chatwoot conta/caixa única quando configurada; mensagens por `empresaId + enviadoEm` e por `enviadoEm`. O índice de mensagens por empresa/data não é único. Perfis não têm coleção própria nem limite de um único administrador.

Cada mensagem registra somente `_id`, `empresaId` e `enviadoEm`. O cabeçalho `Idempotency-Key` deve ser um ObjectId estável por mensagem e é usado como `_id`. Repetir identificador, empresa e instante (precisão de milissegundos) é idempotente; repetir o identificador com dados diferentes retorna 409. Não se usa empresa/data como identificação, pois envios distintos podem ocorrer simultaneamente.

Métricas usam agregação com `$dateTrunc` e contagem de documentos. O gráfico aceita empresa, início inclusivo, fim exclusivo e agrupamento por minuto/hora/dia. A empresa selecionada determina o fuso do agrupamento; consultas globais usam UTC. Intervalos vazios são preenchidos com zero. Totais gerais e últimas 24 horas continuam tendo suas próprias janelas, independentes do período do gráfico.

Sessões permanecem em coleção técnica `sessions`, sem expiração por inatividade; logout invalida a sessão. Não foram alterados o conversor nem o formato das sessões existentes. Inclua sessões nos backups.

## Migração

`ConfiguracaoMongo` cria as quatro coleções e chama `MigracaoModelagemService`. A conversão dos documentos acontece em transação. Só após sucesso e criação dos índices são removidas as coleções anteriores: vínculos, versões do bot, convites, configurações globais, histórico do prompt, auditoria, integrações Chatwoot, eventos antigos e marcador de provisionamento.

Preservam-se credenciais, usuários, empresas, configuração atual do bot, convite atual e prompt atual. Superusuários ficam com empresa nula; usuários comuns recebem a empresa do vínculo. Múltiplos vínculos para usuário comum, empresa inválida ou múltiplos convites interrompem a conversão. Dos eventos anteriores, somente enviados com sucesso são convertidos. UUIDs de eventos antigos são transformados em ObjectIds determinísticos para permitir nova execução sem duplicação.

Faça backup completo antes de iniciar a nova versão e mantenha instâncias antigas paradas durante e após a migração. A conversão é idempotente, mas não reverte automaticamente dados para o formato antigo. Restaurar a versão antiga exige restaurar também o backup correspondente.

O script `infra/migrar-modelagem.sh` executa a migração sem servidor HTTP. Usa a mesma configuração local do backend. Não imprime senha nem token. É obrigatório MongoDB replica set ou cluster com transações. Não existe migração do PostgreSQL neste fluxo.
