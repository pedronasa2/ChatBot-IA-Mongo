# Validação local da refatoração

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
