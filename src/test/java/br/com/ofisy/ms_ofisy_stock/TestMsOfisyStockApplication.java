package br.com.ofisy.ms_ofisy_stock;

import org.springframework.boot.SpringApplication;

public class TestMsOfisyStockApplication {

	public static void main(String[] args) {
		SpringApplication.from(MsOfisyStockApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
