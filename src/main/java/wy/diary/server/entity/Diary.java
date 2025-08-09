package wy.diary.server.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 日记实体类
 */
@Data
public class Diary {
    /**
     * 主键ID，自增
     */
    private Long id;
    
    /**
     * 日记唯一标识
     */
    private String diaryId;
    
    /**
     * 用户OpenID
     */
    private String openId;
    
    /**
     * 日记内容（富文本）
     */
    private String editorContent;
    
    /**
     * 创建时间
     */
    private String createTime;
    
    /**
     * 记录时间（日期）
     */
    private String logTime;
    
    /**
     * 记录星期
     */
    private String logWeek;
    
    /**
     * 农历日期
     */
    private String logLunar;
    
    /**
     * 地址位置
     */
    private String address;
    
    /**
     * 图片URL列表（JSON格式存储）
     */
    private String imageUrls;
    
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
} 