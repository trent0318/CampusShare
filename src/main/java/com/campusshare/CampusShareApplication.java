package com.campusshare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campusshare.mapper")
@EnableScheduling
public class CampusShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusShareApplication.class, args);
    }
}
