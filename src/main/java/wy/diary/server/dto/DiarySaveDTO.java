package wy.diary.server.dto;

import lombok.Data;
import java.util.Date;

@Data
public class DiarySaveDTO {
    private String openId;
    private String editorContent;
    private Date createTime;
    private String logTime;
    private String logWeek;
    private String logLunar;
    private String address;
    private String diaryId;
    private String[] imageUrls;
}
