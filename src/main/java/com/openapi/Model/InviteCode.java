package com.openapi.Model;

import java.util.Date;
import java.util.UUID;

import lombok.Data;


@Data
public class InviteCode {

    private String id = UUID.randomUUID().toString().replace("-","");

    private Integer deleteflg = 0;

    private Date createtime = new Date();

    private Date updatetime = new Date();

    // code的主人
    private String  produceopenid ;
    // 邀请的好友
    private String  consumeopenid ;
    private String code;

}
