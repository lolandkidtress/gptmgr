package com.openapi.Dao;

import java.util.List;
import java.util.Map;

import com.openapi.Model.ApiKey;


public interface ApiKeyMapper {

    ApiKey selectByPrimaryKey(String id);

    int insert(ApiKey record);

    ApiKey searchOne(Map<String, Object> param);

    int searchCount(Map<String, Object> param);

    List<ApiKey> search(Map<String, Object> param);

}
