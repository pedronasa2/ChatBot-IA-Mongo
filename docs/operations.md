# Containers, GitHub e EasyPanel

## Caminho da atualização

1. Push na branch `main`.
2. GitHub Actions executa testes em banco descartável e navegador.
3. O Dockerfile da raiz compila Java e Angular no runner do GitHub.
4. GHCR recebe a imagem unificada com tag do commit.
5. Depois que ela está disponível, o workflow promove `latest`.
6. Se habilitado, chama o gatilho de deploy do EasyPanel. A VPS baixa a imagem e recria o serviço.

A imagem não contém chave MongoDB, token n8n ou conta do painel. Compilar não depende do banco da VPS. O uso de Actions/Packages segue as cotas e cobrança da conta GitHub.

## Implantação de imagens prontas

Use `infra/compose.images.yml` como base. Cadastre acesso ao GHCR privado no host/EasyPanel com credencial somente de leitura dos pacotes. Configure `MONGODB_URI`, tokens de integração e `COOKIE_SECURE=true`. O Angular é servido pelo próprio Spring Boot, na porta interna 8080. Basta um serviço App com a imagem `ghcr.io/pedronasa2/chatbot-ia-mongo-backend:latest`; o nome foi mantido para reaproveitar a credencial já cadastrada.

Publique esse serviço por HTTPS. A porta 4208 do exemplo só escuta no loopback do host; o proxy do EasyPanel deve alcançar o serviço internamente. O MongoDB fica apenas na rede interna. O MongoDB instalado no EasyPanel precisa de replica set; uma instalação standalone padrão deve ser preparada separadamente, com backup antes de qualquer mudança.

Para implantar manualmente, autentique o Docker no GHCR por entrada segura e execute:

```bash
docker compose --env-file infra/.env -f infra/compose.images.yml pull
docker compose --env-file infra/.env -f infra/compose.images.yml up -d --no-build
```

Para a primeira inicialização, rode `run --rm provision` nesse mesmo Compose em terminal interativo. Cria o único superusuário. Não há senha fixa.

## Atualização automática

Depois de criar e validar o serviço, obtenha a Deployment Trigger URL no EasyPanel e cadastre-a no GitHub como secret `EASYPANEL_DEPLOY_URL`. Cadastre a variável de repositório `EASYPANEL_AUTO_DEPLOY=true`. O gatilho precisa implantar a aplicação usando a imagem publicada. Não use o auto-deploy de código do GitHub em paralelo, pois ele pode disparar antes de as imagens terminarem.

O workflow não cria o serviço nem autentica o EasyPanel no GHCR. Também não confirma que uma implantação iniciada pelo webhook terminou saudável: verifique logs e acesso após a primeira implantação.

Para voltar uma versão, defina `IMAGE_TAG=sha-<commit-anterior>` e implante essa tag. Isso volta o código, não reverte dados. Alterações futuras de esquema precisam ser compatíveis com atualização e rollback.

## Backup

Use ferramentas oficiais `mongodump`/`mongorestore`, credenciais em arquivo protegido ou gerenciador de segredos, e destino criptografado fora da VPS. Obtenha snapshot consistente com a topologia e com as transações da aplicação; não trate dumps independentes durante gravações como uma cópia necessariamente consistente. Teste restauração em banco separado antes de depender do backup. Não remova volumes para atualizar.

Referências: [GitHub: publicar imagens](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images), [EasyPanel: imagens e gatilho de deploy](https://easypanel.io/docs/services/app).
