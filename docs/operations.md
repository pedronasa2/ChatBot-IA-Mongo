# Containers, GitHub e EasyPanel

## Caminho da atualização

1. Push na branch `main`.
2. GitHub Actions executa testes em banco descartável e navegador.
3. Dockerfiles compilam Java e Angular no runner do GitHub.
4. GHCR recebe imagens backend/frontend com tag do commit.
5. Depois que ambas estão disponíveis, o workflow promove `latest`.
6. Se habilitado, chama o gatilho de deploy do EasyPanel. A VPS baixa as imagens e recria os serviços.

A imagem não contém chave MongoDB, token n8n ou conta do painel. Compilar não depende do banco da VPS. O uso de Actions/Packages segue as cotas e cobrança da conta GitHub.

## Implantação de imagens prontas

Use `infra/compose.images.yml` como base. Cadastre acesso ao GHCR privado no host/EasyPanel com credencial somente de leitura dos pacotes. Configure `MONGODB_URI`, tokens de integração e `COOKIE_SECURE=true`. O frontend precisa alcançar o backend pelo nome `backend:8080` na rede do Compose. Ao usar serviços App separados, adapte esse destino e rede; não basta colar duas imagens isoladas.

Publique o frontend por HTTPS. A porta 4208 do exemplo só escuta no loopback do host; o proxy do EasyPanel deve alcançar o serviço internamente. Banco e backend não precisam de domínio público. O MongoDB instalado no EasyPanel precisa de replica set; uma instalação standalone padrão deve ser preparada separadamente, com backup antes de qualquer mudança. Não foi alterada por esta implementação.

Para implantar manualmente, autentique o Docker no GHCR por entrada segura e execute:

```bash
docker compose --env-file infra/.env -f infra/compose.images.yml pull
docker compose --env-file infra/.env -f infra/compose.images.yml up -d --no-build
```

Para a primeira inicialização, rode `run --rm provision` nesse mesmo Compose em terminal interativo. Cria o único superusuário. Não há senha fixa.

## Atualização automática

Depois de criar e validar o serviço, obtenha a Deployment Trigger URL no EasyPanel e cadastre-a no GitHub como secret `EASYPANEL_DEPLOY_URL`. Cadastre a variável de repositório `EASYPANEL_AUTO_DEPLOY=true`. O gatilho precisa implantar o conjunto backend/frontend usando as imagens publicadas. Não use o auto-deploy de código do GitHub em paralelo, pois ele pode disparar antes de as imagens terminarem.

O workflow não cria o serviço nem autentica o EasyPanel no GHCR. Também não confirma que uma implantação iniciada pelo webhook terminou saudável: verifique logs e acesso após a primeira implantação. Se a instalação não oferecer gatilho para o conjunto Compose, será necessário adaptar a etapa para os gatilhos dos dois serviços.

Para voltar uma versão, defina `IMAGE_TAG=sha-<commit-anterior>` e implante ambas com a mesma tag. Isso volta o código, não reverte dados. Alterações futuras de esquema precisam ser compatíveis com atualização e rollback.

## Backup

Use ferramentas oficiais `mongodump`/`mongorestore`, credenciais em arquivo protegido ou gerenciador de segredos, e destino criptografado fora da VPS. Obtenha snapshot consistente com a topologia e com as transações da aplicação; não trate dumps independentes durante gravações como uma cópia necessariamente consistente. Teste restauração em banco separado antes de depender do backup. Não remova volumes para atualizar.

Referências: [GitHub: publicar imagens](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images), [EasyPanel: imagens e gatilho de deploy](https://easypanel.io/docs/services/app).
