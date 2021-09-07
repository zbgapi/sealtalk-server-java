package com.rcloud.server.sealtalk.exchange.service;

import com.rcloud.server.sealtalk.exchange.dao.EConfigMapper;
import com.rcloud.server.sealtalk.exchange.domain.EConfig;
import com.rcloud.server.sealtalk.util.MiscUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.genid.GenIdUtil;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Service
public class EConfigService {
    @Resource
    private EConfigMapper eConfigMapper;

    public void saveOrUpdate(EConfig config) {
        Example example = new Example(EConfig.class);
        example.createCriteria()
                .andEqualTo("type", config.getType())
                .andEqualTo("code", config.getCode())
                .andEqualTo("codeName", config.getCodeName());
        List<EConfig> configs = eConfigMapper.selectByExample(example);
        if (configs != null && configs.size() > 0) {
            eConfigMapper.updateByExampleSelective(config, example);
        } else {
            config.setId(MiscUtils.shortUuid());
            eConfigMapper.insert(config);
        }
    }
}
