# Persistência MongoDB

Configure `spring.mongodb.uri` por `MONGODB_URI`. Use banco exclusivo e UUID no formato standard. Não reutilize o banco de n8n ou Chatwoot. IDs continuam UUID no JSON e UUID binário subtipo 4 no MongoDB. Datas são BSON Date; DTOs mantêm ISO-8601. Campos persistidos usam snake_case.

Coleções: `usuarios`, `empresas`, `vinculos_usuarios_empresas`, `versoes_configuracoes_bot`, `eventos_auditoria`, `configuracoes_globais`, `historico_prompts_globais`, `eventos_mensagens_atendimento`, `candidatos`, `agendamentos_atendimento`, `provisionamento`, além da coleção técnica do MongoDB Spring Session.

`ConfiguracaoMongo` cria coleções e índices idempotentes na inicialização. Há unicidade para e-mail, um único superusuário, vínculo usuário/empresa, versão do bot por empresa, um rascunho por empresa, revisão global e evento de origem. MongoDB não oferece foreign keys: referências e acesso por empresa são validados nos serviços. Exclusão de empresas/usuários não foi implementada; scripts externos não devem apagar referências diretamente.

Atualizações usam revisão otimista e retornam conflito quando a versão está obsoleta. Configuração e auditoria, prompt e histórico, provisionamento e operações de módulos usam transações MongoDB. É obrigatório replica set ou cluster com suporte a transações. O acesso aos módulos usa uma escrita na empresa na mesma transação, para conflitar com desativação concorrente. Isso serializa operações por empresa e deve ser medido sob carga.

Sessões são persistidas pelo pacote oficial `mongodb-spring-session`, sem expiração por inatividade, preservando o comportamento anterior. Logout invalida a sessão. Inclua sessões nos backups e proteja o banco.

A aplicação inicia sem dados de produção. O provisionamento cria administrador e empresa inicial. Não há importador PostgreSQL nesta entrega. Migrações SQL antigas ficam fora dos recursos executáveis, em `docs/legado-postgres`.

As consultas de texto usam regex literal sem diferenciar maiúsculas/minúsculas. Não são busca semântica ou full-text. Métricas agregam contagens por janela e empresa; avaliar custo para bases grandes antes de dimensionar.
