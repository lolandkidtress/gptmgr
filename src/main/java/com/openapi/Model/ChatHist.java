package com.openapi.Model;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class ChatHist {
    private String id = UUID.randomUUID().toString().replace("-","");

    private Integer deleteflg = 0;

    private Date createtime = new Date();

    private Date updatetime = new Date();

    private String question;

    private String openid;

    private String result;


}