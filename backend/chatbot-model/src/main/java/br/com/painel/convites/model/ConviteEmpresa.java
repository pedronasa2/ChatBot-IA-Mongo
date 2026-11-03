package br.com.painel.convites.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.*;

import java.time.Instant;
import java.util.UUID;

@org.springframework.data.annotation.TypeAlias("br.com.painel.convites.ConviteEmpresa")
@Document(collection = "convites_empresas")
public class ConviteEmpresa {
    @Id
    public org.bson.types.ObjectId id;

    @Field("empresa_id")
    public UUID empresaId;

    @Field("usuario_id")
    public UUID usuarioId;

    public String email;

    @Field("token_hash")
    public String tokenHash;

    @Field("expira_em")
    public Instant expiraEm;

    @Field("usado_em")
    public Instant usadoEm;

    @Field("criado_em")
    public Instant criadoEm;
}
