package com.zl.horus.controller;

import com.zl.horus.service.ChatGptServiceImpl;
import com.zl.horus.utils.HashUtils;
import com.zl.horus.utils.RedisUtils;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * 微信事件推送及被动回复消息
 *
 * @author honghu
 */
@Slf4j
@RestController
@RequestMapping("wx/mp/welcome")
public class WxMpMsgController {

    @Resource
    private WxMpService wxMpService;

    @Resource
    private ChatGptServiceImpl chatGptService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 消息校验，确定是微信发送的消息
     *
     * @param signature
     * @param timestamp
     * @param nonce
     * @param echostr
     * @return
     * @throws Exception
     */
    @GetMapping
    public String get(String signature, String timestamp, String nonce, String echostr) {
        // 消息签名不正确，说明不是公众平台发过来的消息
        if (!wxMpService.checkSignature(timestamp, nonce, signature)) {
            return null;
        }
        // 消息合法
        return echostr;
    }

    @GetMapping("/test")
    public String test(@Param("msg") String msg) {
        return chatGptService.reply(msg, "test", "");
    }

    /**
     * 被动回复用户消息
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping
    public String post(HttpServletRequest request) throws IOException, WxErrorException, InterruptedException {
        WxMpXmlMessage message = WxMpXmlMessage.fromXml(request.getInputStream());
        String messageType = message.getMsgType();
        String fromUser = message.getFromUser();
        String touser = message.getToUser();
        String text = message.getContent();

        String cacheKey = HashUtils.getHash(StringUtils.join(fromUser, text));

        boolean flag = RedisUtils.setIfAbsent(stringRedisTemplate, cacheKey, text, 5, TimeUnit.SECONDS);

        if (!flag) {
            Thread.sleep(10000);
            return "";
        }

//        log.info("微信入参：{}", message.toString());
        if (flag) {
            log.info("消息类型:" + messageType);
            log.info("发送者账号：" + fromUser);
            log.info("开发者微信：" + touser);
            log.info("文本消息：" + text);
        }

        // 事件推送
        if (messageType.equals("event")) {
            log.info("event：" + message.getEvent());
            // 关注
            if (message.getEvent().equals("subscribe")) {
                log.info("用户关注：{}", fromUser);
                WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                        .TEXT()
                        .toUser(fromUser)
                        .fromUser(touser)
                        .content("谢谢关注，你可以问我任何问题。")
                        .build();

                String result = texts.toXml();

                log.info("响应给用户的消息：" + result);

                return result;
            }
            // 取消关注
            if (message.getEvent().equals("unsubscribe")) {
                log.info("用户取消关注：{}", fromUser);
            }
            // 点击菜单
            if (message.getEvent().equals("CLICK")) {
                log.info("用户点击菜单：{}", message.getEventKey());
            }
            // 点击菜单
            if (message.getEvent().equals("VIEW")) {
                log.info("用户点击菜单：{}", message.getEventKey());
            }
            // 已关注用户扫描带参数二维码
            if (message.getEvent().equals("scancode_waitmsg")) {
                log.info("用户扫描二维码：{}", fromUser);
            }
            // 获取位置信息
            if (message.getEvent().equals("LOCATION")) {
                log.info("用户发送位置信息：经度：{}，纬度：{}", message.getLatitude(), message.getLongitude());
            }
            return null;
        }
        //文本消息
        if (messageType.equals("text")) {
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(fromUser)
                    .fromUser(touser)
                    .content(chatGptService.reply(text, fromUser, cacheKey))
                    .build();
            String result = texts.toXml();
            log.info("响应给用户的消息：" + result);
            return result;
        }
        //图片消息
        if (messageType.equals("image")) {
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(fromUser)
                    .fromUser(touser)
                    .content("已接收到您发的图片信息")
                    .build();
            String result = texts.toXml();
            result.replace("你发送的消息为： ", "");
            log.info("响应给用户的消息：" + result);
            return result;
        }
        /**
         * 语音消息
         */
        if (messageType.equals("voice")) {
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(fromUser)
                    .fromUser(touser)
                    .content("已接收到您发的语音信息")
                    .build();
            String result = texts.toXml();
            log.info("响应给用户的消息：" + result);
            return result;
        }
        /**
         * 视频
         */
        if (messageType.equals("video")) {
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(fromUser)
                    .fromUser(touser)
                    .content("已接收到您发的视频信息")
                    .build();
            String result = texts.toXml();
            log.info("响应给用户的消息：" + result);
            return result;
        }
        /**
         * 小视频
         */
        if (messageType.equals("shortvideo")) {
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(fromUser)
                    .fromUser(touser)
                    .content("已接收到您发的小视频信息")
                    .build();
            String result = texts.toXml();
            log.info("响应给用户的消息：" + result);
            return result;
        }
        /**
         * 地理位置信息
         */
        if (messageType.equals("location")) {
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(fromUser)
                    .fromUser(touser)
                    .content("已接收到您发的地理位置信息")
                    .build();
            String result = texts.toXml();
            log.info("响应给用户的消息：" + result);
            return result;
        }
        /**
         * 链接信息
         */
        if (messageType.equals("link")) {
            WxMpXmlOutTextMessage texts = WxMpXmlOutTextMessage
                    .TEXT()
                    .toUser(fromUser)
                    .fromUser(touser)
                    .content("已接收到您发的链接信息")
                    .build();
            String result = texts.toXml();
            log.info("响应给用户的消息：" + result);
            return result;
        }
        return null;
    }


//    public void kefuMessage(String toUser, String content) throws WxErrorException {
//        WxMpKefuMessage message = new WxMpKefuMessage();
//        message.setToUser(toUser);
//        message.setMsgType("text");
//        message.setContent(content);
//
//        wxMpService.getKefuService().sendKefuMessage(message);
//    }

}
