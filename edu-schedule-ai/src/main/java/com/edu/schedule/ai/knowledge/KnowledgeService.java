package com.edu.schedule.ai.knowledge;


import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.PageResult;
import com.edu.schedule.entity.Knowledge;
import com.edu.schedule.mapper.KnowledgeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
/**
 * 教务知识库服务：自动切片 + 关键词检索召回。
 *
 * 切片策略：按句号/分号/感叹号切分，过滤空片。
 * 检索策略：对每篇知识库的 sliceContent 按关键词命中次数打分，召回 top N。
 */
@Service
public class KnowledgeService {

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    public PageResult<Knowledge> page(Integer page, Integer size, String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Knowledge> info = new PageInfo<>(knowledgeMapper.selectList(keyword));
        return PageResult.of(page, size, info.getTotal(), info.getList());
    }

    public Knowledge getById(Long id) {
        return knowledgeMapper.selectById(id);
    }

    public void create(Knowledge knowledge) {
        slice(knowledge);
        if (knowledge.getStatus() == null) {
            knowledge.setStatus(1);
        }
        knowledgeMapper.insert(knowledge);
    }

    public void update(Knowledge knowledge) {
        if (knowledge.getId() == null) {
            throw new BusinessException("id不能为空");
        }
        // 内容变更时重新切片
        if (knowledge.getContent() != null) {
            slice(knowledge);
        }
        knowledgeMapper.update(knowledge);
    }

    public void delete(Long id) {
        knowledgeMapper.deleteById(id);
    }

    /**
     * 自动切片：写入 sliceContent 与 sliceCount
     */
    private void slice(Knowledge knowledge) {
        String content = knowledge.getContent();
        if (content == null || content.isBlank()) {
            knowledge.setSliceContent("");
            knowledge.setSliceCount(0);
            return;
        }
        String[] parts = content.split("[。；;！!\\n。]+");
        List<String> slices = Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        knowledge.setSliceContent(String.join(";", slices));
        knowledge.setSliceCount(slices.size());
    }

    /**
     * 关键词检索：按命中次数打分召回最相关的切片
     *
     * @param keyword 关键词
     * @param topN    召回数量
     */
    public List<Map<String, Object>> search(String keyword, int topN) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        List<String> keywords = Arrays.stream(keyword.split("[\\s,，]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<Knowledge> all = knowledgeMapper.selectAllEnabled();
        List<Map<String, Object>> scored = new ArrayList<>();
        for (Knowledge k : all) {
            if (k.getSliceContent() == null) {
                continue;
            }
            String[] slices = k.getSliceContent().split(";");
            for (String slice : slices) {
                int score = 0;
                for (String kw : keywords) {
                    if (slice.contains(kw)) {
                        score += 2;
                    }
                }
                if (k.getTitle() != null && k.getTitle().contains(keyword)) {
                    score += 1;
                }
                if (score > 0) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("title", k.getTitle());
                    item.put("category", k.getCategory());
                    item.put("slice", slice);
                    item.put("score", score);
                    scored.add(item);
                }
            }
        }
        scored.sort((a, b) -> Integer.compare((Integer) b.get("score"), (Integer) a.get("score")));
        if (scored.size() > topN) {
            return scored.subList(0, topN);
        }
        return scored;
    }
}
