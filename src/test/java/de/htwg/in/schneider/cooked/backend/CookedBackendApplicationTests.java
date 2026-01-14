package de.htwg.in.schneider.cooked.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import de.htwg.in.schneider.cooked.backend.config.TestSecurityConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class CookedBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
