package com.rcloud.server.sealtalk.dao;

import com.rcloud.server.sealtalk.domain.Users;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface UsersMapper extends Mapper<Users> {
    int insertBatch(@Param("usersList") List<Users> usersList);
}