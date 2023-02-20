package com.openapi.Model;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class CodeUser {
    private String id = UUID.randomUUID().toString().replace("-","");

    private Integer deleteflg = 0;

    private Date createtime = new Date();

    private Date updatetime = new Date();

    private String username;

    private String openid;

    private String unionid;

    private String appid;

    private String apikey;

    // 邀请码
    private String invitecode;

}