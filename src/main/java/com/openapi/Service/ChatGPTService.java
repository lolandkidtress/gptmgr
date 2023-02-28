package com.openapi.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.openapi.tools.ScheduleThreadPool;

import javax.annotation.Resource;


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

    public boolean hasAnswer(String askId){
        return ResultCache.getInstance().isCacheExist(askId);
    }

    public int ask(ChatGPTHist chatGPTHist){

        chatGPTHist.setSrc("ask");
        // TODO 处理 parentid
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

        try {
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    try {
                        //先保存问题,等有答案后再保存结果
                        Return ret = doChatGPTRequest(chatGPTHist.getQuestion(),conversationid,parentid);
                        if(ret.is_success()){
                            Map data = JsonConvert.toObject(JsonConvert.toJson(ret.get_data()),Map.class);
                            //String message = msgCheck(String.valueOf(data.get("message")));
                            String message = String.valueOf(data.get("message"));
                            String parent_id = String.valueOf(data.get("parent_id"));
                            String conversation_id = String.valueOf(data.get("conversation_id"));
                            ChatGPTHist answer = new ChatGPTHist();
                            answer.setAskid(chatGPTHist.getId());
                            answer.setConversationid(conversation_id);
                            answer.setParentid(parent_id);
                            answer.setResult(message);
                            answer.setTopicid(chatGPTHist.getTopicid());
                            answer(answer);
                        } else{
                            logger.error("获取结果异常");
                        }
                    } catch (Exception e) {
                        logger.error("定时同步机器人的好友发送的消息总数", e);
                    }
                }
            };

            ThreadPoolExecutor executor = ScheduleThreadPool.getExcecutor();

            executor.execute(r);

            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("定时同步机器人的好友发送的消息总数", e);
        }

        return 0;


    }

    public int answer(ChatGPTHist chatGPTHist){

        chatGPTHist.setSrc("answer");
        int cnt = _chatGPTHistMapper.insert(chatGPTHist);
        ResultCache.getInstance().setCache(chatGPTHist.getAskid());

        return cnt;
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

    public Return doChatGPTRequest(String text,String conversationId,String parentId){
        // 素园的小程序机器
        String url = _tgDataSourceConfig.getChatserver().concat("/ask");
        // String url = "http://10.0.12.2:10005/ask";
        // String url = "http://127.0.0.1:10005/ask";
        Map param = new HashMap();
        param.put("msg",text);
        param.put("parentId",parentId);
        param.put("conversationId",conversationId);

        Map head = new HashMap();
        try{
            logger.info("chatgpt接口参数:{}",param);
            String str = OkHttpTools.post(OkHttpTools.MEDIA_TYPE_JSON,url,param,150,head,false);
            logger.info("chatgpt接口返回:{}",str);
            Return ret = JsonConvert.toObject(str,Return.class);
            if(ret.get_code() == 0){
                return Return.SUCCESS(BasicCode.success).data(ret.get_data());
            }
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

    public static void main(String[] args) {
        String message = "习近平江泽民,东突恐怖主义,强奸,推翻共产党";
        System.out.println(msgCheck(message));
    }
}
