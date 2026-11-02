package br.com.painel.persistencia;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.*;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.query.*;

@Configuration
public class ConfiguracaoMongo {
  @Bean
  MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
    return new MongoTransactionManager(factory);
  }

  @Bean
  @org.springframework.core.annotation.Order(0)
  ApplicationRunner prepararMongo(MongoTemplate mongo) {
    return args -> {
      for (String nome :
          java.util.List.of(
              "usuarios",
              "empresas",
              "vinculos_usuarios_empresas",
              "versoes_configuracoes_bot",
              "eventos_auditoria",
              "configuracoes_globais",
              "historico_prompts_globais",
              "eventos_mensagens_atendimento",
              "candidatos",
              "agendamentos_atendimento",
              "provisionamento")) if (!mongo.collectionExists(nome)) mongo.createCollection(nome);
      mongo.indexOps("usuarios").ensureIndex(new Index().on("email", Sort.Direction.ASC).unique());
      mongo
          .indexOps("usuarios")
          .ensureIndex(
              new Index()
                  .on("superusuario", Sort.Direction.ASC)
                  .unique()
                  .partial(PartialIndexFilter.of(Criteria.where("superusuario").is(true))));
      mongo
          .indexOps("vinculos_usuarios_empresas")
          .ensureIndex(
              new Index()
                  .on("usuario_id", Sort.Direction.ASC)
                  .on("empresa_id", Sort.Direction.ASC)
                  .unique());
      mongo
          .indexOps("versoes_configuracoes_bot")
          .ensureIndex(
              new Index()
                  .on("empresa_id", Sort.Direction.ASC)
                  .on("versao", Sort.Direction.ASC)
                  .unique());
      mongo
          .indexOps("versoes_configuracoes_bot")
          .ensureIndex(
              new Index()
                  .on("empresa_id", Sort.Direction.ASC)
                  .unique()
                  .partial(PartialIndexFilter.of(Criteria.where("estado").is("rascunho"))));
      mongo
          .indexOps("historico_prompts_globais")
          .ensureIndex(new Index().on("revisao", Sort.Direction.ASC).unique());
      mongo
          .indexOps("eventos_mensagens_atendimento")
          .ensureIndex(
              new Index()
                  .on("origem", Sort.Direction.ASC)
                  .on("id_evento_origem", Sort.Direction.ASC)
                  .unique());
      mongo
          .indexOps("eventos_mensagens_atendimento")
          .ensureIndex(
              new Index()
                  .on("empresa_id", Sort.Direction.ASC)
                  .on("ocorrido_em", Sort.Direction.ASC));
      mongo
          .indexOps("eventos_mensagens_atendimento")
          .ensureIndex(new Index().on("ocorrido_em", Sort.Direction.ASC));
      mongo
          .indexOps("candidatos")
          .ensureIndex(
              new Index()
                  .on("empresa_id", Sort.Direction.ASC)
                  .on("recebido_em", Sort.Direction.DESC));
      mongo
          .indexOps("agendamentos_atendimento")
          .ensureIndex(
              new Index().on("empresa_id", Sort.Direction.ASC).on("inicio", Sort.Direction.ASC));
      mongo.upsert(
          Query.query(Criteria.where("_id").is(1)),
          new Update()
              .setOnInsert("prompt_sistema", "")
              .setOnInsert("complementar_configuracoes_empresa", true)
              .setOnInsert("revisao", 0L)
              .setOnInsert("atualizado_em", new java.util.Date()),
          "configuracoes_globais");
    };
  }
}
