package br.com.ofisy.ms_ofisy_stock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MsOfisyStockApplicationTests {

	@Test
	void contextLoads() {
	}

}
