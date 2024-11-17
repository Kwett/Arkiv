package com.kwett.arkiv;

import com.kwett.arkiv.service.RssService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication

public class ArkivApplication implements CommandLineRunner {

	private final RssService rssService;

	public ArkivApplication(RssService rssService) {
		this.rssService = rssService;
	}

	public static void main(String[] args) {

		SpringApplication.run(ArkivApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		rssService.checkForNewVideos();
	}
}
