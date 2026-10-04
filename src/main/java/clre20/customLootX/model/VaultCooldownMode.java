package clre20.customLootX.model;

/**
 * 試煉寶庫冷卻與重置模式
 */
public enum VaultCooldownMode {

    /**
     * 個人獨立冷卻：每位玩家各自計算時間，倒數結束後該玩家可再次開鎖。
     */
    PLAYER_COOLDOWN("個人獨立冷卻"),

    /**
     * 全域冷卻：一旦有人開啟，整個寶庫進入關閉冷卻狀態，倒數結束後全體重置。
     */
    GLOBAL_COOLDOWN("全域冷卻"),

    /**
     * 終生一次：每位玩家僅能開啟一次（原版寶庫特性）。
     */
    ONCE_PER_PLAYER("終生一次");

    private final String display;

    VaultCooldownMode(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public VaultCooldownMode next() {
        VaultCooldownMode[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static VaultCooldownMode fromString(String str) {
        if (str == null) return PLAYER_COOLDOWN;
        try {
            return VaultCooldownMode.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return PLAYER_COOLDOWN;
        }
    }
}
