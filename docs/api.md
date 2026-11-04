# Contrato HTTP

O painel usa sessão e CSRF nas mutações. A integração de métricas usa token próprio; sessão não substitui essa credencial. Não há CORS aberto.

| Método | Rota | Contrato |
| --- | --- | --- |
| GET | `/api/auth/csrf` | `{token, headerName}` |
| POST | `/api/auth/login` | Formulário com `email`, `senha`; 204 ou 401 |
| GET | `/api/auth/me` | `{email, perfil}`, com `SUPERUSUARIO` ou `USUARIO` |
| POST | `/api/auth/logout` | Invalida sessão; 204 |
| GET | `/api/empresas` | Usuário comum vê sua empresa; superusuário vê todas |
| GET / PUT | `/api/empresas/{id}` | Consulta empresa completa / atualiza dados do negócio |
| PUT | `/api/empresas/{id}/bot` | Atualiza objeto bot e devolve a empresa completa |
| PUT | `/api/superusuario/empresas/{id}/chatwoot` | Atualiza identificadores na empresa; exclusivo do superusuário |
| GET / PUT | `/api/superusuario/prompt-global` | Consulta / atualiza configuração global atual |
| GET | `/api/superusuario/prompt-global/previa/{empresaId}` | Compõe prompt global e configuração atual da empresa |
| GET | `/api/superusuario/metricas` | Totais e gráfico; exclusivo do superusuário |

GET de empresa inclui `bot`, `chatwootAccountId`, `chatwootInboxId` e `revisao`, sem dados de login ou convite. PUT de negócio recebe `nome`, `descricao`, `endereco`, `contato`, `site`, `servicos`, `fusoHorario`, `revisao`. PUT de bot recebe `nome`, `tomVoz`, `usarEmojis`, `instrucoes`, `instrucoesTransferencia`, `revisao`. PUT Chatwoot recebe `chatwootAccountId`, `chatwootInboxId`, `revisao`. IDs Chatwoot são inteiros positivos até o limite seguro de JavaScript; a combinação deve pertencer a uma única empresa.

Esses três editores compartilham a revisão da empresa. Revisão antiga retorna 409. Campos adicionais não concedem privilégio nem alteram empresa, ID ou estado ativo do bot.

A configuração global recebe `promptGlobal`, `complementarConfiguracoesEmpresa` e `revisao`. Não há endpoint de histórico. A prévia não envia mensagens nem publica configurações no n8n.

## Empresa e convite

- `POST /api/superusuario/empresas`: recebe `nome`, `responsavel`, `email`; cria empresa, usuário pendente e convite embutido em transação; 201. Retorna `empresaId`, `email`, `token`, `expiraEm`, `envioSimulado: true`.
- `GET /api/superusuario/empresas/{id}/convite`: destinatário, responsável, validade e estado; nunca retorna token/hash.
- `POST /api/superusuario/empresas/{id}/convite`: renova convite de conta ainda inativa e invalida o token anterior.
- `POST /api/auth/ativar-convite`: público com CSRF; recebe `token`, `email`, `senha`, `confirmacaoSenha`. Ativação e consumo do convite são uma única atualização condicional do usuário; 204. Token expirado, utilizado ou de outro e-mail é recusado.

Não há envio de e-mail nesta versão. O token aleatório tem 256 bits, validade de 48 horas e uso único; somente seu SHA-256 fica no banco. O link de teste usa o fragmento `/#convite=...`. Senha e token não são devolvidos em consultas da empresa.

## Mensagens e gráficos

`POST /api/integracoes/mensagens` exige `X-Token-Metricas` e `Idempotency-Key` (ObjectId de 24 caracteres hexadecimais). Gere o identificador uma vez por mensagem e o reutilize em todas as tentativas. Registre somente depois do envio confirmado:

```json
{
  "empresaId": "UUID da empresa",
  "enviadoEm": "2026-11-04T15:30:12.450Z"
}
```

Novo registro retorna 201 com `registrado: true`; repetição idêntica retorna 200 com `registrado: false`; identificador reutilizado com outro conteúdo retorna 409. Data futura, identificador inválido ou campos ausentes retornam 400. Empresa inexistente retorna 404. O token fica somente no backend e na automação, configurado por `METRICS_INGESTION_TOKEN`.

A consulta de métricas aceita `empresaId`, `de`, `ate` (ISO-8601) e `intervalo` (`minute`, `hour`, `day`). `de` é inclusivo e `ate` exclusivo. Sem datas, usa os últimos 15 minutos, 24 horas ou 30 dias, conforme o agrupamento. Período inválido, futuro ou superior a aproximadamente 1.000 intervalos retorna 400.

A resposta traz `volume: [{inicio, mensagens}]`, `fusoHorario`, `porEmpresa`, totais de mensagens, último minuto, últimas 24 horas, empresas e usuários ativos, última mensagem e instante de atualização. Não há métricas de recebimentos ou erros. O período filtra o gráfico; os cartões e a tabela mantêm os totais e janelas indicados em seus rótulos.

Falhas tratadas retornam `{mensagem}`. Sessão ausente retorna 401; CSRF inválido ou privilégio insuficiente retorna 403; empresa fora do acesso retorna 404. Cookie SESSION permanece HttpOnly, SameSite=Lax e Secure em produção, sem expiração por inatividade. Renove CSRF depois de login/logout.
