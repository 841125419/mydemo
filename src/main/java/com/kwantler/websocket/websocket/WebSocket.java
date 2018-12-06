package com.kwantler.websocket.websocket;
 
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.kwantler.websocket.db.Message;
import com.kwantler.websocket.db.Message;
import com.kwantler.websocket.db.RegistWebSocketUser;
import com.kwantler.websocket.db.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * @author shkstart
 * @create 2018-08-10 16:34
 */
@Component
@ServerEndpoint("/websocket/{username}")
public class WebSocket {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 在线人数
     */
    public static int onlineNumber = 0;
    /**
     * 以用户的姓名为key，WebSocket为对象保存起来
     */
    private static Map<String, WebSocket> clients = new ConcurrentHashMap<String, WebSocket>();
    /**
     * 会话
     */
    private Session session;
    /**
     * 用户名称
     */
    private String username;
    /**
     * 建立连接
     *
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam("username") String username, Session session)
    {
        onlineNumber++;
        logger.info("现在来连接的客户id："+session.getId()+"用户名："+username);
        this.username = username;
        this.session = session;
        logger.info("有新连接加入！ 当前在线人数" + onlineNumber);
        try {
            //messageType 1代表上线 2代表下线 3代表在线名单 4代表普通消息
            //先给所有人发送通知，说我上线了
            sendMessageBefore(username);
            Map<String,Object> map1 = createMessage("1",username);
            //添加消息到对应的客户端用户中
//            addMessageToUserQueue(username,true,map1);

            addMessageToUserQueueExcludeSelf(new String[]{username},map1);

            //从客户端信息中取出需要发送的信息
            sendMessageAll(JSON.toJSONString(map1),username);
 
            //把自己的信息加入到map当中去
            clients.put(username, this);
            //给自己发一条消息：告诉自己现在都有谁在线
            Map<String,Object> map2 = new HashMap<String,Object>();
            map2.put("messageType",3);
            //移除掉自己
            Set<String> set = clients.keySet();
            map2.put("onlineUsers",set);
            sendMessageTo(JSON.toJSONString(map2),username);
        }
        catch (IOException e){
            logger.info(username+"上线的时候通知所有人发生了错误");
        }
 
 
 
    }

    /**
     * 添加到其他用户的消息队列中，排除username
     * @param excluleUsernames 排除的名单
     * @param map1
     */
    private void addMessageToUserQueueExcludeSelf(String[] excluleUsernames, Map<String, Object> map1) {
        Map<String, User> users = RegistWebSocketUser.getUsers();
        List usernames = Arrays.asList(excluleUsernames);
        for (User u : users.values()) {
            if (usernames.contains(u.getUserName())){
                continue;
            }
            Message message = new Message();
            message.setMessageType(map1.get("messageType").toString());
            message.setTousername(map1.get("username").toString());
            u.getQueue().add(message);
        }
    }

    public Map<String,Object> createMessage(String messageType,String username){
        Map<String,Object> map1 = new HashMap<String,Object>();
        map1.put("messageType",messageType);
        map1.put("username",username);
        return map1;
    }

    public void addMessageToUserQueue(String username, boolean toAll, Map<String, Object> map1) {
        if (toAll){
            Map<String, User> users = RegistWebSocketUser.getUsers();
            for (User u : users.values()) {
                Message message = new Message();
                message.setMessageType(map1.get("messageType").toString());
                message.setTousername(u.getUserName());
                u.getQueue().add(message);
            }
        }else{
            User u = RegistWebSocketUser.getUser(username);
            Message message = new Message();
            message.setMessageType(map1.get("messageType").toString());
            message.setTousername(u.getUserName());
            u.getQueue().add(message);
        }
    }

    public void sendMessageBefore(String username) {
        User user = RegistWebSocketUser.getUser(username);
        Queue queue = user.getQueue();
        if (queue.size() > 0){
            System.out.println(queue.size());
            while (queue.peek() != null){
                Message m = user.getQueue().peek();
                if (m != null){
                    WebSocket ws = clients.get(username);
                    if (ws != null){
                        Map<String,Object> map1 = new HashMap<String,Object>();
                        map1.put("messageType",m.getMessageType());
                        map1.put("username",user.getUserName());
                        try {
                            sendMessageTo(JSON.toJSONString(map1),m.getTousername());
                        } catch (IOException e) {
                            System.out.println("消息发送失败"+m);
                            e.printStackTrace();
                        }
                        queue.remove();
                        System.out.println(queue.size());
                    }
                }
                System.out.println(queue.size());
            }
        }
    }


    @OnError
    public void onError(Session session, Throwable error) {
        logger.info("服务端发生了错误"+error.getMessage());
        //error.printStackTrace();
    }
    /**
     * 连接关闭
     */
    @OnClose
    public void onClose()
    {
        onlineNumber--;
        //webSockets.remove(this);
        clients.remove(username);
        try {
            //messageType 1代表上线 2代表下线 3代表在线名单  4代表普通消息
            Map<String,Object> map1 =new HashMap<String,Object>();
            map1.put("messageType",2);
            map1.put("onlineUsers",clients.keySet());
            map1.put("username",username);
            sendMessageAll(JSON.toJSONString(map1),username);
        }
        catch (IOException e){
            logger.info(username+"下线的时候通知所有人发生了错误");
        }
        logger.info("有连接关闭！ 当前在线人数" + onlineNumber);

    }
 
    /**
     * 收到客户端的消息
     *
     * @param message 消息
     * @param session 会话
     */
    @OnMessage
    public void onMessage(String message, Session session)
    {
        try {
            logger.info("来自客户端消息：" + message+"客户端的id是："+session.getId());
            JSONObject jsonObject = JSON.parseObject(message);
            String textMessage = jsonObject.getString("message");
            String fromusername = jsonObject.getString("username");
            String tousername = jsonObject.getString("to");
            //如果不是发给所有，那么就发给某一个人
            //messageType 1代表上线 2代表下线 3代表在线名单  4代表普通消息
            Map<String,Object> map1 =new HashMap<String,Object>();
            map1.put("messageType",4);
            map1.put("textMessage",textMessage);
            map1.put("fromusername",fromusername);
            if(tousername.equals("All")){
                map1.put("tousername","所有人");
                sendMessageAll(JSON.toJSONString(map1),fromusername);
            }
            else{
                map1.put("tousername",tousername);
                sendMessageTo(JSON.toJSONString(map1),tousername);
            }
        }
        catch (Exception e){
            logger.info("发生了错误了");
        }
 
    }
 
 
    public void sendMessageTo(String message, String ToUserName) throws IOException {
        clients.get(ToUserName).session.getAsyncRemote().sendText(message);
//        for (WebSocket item : clients.values()) {
//            if (item.username.equals(ToUserName) ) {
//                item.session.getAsyncRemote().sendText(message);
//                break;
//            }
//        }
    }
 
    public void sendMessageAll(String message,String FromUserName) throws IOException {
        sendToAllUser();
//        for (WebSocket item : clients.values()) {
//            item.session.getAsyncRemote().sendText(message);
//        }
    }

    public void sendToAllUser() {
        Map<String, User> users = RegistWebSocketUser.getUsers();
        System.out.println("需要发送的用户个数为："+users.size());
        int sendCount = 0;
        for (User user: users.values()) {
            Queue queue = user.getQueue();
            if (queue.size() > 0){
                System.out.println(queue.size());
                int qSize = queue.size();

                for (int i = 0; i<qSize; i++){
                    Message m = (Message) queue.poll();
                    if (m != null){
                        WebSocket ws = clients.get(user.getUserName());
                        if (ws != null){
                            Map<String,Object> map1 = new HashMap<String,Object>();
                            map1.put("messageType",m.getMessageType());
                            map1.put("username",user.getUserName());
                            try {
                                sendMessageTo(JSON.toJSONString(map1),m.getTousername());
                                sendCount++;
                            } catch (IOException e) {
                                queue.add(m);
                                System.out.println("消息发送失败"+m);
                                e.printStackTrace();
                            }
                            System.out.println(queue.size());
                        }
                    }else{
                        queue.add(m);
                    }
                    System.out.println(queue.size());
                }
            }
        }
        System.out.println("已经发送的条数为："+sendCount);
    }

    public static synchronized int getOnlineCount() {
        return onlineNumber;
    }
 
}
