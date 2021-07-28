package com.rcloud.server.sealtalk.controller.param;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("设置用户封号状态入参")
public class UserBlockParam {
    @ApiModelProperty("账户id")
    private String id;
    @ApiModelProperty(value = "封号状态，1：封号，0：取消封号")
    private Integer status;
    @ApiModelProperty(value = "封禁时间，单位分钟，不能超过43200")
    private Integer minute;
}
