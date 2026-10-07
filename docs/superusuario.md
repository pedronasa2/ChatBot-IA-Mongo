# Painel do desenvolvedor

O login continua sendo o mesmo. O menu **Superusuário** fica disponível apenas para `pedronasa2@gmail.com`, com permissão independente dos vínculos com empresas. A API confere a permissão atual no banco a cada acesso; uma sessão persistente não conserva uma permissão revogada. Não existe tela ou endpoint para uma empresa conceder esse acesso. O índice `um_superusuario_do_sistema` permite apenas um usuário marcado como superusuário.

## Prompt global

O campo **System prompt supremo** salva até 40.000 caracteres em `configuracoes_globais`. Cada gravação cria uma versão em `historico_prompts_globais`, com autor e data. Revisões impedem sobrescrever uma alteração feita em outra sessão. O painel apresenta as 20 versões mais recentes; as anteriores permanecem no banco.

A prévia coloca as regras globais primeiro e, depois, os dados da empresa e o rascunho do bot, explicitando a prioridade das regras globais. Essa prévia não publica nem envia mensagens. O fluxo de atendimento ainda precisa consumir esse contexto; a integração de produção com o agente n8n não foi alterada nesta entrega.

## Métricas

- Mensagens recebidas e enviadas com sucesso no último minuto (janela móvel de 60 segundos).
- Total de mensagens e mensagens nas últimas 24 horas.
- Falhas nas últimas 24 horas, separadas dos envios com sucesso.
- Volume por minuto nos últimos 15 minutos e totais por empresa.
- Usuários ativos, empresas cadastradas e data da última mensagem.

A atualização automática ocorre a cada 30 segundos enquanto a seção está visível; o botão permite atualizar antes. Sem eventos, o painel indica que está aguardando a coleta, sem criar dados fictícios. As estatísticas começam quando o fluxo passar a chamar a API.

## Como adicionar ao fluxo n8n

Use a sequência **Enviar mensagem → Confirmar sucesso → Preparar evento → HTTP Request: registrar métrica**. Registre recebimentos em um ponto separado do fluxo de entrada, com `direcao=recebida`. Para falhas, use uma saída de erro separada com `resultado=erro`; elas não aumentam o total de mensagens bem-sucedidas.

No nó HTTP Request:

1. Método `POST` e URL `<URL_DO_BACKEND>/api/integracoes/mensagens`.
2. Autenticação genérica **Header Auth**, com cabeçalho `X-Token-Metricas` e o token de integração nas credenciais do n8n.
3. Ative **Send Body**, selecione **JSON** e envie os campos abaixo.

```json
{
  "empresaId": "UUID_DA_EMPRESA_NO_PAINEL",
  "origem": "chatwoot:instancia-principal",
  "idEventoOrigem": "ID_DA_MENSAGEM:enviada:sucesso",
  "direcao": "enviada",
  "resultado": "sucesso",
  "ocorridoEm": "DATA_DO_ENVIO_EM_ISO_8601"
}
```

Substitua os valores pelos dados do seu fluxo. O ID deve usar o identificador estável retornado pelo provedor, e a data deve representar o envio, não uma nova data gerada a cada tentativa de registrar a métrica. Se o provedor não devolver ID, crie um identificador lógico de envio antes de enviar e conserve-o nas repetições. Não use apenas o ID da execução do n8n: uma reexecução pode produzir outro ID e contar novamente o mesmo envio.

A combinação `origem` + `idEventoOrigem` é única no PostgreSQL. Um evento novo responde `201` com `registrado=true`; repetições respondem `200` com `registrado=false`. Não é necessário buscar um contador e somar 1: o backend grava o evento uma vez e calcula as métricas pelos registros e pelas datas. Para o mesmo envio, sucesso e falha são eventos diferentes, por isso o exemplo inclui direção e resultado no identificador.

Se o nó de métricas falhar, tente novamente **apenas o registro da métrica**, mantendo o mesmo ID e a mesma data. Não repita o nó de envio só para tentar atualizar o contador, pois isso pode mandar a mensagem de novo. O registro reflete a confirmação do provedor; confirmação de entrega/leitura é outra métrica, ainda não implementada.

O token está em `backend/config/integracao-local.properties`, ignorado pelo Git, e em `infra/.env` para execução pelo script/Compose. Em outros ambientes, configure `METRICS_INGESTION_TOKEN`. O token permite somente registrar eventos: não autentica no painel e não permite ler ou alterar o prompt. Sem token configurado, a coleta fica desabilitada. A API armazena metadados do evento, sem conteúdo da conversa.

A URL é a do **backend**, não a do PostgreSQL. Para testar nesta máquina, use `http://127.0.0.1:8087`. Se o n8n estiver em outro servidor, esse endereço aponta para o próprio servidor do n8n: o backend precisa de uma URL acessível a ele, preferencialmente HTTPS. O backend local não foi publicado nesta entrega.

Referências oficiais: [nó HTTP Request](https://docs.n8n.io/integrations/builtin/core-nodes/n8n-nodes-base.httprequest/) e [credenciais HTTP Request](https://docs.n8n.io/integrations/builtin/credentials/httprequest/).

## Contratos da API

Todos os endpoints de administração exigem login e permissão de superusuário. PUT exige CSRF.

| Método | Rota | Finalidade |
| --- | --- | --- |
| GET / PUT | `/api/superusuario/prompt-global` | Consultar e salvar regras globais |
| GET | `/api/superusuario/prompt-global/historico` | Consultar últimas 20 versões |
| GET | `/api/superusuario/prompt-global/previa/{empresaId}` | Prévia com rascunho da empresa |
| GET | `/api/superusuario/metricas` | Métricas globais e por empresa |
| POST | `/api/integracoes/mensagens` | Receber evento com token de integração |

PUT do prompt recebe `promptSistema`, `complementarConfiguracoesEmpresa=true` e `revisao`. O endpoint de eventos usa somente o token próprio, sem cookie de login ou CSRF. Eventos futuros além de cinco segundos são recusados; IDs e empresa devem ser válidos.

## Negócios dos clientes

O superusuário ativo visualiza todas as empresas na aba Negócios e no seletor de empresa. Pode consultar e editar os dados cadastrais e o rascunho do bot da empresa selecionada, mesmo sem vínculo empresarial. As alterações mantêm validação, controle de revisão, CSRF e auditoria com o usuário responsável. Usuários comuns continuam vendo Meu negócio e acessam apenas empresas com vínculo. Revogar o privilégio retira o acesso global nas chamadas seguintes.
