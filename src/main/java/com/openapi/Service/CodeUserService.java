package com.openapi.Service;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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


    public Return doRequest(String method, String openId,String questions,Map<String,String> params){

        String apikey = _tgDataSourceConfig.getApikey();

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

        Map header = new HashMap();

        header.put("authorization","Bearer "+"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");
        // 获取所有的模型,这里用来检测apikey是否有效
        if("get".equals(method)){
            try{
                String url = "https://api.openai.com/v1/models";
//                OkHttpClient client = getUnsafeOkHttpClient();
//
//                Request request = new Request.Builder()
//                    .url(url)
//                    .header("authorization","Bearer "+"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")
//                    .build();
//
//                try (Response response = client.newCall(request).execute()) {
//                    return Return.SUCCESS(BasicCode.success).data(response.body().string());
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    logger.error("请求异常" , openId);
//                    return Return.FAIL(BasicCode.error);
//                }
                String str = OkHttpTools.sslget(url,header,30,header);
                return Return.SUCCESS(BasicCode.success).data(str);
            }catch(Exception e){
                e.printStackTrace();
                logger.error("请求异常" , openId);
                return Return.FAIL(BasicCode.error);
            }
        }
        if("post".equals(method)){
            try{
                String url = "https://api.openai.com/v1/completions";
                OkHttpClient client = getUnsafeOkHttpClient();
                Map chatParam = new HashMap();
                chatParam.put("model","text-davinci-003");

                String prompt = "现在你在一所大学中担任老师,我会给你一些题目,你需要通过代码的形式,返回结果,我的问题是:";
                chatParam.put("prompt",prompt.concat(questions));
                // 最多只能输出4k字,所以要控制返回的字符数量
                chatParam.put("max_tokens",3500 - questions.length());

//                String str_ret = OkHttpTools.sslpost(OkHttpTools.MEDIA_TYPE_JSON, url, JsonConvert.toJson(chatParam), 30, header);
//                return Return.SUCCESS(BasicCode.success).data(str_ret);
//

                String requestBody = JsonConvert.toJson(chatParam); // 请求体，JSON 格式

                MediaType mediaType = MediaType.parse("application/json; charset=utf-8"); // 设置请求体的媒体类型
                RequestBody body = RequestBody.create(requestBody, mediaType); // 创建请求体


                Request request = new Request.Builder()
                    .url(url)
                    .header("authorization","Bearer "+"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")
                    .post(body)
                    .build();

                logger.info("{}的问题:{}",openId,questions);
                try (Response response = client.newCall(request).execute()) {
                    String str_res = response.body().string();

                    logger.info("返回:{}",str_res);
                    ChatHist chatHist = new ChatHist();
                    chatHist.setOpenid(openId);
                    chatHist.setQuestion(questions);
                    chatHist.setResult(str_res);
                    _chatHistMapper.insert(chatHist);

                    Map map_res = JsonConvert.toObject(str_res,Map.class);
                    if(map_res.containsKey("error")){
                        Map errorMsg = JsonConvert.toObject(JsonConvert.toJson(map_res.get("error")), Map.class);
                        String type = String.valueOf(errorMsg.get("type"));
                        // TODO 提醒
                        if("insufficient_quota".equals(type)){

                        }
                        return Return.FAIL(BasicCode.error);
                    }else{
                        List choices = JsonConvert.toObject(JsonConvert.toJson(map_res.get("choices")), List.class);
                        Map content = JsonConvert.toObject(JsonConvert.toJson(choices.get(0)), Map.class);
                        String text = String.valueOf(content.get("text"));
                        return Return.SUCCESS(BasicCode.success).data(text);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.error("请求异常" , openId);
                    return Return.FAIL(BasicCode.error);
                }
            }catch(Exception e){
                e.printStackTrace();
                logger.error("请求异常" , openId);
                return Return.FAIL(BasicCode.error);
            }
        }
        // 小程序接受到响应后再扣除
        // incrQuota(openId);

        return Return.SUCCESS(BasicCode.success).data("问题答案");

    }

    public static long getTodayZeroTimeStamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
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