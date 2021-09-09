package com.rcloud.server.sealtalk.exchange.service;

import com.rcloud.server.sealtalk.exchange.dao.EConfigMapper;
import com.rcloud.server.sealtalk.exchange.domain.EConfig;
import com.rcloud.server.sealtalk.util.MiscUtils;
import com.rcloud.server.sealtalk.util.N3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import javax.annotation.Resource;
import java.util.List;

@Service
public class EConfigService {
    private static Logger log = LoggerFactory.getLogger(EConfigService.class);

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

    public void configTalkMarket(String marketName, Integer groupId, Integer creatorId, String oldMarketName) {
        try {
            if (StringUtils.isEmpty(marketName) && StringUtils.isEmpty(oldMarketName)) {
                return;
            }

            if (!StringUtils.isEmpty(oldMarketName)) {
                Example example = new Example(EConfig.class);
                example.createCriteria()
                        .andEqualTo("type", 6)
                        .andEqualTo("code", oldMarketName.toLowerCase())
                        .andEqualTo("codeName", "hotGroupLink");

                this.eConfigMapper.deleteByExample(example);
            }

            if (!StringUtils.isEmpty(marketName)) {
                String enGroupId = N3d.encode(groupId);
                String enCreatorId = N3d.encode(creatorId);
                EConfig config = new EConfig();
                config.setType(6);
                config.setCode(marketName.toLowerCase());
                config.setCodeName("hotGroupLink");
                config.setValue(String.format("/appstore?key=sealtalk://group/join?g=%s&u=%s", enGroupId, enCreatorId));
                saveOrUpdate(config);
            }

        } catch (Exception e) {
            log.error("修改热聊市场异常", e);
        }
    }
}
