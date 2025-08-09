package wy.diary.server.mapper;

import wy.diary.server.entity.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 日记Mapper接口
 */
@Mapper
public interface DiaryMapper {
    
    /**
     * 插入日记
     */
    int insert(Diary diary);
    
    /**
     * 根据ID查询日记
     */
    Diary selectById(@Param("id") Long id);
    
    /**
     * 根据日记ID查询日记
     */
    Diary selectByDiaryId(@Param("diaryId") String diaryId);
    
    /**
     * 根据用户OpenID分页查询日记列表
     */
    List<Diary> selectByOpenIdWithPage(@Param("openId") String openId, 
                                       @Param("offset") int offset, 
                                       @Param("pageSize") int pageSize);
    
    /**
     * 根据用户OpenID查询日记总数
     */
    int countByOpenId(@Param("openId") String openId);
    
    /**
     * 更新日记
     */
    int updateById(Diary diary);
    
    /**
     * 逻辑删除日记
     */
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据日记ID逻辑删除日记
     */
    int deleteByDiaryId(@Param("diaryId") String diaryId);
    
    /**
     * 根据用户OpenID和年份查询日记列表
     */
    List<Diary> selectByOpenIdAndYear(@Param("openId") String openId, 
                                      @Param("year") String year);

    /**
     * 根据用户OpenID查询所有日记（不分页）
     */
    List<Diary> selectByOpenId(@Param("openId") String openId);

    /**
     * 查询所有不重复的用户OpenID
     */
    List<String> selectAllDistinctOpenIds();

    /**
     * 查询所有日记数据（用于全量备份）
     */
    List<Diary> selectAll();
} 