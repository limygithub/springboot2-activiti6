package com.limy;

import org.activiti.spring.boot.SecurityAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@ComponentScan("com.limy")
@EntityScan("com.limy.model")
public class Springboot2Activiti6Application {


	public static void main(String[] args) {
		SpringApplication.run(Springboot2Activiti6Application.class, args);
		System.out.println("启动成功");
	}

}
