package com.db.teamdb;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/request")
public class SystemController {

    //页面请求
    @PostMapping ("/call")
    public DataMessage socket(String msg) {
        System.out.println(msg);
        return Operating.handleCmd(msg);
    }

}

