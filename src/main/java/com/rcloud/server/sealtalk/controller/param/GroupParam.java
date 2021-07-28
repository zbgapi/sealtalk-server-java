package com.rcloud.server.sealtalk.controller.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/8/24
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Data
public class GroupParam {

    private String groupId;
    private String name;
    private String memberId;
    private String[] memberIds;
    private String portraitUri;

    private String[] userId;

    private String bulletin;

    private String displayName;

    private Integer certiStatus;

    private String receiverId;

    private String status;

    private Integer muteStatus;

    private Integer clearStatus;

    private String groupNickname;
    private String region;
    private String phone;

    @JsonProperty(value = "WeChat")
    private String weChat;

    @JsonProperty(value = "Alipay")
    private String alipay;
    private String[] memberDesc;

    private String content;

    private Integer memberProtection;  //成员保护模式: 0 关闭、1 开启

    private Integer maxMemberCount;

    //  以下是自定义字段
    /**
     * 入群持有币种名限制
     */
    private String currencyName;
    /**
     * 入群持有币种数量
     */
    private BigDecimal amount;
    /**
     * 入群验证码
     */
    private String verificationCode;
    /**
     * 关联交易对
     */
    private String marketName;
    /**
     * 是否推荐群
     */
    private Integer referFlag;
    /**
     * 是否推荐群
     */
    private Integer hotFlag;
}
