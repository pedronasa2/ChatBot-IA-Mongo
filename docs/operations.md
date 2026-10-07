# Operação — preparação para Easypanel/VPS

O Compose entregue é de desenvolvimento. Seu build e execução em Docker ainda precisam ser verificados num host com Docker. Não foi feito deploy.

## Preparar produção

1. Use serviços PostgreSQL 18.6 com volume persistente em `/var/lib/postgresql` (layout das imagens PostgreSQL 18), backend e frontend na mesma rede privada. Banco e backend não devem publicar portas públicas.
2. Construa as imagens a partir deste código e lockfiles. Registre versões e digests das imagens; execute testes antes de promover.
3. Cadastre `DATABASE_PASSWORD`, URL e usuário JDBC no gerenciador de segredos do Easypanel. Não acrescente segredos ao frontend ou à imagem.
4. Direcione o domínio com HTTPS para o frontend. Defina `COOKIE_SECURE=true` no backend; confira pelo navegador cookie HttpOnly/Secure/SameSite e login/CSRF. Restrinja acesso à origem ao proxy confiável; não habilite confiança irrestrita em headers encaminhados.
5. Faça provisionamento em terminal interativo na rede privada com profile `provision`, sem iniciar servidor web. Desative esse caminho operacional após criar o primeiro usuário. Não há senha fixa.
6. As sessões ficam em `spring_session`/`spring_session_attributes` no PostgreSQL e não expiram por inatividade. Reiniciar o backend preserva o login. O botão Sair remove a sessão; inclua essas tabelas nos backups e mantenha acesso ao banco restrito. O cookie persistente é renovado durante o uso; retenção e limpeza de cookies são controladas pelo navegador.
7. Configure monitoramento de falhas, espaço livre, latência e reinícios. Não registre corpo de login, cookies ou conteúdo de prompts. A API de login requer limitação de tentativas no proxy antes de exposição pública.

## Backup e restauração

- Agende `pg_dump -Fc` com credencial obtida de arquivo protegido ou gerenciador de segredos, evitando senha em argumentos e logs.
- Envie backups criptografados para armazenamento fora da VPS, com retenção definida e alertas para falha. Backup no próprio volume não protege contra perda da VPS.
- Teste periodicamente a restauração em banco descartável com `pg_restore --exit-on-error`; valide usuários, vínculos, configurações e histórico Flyway, além de login e gravação pelo painel restaurado.
- Registre data, duração e resultado. Defina RPO/RTO a partir da frequência e tempo medidos; não foram medidos nesta entrega.
- Não apague o volume para atualizar. Leia incompatibilidades antes de mudar a versão principal do PostgreSQL e teste migrações em uma cópia restaurada.

## Capacidade

Não há promessa de quantidade de clientes. Meça memória e CPU em repouso e sob carga representativa, pool/conexões PostgreSQL, latência p95/p99, espaço e duração de backup/restauração antes de dimensionar. Serviços reais n8n/Chatwoot serão avaliados na etapa C com os acessos autorizados. Transferência para humano, autenticação de webhook e deduplicação são requisitos antes de ativar o atendimento real.
