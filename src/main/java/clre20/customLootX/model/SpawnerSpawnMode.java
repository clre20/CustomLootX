package clre20.customLootX.model;

/**
 * 試煉生怪磚怪物生成模式
 */
public enum SpawnerSpawnMode {

    /**
     * 隨機模式：依怪物池各怪物的機率（需達 100%）隨機抽取生成
     */
    RANDOM("隨機模式", "&a隨機抽取"),

    /**
     * 自訂順序模式：依據一輪幾隻與波次順序，依序生成指定的怪物（可包含隨機格子）
     */
    SEQUENCE("自訂順序模式", "&b自訂順序");

    private final String label;
    private final String display;

    SpawnerSpawnMode(String label, String display) {
        this.label = label;
        this.display = display;
    }

    public String getLabel() {
        return label;
    }

    public String getDisplay() {
        return display;
    }

    public static SpawnerSpawnMode fromString(String str) {
        if (str == null) return RANDOM;
        try {
            return SpawnerSpawnMode.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RANDOM;
        }
    }
}
