package br.com.painel.superusuario.repository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UsoSistemaRepository {

    private static final List<String> CHAVES = List.of("atendeflow:uso:logins");

    private static final RedisScript<Long> REGISTRAR_LOGIN =
            RedisScript.of("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

    private static final RedisScript<Long> CONSULTAR_LOGIN =
            RedisScript.of("return redis.call('BF.EXISTS', KEYS[1], ARGV[1])", Long.class);

    private static final RedisScript<Long> CONTAR_USUARIOS =
            RedisScript.of("return redis.call('BF.CARD', KEYS[1])", Long.class);

    private final StringRedisTemplate redis;

    public UsoSistemaRepository(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void registrarLogin(String email) {
        redis.execute(REGISTRAR_LOGIN, CHAVES, email);
    }

    public boolean possivelmenteFezLogin(String email) {
        return redis.execute(CONSULTAR_LOGIN, CHAVES, email) == 1L;
    }

    public long estimarUsuarios() {
        return redis.execute(CONTAR_USUARIOS, CHAVES);
    }
}
