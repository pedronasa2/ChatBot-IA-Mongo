# ChatBot-IA-Mongo

Versão independente do painel ChatBot-IA com Angular, Spring Boot e MongoDB. Mantém login, isolamento por empresa, configurações do bot, administração, métricas e módulos opcionais de candidatos e agenda. O repositório PostgreSQL original permanece separado.

## Principais entidades e suas funções

A empresa é a referência para separar os dados e controlar o acesso no painel. As principais entidades e representações de domínio são:

| Entidade | Função no projeto | Persistência no MongoDB |
| --- | --- | --- |
| `Empresa` | Guarda os dados do negócio, como nome, contato, serviços e fuso horário. Também controla se o bot está ativo e se os módulos de candidatos e agenda estão habilitados. | `empresas` |
| `Usuario` | Representa quem acessa o painel. Armazena nome, e-mail, hash da senha, situação de ativação e indicação de superusuário. | `usuarios` |
| `VinculoUsuarioEmpresa` | Associa um usuário a uma empresa e registra seu papel. Sustenta a verificação de acesso aos dados de cada empresa. | `vinculos_usuarios_empresas` |
| `ConviteEmpresa` | Controla o primeiro acesso do responsável pela empresa. Relaciona empresa, usuário e e-mail, além de guardar o hash do token, a validade e a data de uso do convite. | `convites_empresas` |
| `ConfiguracaoBot` | Guarda a configuração do bot de uma empresa: nome, tom de voz, uso de emojis, instruções e orientações de transferência. Possui versão, estado, autor e revisão; o painel permite consultar e editar o rascunho. | `versoes_configuracoes_bot` |
| `PromptGlobal` | Representa as instruções gerais administradas pelo superusuário e a opção de complementá-las com as configurações da empresa. | `configuracoes_globais` |
| `VersaoPromptGlobal` | Representa uma versão salva do prompt global, com revisão, data e responsável, para consulta do histórico de alterações. | `historico_prompts_globais` |
| `VinculoChatwoot` | Representa a associação da empresa a uma conta e uma caixa de entrada do Chatwoot, por meio de `accountId` e `inboxId`. | Campos da coleção `empresas` |
| `Candidato` | Organiza os candidatos de uma empresa, incluindo contato, cargo pretendido, URL do currículo, resumo profissional, situação e arquivamento. Depende da habilitação do módulo de candidatos. | `candidatos` |
| `Agendamento` | Organiza os atendimentos agendados por empresa, com pessoa, contato, assunto, responsável, início, fim, situação e arquivamento. Depende da habilitação do módulo de agenda. | `agendamentos_atendimento` |
| `EventoMensagem` | Registra eventos de mensagens recebidas ou enviadas, com origem, resultado e horário. Fornece os dados usados nas métricas de atendimento. | `eventos_mensagens_atendimento` |
| Evento de auditoria | Registra ações com empresa, usuário responsável, referência da configuração e data, permitindo rastrear alterações. É gravado como documento pelo `AuditoriaRepository`. | `eventos_auditoria` |

Uma empresa pode ter vários vínculos de usuários, configurações do bot, candidatos, agendamentos e eventos de mensagens. O prompt global e seu histórico são compartilhados pelo sistema.

Os modelos Java ficam em `backend/chatbot-model/src/main/java/br/com/painel`, organizados por funcionalidade. Algumas representações são records em pacotes `model/vo`, persistidos pelos repositories como documentos MongoDB. As regras de negócio ficam em `chatbot-service`, e o acesso ao banco em `chatbot-repository`. Consulte também os detalhes de [persistência](docs/database.md) e [arquitetura](docs/arquitetura.md).

## Banco e desenvolvimento

Requer Java 25 LTS, Maven, Node 26.10 e MongoDB **replica set**. Transações protegem operações que envolvem vários documentos, como alteração com auditoria e provisionamento. Um MongoDB standalone não basta. Um replica set de um membro atende ao teste, mas não oferece alta disponibilidade.

```bash
export JAVA_HOME=/caminho/do/seu/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
bash infra/start-mongo-local.sh
bash infra/run-backend-local.sh provision
bash infra/run-backend-local.sh
# Em outro terminal:
cd frontend
npm ci
npm start -- --port 4207 --proxy-config proxy.e2e.json
```

O banco local usa somente `127.0.0.1:27027`; o backend usa 8087. O provisionamento interativo cria o primeiro superusuário e empresa, com módulos desabilitados e bot pausado. Não existe senha padrão nem autocadastro. O banco deve estar vazio. Não execute contra o banco de outro sistema.

A imagem local padrão é MongoDB 8.0. Em kernels incompatíveis com MongoDB 8, como o encontrado nesta máquina, o ambiente de testes foi validado com `MONGO_IMAGE=mongo:7.0 bash infra/start-mongo-local.sh`. Escolha a versão antes de criar o volume: não faça downgrade de um volume já usado por outra versão principal. Consulte a [matriz oficial de compatibilidade do kernel](https://jira.mongodb.org/browse/SERVER-125742).

Para outro servidor, defina `MONGODB_URI` no ambiente. O exemplo sem credenciais reais está em `infra/.env.example`. Codifique caracteres especiais da senha na URI. A conexão precisa de `uuidRepresentation=standard`, nome do banco dedicado, autenticação adequada e replica set. Senhas e tokens não pertencem ao Git ou frontend.

## Novas empresas e primeiro acesso

Entre como superusuário e abra **Negócios → Nova empresa**. Informe o nome da empresa, responsável, e-mail e os módulos liberados. O botão **Criar empresa e simular convite** não envia e-mail: mostra um link para testar a ativação. Abra-o em outra aba (ou janela anônima), informe o e-mail cadastrado e crie/confirme a senha. Depois entre pelo login normal. Em Negócios também é possível consultar o aceite ou simular um reenvio, invalidando o link anterior.

## Containers e atualização

A imagem unificada contém Angular compilado servido pelo Spring Boot na porta 8080. O banco é um serviço separado. Para construir localmente:

```bash
cp infra/.env.example infra/.env
# Preencha MONGODB_URI e tokens em infra/.env.
docker compose --env-file infra/.env -f infra/compose.yml up -d --build
docker compose --env-file infra/.env -f infra/compose.yml run --rm provision
```

O GitHub Actions em `.github/workflows/images.yml` testa o backend e o navegador, compila e publica a imagem privada no GHCR ao receber um push em `main`. Ela recebe a tag `sha-<commit>`; `latest` só é promovida depois que a publicação termina. Pull requests somente testam, sem publicar.

A VPS usa `infra/compose.images.yml` para baixar imagens prontas. O aplicativo não verifica o GitHub: o workflow pode avisar o EasyPanel por webhook após a publicação. Essa etapa exige cadastrar o secret `EASYPANEL_DEPLOY_URL` e a variável `EASYPANEL_AUTO_DEPLOY=true`, além do serviço e acesso ao registro privado no EasyPanel. Sem essa configuração, as imagens são publicadas, mas a VPS não muda. Veja [operação e atualização](docs/operations.md).

## Testes

```bash
bash infra/start-mongo-local.sh
cd backend && mvn verify
cd ../frontend && npm ci && npm run build
npx playwright install chromium
cd .. && bash infra/test-browser.sh
```

Os testes usam banco aleatório exclusivo no MongoDB local em 27027, sem credenciais de produção. Backend e testes de navegador removem seus bancos ao finalizar normalmente. Não chamam IA, Chatwoot ou n8n. Os testes de navegador usam a aplicação unificada na porta 8087; defina `E2E_BACKEND_PORT=18087` se sua aplicação local já estiver aberta.

## Limites desta versão

- Não transfere dados nem sessões existentes do PostgreSQL. O primeiro uso começa num banco MongoDB vazio. Uma migração de dados exige trabalho separado com backup e validação.
- SQL antigo está apenas em `docs/legado-postgres`, sem execução no backend.
- O contrato HTTP e as telas foram preservados. Candidatos e agenda continuam opcionais por empresa, com permissão verificada no servidor.
- A configuração do painel não publica automaticamente mudanças num agente n8n existente.
- Esta cópia não realiza deploy nem altera os bancos da VPS por conta própria.

[API](docs/api.md) · [Banco](docs/database.md) · [Módulos](docs/modulos-empresa.md) · [Validação](docs/validation.md)


## Estrutura e JDK

O alvo deste repositório é Java 25 LTS. Configure também o SDK do projeto e o JDK do Maven da IDE para 25; `.java-version`, compilador, CI e imagens usam essa versão. Os scripts de desenvolvimento/teste recusam outro JDK. Node continua em 26.10, independentemente do Java.

O backend separa adaptação HTTP, casos de uso transacionais, contratos e repositories por responsabilidade. O frontend organiza telas, modelos em classes e serviços HTTP por funcionalidade. Veja o mapa e as decisões em [arquitetura](docs/arquitetura.md), e os resultados reproduzíveis em [validação](docs/validation.md).

O Dockerfile da raiz é o caminho principal de distribuição (Angular dentro do JAR). Os Dockerfiles separados em `backend/` e `frontend/` são alternativas mantidas para composição manual; não são o artefato publicado pelo workflow principal.
