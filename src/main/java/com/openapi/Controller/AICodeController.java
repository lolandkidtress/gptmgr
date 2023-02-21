package com.openapi.Controller;

import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openapi.Basic.BasicCode;
import com.openapi.Basic.Return;
import com.openapi.Model.CodeUser;
import com.openapi.Model.CodeUserQuota;
import com.openapi.Service.CodeUserService;
import com.google.common.base.Strings;


@RestController
@RequestMapping("/aicode")
public class AICodeController {

    private static Logger logger = LogManager.getLogger(AICodeController.class);

    @Autowired
    CodeUserService _codeUserService;

    @GetMapping("/getUser")
    public Return getUser(String openId) throws Exception {

        if(Strings.isNullOrEmpty(openId)){
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        CodeUser user = _codeUserService.getCodeUserByOpenId(openId);
        if (user == null) {
            return Return.FAIL(BasicCode.data_not_found);
        } else {
            return Return.SUCCESS(BasicCode.success).data(user);
        }
    }

    @PostMapping("/register")
    public Return register(@RequestBody CodeUser user) throws Exception {

        if(Strings.isNullOrEmpty(user.getOpenid())){
            return Return.FAIL(BasicCode.parameters_incorrect);
        }

        // 8位随机字符串
        int length = 8;
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        String code = sb.toString();

        user.setInvitecode(code);

        int cnt = _codeUserService.save(user);
        if (cnt <= 0) {
            return Return.FAIL(BasicCode.error);
        } else {
            return Return.SUCCESS(BasicCode.success).data(user);
        }
    }

    @GetMapping("/getQuota")
    public Return getQuota(String openId) {

        if(Strings.isNullOrEmpty(openId)){
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        CodeUserQuota codeUserQuota = _codeUserService.getQuota(openId);

        return Return.SUCCESS(BasicCode.success).data(codeUserQuota);

    }

    @PostMapping("/incrQuota")
    public Return incrQuota(@RequestBody CodeUserQuota codeUserQuota) {

        if(Strings.isNullOrEmpty(codeUserQuota.getOpenid())){
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        int cnt = _codeUserService.incrQuota(codeUserQuota.getOpenid());
        if (cnt > 0) {
            return Return.SUCCESS(BasicCode.success);
        }else{
            return Return.FAIL(BasicCode.error);
        }

    }

    @PostMapping("/setQuota")
    public Return setQuota(@RequestBody CodeUserQuota codeUserQuota) {

        if(Strings.isNullOrEmpty(codeUserQuota.getOpenid())){
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        int cnt = _codeUserService.saveQuota(codeUserQuota);
        if (cnt > 0) {
            return Return.SUCCESS(BasicCode.success).data(codeUserQuota);
        }else{
            return Return.FAIL(BasicCode.error);
        }
    }

    @PostMapping("/inputCode")
    public Return inputCode(@RequestBody Map<String,String> params) {
        String openId = params.get("openId");
        String code = params.get("code");

        if(Strings.isNullOrEmpty(openId)){
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        return _codeUserService.inputCode(openId,code);
    }

    @PostMapping("/doRequest")
    public Return doRequest(@RequestBody Map<String,String> postQuestion) {
        String openId = postQuestion.get("openId");
        String question = postQuestion.get("question");
        String method = postQuestion.get("method");

        if (Strings.isNullOrEmpty(openId)) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        if (Strings.isNullOrEmpty(question)) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }

        return _codeUserService.doRequest(method,openId,question,postQuestion);
    }
    // 假接口,用于调试
    @PostMapping("/doDummy")
    public Return doDummy(@RequestBody Map<String,String> postQuestion) {
        String openId = postQuestion.get("openId");
        String question = postQuestion.get("question");
        String method = postQuestion.get("method");

        if (Strings.isNullOrEmpty(openId)) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        if (Strings.isNullOrEmpty(question)) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        return Return.SUCCESS(BasicCode.success).data("\n\npublic class FibonacciSeries { \n    public static void main(String[] args) { \n\n        int num1 = 0; \n        int num2 = 1; \n\n        System.out.print(\"Fibonacci Series of first 10 numbers: \"); \n\n        for (int i = 1; i <= 10; ++i) \n        { \n            System.out.print(num1 + \" \"); \n\n            int temp = num1 + num2; \n            num1 = num2; \n            num2 = temp; \n        } \n    } \n}");
    }

    @PostMapping("/getModel")
    public Return getModel(@RequestBody Map<String,String> postQuestion) throws Exception{
        String openId = postQuestion.get("openId");
        String question = postQuestion.get("question");

        if (Strings.isNullOrEmpty(openId)) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        if (Strings.isNullOrEmpty(question)) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }


        return _codeUserService.doRequest("get",openId,question,postQuestion);
    }


}
