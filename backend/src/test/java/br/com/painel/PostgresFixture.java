package br.com.painel;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

public class PostgresFixture {
  public static final EmbeddedPostgres DB;

  static {
    try {
      DB = EmbeddedPostgres.builder().setPort(0).start();
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }
}
