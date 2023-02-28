package com.openapi.Model.chatgpt;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class ChatGPTTopic {
    private String id = UUID.randomUUID().toString().replace("-","");

    private Integer deleteflg = 0;

    private Date createtime = new Date();

    private Date updatetime = new Date();

    private String orgid;
    // userid
    private String openid;

    private String topic;
}