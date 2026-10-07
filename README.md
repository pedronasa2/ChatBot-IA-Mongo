# Painel de configuração de bots

Etapa A do prompt: aplicação Angular + Spring Boot, PostgreSQL com Flyway, login/logout por sessão, autorização por empresa, dados do negócio e rascunho do bot. A interface usa português brasileiro e Angular Material, com layout para celular e desktop.

## PostgreSQL local de teste

A aplicação está configurada para PostgreSQL em `189.126.111.15:5432`, banco `postgres` e usuário `pedro`. A senha informada está em `infra/.env` e no perfil `local`, permitindo iniciar pela IDE ou por `infra/run-backend-local.sh`. Variáveis de ambiente têm prioridade. Reinicie o backend depois de trocar a configuração. O banco remoto recebeu os dados do banco local, incluindo usuários, empresas, configurações, sessões e histórico Flyway. Cópias de segurança estão em `infra/backups/2026-10-06-transfer/`.

Neste host o Docker não está instalado e a instalação precisa da senha de sudo. Execute em seu terminal, na raiz do projeto:

```bash
sudo bash infra/install-docker-local.sh
```

O script usa o repositório oficial stable do Docker para a base Ubuntu Noble do Zorin, inicia o daemon, sobe **apenas o PostgreSQL 18.6**, espera o healthcheck e confere login por TCP com a senha configurada. Não altera grupos do usuário. Em execuções seguintes:

```bash
bash infra/start-postgres-local.sh
# Com JDK 26 e Maven disponíveis no PATH:
bash infra/run-backend-local.sh provision
bash infra/run-backend-local.sh
```

O backend local inicia em 8087. Na IDE, basta executar `br.com.painel.Aplicacao` com JDK 26; não é necessário cadastrar variáveis de ambiente. Para produção, selecione explicitamente `SPRING_PROFILES_ACTIVE=production` e forneça as credenciais por ambiente; o Compose seleciona `docker` e mantém a porta interna 8080. Para o frontend nesse modo:

```bash
cd frontend
npm start -- --port 4207 --proxy-config proxy.e2e.json
```

O provisionamento cria o usuário do painel; a credencial do PostgreSQL não serve para entrar na tela de login. Caso um volume existente tenha sido criado com outra senha, alterar `.env` não troca a senha no banco: ajuste-a usando o administrador original, preservando o volume.

**Estado atual (06/10/2026):** Docker instalado pelo usuário; PostgreSQL 18.6 acessível em 127.0.0.1:5433. Conexão JDBC com a credencial local validada, migração Flyway V1 aplicada e backend iniciado em 8087. O banco ainda não contém usuário do painel nem empresa; execute o provisionamento acima para criá-los. Acesso ao Docker liberado pelo grupo `docker` e verificado com `sg docker`; container `infra-postgres-1` saudável. Sessões já abertas antes da alteração de grupo precisam ser reiniciadas ou usar `sg docker` até receberem a nova associação.

## Executar com Docker Compose

Pré-requisito: Docker Engine e Compose já instalados. Nenhum serviço externo é alterado.

```bash
cp infra/.env.example infra/.env
# Edite infra/.env e preencha DATABASE_PASSWORD com uma senha própria.
docker compose --env-file infra/.env -f infra/compose.yml up -d --build
docker compose --env-file infra/.env -f infra/compose.yml run --rm provision
```

O provisionamento pede e-mail, nome da empresa e senha confirmada no terminal. A senha não aparece no terminal, não existe senha padrão e o procedimento recusa um banco que já tenha usuários. Acesso em http://localhost:4207. O banco e o backend não expõem portas à rede do host. O volume preserva o banco; não use `down -v` para parar a aplicação.

## Desenvolvimento sem Docker

Requer JDK 26, Maven 3.10.0, Node 26.10.0 e um PostgreSQL 18.6 acessível. Node 24.15+ também é compatível com Angular; o Node 18 instalado no host não serve. Exporte `DATABASE_URL` (URL JDBC), `DATABASE_USER` e `DATABASE_PASSWORD` no terminal do backend. Não ponha segredos na linha de comando.

```bash
cd backend
mvn verify
mvn package -DskipTests
java -jar target/painel-bot-0.1.0.jar --spring.profiles.active=provision --spring.main.web-application-type=none
# Em outro terminal com as variáveis do banco:
COOKIE_SECURE=false java -jar target/painel-bot-0.1.0.jar
```

```bash
cd frontend
npm ci
npm start -- --port 4207
```

O proxy de desenvolvimento encaminha `/api` para o backend em 8080. Se essa porta estiver ocupada, ajuste a porta do backend e `proxy.conf.json`. Cookie Secure só é desligado no desenvolvimento com HTTP; produção exige HTTPS e `COOKIE_SECURE=true`. A sessão autenticada não expira por inatividade e fica persistida no PostgreSQL, inclusive após reiniciar o backend. O botão Sair invalida a sessão no banco e apaga o cookie. CSRF permanece ativo. O cookie HttpOnly é persistente e renovado em cada acesso; limpar cookies, usar janela anônima ou limites de retenção do navegador podem exigir novo login.

## Verificações

```bash
cd backend
mvn verify
cd ../frontend
npm ci
npm run build
npx playwright install chromium
cd ..
bash infra/test-browser.sh
```

Os testes de integração iniciam PostgreSQL 18.6 temporário em uma porta aleatória (sem H2 ou Docker), validam migrações, autenticação, CSRF, isolamento entre empresas, salvamento, auditoria e conflitos de revisão. Execute como usuário comum, pois PostgreSQL não inicia como root. O script de navegador inicia backend em 8087 e frontend em 4207; essas portas precisam estar livres. Ele gera credencial aleatória fora do repositório e remove os processos e dados temporários ao terminar. Os testes cobrem desktop e celular com login, edição, gravação, recarga, logout e sessão removida. Nenhum teste chama n8n, Chatwoot ou IA.

## Escopo atual e próximos passos

A empresa nasce com bot pausado e um rascunho. Não há publicação nem envio automático nesta entrega. As informações da empresa estão em `empresas`; antes da etapa C, o processo de publicação da etapa B deverá copiar os dados do negócio para um snapshot versionado para que edições não alterem o contexto de produção. O nome do bot, tom e instruções já ficam em `versoes_configuracoes_bot`. Auditoria registra IDs, ação e data, sem conteúdo ou segredos. Revisões protegem edições concorrentes.

- **B:** horários, exceções, FAQs, avisos e publicação transacional com snapshot completo.
- **C:** API interna com credencial própria e escopo por canal, descoberta da instalação real e teste isolado no n8n.
- **D:** estado por conversa, transferência confirmada, retomada, autenticação de eventos e deduplicação.

Sem autocadastro, convites, recuperação por e-mail ou gestão de membros nesta fase. Não existem botões dessas funcionalidades. A integração aparece como não verificada; nenhuma estatística foi inventada. Não houve deploy nem alteração de workflow real.

Veja [versões](docs/versions.md), [contrato da etapa A](docs/api.md), [operação na VPS](docs/operations.md) e [resultado de validação](docs/validation.md).

## Iniciar Angular com um comando

Na raiz do projeto ou dentro de `frontend`, execute `ng server` (ou `ng serve`). O atalho `~/.local/bin/ng` usa o CLI do projeto e o Node 26.10.0 instalado em `~/.local/share/painel/node26/bin/node`. Porta padrão: 4207; proxy `/api`: backend em 8087. Encerre com Ctrl+C. A porta deve estar livre; para outra porta use `ng server --port 4208`.

## Painel do desenvolvedor

O menu Superusuário permite administrar o prompt global e consultar métricas. O acesso é independente dos papéis das empresas e exclusivo ao usuário do desenvolvedor. Veja [configuração e coleta pelo n8n](docs/superusuario.md). A coleta começa após o fluxo registrar eventos na API; editar o prompt no painel ainda não altera um agente n8n existente.
