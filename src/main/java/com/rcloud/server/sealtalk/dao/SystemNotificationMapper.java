package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.domain.SystemNotification;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SystemNotificationMapper extends Mapper<SystemNotification> {

    int insertList(@Param("notificationList") List<SystemNotification> notificationList);

    List<SystemNotification> querySystemMessageWithUserIdAndTime(@Param("memberId") Integer memberId, @Param("startTime")String startTime, @Param("endTime")String endTime);
}