package com.openapi.Dao;

import java.util.List;
import java.util.Map;

import com.openapi.Model.chatgpt.ChatGPTHist;


public interface ChatGPTHistMapper {

    ChatGPTHist selectByPrimaryKey(String id);

    ChatGPTHist fetchLast(String topicid);

    int insert(ChatGPTHist record);

    ChatGPTHist searchOne(Map<String, Object> param);

    int searchCount(Map<String, Object> param);

    List<ChatGPTHist> search(Map<String, Object> param);

    // 答案
    ChatGPTHist getAnswer(String parentid);

}
