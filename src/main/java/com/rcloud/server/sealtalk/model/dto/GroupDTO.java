package com.rcloud.server.sealtalk.model.dto;

import com.rcloud.server.sealtalk.domain.Groups;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.util.N3d;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/8/25
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Data
public class GroupDTO {
    @ApiModelProperty("群ID")
    private String id;
    @ApiModelProperty("群名称")
    private String name;
    @ApiModelProperty("群头像")
    private String portraitUri;
    @ApiModelProperty("群成员数量")
    private Integer memberCount;
    @ApiModelProperty("群成员数量上限")
    private Integer maxMemberCount;
    @ApiModelProperty("群创建者ID")
    private String creatorId;
    @ApiModelProperty("群公告")
    private String bulletin;

    private Date deletedAt;
    @ApiModelProperty("是否群禁言")
    private Integer isMute;
    private Integer certiStatus;
    private Integer memberProtection;

    //  以下是自定义字段

    @ApiModelProperty("入群持有币种名限制")
    private String currencyName;
    @ApiModelProperty("入群持有币种数量")
    private BigDecimal amount;
    @ApiModelProperty("入群验证码")
    private String verificationCode;
    @ApiModelProperty("关联交易对")
    private String marketName;
    @ApiModelProperty("是否推荐群")
    private Integer referFlag;
    @ApiModelProperty("是否推荐群")
    private Integer hotFlag;

    public static GroupDTO copyOf(Groups group) throws ServiceException {
        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setId(N3d.encode(group.getId()));
        groupDTO.setName(group.getName());
        groupDTO.setPortraitUri(group.getPortraitUri());
        groupDTO.setCreatorId(N3d.encode(group.getCreatorId()));
        groupDTO.setMemberCount(group.getMemberCount());
        groupDTO.setMaxMemberCount(group.getMaxMemberCount());
        groupDTO.setCertiStatus(group.getCertiStatus());
        groupDTO.setBulletin(group.getBulletin());
        groupDTO.setIsMute(group.getIsMute());
        groupDTO.setMemberProtection(group.getMemberProtection());
        groupDTO.setDeletedAt(group.getDeletedAt());
        groupDTO.setCurrencyName(group.getCurrencyName());
        groupDTO.setAmount(group.getAmount());
        groupDTO.setVerificationCode(group.getVerificationCode());
        groupDTO.setMarketName(group.getMarketName());
        groupDTO.setReferFlag(group.getReferFlag());
        groupDTO.setHotFlag(group.getHotFlag());
        return groupDTO;
    }
}
