# Contrato HTTP — etapa A

Mesmo domínio do painel. Autenticação do painel em sessão; suas mutações requerem CSRF. As integrações de módulos e métricas usam tokens exclusivos e não aceitam sessão como substituto. Não há CORS aberto nem tokens de serviços no navegador.

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | /api/auth/csrf | `{token, headerName}` para a sessão atual |
| POST | /api/auth/login | form-urlencoded com `email`, `senha`; CSRF; 204 ou 401 |
| GET | /api/auth/me | `{email, superusuario}`; 401 sem sessão |
| POST | /api/auth/logout | CSRF; invalida sessão; 204 |
| GET | /api/empresas | empresas vinculadas ao usuário ativo; superusuário ativo vê todas |
| GET / PUT | /api/empresas/{uuid} | dados do negócio |
| GET / PUT | /api/empresas/{uuid}/rascunho | rascunho do bot |

Depois de login e logout, obtenha novo token CSRF. Cookie SESSION é persistente, HttpOnly, SameSite=Lax e Secure em produção; é renovado em cada acesso autenticado. Sessões MongoDB têm intervalo negativo, sem expiração por inatividade; logout elimina a sessão e impede reutilizar o cookie. A credencial do login nunca é devolvida. Sem vínculo e sem privilégio atual de superusuário (ou usuário desativado), consultas e gravações por empresa retornam 404. Não há API de gestão de membros nesta etapa; proprietário e editor podem editar os mesmos campos.

PUT do negócio requer `nome`, `descricao`, `endereco`, `contato`, `site`, `servicos`, `fusoHorario`, `revisao`. Site vazio é permitido; preenchido exige URL HTTP/HTTPS sem credenciais. Fuso precisa existir em `ZoneId`. PUT de rascunho requer `nomeBot`, `tomVoz` (`cordial`, `formal`, `descontraido`), `usarEmojis`, `instrucoes`, `instrucoesTransferencia`, `revisao`. Campos adicionais do cliente não alteram IDs, empresa, estado, autor ou bot ativo. A revisão é obtida no GET; tentativa de gravar revisão obsoleta retorna 409.

Validação retorna 400; CSRF inválido retorna 403; sessão ausente/expirada retorna 401; falhas inesperadas retornam erro sem stack trace ou mensagem interna. Uma gravação só aparece como sucesso após confirmação do servidor. O frontend mantém dados locais após falha recuperável.

A API interna `/internal/bot-context/resolve` será definida e implementada na etapa C. Esta entrega não expõe contextos de produção. As credenciais já existentes das integrações de módulos e métricas são independentes dessa API futura.

Erros tratados pela API usam o atributo JSON `mensagem`. Os estados do bot são `rascunho` e `publicado`; o papel de proprietário usa `proprietario`.

O painel do desenvolvedor e a API de eventos estão documentados em [superusuario.md](superusuario.md). Permissão de superusuário é independente dos vínculos de empresa.

Módulos por empresa, rotas protegidas e integração n8n: [modulos-empresa.md](modulos-empresa.md).

## Cadastro de empresa e convite simulado

- `POST /api/superusuario/empresas`: exclusivo do superusuário, com CSRF. Recebe `nome`, `responsavel`, `email`, `moduloCandidatosHabilitado` e `moduloAgendaHabilitado`. Cria empresa, usuário inativo, vínculo de proprietário, rascunho do bot pausado e convite em uma transação. Retorna `empresaId`, `email`, `token`, `expiraEm` e `envioSimulado: true`. E-mail já cadastrado retorna 409; não redefine senhas de contas existentes.
- `GET /api/superusuario/empresas/{id}/convite`: consulta destinatário e estado (`pendente`, `aceito`, `expirado`, `sem_convite`), sem retornar o token.
- `POST /api/superusuario/empresas/{id}/convite`: gera outro convite simulado para conta ainda inativa e invalida o anterior. Exige superusuário e CSRF.
- `POST /api/auth/ativar-convite`: público, protegido por CSRF. Recebe `token`, `email`, `senha` e `confirmacaoSenha`. Exige senha de 6 a 21 caracteres (respeitando também o limite técnico de 72 bytes), confirmação idêntica e convite válido do mesmo e-mail. Ativa somente a conta pendente e consome o token na mesma transação. O cliente entra pelo login normal em seguida.

Não há envio de e-mail nesta versão. A área Negócios mostra o link de teste ao superusuário, explicitamente identificado como simulação. O token aleatório tem 256 bits, validade de 48 horas e uso único; somente seu SHA-256 é persistido. O link usa fragmento (`/#convite=...`), que não vai nos acessos HTTP ao frontend. Trate o link como credencial temporária; não compartilhe fora do destinatário previsto.

## Vínculo com Chatwoot

GET e PUT em `/api/superusuario/empresas/{empresaId}/chatwoot` são exclusivos do superusuário. PUT exige CSRF e recebe `accountId` e `inboxId`, números inteiros positivos. Uma combinação já usada por outra empresa retorna 409. O GET retorna IDs nulos quando ainda não existe vínculo.

No webhook atual, os valores vêm de `body.account.id` e `body.inbox.id`. Agora a própria coleção `empresas` guarda `chatwoot_account_id` e `chatwoot_inbox_id` como números. Um índice único parcial impede duplicar a combinação e permite empresas sem configuração. Na inicialização, os vínculos antigos são copiados para empresas ainda não configuradas, sem remover o legado nem sobrescrever configurações novas. O n8n pode localizar a empresa diretamente pelos dois campos. Não foi alterado o workflow do n8n. O vínculo considera uma instalação Chatwoot; múltiplas instalações exigirão também uma identificação de origem.
