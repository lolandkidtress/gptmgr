package com.openapi.Controller;

import java.util.List;
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

import com.google.common.base.Strings;
import com.openapi.Basic.BasicCode;
import com.openapi.Basic.JsonConvert;
import com.openapi.Basic.Return;
import com.openapi.Model.CodeUser;
import com.openapi.Model.CodeUserQuota;
import com.openapi.Service.CodeUserService;


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
    // 3.5 官方接口
    @PostMapping("/doAsk")
    public Return doAsk(@RequestBody Map<String,Object> postQuestion) {


        if (!postQuestion.containsKey("openId")) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        if (!postQuestion.containsKey("question")) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }

        String openId = String.valueOf(postQuestion.get("openId"));
        // list 提示词,用于传上下文
        Object hint = postQuestion.get("hint");
        // 实际的问题
        String question = String.valueOf(postQuestion.get("question"));

        String apikey = String.valueOf(postQuestion.get("apikey"));

        return _codeUserService.doAsk(apikey,openId,hint,question);
    }


    // 给小程序调用的
    @PostMapping("/doRequest")
    public Return doRequest(@RequestBody Map<String,Object> postQuestion) {

        if (!postQuestion.containsKey("apikey")) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        if (!postQuestion.containsKey("question")) {
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
        try{
            String apikey = String.valueOf(postQuestion.get("apikey"));
            List question = JsonConvert.toObject(JsonConvert.toJson(postQuestion.get("question")), List.class);
            Double temperature = 0.2;
            if (postQuestion.containsKey("temperature")) {

                temperature = Double.parseDouble(String.valueOf(postQuestion.get("temperature")));
                if(temperature > 1){
                    return Return.FAIL(BasicCode.parameters_incorrect);
                }
            }

            return _codeUserService.doRequest(apikey,temperature,question);
        }catch(Exception e){
            logger.error(e);
            return Return.FAIL(BasicCode.parameters_incorrect);
        }
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
        return Return.SUCCESS(BasicCode.success).data("中所有低于等于500的偶数?\\n\\ndef evenFibonacci(n):\\n    result = []\\n    # Initialize first two even Fibonacci numbers \\n    f1 = 0\\n    f2 = 2\\n \\n    # Add the first two even numbers to result \\n    result.append(f1)\\n    result.append(f2)\\n \\n    next_even_fib = f1 + f2\\n \\n    # Calculate and add remaining even numbers of the \\n    # Fibonacci series till n\\n    while(next_even_fib <= n):\\n        result.append(next_even_fib)\\n        next_even_fib = 4 * f2 + f1\\n        f1 = f2\\n        f2 = next_even_fib\\n \\n    # Print all even elements of result\\n    print(*result, sep = \\\", \\\")\\n \\n# Driver code\\nn = 500\\nevenFibonacci(n)");

    }

}
