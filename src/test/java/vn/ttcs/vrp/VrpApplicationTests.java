package vn.ttcs.vrp;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test — chỉ chạy khi có PostgreSQL connection.
 * CI pipeline dùng VrpSolverTest + ApiIntegrationTest (không cần DB).
 */
@SpringBootTest
@Disabled("Requires PostgreSQL — run manually or in Docker CI")
class VrpApplicationTests {

	@Test
	void contextLoads() {
	}

}
