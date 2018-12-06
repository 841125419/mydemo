package com.kwantler.websocket.db;

import java.util.HashMap;
import java.util.Map;

/**
 * 模拟订阅的客户——之后要用redis数据库进行存储
 */
public class RegistWebSocketUser {
    private static Map<String,User> users = new HashMap<>();
    public static void setUser(User user){
        if (users.containsKey(user.getUserName())){
            return;
        }
        users.put(user.getUserName(),user);
    }
    public static void removeUser(User user){
        users.remove(user.getUserName());
    }

    public static Map<String,User> getUsers(){
        return users;
    }

    public static User getUser(String username) {
        return users.get(username);
    }
}
