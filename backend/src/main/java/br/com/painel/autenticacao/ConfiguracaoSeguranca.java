package br.com.painel.autenticacao;

import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracaoSeguranca {
  @Bean
  PasswordEncoder passwords() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService users(JdbcTemplate jdbc) {
    return email -> {
      var listar =
          jdbc.query(
              "select email,hash_senha,ativo from usuarios where email=?",
              (rs, n) ->
                  User.withUsername(rs.getString(1))
                      .password(rs.getString(2))
                      .disabled(!rs.getBoolean(3))
                      .roles("USER")
                      .build(),
              email.toLowerCase(java.util.Locale.ROOT));
      if (listar.isEmpty()) throw new UsernameNotFoundException("Credenciais inválidas");
      return listar.getFirst();
    };
  }

  @Bean
  @Profile("!provision")
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/integracoes/mensagens", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .csrf(c -> c.ignoringRequestMatchers("/api/integracoes/mensagens"))
        .formLogin(
            f ->
                f.loginProcessingUrl("/api/auth/login")
                    .usernameParameter("email")
                    .passwordParameter("senha")
                    .successHandler((req, res, auth) -> res.setStatus(204))
                    .failureHandler((req, res, e) -> res.sendError(401)))
        .logout(
            l ->
                l.logoutUrl("/api/auth/logout")
                    .deleteCookies("SESSION")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(204)))
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                    .accessDeniedHandler((req, res, ex) -> res.sendError(403)));
    return http.build();
  }
}
