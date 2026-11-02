package br.com.painel.autenticacao;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@org.mongodb.spring.session.config.annotation.web.http.EnableMongoHttpSession(
    maxInactiveIntervalInSeconds = -1)
@Profile("!provision")
public class ConfiguracaoSessaoPersistente {
  /** A biblioteca grava expireAt no passado para intervalo negativo; o TTL apagaria a sessão. */
  @Bean
  org.mongodb.spring.session.AbstractMongoSessionConverter sessionConverter() {
    return new org.mongodb.spring.session.JdkMongoSessionConverter(
        java.time.Duration.ofSeconds(-1)) {
      @Override
      protected org.bson.Document convert(org.mongodb.spring.session.MongoSession session) {
        var document = super.convert(session);
        if (session.getMaxInactiveInterval().isNegative()) document.remove("expireAt");
        return document;
      }

      @Override
      protected org.mongodb.spring.session.MongoSession convert(org.bson.Document document) {
        // O conversor exige uma data ao ler; sessões permanentes não têm data no Mongo.
        var copy = new org.bson.Document(document);
        if (!copy.containsKey("expireAt")) {
          copy.put(
              "expireAt", java.util.Date.from(java.time.Instant.parse("9999-12-31T00:00:00Z")));
        }
        return super.convert(copy);
      }
    };
  }

  @Bean
  CookieSerializer persistentCookieSerializer(
      @Value("${server.servlet.session.cookie.secure:true}") boolean secure) {
    var serializer = new DefaultCookieSerializer();
    serializer.setCookieName("SESSION");
    serializer.setCookiePath("/");
    serializer.setCookieMaxAge(Integer.MAX_VALUE);
    serializer.setUseHttpOnlyCookie(true);
    serializer.setUseSecureCookie(secure);
    serializer.setSameSite("Lax");
    return serializer;
  }

  /** Renova o cookie em cada uso para respeitar os limites de retenção do navegador. */
  @Bean
  OncePerRequestFilter refreshSessionCookie(CookieSerializer serializer) {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(
          HttpServletRequest request, HttpServletResponse response, FilterChain chain)
          throws ServletException, IOException {
        var session = request.getSession(false);
        if (session != null
            && session.getAttribute("SPRING_SECURITY_CONTEXT") != null
            && !request.getServletPath().equals("/api/auth/logout")) {
          serializer.writeCookieValue(
              new CookieSerializer.CookieValue(request, response, session.getId()));
        }
        chain.doFilter(request, response);
      }
    };
  }
}
