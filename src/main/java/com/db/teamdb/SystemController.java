package com.db.teamdb;

import org.springframework.web.bind.annotation.*;

//@Controller("web_Socket_system")
@RestController
@CrossOrigin
@RequestMapping("/request")
public class SystemController {

    //页面请求
    @PostMapping ("/call")
    public DataMessage socket(String msg) {
        System.out.println(msg);
        System.out.println(111111);
        return Operating.handleCmd(msg);
    }

}

