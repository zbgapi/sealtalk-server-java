package com.rcloud.server.sealtalk.model.dto;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/8/24
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Data
public class UserDTO {

    @ApiModelProperty("用户id")
    private String id;
    @ApiModelProperty("用户昵称")
    private String nickname;
    @ApiModelProperty("区号，废弃")
    private String region;
    @ApiModelProperty("ZBG用户id")
    private String phone;
    @ApiModelProperty("用户头像")
    private String portraitUri;
    @ApiModelProperty("性别，male/female")
    private String gender;
    @ApiModelProperty("黑名单")
    private String stAccount;
    @ApiModelProperty("禁封状态，1：禁封，0：没有封号")
    private Integer blockStatus;


}
