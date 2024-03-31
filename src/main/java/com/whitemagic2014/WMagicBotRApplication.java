package com.whitemagic2014;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.dingCreator")
public class WMagicBotRApplication {

    public static void main(String[] args) {
        SpringApplication.run(WMagicBotRApplication.class, args);
    }

}
