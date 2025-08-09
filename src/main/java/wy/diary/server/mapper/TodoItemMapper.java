package wy.diary.server.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import wy.diary.server.entity.TodoItem;

import java.time.LocalDate;
import java.util.List;

/**
 * 待做事项Mapper接口
 */
@Mapper
public interface TodoItemMapper {
    
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
    int batchInsert(@Param("todoItems") List<TodoItem> todoItems);
    
    /**
     * 根据ID查询待做事项
     * @param id 主键ID
     * @return 待做事项
     */
    TodoItem selectById(@Param("id") Long id);
    
    /**
     * 根据用户OpenID查询待做事项列表
     * @param openId 用户OpenID
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenId(@Param("openId") String openId);
    
    /**
     * 根据用户OpenID和类别查询待做事项列表
     * @param openId 用户OpenID
     * @param category 事项类别
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndCategory(@Param("openId") String openId, @Param("category") TodoItem.Category category);
    
    /**
     * 根据用户OpenID和状态查询待做事项列表
     * @param openId 用户OpenID
     * @param status 状态
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndStatus(@Param("openId") String openId, @Param("status") TodoItem.Status status);
    
    /**
     * 根据用户OpenID和创建日期查询待做事项列表
     * @param openId 用户OpenID
     * @param createdDate 创建日期
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndCreatedDate(@Param("openId") String openId, @Param("createdDate") LocalDate createdDate);
    
    /**
     * 根据用户OpenID查询指定日期范围内的待做事项
     * @param openId 用户OpenID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 待做事项列表
     */
    List<TodoItem> selectByOpenIdAndDateRange(@Param("openId") String openId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
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
    int updateStatus(@Param("id") Long id, @Param("status") TodoItem.Status status);
    
    /**
     * 逻辑删除待做事项
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据用户OpenID逻辑删除所有待做事项
     * @param openId 用户OpenID
     * @return 影响行数
     */
    int deleteByOpenId(@Param("openId") String openId);
} 