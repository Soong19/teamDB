package com.db.teamdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TeamDbApplication {

    public static void main(String[] args) {
        Table.init("user1","db1");
        SpringApplication.run(TeamDbApplication.class, args);
    }

}
