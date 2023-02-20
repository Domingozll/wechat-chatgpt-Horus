package com.zl.horus.utils; /**
 * @Author: zhanglin
 * @Date: 2023/2/18 23:37
 * @Description：TODO
 */

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @BelongsProject: wechat_chatGPT_horus
 * @BelongsPackage: com.zl.horus.utils
 * @Author: zhanglin
 * @CreateTime: 2023-02-18  23:37
 * @Description: TODO
 * @Version: 1.0
 */
@Slf4j
public class HashUtils {

    public static String getHash(String val) {
        try {
            //MessageDigest 类为应用程序提供信息摘要算法的功能，如 MD5 或 SHA 算法。
            //信息摘要是安全的单向哈希函数，它接收 任意大小的数据，并输出固定长度的哈希值。
            //MessageDigest 对象开始被初始化。
            MessageDigest mDigest = MessageDigest.getInstance("MD5");
            //通过使用 update 方法处理数据
            mDigest.update(val.getBytes());
            //调用 digest 方法之一完成哈希计算同时将Byte数组转换成16进制
            return bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("字符串转hash异常：{} ", e);
        }

        return "emptykey";
    }

    private static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        //MD5加密后bytes长度16转换成32位16进制字符串
        for (int i = 0; i < bytes.length; i++) {
            /**
             * 在32位的电脑中数字都是以32格式存放的，如果是一个byte(8位)类型的数字，
             * 他的高24位里面都是随机数字，低8位才是实际的数据。
             * java.lang.Integer.toHexString() 方法的参数是int(32位)类型.
             * 如果输入一个byte(8位)类型的数字，这个方法会把这个数字的高24为也看作有效位，
             * 这就必然导致错误，使用& 0XFF操作，可以把高24位置0以避免这样错误.
             *
             * 0xFF = 1111 1111　 低8位为1，高位都为0
             * 故 &0xFF 可将数字的高位都置为0，低8位不变
             *
             * */
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

}
