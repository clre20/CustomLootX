package clre20.customLootX.model;

public enum DraftType {
    SUSPICIOUS("suspicious", "可疑方塊"),
    VAULT("vault", "試煉寶庫"),
    SPAWNER("spawner", "試煉生怪磚");

    private final String folderName;
    private final String displayName;

    DraftType(String folderName, String displayName) {
        this.folderName = folderName;
        this.displayName = displayName;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DraftType fromString(String name) {
        if (name == null) return null;
        for (DraftType t : values()) {
            if (t.name().equalsIgnoreCase(name) || t.folderName.equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }
}
