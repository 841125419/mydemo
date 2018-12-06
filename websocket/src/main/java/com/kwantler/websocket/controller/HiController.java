package com.kwantler.websocket.controller;

import com.kwantler.websocket.db.Message;
import com.kwantler.websocket.db.RegistWebSocketUser;
import com.kwantler.websocket.db.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

@Controller
public class HiController {
 
    private Logger logger = LoggerFactory.getLogger(this.getClass());
 
 
    @RequestMapping("/websocket/{name}")
    public String webSocket(@PathVariable String name,Model model){
        try{
            logger.info("跳转到websocket的页面上");
            registWebSocket(name);
            //通过Model进行对象数据的传递
            model.addAttribute("username",name);
            return "socket";
        }
        catch (Exception e){
            logger.info("跳转到websocket的页面上发生异常，异常信息是："+e.getMessage());
            return "error";
        }
    }
    public User registWebSocket(String username) {
        User user = new User();
        user.setUserName(username);
        user.setRole("");
        Message message = new Message();
        message.setDate(new SimpleDateFormat("yyyy-MM-DD HH:mm:ss").format(new Date()));
        Queue<Message> q = new LinkedBlockingDeque<>();
        user.setQueue(q);
        RegistWebSocketUser.setUser(user);
        return user;
    }
}
