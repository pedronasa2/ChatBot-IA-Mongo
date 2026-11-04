# Administração do sistema

O atributo `Usuario.perfil` usa `SUPERUSUARIO` ou `USUARIO`. O administrador pode ter `empresaId` nulo e acessar todas as empresas. Usuários comuns acessam somente a empresa indicada no próprio documento. Privilégios são conferidos no banco em cada operação administrativa, inclusive quando a sessão já estava aberta.

A tela Superusuário permite editar o prompt global atual, visualizar sua composição com a configuração de uma empresa e consultar métricas de mensagens enviadas. O prompt fica em `configuracoes_sistema`, sem histórico de versões ou auditoria. Os filtros do gráfico permitem empresa, período e minuto/hora/dia; totais e últimas 24h têm as janelas indicadas nos rótulos.

Negócios permite criar empresa, simular convite para o primeiro acesso e salvar os identificadores Chatwoot na própria empresa. A seleção da empresa fica somente no cabeçalho. Convites ficam embutidos no usuário e não enviam e-mail nesta versão.

A coleta de mensagens usa `POST /api/integracoes/mensagens` com token de serviço e `Idempotency-Key`. O documento persistido tem somente `_id`, `empresaId` e `enviadoEm`. Não são guardados conteúdo, direção, origem nem resultado. Registre apenas envios confirmados e reutilize o mesmo ID nas tentativas da mesma mensagem.

Veja os contratos completos em [api.md](api.md), os documentos em [database.md](database.md) e o diagrama no [README](../README.md). O fluxo n8n externo precisa usar o novo contrato; alterar este repositório não publica nem modifica workflows.
