package com.openapi.Dao;

import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.openapi.Model.CodeUser;


public interface CodeUserMapper {
    int count(Map<String, Object> param);

    int deleteByOpenId(@Param(value = "openid") String openid);

    int upsert(CodeUser record);

    CodeUser selectById(@Param(value = "id") String id);

    CodeUser getCodeUserByOpenId(@Param(value = "openid") String openId);

    CodeUser getCodeUserByInviteCode(@Param(value = "invitecode") String invitecode);


}