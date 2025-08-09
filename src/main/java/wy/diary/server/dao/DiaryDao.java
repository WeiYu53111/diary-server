package wy.diary.server.dao;

import wy.diary.server.entity.Diary;
import java.util.List;

/**
 * 日记DAO接口
 */
public interface DiaryDao {
    
    /**
     * 保存日记
     */
    int saveDiary(Diary diary);
    
    /**
     * 根据日记ID查询日记
     */
    Diary getDiaryByDiaryId(String diaryId);
    
    /**
     * 根据用户OpenID分页查询日记列表
     */
    List<Diary> getDiariesByOpenId(String openId, int pageIndex, int pageSize);
    
    /**
     * 根据用户OpenID查询日记总数
     */
    int getDiaryCountByOpenId(String openId);
    
    /**
     * 更新日记
     */
    int updateDiary(Diary diary);
    
    /**
     * 删除日记
     */
    int deleteDiary(String diaryId);
    
    /**
     * 根据用户OpenID和年份查询日记列表
     */
    List<Diary> getDiariesByOpenIdAndYear(String openId, String year);

    /**
     * 根据用户OpenID查询所有日记（不分页）
     */
    List<Diary> getDiariesByOpenId(String openId);

    /**
     * 查询所有不重复的用户OpenID
     */
    List<String> getAllDistinctOpenIds();

    /**
     * 查询所有日记数据（用于全量备份）
     */
    List<Diary> getAllDiaries();
} 