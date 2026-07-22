package com.notemind;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.notemind.mapper")
public class NoteMindApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoteMindApplication.class, args);
    }
}
