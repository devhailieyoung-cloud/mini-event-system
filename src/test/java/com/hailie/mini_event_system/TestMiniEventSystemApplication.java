package com.hailie.mini_event_system;

import org.springframework.boot.SpringApplication;

public class TestMiniEventSystemApplication {

	public static void main(String[] args) {
		SpringApplication.from(MiniEventSystemApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
