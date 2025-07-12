package com.example.pdfqa;

import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication(
		exclude = RedisVectorStoreAutoConfiguration.class
)
public class PdfqaApplication implements CommandLineRunner {

	@Value("${pdf.storage-dir}")
	private String storageDir;

	public static void main(String[] args) {
		SpringApplication.run(PdfqaApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		Files.createDirectories(Paths.get(storageDir));
	}

}