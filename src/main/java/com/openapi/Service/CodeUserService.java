package com.openapi.Service;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Strings;
import com.openapi.Basic.BasicCode;
import com.openapi.Basic.JsonConvert;
import com.openapi.Basic.Return;
import com.openapi.Dao.ApiKeyMapper;
import com.openapi.Dao.ChatHistMapper;
import com.openapi.Dao.CodeUserMapper;
import com.openapi.Dao.CodeUserQuotaMapper;
import com.openapi.Dao.InviteCodeMapper;
import com.openapi.Database.TgDataSourceConfig;
import com.openapi.Model.ApiKey;
import com.openapi.Model.ChatHist;
import com.openapi.Model.CodeUser;
import com.openapi.Model.CodeUserQuota;
import com.openapi.Model.InviteCode;
import com.openapi.tools.OkHttpClientUtil.OkHttpTools;
import com.openapi.tools.SendAlarmTools;

import javax.annotation.Resource;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Headers;
import okhttp3.OkHttpClient;


@Service
public class CodeUserService {

    private static Logger logger = LogManager.getLogger(CodeUserService.class);

    public Set<String> Invalidkey = new HashSet<>();
    // quota用完的
    public Set<String> QuotaKey = new HashSet<>();

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

    @Resource
    private ApiKeyMapper _apiKeyMapper;



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

    public String getServerUsability(){
        List<ApiKey> keys = getAllKeys();
        if(keys.size()<=0){
            return "高负载";
        }
        Double f =  (1 - (Double.valueOf(Invalidkey.size()) / Double.valueOf(keys.size()))) ;

        if(f.compareTo(0.7D)>=1){
            return "低负载";
        }

        if(f.compareTo(0.5D)>=1){
            return "中负载";
        }
        return "高负载";

    }

    @Scheduled(initialDelay = 10000, fixedRate = 300 * 1000)
    public void checkValidKey(){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                logger.info("开始检查key的可用性");
                List<ApiKey> keys = getAllKeys();
                if (keys.size() > 0) {
                    Invalidkey.clear();
                }
                for (ApiKey key : keys) {
                    Return ret = getModel(key.getApikey());
                    if(!ret.is_success()){
                        Invalidkey.add(key.getApikey());
                    }
                }
                if(Invalidkey.size()>0){
                    logger.error("失效的key:{}",JsonConvert.toJson(Invalidkey));

                    SendAlarmTools.sendAlarm("api失效的key:"+JsonConvert.toJson(Invalidkey));
                }else{
                    // logger.info("全部通过");
                }
            }
        };
        Thread th = new Thread(r);
        th.start();

    }
    public Return getModel(String key){

        Map header = new HashMap();
        // header.put("authorization","Bearer "+ "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");
        header.put("authorization","Bearer "+ key);
        Headers headers = Headers.of(header);

//        try{
//            String url = "https://api.openai.com/v1/models";
//            OkHttpClient client = getUnsafeOkHttpClient();
//
//            Request request = new Request.Builder()
//                .url(url)
//                .headers(headers)
//                // .header("authorization","Bearer "+"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")
//                .build();
//
//            try (Response response = client.newCall(request).execute()) {
//
//                String str = response.body().string();
//                logger.info("查询model返回:{}",str);
//                Map data = JsonConvert.toObject(str,Map.class);
//                if(data.containsKey("error")){
//                    String message = String.valueOf(data.get("message"));
//                    return Return.FAIL(BasicCode.error).note(message);
//                }else{
//                    return Return.SUCCESS(BasicCode.success).data(str);
//                }
//                // return Return.SUCCESS(BasicCode.success).data(response.body().string());
//            } catch (IOException e) {
//                e.printStackTrace();
//                logger.error("请求异常:{}",e);
//                return Return.FAIL(BasicCode.error);
//            }
//
//
//        }catch(Exception e){
//            e.printStackTrace();
//            logger.error("请求异常:{}" , e.getMessage());
//            return Return.FAIL(BasicCode.error);
//        }

        try{
            String url = _tgDataSourceConfig.getChatserver().concat("/aicode/getModel");
            Map param = new HashMap();
            param.put("apikey",key);

            String str = OkHttpTools.get(url,param);
            // logger.debug("查询model返回:{}",str);

            Return ret_data = JsonConvert.toObject(str,Return.class);

            if(ret_data.is_success()){
                return Return.SUCCESS(BasicCode.success).data(ret_data.get_data());
            }else{
                return Return.FAIL(BasicCode.error);
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error("请求异常:{}" , e.getMessage());
            return Return.FAIL(BasicCode.error);
        }
    }
//    public Return doRequest(String openId,String questions){
//
//        String apikey = getRandomKey();
//
//        if(Strings.isNullOrEmpty(apikey)){
//            logger.info("没有apikey" );
//            return Return.FAIL(BasicCode.error);
//        }
//
//        Map header = new HashMap();
//        header.put("authorization","Bearer "+ "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");
//        Headers headers = Headers.of(header);
//
//        CodeUserQuota quota = getQuota(openId);
//
//        if(quota==null){
//            logger.info("没有 {} 的quota的记录" , openId);
//            return Return.FAIL(BasicCode.quota_over_limit);
//        }
//
//        if(quota.getCnt() > quota.getMaxcnt()){
//
//            if(quota.getUpdatetime().getTime() >= getTodayZeroTimeStamp() ){
//                logger.info("{} 的quota超过" , openId);
//                return Return.FAIL(BasicCode.quota_over_limit);
//            }else{
//                // 每天重置
//                quota.setCnt(0);
//                quota.setUpdatetime(new Date());
//                saveQuota(quota);
//            }
//        }
//
//        try{
//            String url = "https://api.openai.com/v1/completions";
//            OkHttpClient client = getUnsafeOkHttpClient();
//            Map chatParam = new HashMap();
//            chatParam.put("model","text-davinci-003");
//
//            String prompt = "现在你在一所大学中担任老师,我会给你一些题目,你需要通过代码的形式,返回结果,我的问题是:";
//            chatParam.put("prompt",prompt.concat(questions));
//            // 最多只能输出4k字,所以要控制返回的字符数量
//            chatParam.put("max_tokens",3500 - questions.length());
//
////                String str_ret = OkHttpTools.sslpost(OkHttpTools.MEDIA_TYPE_JSON, url, JsonConvert.toJson(chatParam), 30, header);
////                return Return.SUCCESS(BasicCode.success).data(str_ret);
////
//
//            String requestBody = JsonConvert.toJson(chatParam); // 请求体，JSON 格式
//
//            MediaType mediaType = MediaType.parse("application/json; charset=utf-8"); // 设置请求体的媒体类型
//            RequestBody body = RequestBody.create(requestBody, mediaType); // 创建请求体
//
//
//            Request request = new Request.Builder()
//                .url(url)
//                .headers(headers)
//                //.header("authorization","Bearer "+ "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")
//                .post(body)
//                .build();
//
//            logger.info("{}的问题:{}",openId,questions);
//            try (Response response = client.newCall(request).execute()) {
//                String str_res = response.body().string();
//
//                logger.info("返回:{}",str_res);
//                ChatHist chatHist = new ChatHist();
//                chatHist.setOpenid(openId);
//                chatHist.setQuestion(questions);
//                chatHist.setResult(str_res);
//                _chatHistMapper.insert(chatHist);
//
//                Map map_res = JsonConvert.toObject(str_res,Map.class);
//                if(map_res.containsKey("error")){
//                    Map errorMsg = JsonConvert.toObject(JsonConvert.toJson(map_res.get("error")), Map.class);
//                    String type = String.valueOf(errorMsg.get("type"));
//                    // TODO 提醒
//                    if("insufficient_quota".equals(type)){
//
//                    }
//                    return Return.FAIL(BasicCode.error);
//                }else{
//                    List choices = JsonConvert.toObject(JsonConvert.toJson(map_res.get("choices")), List.class);
//                    Map content = JsonConvert.toObject(JsonConvert.toJson(choices.get(0)), Map.class);
//                    String text = String.valueOf(content.get("text"));
//                    return Return.SUCCESS(BasicCode.success).data(text);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//                logger.error("请求异常:{}" , e.getMessage());
//                return Return.FAIL(BasicCode.error);
//            }
//        }catch(Exception e){
//            e.printStackTrace();
//            logger.error("请求异常" , openId);
//            return Return.FAIL(BasicCode.error);
//        }

    public Return doRequest(String openId,String questions){

        String apikey = getRandomKey();

        if(Strings.isNullOrEmpty(apikey)){
            logger.info("没有apikey" );
            return Return.FAIL(BasicCode.error);
        }

        CodeUserQuota quota = getQuota(openId);

        if(quota==null){
            logger.info("没有 {} 的quota的记录" , openId);
            return Return.FAIL(BasicCode.quota_over_limit);
        }

        if(quota.getCnt() > quota.getMaxcnt()){

            if(quota.getUpdatetime().getTime() >= getTodayZeroTimeStamp() ){
                logger.info("{} 的quota超过" , openId);
                return Return.FAIL(BasicCode.quota_over_limit);
            }else{
                // 每天重置
                quota.setCnt(0);
                quota.setUpdatetime(new Date());
                saveQuota(quota);
            }
        }

        try{
            // String prompt = "现在你在一所大学中担任老师,我会给你一些题目,你需要通过代码的形式,返回结果,我的问题是:";
            String prompt = "";
            String url = _tgDataSourceConfig.getChatserver().concat("/aicode/doRequest");

            Map chatParam = new HashMap();
            chatParam.put("apikey",apikey);
            chatParam.put("question",prompt.concat(questions));
            chatParam.put("openId",openId);
            chatParam.put("model",openId);
            // 最多只能输出4k字,所以要控制返回的字符数量
            String str_res = OkHttpTools.post(OkHttpTools.MEDIA_TYPE_JSON,url,JsonConvert.toJson(chatParam));


            logger.info("返回:{}",str_res);
            ChatHist chatHist = new ChatHist();
            chatHist.setOpenid(openId);
            chatHist.setQuestion(questions);
            chatHist.setResult(str_res);
            _chatHistMapper.insert(chatHist);


            Return ret_resp = JsonConvert.toObject(str_res,Return.class);

            if(!ret_resp.is_success()){
                if(str_res.contains("insufficient_quota")){
                    logger.error("{} 余额不足",apikey);
                    SendAlarmTools.sendAlarm(apikey.concat("余额不足"));
                }
                return Return.FAIL(BasicCode.error);
            }else{
                String text = String.valueOf(ret_resp.get_data());
                return Return.SUCCESS(BasicCode.success).data(text);
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error("请求异常" , openId);
            return Return.FAIL(BasicCode.error);
        }

        // 小程序接受到响应后再扣除,所以不再后端处理
        // incrQuota(openId);

    }

    public long getTodayZeroTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    public String getRandomKey(){
        List<ApiKey> lists = getAllKeys();

        List<String> keys = new ArrayList<>();
        for (ApiKey list : lists) {
            keys.add(list.getApikey());
        }

        keys.remove(Invalidkey);
        keys.remove(QuotaKey);
        Collections.shuffle(keys);
        if(keys.size()>0){
            return keys.get(0);
        }else{
            // 没有可用的keys
            return "";
        }
    }

    public List<ApiKey> getAllKeys(){
        List<ApiKey> keys = _apiKeyMapper.search(new HashMap<>());
        return keys;
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
