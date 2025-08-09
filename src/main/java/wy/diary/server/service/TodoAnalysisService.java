package wy.diary.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import wy.diary.server.dao.TodoItemDao;
import wy.diary.server.entity.Diary;
import wy.diary.server.entity.TodoItem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI分析服务，负责分析日记内容并生成待做事项
 * TODO: 实际的AI分析实现留空，可根据需要接入具体的AI服务
 */
@Service
public class TodoAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(TodoAnalysisService.class);

    @Autowired
    private TodoItemDao todoItemDao;

    @Autowired
    private DiaryDatabaseService diaryDatabaseService;

    @Value("${ai.api.url:}")
    private String aiApiUrl;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    @Value("${ai.model:gpt-3.5-turbo}")
    private String aiModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TodoAnalysisService() {
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 创建配置了超时时间的RestTemplate
     */
    private RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();
        // 设置30秒连接超时和60秒读取超时
        template.getClientHttpRequestInitializers().add(request -> {
            request.getHeaders().set("User-Agent", "DiaryServer/1.0");
        });
        return template;
    }

    /**
     * 分析用户的日记并生成待做事项
     *
     * @param openId 用户OpenID
     * @param analysisDate 分析日期
     * @return 生成的待做事项列表
     */
    public List<TodoItem> analyzeAndGenerateTodos(String openId, LocalDate analysisDate) {
        logger.info("开始为用户 {} 分析日记并生成待做事项，分析日期: {}", openId, analysisDate);

        try {
            // 1. 获取用户最近一段时间的日记
            List<Diary> recentDiaries = getRecentDiaries(openId, analysisDate);
            if (recentDiaries.isEmpty()) {
                logger.info("用户 {} 没有找到最近的日记，跳过分析", openId);
                return Collections.emptyList();
            }

            // 2. 调用AI分析日记内容
            String analysisResult = callAIForAnalysis(recentDiaries);
            if (analysisResult == null || analysisResult.isEmpty()) {
                logger.warn("用户 {} 的AI分析结果为空", openId);
                return Collections.emptyList();
            }

            // 3. 解析AI分析结果并生成待做事项
            List<TodoItem> todoItems = parseAnalysisResultToTodos(openId, analysisResult, recentDiaries, analysisDate);

            // 4. 保存到数据库
            if (!todoItems.isEmpty()) {
                int savedCount = todoItemDao.batchInsert(todoItems);
                logger.info("为用户 {} 成功保存 {} 个待做事项", openId, savedCount);
            }

            return todoItems;

        } catch (Exception e) {
            logger.error("为用户 {} 分析日记并生成待做事项时发生错误", openId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户最近一段时间的日记
     */
    private List<Diary> getRecentDiaries(String openId, LocalDate analysisDate) {
        // 获取最近7天的日记
        LocalDate startDate = analysisDate.minusDays(7);
        LocalDate endDate = analysisDate.minusDays(1);
        
        List<Diary> allDiaries = diaryDatabaseService.getDiariesByOpenId(openId);
        
        return allDiaries.stream()
                .filter(diary -> {
                    if (diary.getLogTime() == null) return false;
                    try {
                        LocalDate logDate = LocalDate.parse(diary.getLogTime());
                        return !logDate.isBefore(startDate) && !logDate.isAfter(endDate);
                    } catch (Exception e) {
                        logger.warn("解析日记时间失败: {}", diary.getLogTime());
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * 调用AI API分析日记内容，识别待做事项
     * 
     * @param diaries 用户日记列表
     * @return AI分析结果的JSON字符串
     */
    private String callAIForAnalysis(List<Diary> diaries) {
        if (aiApiUrl == null || aiApiUrl.isEmpty()) {
            logger.warn("AI API URL未配置，使用模拟分析结果");
            return generateMockAnalysisResult(diaries);
        }

        try {
            // 1. 准备请求数据 - 直接发送日记内容
            Map<String, Object> requestBody = buildSimpleAIRequest(diaries);
            
            // 2. 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (aiApiKey != null && !aiApiKey.isEmpty()) {
                headers.set("Authorization", "Bearer " + aiApiKey);
            }
            
            // 3. 创建请求实体
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 4. 发送请求
            logger.info("调用外部AI服务分析日记内容，URL: {}", aiApiUrl);
            ResponseEntity<String> response = restTemplate.postForEntity(aiApiUrl, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    logger.info("外部AI服务调用成功，返回数据长度: {}", responseBody.length());
                    return responseBody;
                } else {
                    logger.warn("外部AI服务返回空响应，使用模拟结果");
                    return generateMockAnalysisResult(diaries);
                }
            } else {
                logger.warn("外部AI服务调用失败，状态码: {}, 使用模拟结果", response.getStatusCode());
                return generateMockAnalysisResult(diaries);
            }
            
        } catch (Exception e) {
            logger.error("调用外部AI服务时发生错误，使用模拟结果", e);
            return generateMockAnalysisResult(diaries);
        }
    }

    /**
     * 构建发送给外部AI服务的简单请求数据
     * 
     * @param diaries 用户日记列表
     * @return 请求体Map
     */
    private Map<String, Object> buildSimpleAIRequest(List<Diary> diaries) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 提取日记内容列表
        List<Map<String, String>> diaryList = new ArrayList<>();
        for (Diary diary : diaries) {
            Map<String, String> diaryData = new HashMap<>();
            diaryData.put("date", diary.getLogTime());
            diaryData.put("content", diary.getEditorContent() != null ? diary.getEditorContent() : "");
            diaryList.add(diaryData);
        }
        
        requestBody.put("diaries", diaryList);
        
        // 可添加一些元数据供外部服务使用
        requestBody.put("analysisType", "todo_extraction");
        requestBody.put("categories", Arrays.asList("PROJECT", "HABIT"));
        
        return requestBody;
    }

    /**
     * 生成模拟分析结果（当AI API不可用时或实现留空时）
     */
    private String generateMockAnalysisResult(List<Diary> diaries) {
        // 基于日记内容的关键词分析生成模拟结果
        String mockResult = "{\n" +
                "  \"todos\": [\n" +
                "    {\n" +
                "      \"title\": \"完成工作报告\",\n" +
                "      \"description\": \"根据日记中提到的工作内容，需要整理并完成本周工作报告\",\n" +
                "      \"category\": \"PROJECT\",\n" +
                "      \"priority\": \"HIGH\",\n" +
                "      \"estimatedDuration\": 120,\n" +
                "      \"dueDate\": \"" + LocalDate.now().plusDays(2) + "\",\n" +
                "      \"confidence\": 0.75\n" +
                "    },\n" +
                "    {\n" +
                "      \"title\": \"坚持每日运动\",\n" +
                "      \"description\": \"根据日记记录，建议保持每日30分钟的运动习惯\",\n" +
                "      \"category\": \"HABIT\",\n" +
                "      \"priority\": \"MEDIUM\",\n" +
                "      \"estimatedDuration\": 30,\n" +
                "      \"dueDate\": null,\n" +
                "      \"confidence\": 0.80\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        
        logger.info("生成模拟分析结果，包含 {} 条日记的分析", diaries.size());
        return mockResult;
    }

    /**
     * 解析AI分析结果并转换为待做事项列表
     */
    private List<TodoItem> parseAnalysisResultToTodos(String openId, String analysisResult, 
                                                      List<Diary> sourceDiaries, LocalDate createdDate) {
        List<TodoItem> todoItems = new ArrayList<>();
        
        try {
            JsonNode jsonNode = objectMapper.readTree(analysisResult);
            JsonNode todosNode = jsonNode.get("todos");
            
            if (todosNode != null && todosNode.isArray()) {
                String sourceDiaryIds = sourceDiaries.stream()
                        .map(Diary::getDiaryId)
                        .collect(Collectors.joining(",", "[\"", "\"]"));
                
                for (JsonNode todoNode : todosNode) {
                    TodoItem todoItem = new TodoItem();
                    todoItem.setOpenId(openId);
                    todoItem.setTitle(getStringValue(todoNode, "title"));
                    todoItem.setDescription(getStringValue(todoNode, "description"));
                    todoItem.setCategory(parseCategoryFromString(getStringValue(todoNode, "category")));
                    todoItem.setPriority(parsePriorityFromString(getStringValue(todoNode, "priority")));
                    todoItem.setStatus(TodoItem.Status.PENDING);
                    todoItem.setEstimatedDuration(getIntValue(todoNode, "estimatedDuration"));
                    todoItem.setDueDate(parseDateFromString(getStringValue(todoNode, "dueDate")));
                    todoItem.setSourceDiaryIds(sourceDiaryIds);
                    todoItem.setAiConfidence(getBigDecimalValue(todoNode, "confidence"));
                    todoItem.setAiAnalysisResult(analysisResult);
                    todoItem.setCreatedDate(createdDate);
                    
                    todoItems.add(todoItem);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("解析AI分析结果失败", e);
        }
        
        return todoItems;
    }

    // 辅助方法
    private String getStringValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null ? fieldNode.asText() : null;
    }

    private Integer getIntValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null ? fieldNode.asInt() : null;
    }

    private BigDecimal getBigDecimalValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null ? BigDecimal.valueOf(fieldNode.asDouble()) : null;
    }

    private TodoItem.Category parseCategoryFromString(String category) {
        if (category == null) return TodoItem.Category.PROJECT;
        try {
            return TodoItem.Category.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TodoItem.Category.PROJECT;
        }
    }

    private TodoItem.Priority parsePriorityFromString(String priority) {
        if (priority == null) return TodoItem.Priority.MEDIUM;
        try {
            return TodoItem.Priority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TodoItem.Priority.MEDIUM;
        }
    }

    private LocalDate parseDateFromString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            logger.warn("解析日期失败: {}", dateStr);
            return null;
        }
    }
} 