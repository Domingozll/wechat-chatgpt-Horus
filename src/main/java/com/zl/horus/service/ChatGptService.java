package com.zhanglin.wx_chatgpt.service;

/**
 * @author honghu
 */
public interface ChatGptService {
    /**
     * 调用chatGTP回复信息
     * @param messageContent
     * @param userKey
     * @return
     */
    String reply(String messageContent, String userKey);
}
