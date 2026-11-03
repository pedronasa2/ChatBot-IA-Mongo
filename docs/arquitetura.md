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

Cada módulo organiza os pacotes por domínio: `br.com.painel.<dominio>.model`, `.model.vo`, `.repository`, `.service` ou `.resource`. O fluxo de candidatos, por exemplo, fica em `CandidatoResource` → `CandidatoService` → `CandidatoRepository`. Empresa e rascunho têm resources próprios. Repositories do Spring Data continuam sendo interfaces implementadas pelo framework; os repositories Mongo específicos são classes concretas.

As dependências Maven seguem `model` → `repository` → `service` → `resource` → `boot`. O modelo não depende de persistência executável ou da API; a persistência não depende dos services. Services não importam `MongoTemplate`, BSON ou tipos HTTP. Erros de negócio são traduzidos em status HTTP no resource advice, conservando o envelope `mensagem`.

| Responsabilidade | Dono |
| --- | --- |
| Empresa, rascunho e permissões | Services respectivos, com revisão e auditoria na mesma transação |
| Usuários/vínculos e privilégios atuais | Políticas de acesso e repositories tipados; nenhum CRUD público novo |
| Candidatos e agenda | Services com validação e autorização; repositories limitam empresa, revisão, filtros e paginação |
| Controle contra desativação concorrente | `ControleEmpresaRepository`, chamado pela política dentro da transação do módulo |
| Auditoria | `AuditoriaRepository`, participando da transação do caso de uso |
| Prompt e histórico | `PromptGlobalService` coordena a operação atômica; `PromptGlobalRepository` persiste o agregado |
| Prévia | Consulta tipada e `CompositorPrompt`; não publica nem chama integração externa |
| Eventos e métricas | Ingestão valida/deduplica; consulta de métricas agrega dados em um instante de referência |
| Convites | Caso de uso transacional conserva empresa, usuário pendente, vínculo, rascunho, token e auditoria |
| Provisionamento | Adaptador de console chama o caso de uso; marcador técnico e criação inicial permanecem atômicos |
| Sessões, índices e bootstrap | Configuração em `chatbot-boot`; conversor de sessões permanentes preservado |

A coesão do caso de uso determina suas colaborações, sem impor uma cardinalidade artificial de repositories por service. DTOs, buckets de métricas e coleções técnicas não recebem endpoints artificiais.

Não houve troca de nomes de coleções/campos, UUIDs, índices ou revisão. Aliases mantêm a leitura e a escrita dos metadados `_class` antigos de documentos tipados após a mudança de pacotes. Os contratos JSON de entrada e saída permanecem explícitos em `model.vo`, com todos os campos anteriormente serializados, inclusive metadados de empresa e rascunho. A projeção da prévia preserva também os defaults dos documentos legados.

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
      configuracao/permissoes-modulos/
    candidatos/
      classes/model/
      classes/vo/
      services/
      cadastro/candidato-crud/
    agenda/               mesma divisão para agendamento
    bot/                  modelos, service e configuração
    convites/             modelos, service e ativação
    chatwoot/             modelos, service e configuração do vínculo
    administracao/        modelos, services de prompt/métricas e painel
```

Há uma classe TypeScript por arquivo de modelo. Elas representam os contratos expostos pelo Java, sem copiar infraestrutura de Mongo para o navegador. `RestService` faz HTTP, prepara CSRF para mutações e instancia explicitamente os modelos na fronteira; os services de domínio retornam `Observable` tipado. O componente mantém formulário e apresentação. `UiModule` reúne as dependências visuais comuns, seguindo a organização do GRP sem introduzir seus componentes proprietários ou trocar a biblioteca visual existente.

Candidatos e agenda têm telas, formulários, filtros e modelos próprios. A agenda continua usando o fuso do navegador. Prompt e métricas têm services separados. As consultas usam gerações/contexto para rejeitar respostas antigas; mutações não recebem retry automático. Formulários ficam bloqueados durante a gravação, e uma atualização de outro editor não renova a revisão de um formulário sujo. Guardas cobrem saída, alteração de empresa e histórico do navegador, inclusive o cancelamento do descarte.

A seleção da empresa fica somente no cabeçalho, conforme a decisão do usuário. Tema, utilitários e ajustes internos do Material ficam globais; estilos de módulos, administração, permissões e convites foram separados sem alterar a finalidade dos seletores.

## Distribuição e validação

O Dockerfile da raiz monta Angular dentro de `chatbot-boot/target/painel-bot-0.1.0.jar`. CI e scripts usam o agregador Maven; os testes ficam no módulo executável. `infra/test-browser.sh` aceita opcionalmente `E2E_IMAGE` para testar a imagem real com a mesma fixture descartável.

Java 25 LTS foi alinhado separadamente da reorganização. Não houve atualização de Spring Boot, benchmark, publicação ou deploy. Resultados e comandos estão em [validation.md](validation.md).
