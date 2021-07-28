package com.rcloud.server.sealtalk.service;

import com.rcloud.server.sealtalk.dao.DataVersionsMapper;
import com.rcloud.server.sealtalk.domain.DataVersions;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.common.Mapper;

import javax.annotation.Resource;
import javax.xml.crypto.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: xiuwei.nie
 * @Date: 2020/7/6
 * @Description:
 * @Copyright (c) 2020, rongcloud.cn All Rights Reserved
 */
@Service
public class DataVersionsService extends AbstractBaseService<DataVersions, Integer> {

    @Resource
    private DataVersionsMapper mapper;

    @Override
    protected Mapper<DataVersions> getMapper() {
        return mapper;
    }


    public void updateAllFriendshipVersion(Integer userId, long timestamp) {
        mapper.updateAllFriendshipVersion(userId, timestamp);
    }


    public void updateGroupMemberVersion(Integer groupId, long timestamp) {
        mapper.updateGroupMemberVersion(groupId, timestamp);
    }

    public void updateGroupVersion(Integer groupId, long timestamp) {

        mapper.updateGroupVersion(groupId,timestamp);

    }

    /**
     * 更新黑名单数据版本
     *
     * @param userId
     * @param timestamp
     */
    public void updateBlacklistVersion(Integer userId, long timestamp) {

        DataVersions dataVersions = new DataVersions();
        dataVersions.setUserId(userId);
        dataVersions.setBlacklistVersion(timestamp);
        this.updateByPrimaryKeySelective(dataVersions);
    }

    public void batchInsert(List<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }
        List<DataVersions> dataVersions = new ArrayList<>();
        int index = 0;
        for (Integer id : ids) {
            DataVersions dv = new DataVersions();
            dv.setUserId(id);

            dataVersions.add(dv);

            //批量插入DataVersions，每1000条执行一次insert sql
            index++;
            if( index % 1000 == 0 || index == ids.size()) {
                mapper.insertBatch(dataVersions);
                dataVersions.clear();
            }
        }
    }
}
