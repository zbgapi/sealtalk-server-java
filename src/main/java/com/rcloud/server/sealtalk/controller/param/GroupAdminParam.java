package com.rcloud.server.sealtalk.controller.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class GroupAdminParam {
    @ApiModelProperty(value = "群组id", required = true)
    private String groupId;
    @ApiModelProperty(value = "群成员id，退出群组时必传")
    private String memberId;
    @ApiModelProperty(value = "群成员id列表，设置管理员时必传")
    private String[] memberIds;
    @ApiModelProperty(value = "ZBG用户id列表，添加群成员时必传")
    private String[] userId;
    @ApiModelProperty(value = "群公告，设置群公告时必传")
    private String bulletin;
    @ApiModelProperty(value = "群禁言状态，1：禁言，0：取消禁言，设置/取消 全员禁言时必传")
    private Integer muteStatus;
}
