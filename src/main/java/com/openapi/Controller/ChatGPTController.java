package com.openapi.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;
import com.openapi.Basic.BasicCode;
import com.openapi.Basic.Return;
import com.openapi.Model.chatgpt.ChatGPTHist;
import com.openapi.Model.chatgpt.ChatGPTTopic;
import com.openapi.Service.ChatGPTService;


@RestController
@RequestMapping("/v1/chat")
public class ChatGPTController {

  private Logger logger = LogManager.getLogger(ChatGPTController.class);

  @Autowired
  ChatGPTService _chatGPTService;

  @PostMapping("/insertTopic")
  public Return insertTopic(@RequestBody ChatGPTTopic chatGPTTopic) throws Exception{

    if(Strings.isNullOrEmpty(chatGPTTopic.getTopic())) {
      return Return.FAIL(BasicCode.parameters_incorrect.code,BasicCode.parameters_incorrect.note);
    }
    if(Strings.isNullOrEmpty(chatGPTTopic.getOpenid())) {
      return Return.FAIL(BasicCode.parameters_incorrect.code,BasicCode.parameters_incorrect.note);
    }

    int cnt = _chatGPTService.insertTopic(chatGPTTopic);
    if(cnt > 0){
      return Return.SUCCESS(BasicCode.success).data(chatGPTTopic.getId());
    }else {
      return Return.FAIL(BasicCode.error);
    }
  }


  @PostMapping("/deleteTopic")
  public Return deleteTopic(String id) throws Exception{

    if(Strings.isNullOrEmpty(id)){
      return Return.FAIL(BasicCode.parameters_incorrect.code,"id"+BasicCode.parameters_incorrect.note);
    }

    int i = _chatGPTService.delete(id);
    if(i<=0){
      return Return.FAIL(BasicCode.data_not_found);
    }
    return Return.SUCCESS(BasicCode.success);
  }

  @GetMapping("/searchTopic")
  public Return searchTopic(String orgId,String topic,
      @RequestParam(required = false, defaultValue = "0") int pageFrom,
      @RequestParam(required = false, defaultValue = "15") int pageSize) throws Exception{
    if (Strings.isNullOrEmpty(orgId)) {
      return Return.FAIL(BasicCode.parameters_incorrect.code, "orgId"+BasicCode.parameters_incorrect.note);
    }
//    if (Strings.isNullOrEmpty(topic)) {
//      return Return.FAIL(Code.PARAM_FORMAT_INCORRECT.code, "topic"+Code.PARAM_FORMAT_INCORRECT.note);
//    }
    Map param = new HashMap();
    param.put("orgId",orgId);
    param.put("topic",topic);
    List datas = _chatGPTService.searchTopic(param,pageSize,(pageFrom - 1) * pageSize);
    if(datas.size() > 0 ){
      return Return.SUCCESS(BasicCode.success).data(datas);
    }
    return Return.FAIL(BasicCode.data_not_found);
  }

  @GetMapping("/countTopic")
  public Return countTopic(String orgId,String topic) throws Exception{
    if (Strings.isNullOrEmpty(orgId)) {
      return Return.FAIL(BasicCode.parameters_incorrect.code, "orgId"+BasicCode.parameters_incorrect.note);
    }
//    if (Strings.isNullOrEmpty(topic)) {
//      return Return.FAIL(Code.PARAM_FORMAT_INCORRECT.code, "topic"+Code.PARAM_FORMAT_INCORRECT.note);
//    }
    Map param = new HashMap();
    param.put("orgId",orgId);
    param.put("topic",topic);
    int cnt = _chatGPTService.countTopic(param);

    return Return.SUCCESS(BasicCode.success).total(cnt);
  }

  // 没有topic就新建,有就直接返回
  @GetMapping("/getCurrentTopicOrSet")
  public Return ask(String openid) throws Exception{

    Map param = new HashMap();
    param.put("openid",openid);

    List<ChatGPTTopic> datas = _chatGPTService.searchTopic(param,1,0);
    if(datas.size()<=0){
      ChatGPTTopic chatGPTTopic = new ChatGPTTopic();
      chatGPTTopic.setOpenid(openid);

      int cnt = _chatGPTService.insertTopic(chatGPTTopic);
      if(cnt > 0){
        return Return.SUCCESS(BasicCode.success).data(chatGPTTopic.getId());
      }else {
        return Return.FAIL(BasicCode.error);
      }

    }else{
      return Return.SUCCESS(BasicCode.success).data(datas.get(0).getId());
    }
  }


  @PostMapping("/ask")
  public Return ask(@RequestBody ChatGPTHist chatGPTHist) throws Exception{

    if(Strings.isNullOrEmpty(chatGPTHist.getTopicid())) {
      return Return.FAIL(BasicCode.parameters_incorrect);
    }
    if(Strings.isNullOrEmpty(chatGPTHist.getQuestion())) {
      return Return.FAIL(BasicCode.parameters_incorrect);
    }

    int cnt = _chatGPTService.ask(chatGPTHist);
    if(cnt > 0){
      return Return.SUCCESS(BasicCode.success).data(chatGPTHist.getId());
    }else {
      return Return.FAIL(BasicCode.error);
    }
  }

  @PostMapping("/answer")
  public Return answer(@RequestBody ChatGPTHist chatGPTHist) throws Exception{

    if(Strings.isNullOrEmpty(chatGPTHist.getTopicid())) {
      return Return.FAIL(BasicCode.parameters_incorrect);
    }
    if(Strings.isNullOrEmpty(chatGPTHist.getResult())) {
      return Return.FAIL(BasicCode.parameters_incorrect);
    }
    if(Strings.isNullOrEmpty(chatGPTHist.getAskid())) {
      return Return.FAIL(BasicCode.parameters_incorrect);
    }

    int cnt = _chatGPTService.answer(chatGPTHist);
    if(cnt > 0){
      return Return.SUCCESS(BasicCode.success);
    }else {
      return Return.FAIL(BasicCode.error);
    }

  }

  @GetMapping("/hasAnswer")
  public Return hasAnswer(String askId) throws Exception{

    if(Strings.isNullOrEmpty(askId)) {
      return Return.FAIL(BasicCode.parameters_incorrect);
    }
    if(_chatGPTService.hasAnswer(askId)){
      return Return.SUCCESS(BasicCode.success);
    } else {
      return Return.FAIL(BasicCode.data_not_found);
    }
  }

  @GetMapping("/searchHist")
  public Return searchHist(String orgId,String topicid,
      @RequestParam(required = false, defaultValue = "0") int pageFrom,
      @RequestParam(required = false, defaultValue = "15") int pageSize) throws Exception{
    if (Strings.isNullOrEmpty(orgId)) {
      return Return.FAIL(BasicCode.parameters_incorrect.code, "orgId"+BasicCode.parameters_incorrect.note);
    }
//    if (Strings.isNullOrEmpty(topic)) {
//      return Return.FAIL(Code.PARAM_FORMAT_INCORRECT.code, "topic"+Code.PARAM_FORMAT_INCORRECT.note);
//    }
    Map param = new HashMap();
    param.put("orgId",orgId);
    param.put("topicid",topicid);
    List datas = _chatGPTService.searchHist(param,pageSize,(pageFrom - 1) * pageSize);
    if(datas.size() > 0 ){
      return Return.SUCCESS(BasicCode.success).data(datas);
    }
    return Return.FAIL(BasicCode.data_not_found);
  }

  @GetMapping("/countHist")
  public Return countHist(String orgId,String topicid) throws Exception{
    if (Strings.isNullOrEmpty(orgId)) {
      return Return.FAIL(BasicCode.parameters_incorrect.code, "orgId"+BasicCode.parameters_incorrect.note);
    }
    if (Strings.isNullOrEmpty(topicid)) {
      return Return.FAIL(BasicCode.parameters_incorrect.code, "topicid"+BasicCode.parameters_incorrect.note);
    }
    Map param = new HashMap();
    param.put("orgId",orgId);
    param.put("topicid",topicid);
    int cnt = _chatGPTService.countHist(param);

    return Return.SUCCESS(BasicCode.success).total(cnt);
  }


}
