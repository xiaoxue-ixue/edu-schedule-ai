package com.edu.schedule.ai;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.entity.*;
import com.edu.schedule.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiQualityAnalysisService {

    @Autowired private AiQualityAnalysisMapper analysisMapper;
    @Autowired private TeachingEvaluationMapper evaluationMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private TeacherMapper teacherMapper;
    @Autowired private ClazzMapper clazzMapper;
    @Autowired private SpringAiModelFactory aiFactory;

    public AiQualityAnalysis generate(Long termId, String dimension, Long targetId) {
        List<TeachingEvaluation> evals = evaluationMapper.selectByTerm(termId);
        if (evals == null || evals.isEmpty()) {
            throw new BusinessException("该学期暂无教学评价数据，无法生成质量分析");
        }

        List<AiQualityAnalysis> results = new ArrayList<>();

        switch (dimension.toUpperCase()) {
            case "COURSE":
                // 按课程分组
                Map<Long, List<TeachingEvaluation>> byCourse = evals.stream()
                        .filter(e -> targetId == null || targetId.equals(e.getCourseId()))
                        .collect(Collectors.groupingBy(TeachingEvaluation::getCourseId));
                for (Map.Entry<Long, List<TeachingEvaluation>> entry : byCourse.entrySet()) {
                    Long cid = entry.getKey();
                    String cname = courseMapper.selectById(cid) != null ? courseMapper.selectById(cid).getCourseName() : "课程" + cid;
                    results.add(buildAnalysis(termId, "COURSE", cid, cname, entry.getValue()));
                }
                break;
            case "TEACHER":
                Map<Long, List<TeachingEvaluation>> byTeacher = evals.stream()
                        .filter(e -> targetId == null || targetId.equals(e.getTeacherId()))
                        .collect(Collectors.groupingBy(TeachingEvaluation::getTeacherId));
                for (Map.Entry<Long, List<TeachingEvaluation>> entry : byTeacher.entrySet()) {
                    Long tid = entry.getKey();
                    String tname = teacherMapper.selectById(tid) != null ? teacherMapper.selectById(tid).getName() : "教师" + tid;
                    results.add(buildAnalysis(termId, "TEACHER", tid, tname, entry.getValue()));
                }
                break;
            case "CLASS":
                Map<Long, List<TeachingEvaluation>> byClass = evals.stream()
                        .filter(e -> targetId == null || targetId.equals(e.getClassId()))
                        .collect(Collectors.groupingBy(TeachingEvaluation::getClassId));
                for (Map.Entry<Long, List<TeachingEvaluation>> entry : byClass.entrySet()) {
                    Long cid = entry.getKey();
                    String cname = clazzMapper.selectById(cid) != null ? clazzMapper.selectById(cid).getClassName() : "班级" + cid;
                    results.add(buildAnalysis(termId, "CLASS", cid, cname, entry.getValue()));
                }
                break;
            default:
                throw new BusinessException("不支持的分析维度: " + dimension);
        }

        // 逐条生成 + 入库（第一条作为返回值）
        AiQualityAnalysis first = null;
        for (int i = 0; i < results.size(); i++) {
            AiQualityAnalysis a = results.get(i);
            // 尝试 AI 增强，如果失败用本地规则
            try {
                enhanceWithAi(a);
            } catch (Exception ignore) {
                // AI 失败不阻塞，用已有 summary 即可
            }
            analysisMapper.insert(a);
            if (i == 0) first = a;
        }

        if (first == null) throw new BusinessException("未生成任何分析报告");
        return first;
    }

    private AiQualityAnalysis buildAnalysis(Long termId, String dimension, Long targetId, String targetName, List<TeachingEvaluation> evals) {
        AiQualityAnalysis a = new AiQualityAnalysis();
        a.setTermId(termId);
        a.setDimension(dimension);
        a.setTargetId(targetId);
        a.setTargetName(targetName);
        a.setEvalCount(evals.size());

        BigDecimal avg = avgOverall(evals);
        a.setAvgScore(avg);
        a.setQualityLevel(levelOf(avg));

        // 本地规则 summary（AI 可能覆盖）
        String summary = String.format("%s「%s」共收到 %d 条评价，综合得分 %.1f 分，质量等级：%s。",
                dimText(dimension), targetName, evals.size(), avg, a.getQualityLevel());
        a.setSummary(summary);

        // 优势：看哪些维度分最高
        Map<String, BigDecimal> dimAvgs = dimAverages(evals);
        List<Map.Entry<String, BigDecimal>> topDims = dimAvgs.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(2)
                .collect(Collectors.toList());
        StringBuilder strengths = new StringBuilder("得分较高维度：");
        for (Map.Entry<String, BigDecimal> e : topDims) {
            strengths.append(dimName(e.getKey())).append("(").append(e.getValue().setScale(1, RoundingMode.HALF_UP)).append("分) ");
        }
        a.setStrengths(strengths.toString().trim());

        // 问题：最低维度
        List<Map.Entry<String, BigDecimal>> lowDims = dimAvgs.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue())
                .limit(2)
                .collect(Collectors.toList());
        StringBuilder problems = new StringBuilder("待改进维度：");
        for (Map.Entry<String, BigDecimal> e : lowDims) {
            problems.append(dimName(e.getKey())).append("(").append(e.getValue().setScale(1, RoundingMode.HALF_UP)).append("分) ");
        }
        a.setProblems(problems.toString().trim());

        // 建议
        a.setSuggestions(generateSuggestion(a.getQualityLevel(), lowDims));

        // 完整报告
        StringBuilder report = new StringBuilder();
        report.append("📊 教学质量分析报告\n");
        report.append("━━━━━━━━━━━━━━━━━━━━━━\n");
        report.append(String.format("分析维度: %s\n", dimText(dimension)));
        report.append(String.format("分析对象: %s\n", targetName));
        report.append(String.format("样本数量: %d 条\n", evals.size()));
        report.append(String.format("综合得分: %.1f 分\n", avg));
        report.append(String.format("质量等级: %s\n", a.getQualityLevel()));
        report.append("\n📝 摘要:\n").append(summary).append("\n");
        report.append("\n✅ 优势:\n").append(a.getStrengths()).append("\n");
        report.append("\n⚠️  问题:\n").append(a.getProblems()).append("\n");
        report.append("\n💡 建议:\n").append(a.getSuggestions()).append("\n");
        a.setReport(report.toString());

        return a;
    }

    private void enhanceWithAi(AiQualityAnalysis a) {
        String prompt = "请根据以下教学质量数据，生成一段50字以内的专业摘要和3条改进建议。\n" +
                "维度:" + dimText(a.getDimension()) + "\n" +
                "对象:" + a.getTargetName() + "\n" +
                "平均分:" + a.getAvgScore() + "\n" +
                "等级:" + a.getQualityLevel() + "\n" +
                "优势:" + a.getStrengths() + "\n" +
                "问题:" + a.getProblems() + "\n" +
                "请直接输出 JSON: {\"summary\":\"...\",\"suggestions\":\"...\",\"report\":\"...\"}";
        try {
            String aiResult = aiFactory.getDefaultChatClient()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            // 简单解析（如果是 JSON 就用，否则用原文）
            if (aiResult != null && aiResult.contains("{")) {
                // 尝试提取 JSON
                int start = aiResult.indexOf('{');
                int end = aiResult.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    String json = aiResult.substring(start, end + 1);
                    com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, String> map = om.readValue(json, Map.class);
                        if (map.containsKey("summary")) a.setSummary(map.get("summary"));
                        if (map.containsKey("suggestions")) a.setSuggestions(map.get("suggestions"));
                        if (map.containsKey("report")) a.setReport(map.get("report"));
                        else if (map.containsKey("summary")) {
                            a.setReport(a.getSummary());
                        }
                    } catch (Exception e) {
                        // JSON 解析失败，用原文
                        if (aiResult.length() > 50) {
                            a.setReport(aiResult);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // AI 失败静默
        }
    }

    private BigDecimal avgOverall(List<TeachingEvaluation> evals) {
        return evals.stream()
                .map(TeachingEvaluation::getOverallScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(evals.size()), 1, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> dimAverages(List<TeachingEvaluation> evals) {
        Map<String, List<BigDecimal>> dims = new HashMap<>();
        dims.put("attitude", new ArrayList<>());
        dims.put("content", new ArrayList<>());
        dims.put("method", new ArrayList<>());
        dims.put("effect", new ArrayList<>());
        for (TeachingEvaluation e : evals) {
            if (e.getDimAttitude() != null) dims.get("attitude").add(e.getDimAttitude());
            if (e.getDimContent() != null) dims.get("content").add(e.getDimContent());
            if (e.getDimMethod() != null) dims.get("method").add(e.getDimMethod());
            if (e.getDimEffect() != null) dims.get("effect").add(e.getDimEffect());
        }
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<BigDecimal>> e : dims.entrySet()) {
            List<BigDecimal> list = e.getValue();
            if (!list.isEmpty()) {
                BigDecimal sum = list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                result.put(e.getKey(), sum.divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP));
            } else {
                result.put(e.getKey(), BigDecimal.ZERO);
            }
        }
        return result;
    }

    private String dimName(String key) {
        return switch (key) {
            case "attitude" -> "教学态度";
            case "content" -> "教学内容";
            case "method" -> "教学方法";
            case "effect" -> "教学效果";
            default -> key;
        };
    }

    private String dimText(String d) {
        return switch (d.toUpperCase()) {
            case "COURSE" -> "按课程";
            case "TEACHER" -> "按教师";
            case "CLASS" -> "按班级";
            default -> d;
        };
    }

    private String levelOf(BigDecimal avg) {
        if (avg.compareTo(new BigDecimal("90")) >= 0) return "优秀";
        if (avg.compareTo(new BigDecimal("80")) >= 0) return "良好";
        if (avg.compareTo(new BigDecimal("60")) >= 0) return "合格";
        return "不合格";
    }

    private String generateSuggestion(String level, List<Map.Entry<String, BigDecimal>> lowDims) {
        StringBuilder sb = new StringBuilder();
        if ("优秀".equals(level)) {
            sb.append("继续保持当前教学水平，可作为教学范例推广。");
        } else if ("良好".equals(level)) {
            sb.append("整体表现良好，建议重点提升").append(dimName(lowDims.get(0).getKey())).append("环节。");
        } else if ("合格".equals(level)) {
            sb.append("需加强教学改进，重点优化").append(dimName(lowDims.get(0).getKey())).append("和").append(lowDims.size() > 1 ? dimName(lowDims.get(1).getKey()) : dimName(lowDims.get(0).getKey())).append("。");
        } else {
            sb.append("建议立即进行教学整改，重点解决").append(dimName(lowDims.get(0).getKey())).append("问题。");
        }
        return sb.toString();
    }

    public List<AiQualityAnalysis> list(Long termId, String dimension, String qualityLevel) {
        return analysisMapper.selectByCondition(termId, dimension, qualityLevel);
    }

    public AiQualityAnalysis getById(Long id) {
        AiQualityAnalysis a = analysisMapper.selectById(id);
        if (a == null) throw new BusinessException("分析报告不存在");
        return a;
    }
}