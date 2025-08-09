package wy.diary.server.dao;

import wy.diary.server.entity.TodoItem;
import java.time.LocalDate;
import java.util.List;

/**
 * 待做事项DAO接口
 */
public interface TodoItemDao {
    
    /**
     * 插入待做事项
     * @param todoItem 待做事项
     * @return 影响行数
     */
    int insert(TodoItem todoItem);
    
    /**
     * 批量插入待做事项
     * @param todoItems 待做事项列表
     * @return 影响行数
     */
    int batchInsert(List<TodoItem> todoItems);
    
    /**
     * 根据ID查询待做事项
     * @param id 主键ID
     * @return 待做事项
     */
    TodoItem selectById(Long id);
    
    /**
     * 根据用户OpenID查询待做事项列表
     * @param openId 用户OpenID
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenId(String openId);
    
    /**
     * 根据用户OpenID和类别查询待做事项列表
     * @param openId 用户OpenID
     * @param category 事项类别
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndCategory(String openId, TodoItem.Category category);
    
    /**
     * 根据用户OpenID和状态查询待做事项列表
     * @param openId 用户OpenID
     * @param status 状态
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndStatus(String openId, TodoItem.Status status);
    
    /**
     * 根据用户OpenID和创建日期查询待做事项列表
     * @param openId 用户OpenID
     * @param createdDate 创建日期
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndCreatedDate(String openId, LocalDate createdDate);
    
    /**
     * 根据用户OpenID查询指定日期范围内的待做事项
     * @param openId 用户OpenID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndDateRange(String openId, LocalDate startDate, LocalDate endDate);
    
    /**
     * 更新待做事项
     * @param todoItem 待做事项
     * @return 影响行数
     */
    int update(TodoItem todoItem);
    
    /**
     * 更新待做事项状态
     * @param id 主键ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(Long id, TodoItem.Status status);
    
    /**
     * 逻辑删除待做事项
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Long id);
    
    /**
     * 根据用户OpenID逻辑删除所有待做事项
     * @param openId 用户OpenID
     * @return 影响行数
     */
    int deleteByOpenId(String openId);
} 