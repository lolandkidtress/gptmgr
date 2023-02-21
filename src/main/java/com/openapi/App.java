package com.openapi;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;


@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.openapi.*"})
@ServletComponentScan({"com.openapi.*"})
@SpringBootApplication
public class App {
    public static void main(String[] args) {

        System.setProperty("server.servlet.context-path", "/GPTMGR");

        SpringApplicationBuilder builder = new SpringApplicationBuilder(App.class);

        builder.headless(false).run(args);
    }
}
