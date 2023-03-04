package com.openapi.Service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;
import com.openapi.Basic.BasicCode;
import com.openapi.Basic.JsonConvert;
import com.openapi.Basic.Return;
import com.openapi.Dao.ChatHistMapper;
import com.openapi.Dao.CodeUserMapper;
import com.openapi.Dao.CodeUserQuotaMapper;
import com.openapi.Dao.InviteCodeMapper;
import com.openapi.Database.TgDataSourceConfig;
import com.openapi.Model.ChatHist;
import com.openapi.Model.CodeUser;
import com.openapi.Model.CodeUserQuota;
import com.openapi.Model.InviteCode;
import com.openapi.tools.OkHttpClientUtil.OkHttpTools;
import com.openapi.tools.SendAlarmTools;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


@Service
public class CodeUserService {

    private static Logger logger = LogManager.getLogger(CodeUserService.class);

    @Autowired
    TgDataSourceConfig _tgDataSourceConfig;

    @Autowired
    CodeUserMapper _codeUserMapper;

    @Autowired
    CodeUserQuotaMapper _codeUserQuotaMapper;

    @Autowired
    InviteCodeMapper _inviteCodeMapper;

    @Autowired
    ChatHistMapper _chatHistMapper;


    public int save(CodeUser bizUser){

        return _codeUserMapper.upsert(bizUser);
    }

    public CodeUser getCodeUser(String id){
        CodeUser user = _codeUserMapper.selectById(id);
        return user;
    }

    public int deleteByOpenId(String openId){
        return _codeUserMapper.deleteByOpenId(openId);
    }

    public CodeUser getCodeUserByOpenId(String openId){
        return _codeUserMapper.getCodeUserByOpenId(openId);
    }

    public CodeUser getCodeUserByInviteCode(String invitecode){
        return _codeUserMapper.getCodeUserByInviteCode(invitecode);
    }

    public CodeUserQuota getQuota(String openId){
        CodeUserQuota quota = _codeUserQuotaMapper.getCodeUserQuotaByOpenId(openId);
        if(quota == null){
            // 没有先初始化
            quota = new CodeUserQuota();
            quota.setOpenid(openId);
            quota.setCnt(1);
            quota.setMaxcnt(5);
            if(_codeUserQuotaMapper.upsert(quota) > 0){
                return quota;
            }else{
                quota.setCnt(9999999);
                return quota;
            }
        }else{
            return _codeUserQuotaMapper.getCodeUserQuotaByOpenId(openId);
        }
    }

    public int incrQuota(String openId){
        return incrQuota(openId,1);
    }

    public int incrQuota(String openId,int addCnt){

        CodeUserQuota quota = getQuota(openId);
        quota.setCnt(quota.getCnt()+addCnt);
        quota.setUpdatetime(new Date());
        return _codeUserQuotaMapper.upsert(quota);
    }

    public int incrMaxQuota(String openId,int addCnt){

        CodeUserQuota quota = getQuota(openId);
        quota.setMaxcnt(quota.getMaxcnt()+addCnt);
        // 不能更新updatetime,避免计算cnt错误
        // quota.setUpdatetime(new Date());
        return _codeUserQuotaMapper.upsert(quota);
    }

    public int saveQuota(CodeUserQuota quota){
        quota.setUpdatetime(new Date());
        return _codeUserQuotaMapper.upsert(quota);
    }

    @Transactional
    public Return inputCode(String consumeopenId,String code){


        InviteCode iv = _inviteCodeMapper.getByCode(code,consumeopenId);
        if (iv !=null) {
            // 好友已输入过
            return Return.FAIL(BasicCode.data_exist);
        }

        CodeUser produceuser = getCodeUserByInviteCode(code);
        if(produceuser==null){
            // code不存在
            logger.info("code {} 没有user记录" , code);
            return Return.FAIL(BasicCode.data_not_found);
        }
        //
        if(consumeopenId.equals(produceuser.getOpenid())){
            logger.info("code {} 不能自己邀请自己" , code);
            return Return.FAIL(BasicCode.data_not_found);
        }

        CodeUser consumeuser = getCodeUserByOpenId(consumeopenId);
        if(consumeuser==null){
            logger.info("openid {} 没有user记录" , consumeopenId);
            return Return.FAIL(BasicCode.data_not_found);
        }

        //不超过3次
        int total = _inviteCodeMapper.count(code);
        if(total >=3) {
            logger.info("code {} 超过3次" , code);
            return Return.FAIL(BasicCode.quota_over_limit);
        }

        iv = new InviteCode();
        iv.setCode(code);
        iv.setConsumeopenid(consumeopenId);
        iv.setProduceopenid(produceuser.getOpenid());

        int cnt = _inviteCodeMapper.insert(iv);
        if(cnt < 0){
            return Return.FAIL(BasicCode.error);
        }

        // 双方各加次数
        incrMaxQuota(consumeopenId,10);
        incrMaxQuota(produceuser.getOpenid(),10);

        return Return.SUCCESS(BasicCode.success);

    }

    public Return doAsk(String apikey ,String openId,Object hint, String question){

        if(Strings.isNullOrEmpty(apikey) || "null".equals(apikey)){
            apikey = getKey();
        }


//        Return checkQuotaRet = checkQuota(openId,"api");
//
//        if(!checkQuotaRet.is_success()){
//            return checkQuotaRet;
//        }

        try{


            Map head = new HashMap();
            Map chatParam = new HashMap();
            chatParam.put("model","gpt-3.5-turbo");

            List message = new ArrayList();
            if(hint!=null){
                message.add(JsonConvert.toObject(JsonConvert.toJson(hint),List.class));
            }

            Map content = new HashMap();
            content.put("role","user");
            content.put("content",question);

            message.add(content);

            chatParam.put("question",message);
            chatParam.put("apikey",apikey);

            logger.info("chatgpt接口参数:{}",chatParam);

            String url = _tgDataSourceConfig.getApiserver().concat("/GPTMGR/aicode/doRequest");


            String str = OkHttpTools.post(OkHttpTools.MEDIA_TYPE_JSON,url,chatParam);

            logger.info("api接口返回:{}",str);

            Return ret_resp = JsonConvert.toObject(str,Return.class);

            if(!ret_resp.is_success()){
                if(str.contains("insufficient_quota")){
                    logger.error("{} 余额不足",apikey);
                    SendAlarmTools.sendAlarm(apikey.concat("余额不足"));
                }

                ChatHist answer = new ChatHist();
                answer.setOpenid(openId);
                answer.setQuestion(JsonConvert.toJson(chatParam));
                answer.setResult("服务负载过高,请稍后再试");
                _chatHistMapper.insert(answer);
            }else{
                String text = String.valueOf(ret_resp.get_data());

                ChatHist answer = new ChatHist();
                answer.setOpenid(openId);
                answer.setQuestion(JsonConvert.toJson(chatParam));
                answer.setResult(text);
                _chatHistMapper.insert(answer);
                return Return.SUCCESS(BasicCode.success).data(text);
            }


        }catch(Exception e) {
            e.printStackTrace();
            logger.error("请求异常", openId);
        }
        return Return.FAIL(BasicCode.error);
    }

    public Return doRequest(String apikey,double temperature,List questions){

        if(Strings.isNullOrEmpty(apikey)){
            logger.info("没有apikey" );
            return Return.FAIL(BasicCode.error);
        }

        Map header = new HashMap();
        header.put("authorization","Bearer "+ apikey);
        Headers headers = Headers.of(header);

        try{
            String url = "https://api.openai.com/v1/chat/completions";
            OkHttpClient client = getUnsafeOkHttpClient();
            Map chatParam = new HashMap();
            chatParam.put("model","gpt-3.5-turbo");

            chatParam.put("temperature",temperature);
            chatParam.put("messages",questions);

            // 最多只能输出4k字,所以要控制返回的字符数量
            chatParam.put("max_tokens",4000);

//                String str_ret = OkHttpTools.sslpost(OkHttpTools.MEDIA_TYPE_JSON, url, JsonConvert.toJson(chatParam), 30, header);
//                return Return.SUCCESS(BasicCode.success).data(str_ret);
//


            String requestBody = JsonConvert.toJson(chatParam); // 请求体，JSON 格式

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8"); // 设置请求体的媒体类型
            RequestBody body = RequestBody.create(mediaType,requestBody); // 创建请求体


            Request request = new Request.Builder()
                .url(url)
                .headers(headers)
                //.header("authorization","Bearer "+ "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")
                .post(body)
                .build();

            logger.info("{}的问题:{}",chatParam);
            try (Response response = client.newCall(request).execute()) {
                String str_res = response.body().string();

                logger.info("返回:{}",str_res);
                Map map_res = JsonConvert.toObject(str_res,Map.class);
                if(map_res.containsKey("error")){
                    Map errorMsg = JsonConvert.toObject(JsonConvert.toJson(map_res.get("error")), Map.class);
                    String type = String.valueOf(errorMsg.get("type"));
                    // TODO 提醒
                    if("insufficient_quota".equals(type)){

                    }
                    return Return.FAIL(BasicCode.error).note(type);
                }else{
                    List choices = JsonConvert.toObject(JsonConvert.toJson(map_res.get("choices")), List.class);
                    Map message = JsonConvert.toObject(JsonConvert.toJson(choices.get(0)), Map.class);
                    Map answer =  JsonConvert.toObject(JsonConvert.toJson(message.get("message")), Map.class);
                    String text = String.valueOf(answer.get("content"));

                    return Return.SUCCESS(BasicCode.success).data(text);
                }
            } catch (IOException e) {
                e.printStackTrace();
                logger.error("请求异常:{}" , e.getMessage());
                return Return.FAIL(BasicCode.error);
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error("请求异常",e);
            return Return.FAIL(BasicCode.error);
        }
    }

    public static long getTodayZeroTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String getKey(){
        String str_keys = _tgDataSourceConfig.getApikey();
        List<String> keys = Arrays.asList(str_keys.split(","));
        Collections.shuffle(keys);
        return keys.get(0);
    }


    private static OkHttpClient getUnsafeOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        // 创建一个信任所有证书的 TrustManager
        final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        // 创建一个 SSLContext，并指定信任所有证书
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // 创建一个 OkHttpClient，并指定 SSLContext
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.readTimeout(30, TimeUnit.SECONDS);
        builder.connectTimeout(10,TimeUnit.SECONDS);
        builder.writeTimeout(30,TimeUnit.SECONDS);
        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);

        return builder.build();
    }
}
