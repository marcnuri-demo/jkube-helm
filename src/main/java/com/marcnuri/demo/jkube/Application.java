package com.marcnuri.demo.jkube;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class Application {

  @Value("${demo.jkube.greeting:Missing Configuration}")
  private String greeting;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @GetMapping
  public String hello() {
    return String.format("Hello %s!", greeting);
  }
}
