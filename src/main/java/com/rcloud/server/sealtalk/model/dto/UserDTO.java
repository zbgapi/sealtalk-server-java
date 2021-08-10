package com.rcloud.server.sealtalk.model.dto;

import com.rcloud.server.sealtalk.domain.Users;
import com.rcloud.server.sealtalk.exception.ServiceException;
import com.rcloud.server.sealtalk.util.N3d;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @Author: Jianlu.Yu
 * @Date: 2020/8/24
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Data
@ApiModel("用户信息")
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
    @ApiModelProperty("创建时间")
    private String createAt;

    public static UserDTO copyOf(Users u) throws ServiceException {
        UserDTO userDTO = new UserDTO();

        if (u != null) {
            userDTO.setId(N3d.encode(u.getId()));
            userDTO.setNickname(u.getNickname());
            userDTO.setRegion(u.getRegion());
            userDTO.setPhone(u.getPhone());
            userDTO.setGender(u.getGender());
            userDTO.setPortraitUri(u.getPortraitUri());
            userDTO.setStAccount(u.getStAccount());
        }

        return userDTO;
    }
}
