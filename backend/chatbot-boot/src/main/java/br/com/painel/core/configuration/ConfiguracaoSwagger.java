package br.com.painel.core.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SwaggerUiConfigParameters;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "springdoc.swagger-ui.enabled", havingValue = "true")
public class ConfiguracaoSwagger {
    @Bean
    OpenAPI documentacaoApi() {
        return new OpenAPI().info(new Info().title("AtendeFlow API").version("0.1.0")
                .description("Para testar rotas protegidas, execute POST /api/auth/login com seu e-mail e senha. "
                        + "O navegador usa a sessão autenticada e obtém o token CSRF automaticamente nas gravações."));
    }

    @Bean
    OpenApiCustomizer documentarAutenticacao() {
        return api -> {
            var formulario = new ObjectSchema()
                    .addProperty("email", new StringSchema().format("email"))
                    .addProperty("senha", new StringSchema().format("password"))
                    .required(List.of("email", "senha"));
            api.path("/api/auth/login", new PathItem().post(new Operation()
                    .tags(List.of("Autenticação"))
                    .summary("Entrar no painel")
                    .requestBody(new RequestBody().required(true).content(new Content()
                            .addMediaType("application/x-www-form-urlencoded", new MediaType().schema(formulario))))
                    .responses(new ApiResponses()
                            .addApiResponse("204", new ApiResponse().description("Sessão iniciada"))
                            .addApiResponse("401", new ApiResponse().description("E-mail ou senha inválidos"))
                            .addApiResponse("403", new ApiResponse().description("CSRF inválido")))));
            api.path("/api/auth/logout", new PathItem().post(new Operation()
                    .tags(List.of("Autenticação"))
                    .summary("Sair do painel")
                    .responses(new ApiResponses().addApiResponse("204", new ApiResponse().description("Sessão encerrada")))));
        };
    }

    @Bean
    SwaggerIndexTransformer requisicoesSwagger(
            SwaggerUiConfigProperties configuracao, SwaggerUiOAuthProperties oauth,
            SwaggerWelcomeCommon welcome, ObjectMapperProvider mapper) {
        return new SwaggerIndexPageTransformer(configuracao, oauth, welcome, mapper) {
            @Override
            protected String defaultTransformations(SwaggerUiConfigParameters parametros, InputStream conteudo)
                    throws IOException {
                var inicializador = super.defaultTransformations(parametros, conteudo);
                return inicializador.replace("SwaggerUIBundle({", """
                        SwaggerUIBundle({
                          requestInterceptor: async function(request) {
                            const destino = new URL(request.url, window.location.href);
                            const leitura = ['GET', 'HEAD', 'OPTIONS'].includes((request.method || 'GET').toUpperCase());
                            if (destino.origin === window.location.origin && !leitura && destino.pathname.includes('/api/')) {
                              const base = destino.pathname.substring(0, destino.pathname.indexOf('/api/'));
                              const resposta = await fetch(base + '/api/auth/csrf', {credentials: 'same-origin'});
                              if (!resposta.ok) throw new Error('Não foi possível obter o token CSRF. Recarregue a página.');
                              const csrf = await resposta.json();
                              request.headers = request.headers || {};
                              request.headers[csrf.headerName] = csrf.token;
                            }
                            return request;
                          },
                        """);
            }
        };
    }
}
