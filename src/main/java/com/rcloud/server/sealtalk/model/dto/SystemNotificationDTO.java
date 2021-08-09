package com.rcloud.server.sealtalk.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("系统消息返回值")
public class SystemNotificationDTO {
    @ApiModelProperty(value = "消息ID", required = true)
    private Integer id;
    @ApiModelProperty(value = "消息序列号，同一批发送的消息该字段相同", required = true)
    private Long serialNo;
    @ApiModelProperty(value = "成员ID", required = true)
    private String memberId;
    @ApiModelProperty(value = "消息内容", required = true)
    private String content;
    @ApiModelProperty(value = "发送时间", required = true)
    private String createdAt;
    @ApiModelProperty(value = "更新时间", required = true)
    private String updatedAt;

    @ApiModelProperty(value = "接受用户", required = true)
    private UserDTO users;
}
