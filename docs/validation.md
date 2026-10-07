# Validação — etapa A

Concluída em 6 de outubro de 2026, usando Java Temurin 26.0.2.1, Node 26.10.0, Angular 22.2.1 e PostgreSQL 18.6 temporário.

| Verificação | Resultado |
| --- | --- |
| `mvn verify` | 4 testes, 0 falhas, 0 erros; JAR executável gerado |
| `npm run build` | Compilação de produção concluída; bundle inicial ~539 kB, transferência estimada ~117 kB |
| `bash infra/test-browser.sh` | 4 testes aprovados: desktop 1440 px e celular 390 px |
| Provisionamento em terminal | Usuário, empresa, vínculo owner e rascunho confirmados no PostgreSQL; senha oculta; processo sem servidor web |
| Inspeção visual das capturas | Formulário legível nos dois tamanhos, sem rolagem horizontal; textos de ajuda ajustados no celular |
| Limpeza dos testes | Backend/frontend de teste encerrados; PostgreSQL descartável fechado e arquivo de credencial removido |

Backend: login/logout, sessão, CSRF, listagem filtrada por vínculo, leitura e escrita bloqueadas para outra empresa, dados persistidos, validação de fuso/URL, revisão obsoleta recusada e auditoria consistente na mesma transação.

Navegador: login real contra a API, alteração dos dados do negócio e do rascunho, gravação no PostgreSQL, recarga e conferência dos valores, logout, perda da sessão durante uma gravação e retorno ao login com mensagem clara. Capturas locais em `frontend/test-results/desktop-bot.png` e `mobile-bot.png` são artefatos ignorados pelo versionamento. O teste de expiração remove o cookie para reproduzir uma sessão ausente; não espera os 30 minutos de timeout real.

## Limites desta verificação

- Docker Compose e builds das imagens não foram executados: Docker não está instalado no host. As tags usadas foram conferidas no registro; execução e persistência de volumes devem ser verificadas no host de destino.
- Provisionamento foi executado no JAR contra PostgreSQL descartável, não em produção.
- Nenhuma integração real com n8n, Chatwoot ou IA foi executada. Não há simulação apresentada como atendimento real.
- Publicação, horários/exceções, FAQs, avisos e contrato interno são etapas posteriores. Por isso ainda não existem testes de publicação ou horários nem uma conversa de teste no painel.
- Backup/restauração e capacidade na VPS não foram medidos. Há procedimento documentado para execução antes do deploy.
- Rótulos, foco e layout foram implementados; não foi realizada auditoria completa de acessibilidade com leitor de tela.

A base da etapa A está executável e verificada fora de Docker. As etapas B, C e D continuam pendentes.

## PostgreSQL local em Docker — 06/10/2026

Docker instalado e container iniciado pelo usuário através do script de instalação. Verificado posteriormente pela aplicação:

- Conexão JDBC por TCP a `127.0.0.1:5433`, banco `painel` e usuário `painel`, usando a senha de `infra/.env`.
- Servidor retornou PostgreSQL 18.6 (Debian 18.6-1.pgdg13+2).
- Backend Spring Boot iniciou em `127.0.0.1:8087` e Flyway aplicou V1 com sucesso.
- `/api/auth/csrf` respondeu HTTP 200; `/api/companies` sem sessão respondeu HTTP 401.
- Banco com zero usuários/empresas; provisionamento do painel ainda pendente.

A API Docker não foi inspecionada pelo agente: o socket existe, mas pedro não tem permissão e sudo solicita senha. Não foram verificados reinício do container, persistência após recriação ou builds das imagens frontend/backend. A conexão do backend com o PostgreSQL Docker foi verificada de verdade.

### Acesso administrativo ao Docker confirmado

Após inclusão de pedro no grupo docker, acesso confirmado usando `sg docker` na sessão existente. Docker Engine 29.8.2; container `infra-postgres-1` com PostgreSQL 18.6 em estado healthy, publicando somente `127.0.0.1:5433`. O comando de subida foi reexecutado sem recriar o container e a autenticação TCP por senha foi novamente validada dentro dele. As verificações de reinício, restauração e builds frontend/backend continuam pendentes.

### Execução direta pela IDE corrigida

O erro enviado pelo usuário revelou que a configuração anterior só carregava a credencial local pelo script shell; executar Application pela IDE tentava localhost:5432. Adicionado `application-local.yml` e perfil padrão `local`, com URL 127.0.0.1:5433, usuário/senha de teste e porta 8087. O Compose seleciona perfil docker para manter as configurações internas.

JAR recompilado e iniciado sem nenhuma variável de banco nem perfil fornecido. Confirmados perfil local automático, conexão Flyway ao PostgreSQL Docker 18.6 e HTTP 200 em /api/auth/csrf. A verificação usou a porta temporária 18087, encerrada ao terminar; a porta 8087 ficou livre para o usuário iniciar pela IDE. Configuração Compose validada com `docker compose config --quiet`.

### Visão geral sem rolagem

Layout inicial compactado: cabeçalho, cartões de estado, passos de configuração e rodapé permanecem visíveis. Cartões lado a lado no desktop; duas colunas e cartão principal em largura total nas telas menores. Login também ajustado para caber no celular.

Build Angular aprovado. Conferência no Chromium contra a aplicação em execução: login e visão geral sem rolagem vertical ou horizontal em 1440×900, 1366×768, 1280×720, 1024×768, 390×844 e 360×640. A altura de scroll foi comparada à altura real do viewport. Não foi usado overflow hidden nem truncamento para esconder conteúdo; telas de edição mantêm seu fluxo normal. Zoom ampliado, erros extensos e telas menores podem exigir rolagem para preservar acesso às informações.

### Login sem expiração por inatividade

Por solicitação do usuário, sessões passaram a usar Spring Session JDBC, com intervalo negativo (`-1s`), persistência nas tabelas criadas por Flyway V2 e cookie HttpOnly persistente renovado durante o acesso. O botão Sair invalida a sessão no PostgreSQL e remove o cookie. A mensagem de sessão indisponível deixou de atribuir a causa a uma expiração por tempo.

- `mvn verify`: 5 testes aprovados, incluindo sessão com último acesso simulado em 2000, limpeza de sessões e recuperação por uma nova instância do repositório sem perder autenticação. Testes existentes de login/logout, CSRF e autorização entre empresas continuam aprovados.
- `npm run build`: aprovado.
- Teste HTTP real contra PostgreSQL Docker: login, encerramento e reinício de uma instância de backend de teste preservaram a mesma sessão autenticada; cada acesso renovou o cookie. Logout retornou 204 e cookie vazio com data de expiração em 1970; reutilizar o cookie anterior retornou 401.
- A migração V2 foi aplicada no banco local. A instância de teste em 18087 foi encerrada. O backend iniciado pelo usuário em 8087 não foi interrompido e precisa ser reiniciado para carregar esta alteração; a troca de implementação de sessão exige um novo login inicial.

Não há timeout da sessão no servidor. O navegador ainda controla retenção e limpeza dos cookies: apagar cookies, usar modo anônimo ou ultrapassar o limite de retenção imposto pelo navegador pode exigir novo login. O cookie recebe a maior duração suportada pelo serializer e é renovado em cada acesso autenticado, sem expor o identificador a JavaScript.

### Design baseado na referência Stitch

Aplicada a identidade AtendeFlow do arquivo `stitch_gestor_de_atendimento_whatsapp.zip`: cores em azul petróleo, fonte Plus Jakarta Sans servida localmente, cabeçalho, navegação lateral, cartões e rodapé. Formulários de negócio e bot mantêm seus campos funcionais; tom de voz agora usa opções de rádio. Estados de integração e publicação continuam refletindo o que a aplicação realmente oferece.

Build Angular aprovado. Conferência visual e funcional no Chromium em 1440×900, 1366×768, 1280×720, 1024×768, 390×844 e 360×640: login e visão geral sem rolagem, formulários sem transbordamento horizontal e nenhum erro JavaScript. Login e logout usaram o backend real. O envio do rascunho foi interceptado no teste para verificar tom e emojis sem alterar os dados do usuário; a resposta simulada confirmou a apresentação da mensagem de sucesso. Capturas estão em `frontend/test-results/stitch-*.png`.

### Troca para PostgreSQL remoto

Configurados perfil local e ambiente do backend/provisionamento Compose para `189.126.111.15:5432`, banco `postgres`, usuário `pedro`. Autenticação real por TCP aprovada, com consulta somente de leitura ao catálogo. O banco não tinha tabelas de aplicação. Não foram executadas migrações remotas nem transferidos dados locais nesta troca; o backend do usuário deve ser reiniciado para carregar a configuração e executar Flyway.

### Transferência dos dados locais para o remoto

Backup de origem e destino antes da transferência em `infra/backups/2026-10-06-transfer/`, com acesso restrito e ignorado pelo Git. Destino `postgres` em `189.126.111.15:5432` sem tabelas públicas antes da restauração. Dump restaurado em transação única, sem remover dados da origem. Conferidos quantidade e conteúdo integral das oito tabelas: 1 usuário, 1 empresa, 1 vínculo, 1 configuração, 3 eventos de auditoria, 12 sessões, 13 atributos e 2 migrações Flyway. Conta `pedronasa2@gmail.com` habilitada e hash da senha preservado. Backend do usuário não foi reiniciado pelo agente.

### Nomes descritivos das tabelas

Migração Flyway V3 aplicada no banco remoto: `usuarios`, `empresas`, `vinculos_usuarios_empresas`, `versoes_configuracoes_bot` e `eventos_auditoria`. Mapeamentos JPA, consultas SQL, provisionamento e testes atualizados. Adicionados comentários descritivos no catálogo. Backup anterior à migração preservado em `infra/backups/2026-10-06-transfer/before-table-renames.dump`.

`mvn verify`: cinco testes aprovados. Quantidades e assinaturas do conteúdo integral das cinco tabelas conferidas antes/depois, sem alteração nos dados. Instância temporária em 18087 validou login real, listagem de empresas, leitura da empresa e rascunho, e logout contra o remoto. Instância temporária encerrada; backend do usuário não foi interrompido e precisa reiniciar com o código atualizado.

### Inicialização Angular pelo terminal

Atalho `ng` instalado em `~/.local/bin`, apontando para `infra/ng`; aceita `server` como alias de `serve`, encontra o frontend pela raiz ou subpastas e usa Node 26.10.0 persistente. Configurados host 127.0.0.1, porta 4207 e proxy para 8087 no Angular. `ng server --help` e `ng version` aprovados; inicialização real com `ng server --port 14207` compilou e respondeu HTTP 200 com a página AtendeFlow. Servidor temporário encerrado; instância do usuário em 4207 preservada.

### Entidades, atributos e colunas em português

Classes e pacotes de negócio renomeados para português: Empresa, ConfiguracaoBot, ControladorConfiguracoes, repositórios e acesso às empresas. Atributos dos modelos, DTOs, formulários e variáveis do domínio Angular traduzidos; rotas atualizadas para `/api/empresas` e `/rascunho`, login com `senha` e erros tratados com `mensagem`. Classe de entrada: `br.com.painel.Aplicacao`.

Flyway V4 aplicado ao banco remoto após backup `infra/backups/2026-10-06-transfer/before-portuguese-columns.dump`. Colunas de negócio renomeadas; papéis, estados e ações de auditoria traduzidos. Cada registro das cinco tabelas foi comparado integralmente ao conteúdo esperado após a conversão, com dados preservados. Tabelas técnicas de Spring Session e Flyway mantiveram seus contratos.

Validação: cinco testes de integração aprovados; build Angular aprovado; quatro testes Playwright aprovados em desktop/celular, com login, edição, gravação, recarga, logout e remoção de sessão, usando banco descartável e portas 14187/14207. Login e leitura de empresa/rascunho também aprovados no remoto com o contrato novo. Instância temporária 18087 encerrada; servidores do usuário não interrompidos. Reiniciar o backend pela classe Aplicacao para carregar as alterações.

### Painel exclusivo de superusuário

Flyway V5 aplicado ao PostgreSQL remoto após backup `infra/backups/2026-10-06-transfer/before-superuser-panel.dump`. Habilitado somente `pedronasa2@gmail.com`; índice único impede um segundo superusuário. Papéis de empresas não concedem administração. Permissão conferida no banco a cada chamada, inclusive em sessões persistentes.

Implementados editor do prompt global, revisão concorrente, histórico de autoria, prévia com rascunho por empresa, métricas e API dedicada de eventos. Token de integração gerado em arquivo local ignorado pelo Git e ambiente Compose; permite apenas registrar metadados. Eventos repetidos não duplicam contagem. Falhas são contabilizadas separadamente dos envios bem-sucedidos. Não foi alterado workflow n8n real, nem conectado o consumo do prompt ao agente.

- `mvn verify`: 10 testes aprovados, cobrindo autorização, revogação em sessão aberta, exclusividade, CSRF, histórico, conflito de revisão, ingestão autenticada, duplicação e janelas de tempo, além dos fluxos existentes.
- Build Angular aprovado, sem ultrapassar os budgets.
- Playwright: 8 testes aprovados no desktop/celular, incluindo usuário de empresa sem acesso, desenvolvedor sem vínculo empresarial, salvamento e recarga do prompt, preservação do texto após CSRF 403, prévia, métricas e deduplicação. Testes de gravação usaram banco descartável.
- Conferência real no remoto: dados das cinco tabelas existentes preservados, com alteração autorizada apenas na permissão e data de atualização do desenvolvedor. Login, identidade, prompt inicial vazio, histórico inicial vazio e métricas iniciais zeradas confirmados. Token local validado com empresa inexistente, recusada sem gravar eventos. Nenhum prompt ou evento fictício gravado no remoto.
- Visão geral com menu de superusuário sem rolagem em 1440×900, 1366×768, 1280×720, 1024×768, 390×844 e 360×640; nenhum erro JavaScript. Capturas em `frontend/test-results/superusuario-real-*.png`.

Instâncias temporárias 18087 e 14207 encerradas. Servidores do usuário em 8087/4207 preservados. Reiniciar o backend e atualizar a página para carregar o novo painel. Guia de integração: `docs/superusuario.md`.
