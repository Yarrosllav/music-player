package com.musicplayer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MusicPlayerApplication {
	public static void main(String[] args) {
		SpringApplication.run(MusicPlayerApplication.class, args);
		System.out.println("Music Player Backend is running on http://localhost:8080");
	}
}
