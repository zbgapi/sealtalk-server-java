package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.domain.Groups;
import com.rcloud.server.sealtalk.model.dto.GroupAdminDTO;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface GroupsMapper extends Mapper<Groups> {
    List<GroupAdminDTO> selectGroupsForAdmin(@Param("name") String name, @Param("creatorUid") String creatorUid,
                                             @Param("referFlag") Integer referFlag, @Param("hotFlag") Integer hotFlag);

}