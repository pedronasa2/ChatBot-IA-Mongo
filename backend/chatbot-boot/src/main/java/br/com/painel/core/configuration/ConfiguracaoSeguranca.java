package br.com.painel.core.configuration;

import br.com.painel.autenticacao.repository.UsuarioRepository;

import org.springframework.context.annotation.*;
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
    UserDetailsService users(UsuarioRepository usuarios) {
        return email -> {
            var u = usuarios.findByEmail(email.toLowerCase(java.util.Locale.ROOT)).orElse(null);
            if (u == null) {
                throw new UsernameNotFoundException("Credenciais inválidas");
            }
            return User.withUsername(u.email)
                    .password(u.hashSenha)
                    .disabled(!u.ativo)
                    .roles("USER")
                    .build();
        };
    }

    @Bean
    @Profile("!provision")
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/auth/csrf",
                                                "/api/auth/login",
                                                "/api/auth/ativar-convite",
                                                "/api/integracoes/mensagens",
                                                "/api/integracoes/empresas/**",
                                                "/error")
                                        .permitAll()
                                        .requestMatchers("/api/**")
                                        .authenticated()
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.GET, "/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .headers(
                        h ->
                                h.contentSecurityPolicy(
                                        c ->
                                                c.policyDirectives(
                                                        "default-src 'self'; script-src 'self';"
                                                            + " style-src 'self' 'unsafe-inline';"
                                                            + " img-src 'self' data:; font-src"
                                                            + " 'self'; connect-src 'self';"
                                                            + " frame-ancestors 'none'; base-uri"
                                                            + " 'self'; form-action 'self'")))
                .csrf(
                        c ->
                                c.ignoringRequestMatchers(
                                        "/api/integracoes/mensagens",
                                        "/api/integracoes/empresas/**"))
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
                                        .logoutSuccessHandler(
                                                (req, res, auth) -> res.setStatus(204)))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint((req, res, ex) -> res.sendError(401))
                                        .accessDeniedHandler((req, res, ex) -> res.sendError(403)));
        return http.build();
    }
}
