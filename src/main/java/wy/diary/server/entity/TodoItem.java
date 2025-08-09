package wy.diary.server.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待做事项实体类
 */
@Data
public class TodoItem {
    /**
     * 主键ID，自增
     */
    private Long id;
    
    /**
     * 用户OpenID
     */
    private String openId;
    
    /**
     * 事项标题
     */
    private String title;
    
    /**
     * 事项描述
     */
    private String description;
    
    /**
     * 事项类别：PROJECT-项目，HABIT-习惯养成
     */
    private Category category;
    
    /**
     * 优先级
     */
    private Priority priority;
    
    /**
     * 状态
     */
    private Status status;
    
    /**
     * 截止日期
     */
    private LocalDate dueDate;
    
    /**
     * 预计完成时长（分钟）
     */
    private Integer estimatedDuration;
    
    /**
     * 来源日记ID列表（JSON格式）
     */
    private String sourceDiaryIds;
    
    /**
     * AI分析置信度(0.00-1.00)
     */
    private BigDecimal aiConfidence;
    
    /**
     * AI分析详细结果（JSON格式）
     */
    private String aiAnalysisResult;
    
    /**
     * 创建日期
     */
    private LocalDate createdDate;
    
    /**
     * 数据库创建时间
     */
    private LocalDateTime dbCreateTime;
    
    /**
     * 数据库更新时间
     */
    private LocalDateTime dbUpdateTime;
    
    /**
     * 逻辑删除标识 0-未删除 1-已删除
     */
    private Integer deleted;
    
    /**
     * 事项类别枚举
     */
    public enum Category {
        PROJECT, HABIT
    }
    
    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
    
    /**
     * 状态枚举
     */
    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
} 