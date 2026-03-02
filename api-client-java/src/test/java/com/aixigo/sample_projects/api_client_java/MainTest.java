package com.aixigo.sample_projects.api_client_java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

  @Test
  void projectHasMainClass() {
    // just checks the class exists & loads
    assertDoesNotThrow(() -> Class.forName("com.aixigo.sample_projects.api_client_java.Main"));
  }

  @Test
  void ensureNotNaNThrowsOnNaN() throws Exception {
    var m = Main.class.getDeclaredMethod("ensureNotNaN", double.class, String.class);
    m.setAccessible(true);

    // should not throw
    assertDoesNotThrow(() -> m.invoke(null, 1.0, "ok"));

    // should throw
    var ex = assertThrows(Exception.class, () -> m.invoke(null, Double.NaN, "bad"));
    assertNotNull(ex);
  }
}
