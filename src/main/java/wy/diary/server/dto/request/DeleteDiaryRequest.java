package wy.diary.server.dto.request;

public class DeleteDiaryRequest {
    private String diaryId;
    private String createYear;

    // Getter 和 Setter

    public String getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(String diaryId) {
        this.diaryId = diaryId;
    }

    public String getCreateYear() {
        return createYear;
    }

    public void setCreateYear(String createYear) {
        this.createYear = createYear;
    }
}
