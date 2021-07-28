package com.rcloud.server.sealtalk.model.dto;

import lombok.Data;

import javax.persistence.Column;
import java.math.BigDecimal;
import java.util.Date;


@Data
public class GroupAdminDTO {
    private String id;
    private String name;
    private String portraitUri;
    private Integer memberCount;
    private Integer maxMemberCount;
    private String creatorUId;
    private String bulletin;
    private Date deletedAt;
    private Integer isMute;
    private Integer certiStatus;
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
