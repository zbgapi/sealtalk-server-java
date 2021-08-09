package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.dao.SystemNotificationMapper;
import com.rcloud.server.sealtalk.domain.SystemNotification;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.common.Mapper;

import javax.annotation.Resource;
import java.util.List;

@Service
public class SystemNotificationService extends AbstractBaseService<SystemNotification, Integer> {
    @Resource
    private SystemNotificationMapper mapper;
    @Override
    protected Mapper<SystemNotification> getMapper() {
        return this.mapper;
    }

    public void batchInsert(List<SystemNotification> notificationList) {
        if (notificationList.isEmpty()) {
            return;
        }

        mapper.insertList(notificationList);
    }

    public List<SystemNotification> querySystemMessageWithUserIdAndTime(Integer memberId, String startTime, String endTime) {
        return mapper.querySystemMessageWithUserIdAndTime(memberId, startTime, endTime);
    }
}
