package com.openapi.Model.chatgpt;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class ChatGPTHist {
    private String id = UUID.randomUUID().toString().replace("-","");

    private Integer deleteflg = 0;

    private Date createtime = new Date();

    private Date updatetime = new Date();
    // ask/answer
    private String src;
    private String question;

    private String result;
    // userid
    private String openid;

    private String topicid;
    // 问题的histid
    private String askid ;

    // 上下文id,每次问就会变
    private String parentid;
    // 会话id,不变
    private String conversationid;


}