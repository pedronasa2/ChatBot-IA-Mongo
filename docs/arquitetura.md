# Estrutura do ChatBot-IA-Mongo

A organização usa como referência o GRP Web de `/home/pedro/Trabalho/grp-web`: camadas Java próprias e Angular por domínio, com `classes`, `services` e pastas de telas. O código de referência foi apenas consultado. As classes de aplicação e de persistência são diretas, sem uma camada adicional de interfaces e classes `Impl`, conforme solicitado.

## Backend

`backend/pom.xml` é o agregador Maven. O resultado continua sendo uma aplicação Spring Boot única.

```text
backend/
  chatbot-model/         documentos Mongo, contratos e valores
  chatbot-repository/    persistência e consultas
  chatbot-service/       regras, autorização e transações
  chatbot-resource/      endpoints HTTP e tradução de erros
  chatbot-boot/          inicialização, configurações, recursos e testes
```

Cada módulo organiza os pacotes por domínio: `br.com.painel.<dominio>.model`, `.model.vo`, `.repository`, `.service` ou `.resource`. O fluxo de empresa, por exemplo, fica em `EmpresaResource` → `EmpresaService` → `EmpresaRepository`. Dados da empresa e configuração do bot têm resources próprios, mas persistem no mesmo documento de empresa. Repositories do Spring Data continuam sendo interfaces implementadas pelo framework; os repositories Mongo específicos são classes concretas.

As dependências Maven seguem `model` → `repository` → `service` → `resource` → `boot`. O modelo não depende de persistência executável ou da API; a persistência não depende dos services. Services não importam `MongoTemplate`, BSON ou tipos HTTP. O serviço de mensagens valida o identificador de idempotência como texto; consultas e escrita BSON ficam nos repositories. Erros de negócio são traduzidos em status HTTP no resource advice, conservando o envelope `mensagem`.

| Responsabilidade | Dono |
| --- | --- |
| Empresa, bot e Chatwoot | Atualizações na empresa com a mesma revisão otimista; o bot é um objeto embutido |
| Usuários e privilégios | `perfil` e `empresaId` no usuário; políticas consultam o estado atual no banco |
| Prompt global | `ConfiguracaoSistema` guarda o documento compartilhado atual; não há histórico |
| Prévia | `CompositorPrompt` recebe configuração global e empresa; não chama integrações |
| Mensagens e métricas | Evento com ObjectId, empresa e instante; idempotência pelo próprio ID; agregação por período |
| Convites | Objeto no usuário; consumo e ativação em uma atualização condicional; criação com empresa em transação |
| Provisionamento | Criação inicial de administrador e empresa em transação; sem coleção de marcador |
| Migração | `MigracaoModelagemService` controla a transação e `MigracaoModelagemRepository` converte documentos; bootstrap remove o legado somente após sucesso |
| Sessões e índices | Configuração em `chatbot-boot`; conversor de sessões permanentes preservado |

A coesão do caso de uso determina suas colaborações, sem impor uma cardinalidade artificial de repositories por service. DTOs, buckets de métricas e coleções técnicas não recebem endpoints artificiais.

O domínio usa quatro coleções, descritas no README. Não há entidades de vínculo de empresa, convite separado, versões do bot, histórico do prompt ou auditoria. Os documentos atuais usam camelCase. A migração preserva credenciais e referências e remove os metadados antigos durante a reconstrução dos documentos. Os contratos de entrada/saída ficam em `model.vo`; objetos embutidos não recebem repositories próprios.

## Angular

```text
frontend/src/app/
  app.routes.ts
  modules/
    app/                  shell e navegação
    core/                 sessão, CSRF, RestService e guardas
    ui/                   UiModule, ícones e estilos comuns
    empresas/
      classes/model/
      classes/vo/
      services/
      cadastro/empresa-crud/
      cadastro/empresa-cadastro/
    bot/                  modelos, service e configuração
    convites/             modelos, service e ativação
    chatwoot/             modelos, service e configuração do vínculo
    administracao/        modelos, services de prompt/métricas e painel
```

Há uma classe TypeScript por arquivo de modelo. Elas representam os contratos expostos pelo Java, sem copiar infraestrutura de Mongo para o navegador. `RestService` faz HTTP, prepara CSRF para mutações e instancia explicitamente os modelos na fronteira; os services de domínio retornam `Observable` tipado. O componente mantém formulário e apresentação. `UiModule` reúne as dependências visuais comuns, seguindo a organização do GRP sem introduzir seus componentes proprietários ou trocar a biblioteca visual existente.

Prompt e métricas têm services separados. As consultas usam gerações/contexto para rejeitar respostas antigas; mutações não recebem retry automático. Formulários ficam bloqueados durante a gravação, e uma atualização de outro editor não renova a revisão de um formulário sujo. Guardas cobrem saída, alteração de empresa e histórico do navegador, inclusive o cancelamento do descarte.

A seleção da empresa fica somente no cabeçalho, conforme a decisão do usuário. Tema, utilitários e ajustes internos do Material ficam globais; estilos de administração e convites foram separados sem alterar a finalidade dos seletores.

## Distribuição e validação

O Dockerfile da raiz monta Angular dentro de `chatbot-boot/target/painel-bot-0.1.0.jar`. CI e scripts usam o agregador Maven; os testes ficam no módulo executável. `infra/test-browser.sh` aceita opcionalmente `E2E_IMAGE` para testar a imagem real com a mesma fixture descartável.

Java 25 LTS foi alinhado separadamente da reorganização. Não houve atualização de Spring Boot, benchmark, publicação ou deploy. Resultados e comandos estão em [validation.md](validation.md).
