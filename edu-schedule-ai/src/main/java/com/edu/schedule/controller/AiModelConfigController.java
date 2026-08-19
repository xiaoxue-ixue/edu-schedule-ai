package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.ai.SpringAiModelFactory;
import com.edu.schedule.entity.AiModelConfig;
import com.edu.schedule.mapper.AiModelConfigMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai-model-config")
public class AiModelConfigController {

    @Autowired
    private AiModelConfigMapper mapper;

    @Autowired
    private SpringAiModelFactory factory;

    @GetMapping
    public Result<PageResult<AiModelConfig>> page(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<AiModelConfig> info = new PageInfo<>(mapper.selectList(keyword));
        // 脱敏 apiKey
        info.getList().forEach(this::maskKey);
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<AiModelConfig> get(@PathVariable Long id) {
        AiModelConfig c = mapper.selectById(id);
        maskKey(c);
        return Result.success(c);
    }

    @PostMapping
    public Result<Void> create(@RequestBody AiModelConfig config) {
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        if (config.getIsDefault() == null) {
            config.setIsDefault(0);
        }
        mapper.insert(config);
        factory.evict(config.getModelCode());
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody AiModelConfig config) {
        config.setId(id);
        // apiKey 为空表示不修改
        mapper.update(config);
        factory.evict(null);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AiModelConfig c = mapper.selectById(id);
        mapper.deleteById(id);
        if (c != null) {
            factory.evict(c.getModelCode());
        }
        return Result.success();
    }

    @PostMapping("/{id}/test")
    public Result<String> test(@PathVariable Long id) {
        return Result.success(factory.testConnection(id));
    }

    private void maskKey(AiModelConfig c) {
        if (c != null && c.getApiKey() != null && c.getApiKey().length() > 8) {
            c.setApiKey(c.getApiKey().substring(0, 4) + "****" + c.getApiKey().substring(c.getApiKey().length() - 4));
        }
    }
}
