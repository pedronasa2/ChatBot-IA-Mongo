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
