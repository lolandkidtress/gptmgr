package com.openapi.tools;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;


public class CommonUtils {

    public final static ZoneId ZONDID = ZoneId.of( "GMT+08:00" );
    public final static Charset CHARSET = StandardCharsets.UTF_8;
    public final static String SLASH = System.getProperty("file.separator");
    public final static String UNDERLINE = "_";
    public final static String HYPHEN = "-";
    public final static String COLON = ":";
    public final static String DEFAULTVERSION = "defaultVersion";
    public final static String HTTP_PROTOCOL_PREFIX = "http://";


    public static String byteArrayToHex(byte[] byteArray) {

        // 首先初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9', 'A','B','C','D','E','F' };

        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray =new char[byteArray.length * 2];

        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b>>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b& 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }

    public static String generalMD5(String input){
        try {
            // 拿到一个MD5转换器（如果想要SHA1参数换成”SHA1”）
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");

            // 输入的字符串转换成字节数组
            byte[] inputByteArray = input.getBytes();

            // inputByteArray是输入字符串转换得到的字节数组
            messageDigest.update(inputByteArray);

            // 转换并返回结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();

            // 字符数组转换成字符串返回
            return byteArrayToHex(resultByteArray);
        } catch (NoSuchAlgorithmException e) {
            //异常就原样返回
            return input;
        }
    }

    //生成加盐的MD5
    public static String generalNonce(String s,String salt){

        return CommonUtils.generalMD5(CommonUtils.generalMD5(s.concat(salt)));
    }


}
