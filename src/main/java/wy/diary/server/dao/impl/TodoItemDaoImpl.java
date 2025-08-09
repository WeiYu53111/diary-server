package wy.diary.server.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import wy.diary.server.dao.TodoItemDao;
import wy.diary.server.entity.TodoItem;
import wy.diary.server.mapper.TodoItemMapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 待做事项DAO实现类
 */
@Repository
public class TodoItemDaoImpl implements TodoItemDao {
    
    @Autowired
    private TodoItemMapper todoItemMapper;
    
    @Override
    public int insert(TodoItem todoItem) {
        return todoItemMapper.insert(todoItem);
    }
    
    @Override
    public int batchInsert(List<TodoItem> todoItems) {
        return todoItemMapper.batchInsert(todoItems);
    }
    
    @Override
    public TodoItem selectById(Long id) {
        return todoItemMapper.selectById(id);
    }
    
    @Override
    public List<TodoItem> selectByOpenId(String openId) {
        return todoItemMapper.selectByOpenId(openId);
    }
    
    @Override
    public List<TodoItem> selectByOpenIdAndCategory(String openId, TodoItem.Category category) {
        return todoItemMapper.selectByOpenIdAndCategory(openId, category);
    }
    
    @Override
    public List<TodoItem> selectByOpenIdAndStatus(String openId, TodoItem.Status status) {
        return todoItemMapper.selectByOpenIdAndStatus(openId, status);
    }
    
    @Override
    public List<TodoItem> selectByOpenIdAndCreatedDate(String openId, LocalDate createdDate) {
        return todoItemMapper.selectByOpenIdAndCreatedDate(openId, createdDate);
    }
    
    @Override
    public List<TodoItem> selectByOpenIdAndDateRange(String openId, LocalDate startDate, LocalDate endDate) {
        return todoItemMapper.selectByOpenIdAndDateRange(openId, startDate, endDate);
    }
    
    @Override
    public int update(TodoItem todoItem) {
        return todoItemMapper.update(todoItem);
    }
    
    @Override
    public int updateStatus(Long id, TodoItem.Status status) {
        return todoItemMapper.updateStatus(id, status);
    }
    
    @Override
    public int deleteById(Long id) {
        return todoItemMapper.deleteById(id);
    }
    
    @Override
    public int deleteByOpenId(String openId) {
        return todoItemMapper.deleteByOpenId(openId);
    }
} 