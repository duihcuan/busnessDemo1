package com.meishan.agri;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.meishan.agri.**.mapper")
public class AgroApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgroApplication.class, args);
    }
}
