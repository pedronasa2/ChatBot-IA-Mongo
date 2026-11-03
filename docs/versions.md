# Versões fixadas e alinhamento de Java 25 LTS

| Componente | Versão | Origem |
| --- | --- | --- |
| Angular, CLI, build, Material e CDK | 22.2.1 | tags estáveis do registro npm |
| TypeScript | 6.0.3 | última versão no intervalo >=6.0 <6.1 exigido pelo Angular 22.2.1 |
| RxJS | 7.8.2 | registro npm |
| Node | 26.10.0 | https://nodejs.org/dist/index.json |
| Java | Temurin 25.0.5+8 LTS | API oficial Adoptium; build/testes locais e imagem em Java 25 |
| Spring Boot | 4.1.1 | Maven Central e https://docs.spring.io/spring-boot/system-requirements.html |
| Maven | 3.10.0 | última release sem sufixo preview no Maven Central |
| Nginx | 1.30.5 | canal stable da imagem oficial |
| Playwright | 1.63.0 | registro npm |
| Plus Jakarta Sans variável (Fontsource) | 5.3.0 | pacote npm fixado, fonte servida localmente |

TypeScript 7.0.2 não satisfaz a restrição do Angular; foi selecionada a última versão compatível. A versão-alvo deste projeto é Java 25 LTS, dentro da matriz da release Spring Boot 4.1.1 fixada. Não são usadas versões preview. Dependências npm são exatas e o lockfile deve ser mantido; dependências Java são fixadas pelo BOM do Spring Boot 4.1.1, com versões explícitas para ferramentas externas. As imagens Docker usam tags de versão; um deploy reproduzível deve registrar também os digests após construir e verificar a imagem na infraestrutura de destino.

Compatibilidade Angular: https://angular.dev/reference/versions e metadados `peerDependencies`/`engines` dos pacotes oficiais publicados. Para revalidar: `npm view @angular/core version`, `npm view @angular/compiler-cli peerDependencies --json`, `npm view @angular/build engines --json`.

## Variante MongoDB

MongoDB Spring Session 4.0.0; Spring Data MongoDB e driver Java gerenciados pelo BOM do Spring Boot. Compose local usa MongoDB 8.0 por padrão; testes nesta máquina usaram 7.0 por incompatibilidade do kernel com 8.0. Sem JPA, JDBC ou Flyway em runtime.


A matriz da release fixada foi conferida no [fonte oficial do Spring Boot v4.1.1](https://github.com/spring-projects/spring-boot/blob/v4.1.1/documentation/spring-boot-docs/src/docs/antora/modules/ROOT/pages/system-requirements.adoc): Java 17 até 26. A URL genérica de documentação pode redirecionar para uma versão posterior e não foi usada como prova da release instalada. Spring Boot e as dependências não foram atualizados nesta refatoração.

Build e runtime Docker usam `maven:3.10.0-eclipse-temurin-25` e `eclipse-temurin:25.0.5_8-jre-noble`, com manifests verificados e construção local executada. O Maven local era 3.8.7; o container usa 3.10.0. Não houve benchmark nem mudança global do Java do sistema.
