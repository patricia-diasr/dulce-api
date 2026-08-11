package com.dulce.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de contexto padrão. Requer um PostgreSQL acessível (variáveis DB_HOST/DB_PORT/DB_NAME etc.,
 * ou os defaults do perfil "dev"), pois o Flyway roda a migração ao subir o contexto.
 */
@SpringBootTest
class DulceAPIApplicationTests {

  @Test
  void contextLoads() {}
}
