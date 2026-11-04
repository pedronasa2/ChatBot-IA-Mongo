# Validação da nova modelagem — 04/11/2026

Implementação do diagrama do README, mantendo os módulos Java e a organização Angular por funcionalidade, sem classes `Impl`.

## Resultado atual

- Quatro coleções de domínio: `empresas`, `usuarios`, `configuracoes_sistema`, `eventos_mensagens`; sessões continuam em coleção técnica separada.
- `Usuario.perfil` controla `SUPERUSUARIO`/`USUARIO`, com empresa diretamente no usuário; bot embutido na empresa, convite embutido no usuário e identificadores Chatwoot diretamente na empresa.
- Sem auditoria, histórico de prompt, coleção de vínculo ou versões do bot. Empresa, bot e Chatwoot usam a mesma revisão otimista.
- Mensagens guardam somente `_id`, `empresaId` e `enviadoEm`. A chave de idempotência vira o próprio ObjectId, sem coleção adicional. Duplicatas idênticas não contam novamente; dados diferentes com o mesmo ID retornam conflito.
- Gráficos filtram empresa/período e agrupam por minuto, hora ou dia. O fuso da empresa é informado na resposta e utilizado nos rótulos; consultas globais usam UTC.

## Verificações

- Java 25: 17 testes de integração e 1 teste de senha, sem falhas/erros/skip. Cobrem autorização por perfil/empresa e revogação em sessão aberta; CSRF/logout/sessão permanente; revisão compartilhada; bot e Chatwoot no documento da empresa; convite de uso único, renovação e expiração; prompt sem histórico; idempotência e schema das mensagens; agrupamento temporal; quatro coleções; conversão idempotente do legado; aborto transacional diante de múltiplas empresas.
- Build Angular de produção aprovado com Node 26, incluindo templates, tipos e Sass.
- Playwright: 20 execuções aprovadas (10 cenários em desktop e celular), incluindo criação/aceite de convite, Chatwoot, gravação/recarga do bot, filtros por hora, idempotência da coleta e revisão compartilhada entre formulários sujos de empresa e bot.
- Conferência visual do painel de métricas em celular. O gráfico foi ajustado para manter os intervalos na mesma linha, com rolagem interna quando necessário.
- `git diff --check`, sintaxe dos scripts e separação entre model/repository/service/resource conferidos.
- Nenhum deploy, commit ou alteração de workflow n8n. A imagem Docker anterior não representa esta nova modelagem e não foi revalidada.

## Migração do banco configurado

Backup BSON completo das 12 coleções anteriores, com contagens, índices e hashes SHA-256, fora do repositório, em diretório privado:

`/home/pedro/.local/share/chatbot-migration/backups/modelagem-2026-11-04T15-53-07.396460494Z`

O banco `painel_mongo`, acessado pelo túnel configurado no projeto, foi migrado. A comparação com o backup confirmou os mesmos 2 usuários e 2 empresas, hashes de senha, perfil de superusuário, nomes/instruções dos bots, token do convite, prompt e 11 sessões. Permanecem somente as quatro coleções de domínio e `sessions`.

A primeira execução do comando de migração usou a conexão do arquivo local, pois esse arquivo prevalecia sobre a variável `MONGODB_URI`. Os scripts foram corrigidos para mapear uma URI explícita para `SPRING_MONGODB_URI`, respeitada pelo Spring. A mesma migração foi depois executada e conferida sobre uma cópia integral do backup em banco local descartável, sem alterar novamente os dados reais.

Os testes e a cópia descartável usaram MongoDB 7 em replica set, em instância local sem volume compartilhado. O ambiente MongoDB 8 padrão apresenta a incompatibilidade de kernel já registrada na validação anterior.

Logs: `/tmp/chatbot-modelagem-java.log`, `/tmp/chatbot-modelagem-angular.log`, `/tmp/chatbot-modelagem-browser.log`, `/tmp/chatbot-modelagem-backup.log`, `/tmp/chatbot-modelagem-banco-conferencia.log`, `/tmp/chatbot-modelagem-migracao-copia.log`, `/tmp/chatbot-modelagem-copia-conferencia.log`.

## Reproduzir

```bash
export JAVA_HOME=/home/pedro/.jdks/temurin-25
export PATH="$JAVA_HOME/bin:/home/pedro/.local/share/painel/node26/bin:$PATH"
mvn -f backend/pom.xml verify
npm --prefix frontend run build
E2E_BACKEND_PORT=18097 bash infra/test-browser.sh
```

Os comandos de teste exigem um MongoDB replica set descartável em `localhost:27027`; nunca usam o banco configurado da aplicação. O comando de migração é diferente e usa o banco da aplicação: veja as instruções de backup e execução no README.

## Histórico anterior à remodelagem

As seções abaixo descrevem execuções e contratos anteriores. Suas contagens não correspondem à suíte atual.

# Validação da simplificação acadêmica — 04/11/2026

Candidatos e agendamentos foram removidos do frontend, backend e modelo de empresa. Foram retiradas as permissões exclusivas desses recursos, as rotas de integração e o token de módulos. A seleção de empresa permanece somente no cabeçalho.

- Java 25: 25 testes de integração e 1 teste de senha aprovados, sem falhas, erros ou testes ignorados. A regressão verifica que as rotas removidas retornam 404, os campos de habilitação não aparecem no JSON e a inicialização não cria as coleções removidas.
- Build Angular aprovado com Node 26.
- Playwright: 16 execuções aprovadas (8 cenários em desktop e celular), incluindo a ausência dos menus e controles removidos e os fluxos restantes de login, negócio, bot, convites, prompt, métricas e revisão concorrente.
- Banco configurado em `backend/config/banco-local.properties`: `painel_mongo`, na VPS por túnel local. Foram excluídas apenas `candidatos` (1 documento) e `agendamentos_atendimento` (0 documentos). Conferência posterior confirmou a preservação das outras 12 coleções.
- O MongoDB 8 local não iniciou devido à incompatibilidade reportada pelo próprio processo com o kernel. Os testes usaram MongoDB 7 em um replica set descartável, em `127.0.0.1:27027`, sem volume compartilhado e sem dados reais.
- Nenhum deploy foi realizado. A imagem Docker anterior não foi revalidada para esta alteração.

Logs desta execução: `/tmp/chatbot-remocao-java.log`, `/tmp/chatbot-remocao-angular.log`, `/tmp/chatbot-remocao-browser.log` e `/tmp/chatbot-remocao-banco.log`.

## Histórico da refatoração anterior

Os resultados abaixo antecedem a remoção de candidatos e agendamentos; as contagens e os fluxos descritos correspondem ao escopo da época.

Alvo: `/home/pedro/Projetos/ChatBot-IA-Mongo`, a partir de `dec7c52722500e301ba56895ed95ccc8ede6206f`, preservando alterações locais de convites e vínculo Chatwoot. A árvore continua sem commit desta execução. O GRP Web foi consultado como referência, sem alterações nele. O projeto PostgreSQL não foi modificado.

## Antes e depois

| Etapa | Resultado executado |
| --- | --- |
| Baseline da pasta principal, Java 26 | 25 testes de integração + 1 teste de validação de senha aprovados; build Angular; 12 execuções Playwright |
| Primeiras extrações, antes da organização GRP | 32 testes Java em JDK 25 e 24 execuções Playwright aprovados |
| Estrutura final por módulos, sem `Impl` | `mvn -q -f backend/pom.xml clean verify`: 32 testes de integração + 1 teste de validação de senha, sem falhas/erros/skip |
| Angular final | Build de produção aprovado, incluindo tipos, templates e Sass |
| Navegador servido pelo Java | 24 execuções Playwright aprovadas: 12 cenários em desktop e celular |
| Imagem unificada final | `docker build -t chatbot-ia-mongo:refatoracao-local .` aprovado |
| Navegador contra a imagem real | Mais 24 execuções Playwright aprovadas, com `E2E_IMAGE=chatbot-ia-mongo:refatoracao-local` |
| Servidor de desenvolvimento do usuário | `http://127.0.0.1:4207/` serviu o bundle atual; login aberto sem overlay e sem erros JavaScript, usando respostas de autenticação simuladas para não acessar dados da aplicação |
| Verificações de estrutura | Sem classes `Impl`; sem dependência de camadas superiores no model/repository; sem MongoTemplate/BSON/HTTP nos services; sem persistência nos resources; HTTP Angular concentrado no core |
| Arquivos auxiliares | `bash -n` nos scripts alterados e `git diff --check` aprovados |

Os testes usaram MongoDB 7.0 real, replica set local, bancos aleatórios descartáveis, UUID standard e credenciais sintéticas. A execução na imagem também usou banco exclusivo, removido após a parada do container de teste. Não houve acesso ao banco de produção, envio de mensagens, publicação ou deploy.

## O que a caracterização protege

- Falha real de inserção da auditoria desfaz as alterações de empresa, rascunho e permissões através dos services transacionais.
- Falha na inserção de histórico desfaz a atualização do prompt global.
- JSON completo de empresa/rascunho equivale ao contrato anterior, incluindo IDs, autoria, estado, versão, revisão, datas, campos Chatwoot e valores nulos.
- Documentos com os `_class` antigos continuam sendo lidos e gravados após a mudança de pacotes.
- A prévia conserva texto completo, precedência global e interpretação de campos ausentes em documentos legados.
- Transações sobrepostas de módulo/desativação e de dois módulos da mesma empresa disputam a escrita de controle; o driver sinaliza `TransientTransactionError`. Após desativação confirmada, uma nova operação é recusada. Não foi acrescentado retry global.
- O cenário de editor usa efetivamente o papel `editor` na fixture.
- A agregação de métricas é comparada com a implementação anterior em um instante fixo: limites de dia/minuto, 15 buckets, eventos futuros, erros e empresa sem eventos. O listener do driver contou 31 comandos antes e 3 depois no cenário de três empresas. Não foi medido ganho de latência nem desempenho do JDK.
- No frontend, respostas de PUT retidas não permitem apagar silenciosamente edições novas; revisão de formulário sujo não é renovada pela gravação de outro editor.
- Leituras A/B invertidas e respostas antigas de prévia/carga de empresa não substituem o contexto atual. Voltar/cancelar/aceitar no histórico conserva a política de descarte.
- Convites, ativação, acesso isolado, Chatwoot, login/logout, CSRF, perda de sessão, privilégios atuais, módulos, filtros, arquivamento, URLs diretas e layout nas duas resoluções seguem cobertos.

A UI anterior foi compilada em uma cópia externa e executada contra a API compatível para os cenários de gravação pendente/revisão suja: as seis execuções desses três cenários nos dois viewports falharam como esperado. Na UI corrigida, os cenários passaram. Isso distingue a caracterização dos riscos anteriores de uma simples movimentação de arquivos.

## Diagnósticos resolvidos

A movimentação inicial de CSS removeu seletores sem seus blocos completos e deixou declarações/chaves soltas. Os blocos foram reconstruídos integralmente; o erro Sass antes apontado na linha 1393 não aparece no build final.

A separação dos modelos exigiu importar `VolumePorMinuto` e `MetricasEmpresa` em `ResumoMetricas`; a mudança da tela de login exigiu atualizar o import para `modules/autenticacao/login/login.component`. Ambos os caminhos estão na árvore final, nos builds aprovados e no bundle servido pelo `ng serve`. O log enviado pelo usuário registrava diagnósticos intermediários da movimentação.

O teste da tela Negócios foi ajustado após a confirmação explícita do usuário de manter somente a seleção do cabeçalho. Ele valida o seletor e sua correspondência com o formulário, em vez de exigir o título/seletor secundários removidos.

## Java e limites de ambiente

Compilação, integração, sessão, navegador e imagem foram executados em Temurin 25.0.5+8 LTS. O alvo é 25 em Maven, `.java-version`, CI, imagens e validação dos scripts. Maven local: 3.8.7; Maven da imagem: 3.10.0. Spring Boot permanece em 4.1.1. Não houve benchmark nem troca global do Java do sistema.

O IntelliJ aberto ainda informa `corretto-26` como SDK do projeto em sua configuração local ignorada pelo Git. Não foi gravado um nome de SDK inexistente: o catálogo da IDE tem Corretto 17 e 26, e ainda requer registrar/selecionar o JDK 25 instalado em `/home/pedro/.jdks/temurin-25`. Os resultados acima não dependem desse SDK da IDE e foram executados explicitamente com JDK 25.

O MongoDB 8.0 e os Dockerfiles alternativos não foram revalidados nesta execução. O caminho testado de distribuição é o Dockerfile unificado da raiz. CI remoto, VPS, restauração de backup e provisionamento interativo em ambiente de destino permanecem fora destas execuções locais.

## Reprodução

Com JDK 25, Node compatível e Mongo replica set local em 27027:

```bash
export JAVA_HOME=/caminho/do/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
mvn -f backend/pom.xml clean verify
npm --prefix frontend run build
E2E_BACKEND_PORT=18098 bash infra/test-browser.sh

docker build -t chatbot-ia-mongo:refatoracao-local .
E2E_BACKEND_PORT=18098 E2E_IMAGE=chatbot-ia-mongo:refatoracao-local bash infra/test-browser.sh
```

Os relatórios Surefire ficam em `backend/chatbot-boot/target/surefire-reports`; resultados e capturas Playwright, em `frontend/test-results`. O teste da imagem usa Docker com rede do host em Linux e endereço do servidor limitado a `127.0.0.1`.
