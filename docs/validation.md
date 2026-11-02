# Validação da variante MongoDB — 2 de novembro de 2026

- Backend: 19 testes de integração aprovados com MongoDB 7.0 real em replica set local. Cobrem autenticação, CSRF, sessão persistente e logout, isolamento por empresa, revisão otimista, rollback de múltiplas escritas, auditoria, superusuário, prompt/histórico, métricas idempotentes e módulos opcionais.
- Frontend: build Angular de produção aprovado.
- Navegador: 10 testes Playwright aprovados, em desktop e celular, incluindo login, edição, recarga, candidatos, agenda e permissões.
- Containers: imagens Docker de backend e frontend construídas localmente com sucesso.
- MongoDB 8.0 não iniciou neste kernel local; o teste usou MongoDB 7.0 em volume descartável antes de qualquer dado de aplicação. Isso não valida a configuração nem a versão da VPS.
- GitHub Actions: workflow preparado para repetir testes, compilar, publicar no GHCR e opcionalmente acionar o EasyPanel. Execução remota e deploy precisam ser verificados separadamente.

O banco PostgreSQL original e os serviços da VPS não foram alterados. Não houve migração de dados para MongoDB nem teste de restauração de backup. O provisionamento interativo precisa ser executado no ambiente de destino, com senha definida pelo administrador.

A primeira execução remota encerrou no preparo do MongoDB 8.0 antes dos testes; CI ajustado para MongoDB 7.0, já validado localmente, e diagnóstico de logs adicionado. A configuração do MongoDB da VPS continua independente.
