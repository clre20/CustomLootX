package clre20.customLootX.model;

public class DraftSession {
    private final String draftId;
    private final DraftType type;
    private final String originalName;
    private final long createdTime;
    private final long expireTime;
    private final Object templateData;

    public DraftSession(String draftId, DraftType type, String originalName, long createdTime, long expireTime, Object templateData) {
        this.draftId = draftId;
        this.type = type;
        this.originalName = originalName;
        this.createdTime = createdTime;
        this.expireTime = expireTime;
        this.templateData = templateData;
    }

    public String getDraftId() {
        return draftId;
    }

    public DraftType getType() {
        return type;
    }

    public String getOriginalName() {
        return originalName;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getExpireTime() {
        return expireTime;
    }

    public Object getTemplateData() {
        return templateData;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
}
