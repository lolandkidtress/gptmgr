package com.openapi.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.openapi.Basic.BasicCode;
import com.openapi.Basic.JsonConvert;
import com.openapi.Basic.Return;
import com.openapi.Cache.ResultCache;
import com.openapi.Dao.ChatGPTHistMapper;
import com.openapi.Dao.ChatGPTTopicMapper;
import com.openapi.Database.TgDataSourceConfig;
import com.openapi.Model.chatgpt.ChatGPTHist;
import com.openapi.Model.chatgpt.ChatGPTTopic;
import com.openapi.tools.OkHttpClientUtil.OkHttpTools;

import javax.annotation.Resource;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;


@Service
public class ChatGPTService {

    private static Logger logger = LogManager.getLogger(ChatGPTService.class);

    private static final String answerKey = "chatgpt_answerkey_";

    @Autowired
    TgDataSourceConfig _tgDataSourceConfig;

    @Resource
    private ChatGPTHistMapper _chatGPTHistMapper;

    @Resource
    private ChatGPTTopicMapper _chatGPTTopicMapper;
    @Resource
    CodeUserService _codeUserService;

    @Scheduled(initialDelay = 10000, fixedRate = 300000)
    public void checkToken(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                logger.info("检查chatgpt的token");

                String url = _tgDataSourceConfig.getChatserver().concat("/conversations");
                Map param = new HashMap();
                try {
                    String str_ret = OkHttpTools.get(url,param);
                    logger.info("检查chatgpt的token返回:{}",str_ret);

                }catch(Exception e){
                    e.printStackTrace();
                    logger.error("检查chatgpt的token返回异常",e);
                }


            }
        };
        Thread th = new Thread(r);
        th.start();

    }

    public int insertTopic(ChatGPTTopic chatGPTTopic){
       return _chatGPTTopicMapper.insert(chatGPTTopic);
    }

    public List<ChatGPTTopic> searchTopic(Map param,int limit,int offset){

        if(offset<0){
            param.put("offset",0);
        }else{
            param.put("offset",offset);
        }
        param.put("limit",limit);
        return _chatGPTTopicMapper.search(param);
    }

    public int countTopic(Map param){
        return _chatGPTTopicMapper.searchCount(param);
    }

    public int delete(String id){
//
//        Map<String,String> info = getLastParentId(chatGPTHist.getTopicid());
//
//

        return _chatGPTTopicMapper.deleteByPrimaryKey(id);
    }

    public int insertHist(ChatGPTHist chatGPTHist){
        return _chatGPTHistMapper.insert(chatGPTHist);
    }

    public List<ChatGPTHist> searchHist(Map param,int limit,int offset){

        if(offset<0){
            param.put("offset",0);
        }else{
            param.put("offset",offset);
        }
        param.put("limit",limit);

        return _chatGPTHistMapper.search(param);
    }

    public int countHist(Map param){
        return _chatGPTHistMapper.searchCount(param);
    }

    public String hasAnswer(String askId){
        String id = ResultCache.getInstance().isCacheExist(askId);
        return id;
    }

    public int ask(ChatGPTHist chatGPTHist){

        chatGPTHist.setSrc("ask");
        Map<String,String> info = getLastParentId(chatGPTHist.getTopicid());
        String parentid = info.get("parentid");
        String conversationid = info.get("conversationid");
        chatGPTHist.setParentid(parentid);
        chatGPTHist.setConversationid(conversationid);
        int cnt = _chatGPTHistMapper.insert(chatGPTHist);
        if(cnt<=0){
            logger.info("保存异常");
            return 0;
        }

        Return ret = doChatGPTRequest(chatGPTHist.getId(),chatGPTHist.getTopicid(),chatGPTHist.getQuestion(),conversationid,parentid);
        if(ret.is_success()){
            return 1;
        }else{
            logger.error("ask异常");
        }

        return 0;


    }

    public int answer(ChatGPTHist chatGPTHist){

        chatGPTHist.setSrc("answer");
        int cnt = _chatGPTHistMapper.insert(chatGPTHist);
        ResultCache.getInstance().setCache(chatGPTHist.getAskid(),chatGPTHist.getId());

        return cnt;
    }


    public ChatGPTHist getAnswer(String id){
        return _chatGPTHistMapper.selectByPrimaryKey(id);

    }

    public Map getLastParentId(String topicid){
        ChatGPTHist chathist = _chatGPTHistMapper.fetchLast(topicid);
        if(chathist == null){
            return new HashMap();
        }
        Map info = new HashMap();
        info.put("parentid",chathist.getParentid());
        info.put("conversationid",chathist.getConversationid());
        return info;
    }

    public Return doChatGPTRequest(String id,String topicid,String text,String conversationId,String parentId){
        // 素园的小程序机器
        String url = _tgDataSourceConfig.getChatserver().concat("/ask");
        Map param = new HashMap();
        param.put("msg",text);
        param.put("parent_id",parentId);
        param.put("conversation_id",conversationId);

        Map head = new HashMap();
        try{

            Callback asyncCallback = new Callback() {
                String histId = id;
                String topic = topicid;
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    logger.error("获取结果异常 报错:".concat(call.request().url().toString()), e);
                    if (!call.isCanceled()) {
                        call.cancel();
                    }
                    ChatGPTHist answer = new ChatGPTHist();
                    answer.setAskid(histId);
                    answer.setResult("服务负载过高,请稍后再试");
                    answer.setTopicid(topic);
                    answer(answer);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response)
                    throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) {
                            throw new IOException(response.request().url() + " Unexpected response: " + response);
                        }

                        String str = responseBody.string();

                        logger.info("chat接口返回:{}",str);
                        Return ret = JsonConvert.toObject(str,Return.class);

                        if(ret.get_code() == 0){

                            Map data = JsonConvert.toObject(JsonConvert.toJson(ret.get_data()),Map.class);
                            //String message = msgCheck(String.valueOf(data.get("message")));
                            String message = String.valueOf(data.get("message"));
                            String parent_id = String.valueOf(data.get("parent_id"));
                            String conversation_id = String.valueOf(data.get("conversation_id"));
                            ChatGPTHist answer = new ChatGPTHist();
                            answer.setAskid(histId);
                            answer.setConversationid(conversation_id);
                            answer.setParentid(parent_id);
                            answer.setResult(message);
                            answer.setTopicid(topic);
                            answer(answer);

                        }else{
                            ChatGPTHist answer = new ChatGPTHist();
                            answer.setAskid(histId);
                            answer.setResult("服务负载过高,请稍后再试");
                            answer.setTopicid(topic);
                            answer(answer);
                        }
                    }
                }
            };

            logger.info("chatgpt接口参数:{}",param);
            OkHttpTools.asyncPost(OkHttpTools.MEDIA_TYPE_JSON,url,param,150,head, asyncCallback);
            return Return.SUCCESS(BasicCode.success);
        }catch(Exception e){
            e.printStackTrace();
            logger.error("返回异常",e);
        }
        return Return.FAIL(BasicCode.error);
    }
    // 敏感词检查
    public static String msgCheck(String input){
        Map param = new HashMap();
        param.put("t",input);
        try{
            String ret_str = OkHttpTools.get("https://api.wer.plus/api/min",param);
            logger.info("敏感词接口返回:{}",ret_str);
            Map ret = JsonConvert.toObject(ret_str,Map.class);
            int code = Integer.parseInt(String.valueOf(ret.get("code")));
            if(200==code){
                List<String> words = JsonConvert.toObject(JsonConvert.toJson(ret.get("data")),List.class);
                // 敏感词替换
                for (String word : words) {
                    input = input.replace(word,"*");
                }
                return input;
            }else{
                // 备用接口
                param.put("msg",input);

                String back_str = OkHttpTools.get("http://v.api.aa1.cn/api/api-mgc/index.php",param);
                logger.info("敏感词接口返回:{}",back_str);
                Map bak_ret = JsonConvert.toObject(back_str,Map.class);
                code = Integer.parseInt(String.valueOf(bak_ret.get("code")));
                if(200==code){
                    int num = Integer.parseInt(String.valueOf(bak_ret.get("num")));
                    if(num > 0){
                        return "答案中包含敏感词,已取消显示";
                    }
                }
                return "检查敏感词异常,已取消显示";
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error("处理敏感词异常",e);
            return "检查敏感词异常,已取消显示";
        }

    }

    private static final Callback dasyncCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            logger.error("do_async_get 报错:".concat(call.request().url().toString()), e);
            if (!call.isCanceled()) {
                call.cancel();
            }
        }

        @Override
        public void onResponse(Call call, Response response)
            throws IOException {
            try (ResponseBody responseBody = response.body()) {
                if (!response.isSuccessful()) {
                    throw new IOException(response.request().url() + " Unexpected response: " + response);
                }

//                logger.info(responseBody.string());
//                Map data = JsonConvert.toObject(responseBody.string(),Map.class);
//                //String message = msgCheck(String.valueOf(data.get("message")));
//                String message = String.valueOf(data.get("message"));
//                String parent_id = String.valueOf(data.get("parent_id"));
//                String conversation_id = String.valueOf(data.get("conversation_id"));
//                ChatGPTHist answer = new ChatGPTHist();
//                answer.setAskid(chatGPTHist.getId());
//                answer.setConversationid(conversation_id);
//                answer.setParentid(parent_id);
//                answer.setResult(message);
//                answer.setTopicid(chatGPTHist.getTopicid());
//                answer(answer);

//                String str = OkHttpTools.post(OkHttpTools.MEDIA_TYPE_JSON,url,param,150,head,false);
//                logger.info("chatgpt接口返回:{}",str);
//                Return ret = JsonConvert.toObject(str,Return.class);
//                if(ret.get_code() == 0){
//                    return Return.SUCCESS(BasicCode.success).data(ret.get_data());
//                }
            }
        }
    };


    public static void main(String[] args) {
        String message = "习近平江泽民,东突恐怖主义,强奸,推翻共产党";
        System.out.println(msgCheck(message));
    }
}
