# CustomLootX

![Release](https://img.shields.io/badge/Release-v2.4.9-orange.svg)
![Version](https://img.shields.io/badge/Minecraft-1.21.x%20%7C%2026.x-brightgreen.svg)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-blue.svg)

**CustomLootX** 是一套專為 **Minecraft 1.21.x ~ 26.x** 伺服器設計的次世代進階戰利品與試煉機制管理插件。

整合了 1.21 & 26.x 核心特色的 **自訂可疑方塊（可疑沙/可疑礫石）**、**自訂試煉寶庫** 以及 **自訂試煉生怪磚**，提供全程可視化的 **設定精靈 GUI**，無需繁瑣編輯 YML，即可輕鬆打造副本、解謎、探索與戰鬥獎勵機制！

---

### 1. 自訂可疑方塊
* **方塊種類支援**：支援 1.20+ 特色的 **可疑沙子** 與 **可疑礫石**。
* **動態刷寶重置**：支援獨立設定每個可疑方塊的自動重置時間，刷出戰利品後自動在倒數完畢時重置，讓玩家可重複探索。
* **視覺化獎池機率**：54 格箱子介面直觀擺放獎勵，支援點擊加減與聊天室精準小數點輸入（精度達 `0.01%`），可自訂「無掉落」機率，並有一鍵自動補齊與平均分配功能。

### 2. 自訂試煉寶庫
* **自訂解鎖鑰匙**：支援任意物品作為開啟鑰匙，支援比對物品材質、自訂顯示名稱、完整 Lore 行與 CustomModelData / NBT，亦可透過專屬指令隨時發放防偽鑰匙。
* **三大多元冷卻模式**：
  * **個人獨立冷卻**：每位玩家各自獨立計算解鎖冷卻時間，適合多人副本各自領獎。
  * **全域伺服器冷卻**：一人解鎖後全服進入冷卻，適合野外首領寶箱爭奪。
  * **終生一次模式**：每位玩家僅限成功開啟領取一次，適合一次性通關獎勵。
* **動態展示與彈出戰利品**：
  * 支援設定多個戰利品抽取次數（1 ~ 64 件）。
  * 支援試煉寶庫經典的內部旋轉物品預覽展示與解鎖時的物品彈出特效。

### 3. 自訂試煉生怪磚
* **戰鬥與波次機制設定**：支援自訂波次生成間隔、感應範圍、Action Bar 即時戰況推播，以及**「等待全滅才下一輪」開關**。
* **動態波次與順序編排看板**：
  * 徹底擺脫固定數量限制，支援**每一輪自由設計不同出怪量**。
  * 每輪末端直覺提供 `[增加格子]` 按鈕，點擊即可擴充該輪隻數；看板末端提供 `[新增波次]` 按鈕隨時增加輪次。
  * 支援指定特定生物或「隨機抽取」，未指定處以紅色玻璃片佔位；**若尚有紅色未指定格子，下一步將自動鎖定防呆**，確保配置完整。
  * 智慧分流：全指定固定怪物免設機率；使用隨機格子自動均分並支援機率微調。
* **MythicMobs 深度整合**：全面支援原版生物與 MythicMobs 自訂怪物。
* **籠內 3D 模型旋轉預覽**：生怪磚方塊內部支援固定特定怪物模型或定時依序循環輪播。
* **通關獎勵與自訂音效**：設定通關戰利品池與機率，內建勝利音效自訂系統（音效、音調、音量微調與 GUI 即時試聽）。
* **純左鍵操作友善**：怪物挑選清單支援「刪除模式」，順序編排介面亦內建「🗑 刪除此怪物格子」按鈕，全介面均可純左鍵輕鬆操作。
* **跨世界防護**：戰鬥進程具備世界安全檢驗，防止玩家戰鬥期間跨界傳送造成例外報錯。

### 4. 戰利品額度限制與全服通告
* **全服與個人數量限制**：稀有掉落物支援設定「全服上限」與「每人每日/累計上限」。
* **智慧剔除機制**：當玩家抽中已達獲取上限的物品時，系統將**自動將該物品剔除於抽取選項**並正常進行抽獎，保障玩家獲獎權益，絕不吞抽！
* **真實自訂名稱廣播**：支援全服出貨公告，通告內容採用物品的真實顯示名稱（而非原生 Material ID），音效與文字皆可自由配置。
* **額度查詢與重置管理**：管理員可透過指令隨時查詢全服或指定玩家的出貨紀錄與剩餘額度，亦可一鍵進行額度重置。

### 5. 視覺化引導精靈與草稿保護
* **零門檻視覺化操作**：手持樣板物品對空氣右鍵即可進入專屬設定精靈，儲存前完整列出所有參數進行總覽確認。
* **草稿自動暫存 (`/clx create`)**：新增過程中若按 `ESC`、關閉介面或斷線，系統會自動在背景存為草稿並防誤放地面，手持右鍵空氣即可接續進度。
* **純淨無草稿編輯 (`/clx edit`)**：修改現有模板採用無草稿模式，直接讀取現有配置，退出不留任何暫存檔案，放棄安全還原，儲存即直接覆寫。

### 6. 全域動態即時同步
* 設定儲存後，線上所有玩家手持與背包內同配置的樣板方塊，Lore 說明均自動更新為最新數據。
* 世界上已放置的生怪磚與寶庫採用動態讀取架構，**後台或 GUI 修改設定後，地圖上無需手動拆除重建即可即時套用最新參數**！

---

## 指令與權限說明

主指令開頭為 `/clx`（別名 `/customlootx`），所有指令皆需要 `customlootx.admin` 權限（預設 OP 等級 2 以上）：

| 指令 | 說明 |
| :--- | :--- |
| `/clx create <suspicious\|vault\|spawner> [ominous\|normal]` | 創建全新自訂樣板物品（手持對空氣右鍵進入精靈引導） |
| `/clx give <suspicious\|vault\|spawner> <名稱> [玩家] [數量]` | 取得或給予指定玩家已設定好的樣板方塊 |
| `/clx key <寶庫名稱> [玩家] [數量]` | 發放指定寶庫的專屬防偽解鎖鑰匙 |
| `/clx edit <suspicious\|vault\|spawner> <名稱>` | 直接開啟現有模板進行步驟修改（純淨無草稿模式） |
| `/clx list [suspicious\|vault\|spawner]` | 列出所有或指定分類下已儲存的配置及其冷卻/重置狀態 |
| `/clx delete <suspicious\|vault\|spawner> <名稱>` | 刪除指定的配置檔案並同步清除草稿 |
| `/clx checklimit [玩家\|all]` | 查詢指定玩家或全體玩家的物品出貨額度與累計紀錄 |
| `/clx resetlimit [玩家\|all]` | 重設指定玩家或全體玩家的戰利品出貨額度累計 |
| `/clx reload` | 重新載入所有設定檔、模板資料與執行中計時器 |
| `/clx help` | 顯示完整的指令清單與用法說明 |

---

## 系統環境與相依性

* **支援核心**：Paper / Purpur 1.21.x ~ 26.x 或更高版本。
* **Java 版本**：Java 21+。
* **相容插件**：
  * [MythicMobs](https://mythiccraft.io/)：支援試煉生怪磚召喚 MM 自訂生物與自動抓取內部展示基底模型。

---

# CustomLootX (English)

![Release](https://img.shields.io/badge/Release-v2.4.9-orange.svg)
![Version](https://img.shields.io/badge/Minecraft-1.21.x%20%7C%2026.x-brightgreen.svg)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-blue.svg)

**CustomLootX** is a next-generation advanced loot and trial mechanics management plugin built specifically for **Minecraft 1.21.x ~ 26.x** servers.

It seamlessly integrates the core mechanics of 1.21: **Custom Suspicious Blocks**, **Custom Trial Vaults**, and **Custom Trial Spawners**. Featuring an intuitive **Setup Wizard GUI**, you can easily design dungeons, puzzles, exploration rewards, and combat encounters without touching tedious YAML configuration files!

---

## Key Features

### 1. Custom Suspicious Blocks
- **Block Type Support**: Fully supports 1.20+ features, including **Suspicious Sand** and **Suspicious Gravel**.
- **Dynamic Brushing & Auto-Reset**: Configure independent respawn/reset timers for each block. Once brushed and claimed, blocks automatically reset after countdown, allowing players to explore them repeatedly.
- **Visual Loot Table & Drop Rates**: Drag and drop rewards directly into an intuitive 54-slot chest GUI. Supports click adjustments and precise decimal chat input (down to `0.01%`), custom "Empty Drop" chances, and one-click auto-fill or balance features.

### 2. Custom Trial Vaults
- **Customizable Keys**: Use any item as an unlocking key (including vanilla Trial Keys, Ominous Trial Keys, or custom RPG keys). Supports matching item material, custom display names, full lore lines, and CustomModelData / NBT tags.
- **Three Versatile Cooldown Modes**:
  * **Individual Player Cooldown**: Tracks unlock cooldowns per player—ideal for multiplayer dungeons where everyone claims their own loot.
  * **Global Server Cooldown**: Triggers a server-wide cooldown once unlocked—ideal for open-world boss chests and competitive objectives.
  * **Once Per Player Mode**: Allows each player to claim rewards only once in a lifetime—perfect for unique dungeon completion treasures.
- **Dynamic Previews & Ejection Effects**:
  * Supports configuring multiple loot roll counts per unlock (1 ~ 64 items).
  * Preserves the classic Trial Vault visual effects: spinning item previews inside the vault and item ejection animations upon unlocking.

### 3. Custom Trial Spawners
- **Battle & Wave Progression**: Configure spawn intervals, player detection radius, Action Bar combat broadcast, and a **"Wait for Wave Clear" toggle** (choose between standard wave-cleared progression or rapid timed onslaught where waves accumulate).
- **Dynamic Wave Matrix & Sequence Board**:
  - Freedom from rigid monster caps: **design unique monster counts for each wave** (e.g., Wave 1: 2 minions, Wave 2: 5 guards, Wave 3: 1 boss).
  - Each wave features an intuitive `[Add Slot]` button at the end to expand monster capacity, plus `[Add Wave]` buttons to append new waves dynamically.
  - Supports specific mobs or "Random Pick", marked by red placeholder glass panes. **The Next Step button automatically locks if unassigned red slots remain**, ensuring mistake-free configuration.
  - Smart branching: fixed-mob setups bypass chances entirely, while random slots automatically balance to 100%.
- **Deep MythicMobs Integration**: Supports vanilla mobs as well as MythicMobs custom entities.
- **Internal 3D Model Display**: Supports fixed preview models or automatic cyclic rotations inside the spawner block.
- **Victory Rewards & Custom Sounds**: Completion loot tables with custom sound selection, pitch/volume adjustments, and in-GUI audio previews.
- **Left-Click Friendly**: Dedicated delete toggles and slot removal buttons ensure a seamless, 100% left-click friendly workflow throughout all menus.
- **Cross-World Safety**: Robust world validation prevents distance calculation errors when players teleport across worlds during battles.

### 4. Loot Limits & Server-Wide Broadcasts
- **Global & Per-Player Quotas**: Configure server-wide caps and per-player quotas for rare items.
- **Smart Pool Exclusion**: When a player rolls an item that has reached its quota, the item is **automatically excluded from the pool**, allowing the roll to proceed seamlessly without blocking normal reward distribution!
- **Real Item Display Names in Broadcasts**: Server-wide reward announcements use the item's custom display name (rather than raw Material IDs), with customizable sound effects and toggles.
- **Limit Auditing & Reset Commands**: Administrators can inspect claim counters and remaining limits via `/clx checklimit` and reset quotas via `/clx resetlimit`.

### 5. Setup Wizard & Draft Protection
- **Visual Wizard**: Right-click the air while holding a template item to access the step-by-step setup wizard with a final confirmation overview.
- **Draft Session Protection (`/clx create`)**: Unfinished creations automatically save to disk and prevent unfinished blocks from being placed. Right-click air to resume.
- **Clean Edit Mode (`/clx edit`)**: Editing existing templates operates in a pure no-draft mode—loads directly from files, leaves no temp files on exit, and commits changes only on final save.

### 6. Global Real-Time Synchronization
- Saved item templates in hand or in inventories update their lore details instantly.
- Placed blocks dynamically read configurations—**changes take effect in-game without needing to break and replace blocks!**

---

## Commands & Permissions

The primary command prefix is `/clx` (alias: `/customlootx`). All administrative and editing commands require the `customlootx.admin` permission (defaults to OP Level 2+):

| Command | Description |
| :--- | :--- |
| `/clx create <suspicious\|vault\|spawner> [ominous\|normal]` | Create a new custom template item (Right-click air while holding to open wizard) |
| `/clx give <suspicious\|vault\|spawner> <name> [player] [amount]` | Give configured template blocks to yourself or a specified player |
| `/clx key <vault_name> [player] [amount]` | Grant the dedicated authentic key for a specific vault |
| `/clx edit <suspicious\|vault\|spawner> <name>` | Open the setup wizard for an existing template (Clean No-Draft Mode) |
| `/clx list [suspicious\|vault\|spawner]` | List all saved configurations and their cooldown/reset status |
| `/clx delete <suspicious\|vault\|spawner> <name>` | Delete a specified configuration profile and clean up related drafts |
| `/clx checklimit [player\|all]` | Inspect claimed quotas and remaining limits for players or globally |
| `/clx resetlimit [player\|all]` | Reset loot claim quotas for a specific player or all players |
| `/clx reload` | Reload all configuration files, template data, and active runtimers |
| `/clx help` | Display the full list of commands and syntax guide |

---

## Environment & Dependencies

- **Supported Platforms**: Paper / Purpur 1.21.x ~ 26.x or higher.
- **Java Version**: Java 21+.
- **Optional Dependencies**:
  * [MythicMobs](https://mythiccraft.io/): Required for spawning custom MM entities and resolving base display models.
