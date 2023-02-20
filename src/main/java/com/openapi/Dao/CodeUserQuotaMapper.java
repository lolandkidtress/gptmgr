package com.openapi.Dao;

import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.openapi.Model.CodeUserQuota;


public interface CodeUserQuotaMapper {
    int count(Map<String, Object> param);

    int deleteByOpenId(@Param(value = "openid") String openid);

    int upsert(CodeUserQuota record);


    CodeUserQuota selectById(@Param(value = "id") String id);

    CodeUserQuota getCodeUserQuotaByOpenId(@Param(value = "openid") String openId);


}