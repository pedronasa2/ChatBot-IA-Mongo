# Contrato HTTP — etapa A

Mesmo domínio do painel. Autenticação em sessão; todas as mutações requerem CSRF. Não há CORS aberto nem tokens de serviços no navegador.

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | /api/auth/csrf | `{token, headerName}` para a sessão atual |
| POST | /api/auth/login | form-urlencoded com `email`, `senha`; CSRF; 204 ou 401 |
| GET | /api/auth/me | `{email, superusuario}`; 401 sem sessão |
| POST | /api/auth/logout | CSRF; invalida sessão; 204 |
| GET | /api/empresas | apenas empresas de que o usuário é membro |
| GET / PUT | /api/empresas/{uuid} | dados do negócio |
| GET / PUT | /api/empresas/{uuid}/rascunho | rascunho do bot |

Depois de login e logout, obtenha novo token CSRF. Cookie SESSION é persistente, HttpOnly, SameSite=Lax e Secure em produção; é renovado em cada acesso autenticado. Sessões JDBC têm intervalo negativo, sem expiração por inatividade; logout elimina a sessão e impede reutilizar o cookie. A credencial do login nunca é devolvida. Sem vínculo (ou usuário desativado), consultas e gravações por empresa retornam 404. Não há API de gestão de membros nesta etapa; proprietário e editor podem editar os mesmos campos.

PUT do negócio requer `nome`, `descricao`, `endereco`, `contato`, `site`, `servicos`, `fusoHorario`, `revisao`. Site vazio é permitido; preenchido exige URL HTTP/HTTPS sem credenciais. Fuso precisa existir em `ZoneId`. PUT de rascunho requer `nomeBot`, `tomVoz` (`cordial`, `formal`, `descontraido`), `usarEmojis`, `instrucoes`, `instrucoesTransferencia`, `revisao`. Campos adicionais do cliente não alteram IDs, empresa, estado, autor ou bot ativo. A revisão é obtida no GET; tentativa de gravar revisão obsoleta retorna 409.

Validação retorna 400; CSRF inválido retorna 403; sessão ausente/expirada retorna 401; falhas inesperadas retornam erro sem stack trace ou mensagem interna. Uma gravação só aparece como sucesso após confirmação do servidor. O frontend mantém dados locais após falha recuperável.

A API interna `/internal/bot-context/resolve` será definida e implementada na etapa C. Esta entrega não expõe contextos de produção e não possui credencial n8n.

Erros tratados pela API usam o atributo JSON `mensagem`. Os estados do bot são `rascunho` e `publicado`; o papel de proprietário usa `proprietario`.

O painel do desenvolvedor e a API de eventos estão documentados em [superusuario.md](superusuario.md). Permissão de superusuário é independente dos vínculos de empresa.
