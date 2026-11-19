package br.com.painel.superusuario.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class UsoSistemaRepository {

    private static final RedisScript<Long> REGISTRAR_LOGIN = RedisScript.of("""
            local inserido = redis.call('BF.ADD', KEYS[1], ARGV[1])
            redis.call('EXPIRE', KEYS[1], 2592000, 'NX')
            return inserido
            """, Long.class);

    private static final RedisScript<Long> CONTAR_USUARIOS =
            RedisScript.of("return redis.call('BF.CARD', KEYS[1])", Long.class);

    private final StringRedisTemplate redis;

    public UsoSistemaRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void registrarLogin(LocalDate dia, String email) {
        redis.execute(REGISTRAR_LOGIN, List.of(chave(dia)), email);
    }

    public long estimarUsuarios(LocalDate dia) {
        return redis.execute(CONTAR_USUARIOS, List.of(chave(dia)));
    }

    private String chave(LocalDate dia) {
        return "atendeflow:uso:logins:" + dia;
    }
}
