package com.openapi.Dao;

import java.util.List;
import java.util.Map;

import com.openapi.Model.chatgpt.ChatGPTTopic;


public interface ChatGPTTopicMapper {

    int insert(ChatGPTTopic record);

    ChatGPTTopic searchOne(Map<String, Object> param);

    int searchCount(Map<String, Object> param);

    List<ChatGPTTopic> search(Map<String, Object> param);

    int deleteByPrimaryKey(String id);

}
