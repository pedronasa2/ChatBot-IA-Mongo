package br.com.painel.empresas.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.*;

import java.util.UUID;

@org.springframework.data.annotation.TypeAlias("br.com.painel.empresas.VinculoUsuarioEmpresa")
@Document(collection = "vinculos_usuarios_empresas")
public class VinculoUsuarioEmpresa {
    @Id
    public UUID id;

    @Field("usuario_id")
    public UUID usuarioId;

    @Field("empresa_id")
    public UUID empresaId;

    public String papel;

    @Field("criado_em")
    public java.time.Instant criadoEm;
}
