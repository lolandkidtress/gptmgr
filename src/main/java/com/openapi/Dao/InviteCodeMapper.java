package com.openapi.Dao;

import org.apache.ibatis.annotations.Param;

import com.openapi.Model.InviteCode;


public interface InviteCodeMapper {
    int count(@Param(value = "code") String code);

    int insert(InviteCode record);

    InviteCode getByCode(@Param(value = "code") String code, @Param(value = "consumeopenId") String consumeopenId);

    InviteCode selectById(@Param(value = "id") String id);


}