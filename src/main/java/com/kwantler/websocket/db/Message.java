package com.kwantler.websocket.db;

public class Message {
    private String text;
    private String date;
    private String messageType;
    private String tousername;

    public String getTousername() {
        return tousername;
    }

    public void setTousername(String tousername) {
        this.tousername = tousername;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Massage{" +
                "text='" + text + '\'' +
                ", date='" + date + '\'' +
                ", messageType='" + messageType + '\'' +
                '}';
    }
}
