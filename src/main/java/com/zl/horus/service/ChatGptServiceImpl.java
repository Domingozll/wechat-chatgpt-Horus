package com.zl.horus.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.zl.horus.entity.MessageResponseBody;
import com.zl.horus.entity.MessageSendBody;
import com.zl.horus.utils.HttpUtil;
import com.zl.horus.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author honghu
 */
@Slf4j
@Service
public class ChatGptServiceImpl implements ChatGptService {

    @Value("${openapi.key}")
    private String apiKey;
    /**
     * 接口请求地址
     */
    private final String url = "https://api.openai.com/v1/completions";

    @Resource
    StringRedisTemplate stringRedisTemplate;

    private final String human = "Human:";
    /**
     * 定义ai的名字
     */
    private final String Ai = "Horus:";

    private final String CONTENT_KEY_PRFIX = "content-key:";

    private final Map<String, String> localCache = new ConcurrentHashMap<String, String>(1000);

    @Override
    public String reply(String messageContent, String userKey, String cacheKey) {
        String contentKey = CONTENT_KEY_PRFIX + cacheKey;
        RedisUtils redisUtils = new RedisUtils(stringRedisTemplate);
        if (redisUtils.hasKey(contentKey)) {
            JSONObject obj = JSON.parseObject(redisUtils.get(contentKey));
            return getString(userKey, redisUtils, obj, false);
        }

        // 默认信息
        String message = "Human:你好\nHorus:你好\n";
        if (redisUtils.hasKey(userKey)) {
            // 如果存在key，拿出来
            message = redisUtils.get(userKey);
        }
        // 拼接字符,设置回去
        message = message + human + messageContent + "\n";
        JSONObject obj = getReplyFromGPT(message);
        new Thread(() -> {
            redisUtils.setEx(contentKey, obj.toJSONString(), 60, TimeUnit.SECONDS);
        }).start();
        // 调用接口获取数据
        return getString(userKey, redisUtils, obj, true);
    }

    private String getString(String userKey, RedisUtils redisUtils, JSONObject obj, boolean chcheMsg) {
        MessageResponseBody messageResponseBody = JSONObject.toJavaObject(obj, MessageResponseBody.class);
        if (messageResponseBody != null) {
            if (!CollectionUtils.isEmpty(messageResponseBody.getChoices())) {
                String replyText = messageResponseBody.getChoices().get(0).getText();
                // 拼接字符,设置回去
                if (chcheMsg) {
                    new Thread(() -> {
                        String msg = redisUtils.get(userKey);
                        msg = msg + Ai + replyText + "\n";
                        // 存储对话内容，让机器人更加智能
                        redisUtils.setEx(userKey, msg, 60, TimeUnit.SECONDS);
                    }).start();
                }
                return replyText.replace("Horus:", "")
                        .replace("Horus:", "");
            }
        }
        return "很抱歉，我出了点故障，让我休息一会儿!";
    }

    private JSONObject getReplyFromGPT(String message) {
        String url = this.url;
        Map<String, String> header = new HashMap();
        header.put("Authorization", "Bearer " + apiKey);
        header.put("Content-Type", "application/json");
        MessageSendBody messageSendBody = buildConfig();
        messageSendBody.setPrompt(message);
        String body = JSON.toJSONString(messageSendBody, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);
        log.info("向chatGpt发送的数据：" + body);
        // 发送请求
        String data = HttpUtil.doPostJson(url, body, header);
        JSONObject obj = JSON.parseObject(data);
        log.info("chatGpt响应的数据：" + obj);
        return obj;
    }

    /**
     * 构建请求体
     *
     * @return
     */
    private MessageSendBody buildConfig() {
        MessageSendBody messageSendBody = new MessageSendBody();
        messageSendBody.setModel("text-davinci-003");
        messageSendBody.setTemperature(0.9);
        messageSendBody.setMaxTokens(1000);
        messageSendBody.setTopP(1);
        messageSendBody.setFrequencyPenalty(0.0);
        messageSendBody.setPresencePenalty(0.6);
        List<String> stop = new ArrayList<>();
        stop.add(" Horus:");
        stop.add(" Human:");
        messageSendBody.setStop(stop);
        return messageSendBody;
    }

    /**
     * 解决大文章问题超5秒问题，但是目前事个人订阅号，没有客服接口权限，暂时没用
     *
     * @param messageContent
     * @param userKey
     * @return
     */
    public String getArticle(String messageContent, String userKey) {
        String url = "https://api.openai.com/v1/completions";
        Map<String, String> header = new HashMap();
        header.put("Authorization", "Bearer " + apiKey);
        header.put("Content-Type", "application/json");

        MessageSendBody messageSendBody = buildConfig();
        messageSendBody.setMaxTokens(150);
        messageSendBody.setPrompt(messageContent);
        String body = JSON.toJSONString(messageSendBody, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue, SerializerFeature.WriteDateUseDateFormat);
        String data = HttpUtil.doPostJson(url, body, header);
        log.info("返回的数据：" + data);
        JSONObject obj = JSON.parseObject(data);
        MessageResponseBody messageResponseBody = JSONObject.toJavaObject(obj, MessageResponseBody.class);
        if (messageResponseBody != null) {
            if (!CollectionUtils.isEmpty(messageResponseBody.getChoices())) {
                String replyText = messageResponseBody.getChoices().get(0).getText();
                return replyText.replace("Horus:", "");
            }
        }
        return "很抱歉，我出了点故障，让我休息一会儿!";
    }
}
