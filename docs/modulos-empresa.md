# Módulos opcionais por empresa

As permissões `moduloCandidatosHabilitado` e `moduloAgendaHabilitado` pertencem à empresa. Ambas começam em `false`, para empresas existentes e novas. São independentes: nenhum módulo, apenas candidatos, apenas agenda ou ambos.

## Controle no painel

O superusuário seleciona a empresa no cabeçalho, abre **Negócios** e altera os controles em **Módulos deste negócio**. O botão **Salvar permissões dos módulos** grava os dois valores para aquela empresa, com controle de revisão e registro de auditoria. Clientes não recebem esses controles nem podem alterar as permissões pelo formulário normal do negócio.

Quando habilitado, o item aparece no menu de todos os usuários autorizados da empresa. Proprietários e editores podem consultar e gerenciar os registros conforme os papéis existentes; usuários inativos ou sem vínculo são recusados. O superusuário também precisa habilitar o módulo para consultar seus registros.

A aplicação verifica as permissões no acesso à rota, ao retornar à janela e a cada 15 segundos enquanto estiver visível. Se o módulo for desabilitado durante o uso, a tela é fechada e seu menu é removido. Toda operação do backend consulta o estado atual no banco, sem depender do cache do navegador ou da sessão. Operações em andamento mantêm um bloqueio compartilhado na empresa até terminar; a desabilitação aguarda essas operações e impede as seguintes.

## Banco de candidatos

Cadastro, consulta, busca, edição e arquivamento de candidatos, com nome completo, e-mail, telefone, cargo pretendido, link do currículo, resumo profissional, observações e situação (`novo`, `em_analise`, `aprovado`, `reprovado`). O currículo é referenciado por um link do provedor/armazenamento; não há upload de arquivos nesta entrega. O botão de abertura do currículo verifica novamente o vínculo e a habilitação no backend.

## Agenda de atendimentos

Cadastro, consulta, busca, filtro por período, edição e arquivamento, com pessoa, contatos, assunto, responsável, início, fim, situação (`agendado`, `confirmado`, `concluido`, `cancelado`) e observações. O fim deve ser posterior ao início. Datas são armazenadas como instantes UTC e apresentadas/editadas no fuso do navegador, identificado na tela. Não há restrição automática de sobreposição, pois uma empresa pode ter mais de um responsável atendendo.

## Preservação dos dados

Desabilitar um módulo altera somente as permissões da empresa: não exclui candidatos ou agendamentos. Ao reabilitar, os registros anteriores voltam a ser consultáveis. **Arquivar** também conserva o registro; para restaurar, marque **Incluir arquivados**, abra a edição e desmarque **Registro arquivado**.

## Rotas e contratos

Rotas protegidas do Angular:

- `/empresas/{empresaId}/candidatos`
- `/empresas/{empresaId}/agenda`

URL direta passa pela verificação de habilitação e acesso à empresa. Quando recusada, a navegação retorna à visão geral sem abrir o módulo.

API do painel, com sessão autenticada e CSRF nas mutações:

| Método | Rota | Operação |
| --- | --- | --- |
| GET / POST | `/api/empresas/{empresaId}/candidatos` | Listar / cadastrar |
| GET / PUT / DELETE | `/api/empresas/{empresaId}/candidatos/{id}` | Consultar / editar / arquivar |
| GET | `/api/empresas/{empresaId}/candidatos/{id}/curriculo` | Abrir link do currículo após autorização |
| GET / POST | `/api/empresas/{empresaId}/agenda` | Listar / cadastrar |
| GET / PUT / DELETE | `/api/empresas/{empresaId}/agenda/{id}` | Consultar / editar / arquivar |
| PUT | `/api/superusuario/empresas/{empresaId}/modulos` | Alterar permissões, somente superusuário |

Listas retornam `{registros,total,pagina,tamanho}`. Parâmetros: `busca`, `incluirArquivados`, `pagina` (a partir de zero) e `tamanho` (1–100). Agenda também aceita `de` e `ate` como datas ISO 8601. PUT exige a `revisao` atual do registro; DELETE recebe `?revisao=N`. Revisão obsoleta retorna 409. IDs são sempre pesquisados dentro da empresa da rota, inclusive quando o usuário tem acesso a mais de uma empresa.

Permissões recebem:

```json
{
  "moduloCandidatosHabilitado": true,
  "moduloAgendaHabilitado": false,
  "revisao": 0
}
```

A revisão é a da empresa e a resposta é a empresa atualizada. O formulário normal do negócio não altera essas flags.

## Integração n8n

Use a API dedicada, com token do serviço de automação em `X-Token-Modulos`:

- `/api/integracoes/empresas/{empresaId}/candidatos`
- `/api/integracoes/empresas/{empresaId}/candidatos/{id}`
- `/api/integracoes/empresas/{empresaId}/agenda`
- `/api/integracoes/empresas/{empresaId}/agenda/{id}`

Os métodos, corpos e paginação são os mesmos da API do painel. Todas as operações verificam os flags atuais da empresa. Módulo desabilitado retorna **403**, sem consultar ou alterar seus registros. O fluxo deve tratar esse retorno como recurso indisponível e interromper as ações desse módulo. Cookie de cliente e token de métricas não substituem esta credencial.

Configure `MODULES_INTEGRATION_TOKEN` no backend/n8n. No desenvolvimento, a credencial está em `backend/config/integracao-local.properties` e `infra/.env`, arquivos ignorados pelo Git. É uma credencial do serviço confiável de automação, que permite operar os módulos habilitados das empresas indicadas na rota; não é fornecida aos clientes e não permite habilitar módulos, editar o prompt global ou administrar empresas. Sem token, a integração fica desabilitada. Use os endpoints da aplicação, em vez de acesso direto às tabelas pelo fluxo n8n.

Exemplo de candidato:

```json
{
  "nomeCompleto": "Ana Silva",
  "email": "ana@example.com",
  "telefone": "",
  "cargoPretendido": "Analista",
  "urlCurriculo": "https://armazenamento.example/curriculo.pdf",
  "resumoProfissional": "Experiência em atendimento",
  "observacoes": "",
  "situacao": "novo",
  "arquivado": false,
  "revisao": 0
}
```

Exemplo de agendamento:

```json
{
  "nomePessoa": "Ana Silva",
  "email": "ana@example.com",
  "telefone": "",
  "assunto": "Entrevista",
  "responsavel": "Pedro",
  "inicio": "2026-10-20T13:00:00Z",
  "fim": "2026-10-20T14:00:00Z",
  "situacao": "agendado",
  "observacoes": "",
  "arquivado": false,
  "revisao": 0
}
```

O workflow real não foi alterado; estes endpoints estão prontos para conexão aos nós do fluxo.
