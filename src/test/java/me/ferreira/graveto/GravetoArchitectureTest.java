package me.ferreira.graveto;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

public class GravetoArchitectureTest {

  @Test
  void verifyModularStructure() {

    final ApplicationModules applicationModules = ApplicationModules.of(GravetoApplication.class);
    applicationModules.verify();
  }

}
