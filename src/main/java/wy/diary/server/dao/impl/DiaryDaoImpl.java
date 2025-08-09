package wy.diary.server.dao.impl;

import wy.diary.server.dao.DiaryDao;
import wy.diary.server.entity.Diary;
import wy.diary.server.mapper.DiaryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * 日记DAO实现类
 */
@Repository
public class DiaryDaoImpl implements DiaryDao {
    
    @Autowired
    private DiaryMapper diaryMapper;
    
    @Override
    public int saveDiary(Diary diary) {
        return diaryMapper.insert(diary);
    }
    
    @Override
    public Diary getDiaryByDiaryId(String diaryId) {
        return diaryMapper.selectByDiaryId(diaryId);
    }
    
    @Override
    public List<Diary> getDiariesByOpenId(String openId, int pageIndex, int pageSize) {
        int offset = (pageIndex - 1) * pageSize;
        return diaryMapper.selectByOpenIdWithPage(openId, offset, pageSize);
    }
    
    @Override
    public int getDiaryCountByOpenId(String openId) {
        return diaryMapper.countByOpenId(openId);
    }
    
    @Override
    public int updateDiary(Diary diary) {
        return diaryMapper.updateById(diary);
    }
    
    @Override
    public int deleteDiary(String diaryId) {
        return diaryMapper.deleteByDiaryId(diaryId);
    }
    
    @Override
    public List<Diary> getDiariesByOpenIdAndYear(String openId, String year) {
        return diaryMapper.selectByOpenIdAndYear(openId, year);
    }

    @Override
    public List<Diary> getDiariesByOpenId(String openId) {
        return diaryMapper.selectByOpenId(openId);
    }

    @Override
    public List<String> getAllDistinctOpenIds() {
        return diaryMapper.selectAllDistinctOpenIds();
    }

    @Override
    public List<Diary> getAllDiaries() {
        return diaryMapper.selectAll();
    }
} 