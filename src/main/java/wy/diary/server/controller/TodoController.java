package wy.diary.server.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import wy.diary.server.dao.TodoItemDao;
import wy.diary.server.entity.TodoItem;
import wy.diary.server.model.ApiResponse;
import wy.diary.server.scheduled.TodoAnalysisTask;
import wy.diary.server.service.TodoAnalysisService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 待做事项控制器
 */
@RestController
@RequestMapping("/api/todo")
@CrossOrigin
public class TodoController {

    private static final Logger logger = LoggerFactory.getLogger(TodoController.class);

    @Autowired
    private TodoItemDao todoItemDao;

    @Autowired
    private TodoAnalysisService todoAnalysisService;

    @Autowired
    private TodoAnalysisTask todoAnalysisTask;

    /**
     * 根据用户OpenID查询待做事项列表
     */
    @GetMapping("/list")
    public Map<String, Object> getTodoList(@RequestParam String openId) {
        try {
            List<TodoItem> todoItems = todoItemDao.selectByOpenId(openId);
            return ApiResponse.success(todoItems);
        } catch (Exception e) {
            logger.error("查询待做事项列表失败", e);
            return ApiResponse.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据类别查询待做事项列表
     */
    @GetMapping("/list/category")
    public Map<String, Object> getTodoListByCategory(
            @RequestParam String openId,
            @RequestParam TodoItem.Category category) {
        try {
            List<TodoItem> todoItems = todoItemDao.selectByOpenIdAndCategory(openId, category);
            return ApiResponse.success(todoItems);
        } catch (Exception e) {
            logger.error("根据类别查询待做事项失败", e);
            return ApiResponse.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据状态查询待做事项列表
     */
    @GetMapping("/list/status")
    public Map<String, Object> getTodoListByStatus(
            @RequestParam String openId,
            @RequestParam TodoItem.Status status) {
        try {
            List<TodoItem> todoItems = todoItemDao.selectByOpenIdAndStatus(openId, status);
            return ApiResponse.success(todoItems);
        } catch (Exception e) {
            logger.error("根据状态查询待做事项失败", e);
            return ApiResponse.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据日期范围查询待做事项列表
     */
    @GetMapping("/list/dateRange")
    public Map<String, Object> getTodoListByDateRange(
            @RequestParam String openId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<TodoItem> todoItems = todoItemDao.selectByOpenIdAndDateRange(openId, startDate, endDate);
            return ApiResponse.success(todoItems);
        } catch (Exception e) {
            logger.error("根据日期范围查询待做事项失败", e);
            return ApiResponse.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询待做事项详情
     */
    @GetMapping("/{id}")
    public Map<String, Object> getTodoDetail(@PathVariable Long id) {
        try {
            TodoItem todoItem = todoItemDao.selectById(id);
            if (todoItem == null) {
                return ApiResponse.error("待做事项不存在");
            }
            return ApiResponse.success(todoItem);
        } catch (Exception e) {
            logger.error("查询待做事项详情失败", e);
            return ApiResponse.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 创建待做事项
     */
    @PostMapping("/create")
    public Map<String, Object> createTodo(@RequestBody TodoItem todoItem) {
        try {
            if (todoItem.getCreatedDate() == null) {
                todoItem.setCreatedDate(LocalDate.now());
            }
            if (todoItem.getStatus() == null) {
                todoItem.setStatus(TodoItem.Status.PENDING);
            }
            if (todoItem.getPriority() == null) {
                todoItem.setPriority(TodoItem.Priority.MEDIUM);
            }

            int result = todoItemDao.insert(todoItem);
            if (result > 0) {
                return ApiResponse.success("创建成功");
            } else {
                return ApiResponse.error("创建失败");
            }
        } catch (Exception e) {
            logger.error("创建待做事项失败", e);
            return ApiResponse.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新待做事项
     */
    @PutMapping("/update")
    public Map<String, Object> updateTodo(@RequestBody TodoItem todoItem) {
        try {
            int result = todoItemDao.update(todoItem);
            if (result > 0) {
                return ApiResponse.success("更新成功");
            } else {
                return ApiResponse.error("更新失败");
            }
        } catch (Exception e) {
            logger.error("更新待做事项失败", e);
            return ApiResponse.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 更新待做事项状态
     */
    @PutMapping("/updateStatus")
    public Map<String, Object> updateTodoStatus(
            @RequestParam Long id,
            @RequestParam TodoItem.Status status) {
        try {
            int result = todoItemDao.updateStatus(id, status);
            if (result > 0) {
                return ApiResponse.success("状态更新成功");
            } else {
                return ApiResponse.error("状态更新失败");
            }
        } catch (Exception e) {
            logger.error("更新待做事项状态失败", e);
            return ApiResponse.error("状态更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除待做事项
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteTodo(@PathVariable Long id) {
        try {
            int result = todoItemDao.deleteById(id);
            if (result > 0) {
                return ApiResponse.success("删除成功");
            } else {
                return ApiResponse.error("删除失败");
            }
        } catch (Exception e) {
            logger.error("删除待做事项失败", e);
            return ApiResponse.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 手动触发AI分析，生成待做事项
     */
    @PostMapping("/analyze")
    public Map<String, Object> analyzeDiariesAndGenerateTodos(@RequestParam String openId) {
        try {
            List<TodoItem> todoItems = todoAnalysisService.analyzeAndGenerateTodos(openId, LocalDate.now());
            return ApiResponse.success(todoItems);
        } catch (Exception e) {
            logger.error("AI分析生成待做事项失败", e);
            return ApiResponse.error("分析失败：" + e.getMessage());
        }
    }

    /**
     * 手动触发全局分析任务
     */
    @PostMapping("/analyze/all")
    public Map<String, Object> analyzeAllUsers() {
        try {
            todoAnalysisTask.manualAnalyze(null);
            return ApiResponse.success("全局分析任务已触发");
        } catch (Exception e) {
            logger.error("触发全局分析任务失败", e);
            return ApiResponse.error("触发失败：" + e.getMessage());
        }
    }
} 