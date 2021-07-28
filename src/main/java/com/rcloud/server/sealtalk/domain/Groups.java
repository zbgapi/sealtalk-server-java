package com.rcloud.server.sealtalk.domain;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Table(name = "`groups`")
public class Groups implements Serializable {
    private static final long serialVersionUID = 1L;
    //关闭群组认证状态标示
    public static final Integer CERTI_STATUS_CLOSED = 1;
    //开启群组认证状态标示
    public static final Integer CERTI_STATUS_OPENED = 0;

    //全员禁言状态 否
    public static final Integer MUTE_STATUS_CLOSE = 0;

    //全员禁言状态 是
    public static final Integer MUTE_STATUS_OPENED = 1;


    //copiedTime 默认值
    public static final Long COPIED_TIME_DEFAUT = 0L;

    //clearStatus 0 关闭 、3 清理 3 天前、 7 清理 7 天前、 36 清理 36 小时前
    public static final Integer CLEAR_STATUS_CLOSED = 0;
    public static final Integer CLEAR_STATUS_D_3 = 3;
    public static final Integer CLEAR_STATUS_D_7 = 7;
    public static final Integer CLEAR_STATUS_H_36 = 36;


    @Id
    @GeneratedValue(generator = "JDBC")
    private Integer id;

    private String name;

    @Column(name = "portraitUri")
    private String portraitUri;

    @Column(name = "memberCount")
    private Integer memberCount;

    @Column(name = "maxMemberCount")
    private Integer maxMemberCount;

    @Column(name = "creatorId")
    private Integer creatorId;
    /**
     * 开启群认证 0-开启 1-关闭
     */
    @Column(name = "certiStatus")
    private Integer certiStatus;

    @Column(name = "isMute")
    private Integer isMute;

    /**
     * 开启/更新 清理群离线消息
     * 清理选项： 0 关闭、 3 清理 3 天前、 7 清理 7 天前、 36 清理 36 小时前
     */
    @Column(name = "clearStatus")
    private Integer clearStatus;

    @Column(name = "clearTimeAt")
    private Long clearTimeAt;

    /**
     * 开启群保护 0-关闭 1-开启
     */
    @Column(name = "memberProtection")
    private Integer memberProtection;

    @Column(name = "copiedTime")
    private Long copiedTime;

    private Long timestamp;

    @Column(name = "createdAt")
    private Date createdAt;

    @Column(name = "updatedAt")
    private Date updatedAt;

    @Column(name = "deletedAt")
    private Date deletedAt;

    @Column(name = "bulletin")
    private String bulletin;

    //  以下是自定义字段
    /**
     * 入群持有币种名限制
     */
    @Column(name = "currencyName")
    private String currencyName;
    /**
     * 入群持有币种数量
     */
    @Column(name = "amount")
    private BigDecimal amount;
    /**
     * 入群验证码
     */
    @Column(name = "verificationCode")
    private String verificationCode;
    /**
     * 关联交易对
     */
    @Column(name = "marketName")
    private String marketName;
    /**
     * 是否推荐群
     */
    @Column(name = "referFlag")
    private Integer referFlag;
    /**
     * 是否推荐群
     */
    @Column(name = "hotFlag")
    private Integer hotFlag;

}