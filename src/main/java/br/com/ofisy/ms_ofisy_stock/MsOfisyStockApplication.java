package br.com.ofisy.ms_ofisy_stock;

import br.com.ofisy.ms_ofisy_stock.config.jwt.JwtProperties;
import br.com.ofisy.ms_ofisy_stock.config.security.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
public class MsOfisyStockApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsOfisyStockApplication.class, args);
	}

}
