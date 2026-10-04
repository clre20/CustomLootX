# CustomLootX

![Release](https://img.shields.io/badge/Release-v1.2.6-orange.svg)
![Version](https://img.shields.io/badge/Minecraft-1.21.x%20%7C%2026.x-brightgreen.svg)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-blue.svg)

**CustomLootX** 是一套專為 **Minecraft 1.21.x ~ 26.x (支援 26.1 / 26.2 / 26.3)** 伺服器設計的次世代進階戰利品與試煉機制管理插件。

整合了 1.21 & 26.x 核心特色的 **自訂可疑方塊（可疑沙/可疑礫石）**、**自訂試煉寶庫** 以及 **自訂試煉生怪磚**，提供全程可視化的 **設定精靈 GUI**，無需繁瑣編輯 YML，即可輕鬆打造副本、解謎、探索與戰鬥獎勵機制！

---
### 1.自訂可疑方塊
* **方塊種類支援**：支援 1.20+ 特色的 **可疑沙子 (Suspicious Sand)** 與 **可疑礫石 (Suspicious Gravel)**。
* **動態刷寶重置**：支援獨立設定每個可疑方塊的自動重置時間（分鐘/秒），刷出戰利品後自動在倒數完畢時重置，讓玩家可重複探索。
* **視覺化獎池機率**：54 格箱子介面直觀擺放獎勵，支援點擊加減與聊天室精準小數點輸入（精度達 `0.01%`），可自訂「刷空（無掉落）」機率，並有一鍵自動補齊與平均分配功能。

### 2.自訂試煉寶庫
* **自訂解鎖鑰匙**：支援任意物品作為開啟鑰匙（包含試煉鑰匙、不祥試煉鑰匙或伺服器自訂 RPG 鑰匙），支援比對物品材質、自訂顯示名稱、完整 Lore 行與 CustomModelData / NBT。
* **靈活冷卻模式**：
  * **獨立個人冷卻**：每位玩家各自獨立計算解鎖冷卻時間，適合多人副本各自領獎。
  * **全域伺服器冷卻**：一人解鎖後全服進入冷卻，適合野外首領寶箱爭奪。
* **動態展示與彈出戰利品**：
  * 支援設定多個戰利品抽取次數。
  * 支援試煉寶庫經典的內部旋轉物品預覽展示與解鎖時的物品彈出特效。

### 3.自訂試煉生怪磚
* **多波次戰鬥系統**：自由配置挑戰波次、每波生成總怪物數、場上最大同時存活量以及挑戰觸發範圍。
* **MythicMobs 深度整合**：
  * 支援 Minecraft 原版生物。
  * 支援 MythicMobs 怪物。
* **生怪磚內部展示模型**：
  * 支援原版生物模型。
  * 支援 MythicMobs 基底生物自動對應顯示。
  * 支援循環模式。
* **勝利獎勵與自訂音效**：
  * 通關獎勵池。
  * 內建勝利音效自訂系統，支援選擇音效、自訂音調與音量，並可在 GUI 內即時試聽！

### 4.視覺化引導精靈
* **零門檻操作**：手持樣板物品對空氣右鍵，即可進入專屬的分步設定引導精靈。
* **即時預覽與總覽確認**：在儲存前完整列出所有設定參數，確認無誤後一鍵儲存。
* **全域動態同步**：
  * 設定儲存後，手持與背包內同配置樣板的 Lore 即刻自動更新最新數據。
  * 世界上已放置的方塊與生怪磚採用動態配置讀取架構，**修改設定後地圖上無需重新拆除放置即可即時生效**！
* **保護機制**：防手滑放置未設定完成的空白樣板方塊。

### 5.草稿暫存保護機制 (Draft System - v1.2.6 新增)
* **未儲存進度自動暫存**：在步驟精靈設定過程中，若因按 `ESC`、關閉介面或斷線而未點擊最後確認儲存，系統會自動將當前進度序列化儲存至伺服器獨立草稿檔案 (`plugins/CustomLootX/drafts/...`)，並在手持物品上標註草稿 ID 與有效期限。
* **嚴格防誤放限制**：所有處於未儲存草稿狀態的方塊，一律**嚴格禁止放置於地面**，避免未完成配置損壞地圖環境。
* **手持右鍵還原編輯**：手持草稿方塊對空氣右鍵，即刻自動載入上次中斷的進度繼續編輯；在確認介面亦可隨時選擇「放棄變更並關閉」一鍵清除草稿檔案與標記。
* **過期自動巡檢與安全還原**：支援自訂草稿保留天數（預設 7 天），逾期草稿會自動清理；若玩家拿出過期草稿物品，說明即時更新為過期狀態，右鍵空氣安全還原為正式版配置。

---
## 指令與權限說明

主指令開頭為 `/clx`（別名 `/customlootx`），所有管理與編輯指令皆需要 `customlootx.admin` 權限（預設 OP 等級 2 以上）：

| 指令 | 說明 |
| :--- | :--- |
| `/clx create <suspicious\|vault\|spawner> [名稱]` | 創建全新自訂樣板物品（手持對空氣右鍵進入精靈引導） |
| `/clx give <suspicious\|vault\|spawner> <名稱> [玩家] [數量]` | 取得或給予指定玩家已設定好的樣板方塊 |
| `/clx edit <suspicious\|vault\|spawner> <名稱>` | 直接開啟指定配置的步驟引導編輯介面 |
| `/clx list <suspicious\|vault\|spawner>` | 列出該分類下所有已儲存的配置及其狀態 |
| `/clx delete <suspicious\|vault\|spawner> <名稱>` | 刪除指定的配置檔案 |
| `/clx reload` | 重新載入所有設定檔、模板資料與執行中計時器 |
| `/clx help` | 顯示完整的指令清單與用法說明 |

---

## 系統環境與相依性

* **支援核心**：Paper / Purpur 1.21.x 或更高版本。
* **Java 版本**：Java 21+。
* **可選依賴**：
  * [MythicMobs](https://mythiccraft.io/)：支援生怪磚召喚 MM 自訂生物與自動抓取基底模型。

---

# CustomLootX

![Release](https://img.shields.io/badge/Release-v1.2.6-orange.svg)
![Version](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen.svg)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Purpur-blue.svg)

**CustomLootX** is a next-generation advanced loot and trial mechanics management plugin built specifically for **Minecraft 1.21.x+** servers.

It seamlessly integrates the core mechanics of 1.21: **Custom Suspicious Blocks (Suspicious Sand / Suspicious Gravel)**, **Custom Trial Vaults**, and **Custom Trial Spawners**. Featuring an intuitive **Setup Wizard GUI**, you can easily design dungeons, puzzles, exploration rewards, and combat encounters without touching tedious YAML configuration files!

---
### 1. Custom Suspicious Blocks
- **Block Type Support**: Fully supports 1.20+ features, including **Suspicious Sand** and **Suspicious Gravel**.
- **Dynamic Brushing & Auto-Reset**: Configure independent respawn/reset timers (minutes/seconds) for each block. Once brushed and claimed, blocks automatically reset after the countdown, allowing players to brush them repeatedly.
- **Visual Loot Table & Drop Rates**: Drag and drop rewards directly into an intuitive 54-slot chest GUI. Supports click adjustments and precise decimal chat input (down to `0.01%`), custom "Empty Drop" chances, and one-click auto-fill or balance features.

### 2. Custom Trial Vaults
- **Customizable Keys**: Use any item as an unlocking key (including vanilla Trial Keys, Ominous Trial Keys, or custom RPG keys). Supports matching item material, custom display names, full lore lines, and CustomModelData / NBT tags.
- **Flexible Cooldown Modes**:
  * **Individual Player Cooldown**: Tracks unlock cooldowns per player—ideal for multiplayer dungeons where everyone claims their own loot.
  * **Global Server Cooldown**: Triggers a server-wide cooldown once unlocked—ideal for open-world boss chests and competitive objectives.
- **Dynamic Previews & Ejection Effects**:
  * Supports configuring multiple loot roll counts per unlock.
  * Preserves the classic Trial Vault visual effects: spinning item previews inside the vault and item ejection animations upon unlocking.

### 3. Custom Trial Spawners
- **Multi-Wave Combat System**: Freely configure challenge waves, total mobs spawned per wave, maximum simultaneous active mobs, and player trigger radius.
- **Deep MythicMobs Integration**:
  * Supports vanilla Minecraft mobs.
  * Supports MythicMobs custom entities.
- **Internal Spawner Display Models**:
  * Supports vanilla mob models.
  * Automatically matches and displays the base entity model for MythicMobs.
  * Supports cycle display mode.
- **Victory Rewards & Custom Sounds**:
  * Configurable completion loot tables.
  * Built-in victory sound system supporting custom sound selection, pitch, and volume adjustments, complete with real-time in-GUI previews!

### 4. Visual Setup Wizard
- **Zero-Friction Workflow**: Right-click the air while holding a template item to enter a dedicated step-by-step setup wizard.
- **Real-Time Preview & Summary**: Displays a comprehensive overview of all configured parameters before saving—commit changes with a single click.
- **Global Dynamic Synchronization**:
  * Once saved, templates held in hand or sitting in player inventories update their lore details instantly.
  * Placed blocks and spawners in the world utilize a dynamic configuration lookup architecture—**updates take effect instantly without needing to break and replace them!**
- **Safety Protection**: Prevents accidental placement of unconfigured blank template blocks.

### 5. Draft Protection System (New in v1.2.6)
- **Automatic Draft Session Persistence**: If a player exits the wizard GUI via `ESC`, closes the inventory, or disconnects without saving, current progress is automatically serialized to disk (`plugins/CustomLootX/drafts/...`) and stamped onto the item with a unique Draft ID and expiration timestamp.
- **Strict Ground Placement Restriction**: Unsaved draft blocks are strictly prohibited from being placed onto the ground, safeguarding world environments.
- **Right-Click Air to Resume**: Right-clicking the air while holding a draft block instantly restores the previous in-progress session. Players can also discard changes and clean up the draft from the final confirmation GUI.
- **Auto Expiration & Safe Reversion**: Configurable expiration duration (default 7 days). Expired drafts are automatically cleaned up, and held items dynamically notify the player and revert safely to the official configuration upon interaction.

---
## Commands & Permissions

The primary command prefix is `/clx` (alias: `/customlootx`). All administrative and editing commands require the `customlootx.admin` permission (defaults to OP Level 2+):

| Command | Description |
| :--- | :--- |
| `/clx create <suspicious\|vault\|spawner> [name]` | Create a new custom template item (Right-click air while holding to open wizard) |
| `/clx give <suspicious\|vault\|spawner> <name> [player] [amount]` | Give configured template blocks to yourself or a specified player |
| `/clx edit <suspicious\|vault\|spawner> <name>` | Open the step-by-step setup wizard GUI for an existing configuration |
| `/clx list <suspicious\|vault\|spawner>` | List all saved configurations and their status under a category |
| `/clx delete <suspicious\|vault\|spawner> <name>` | Delete a specified configuration profile |
| `/clx reload` | Reload all configuration files, template data, and active runtimers |
| `/clx help` | Display the full list of commands and syntax guide |

---

## Environment & Dependencies

- **Supported Platforms**: Paper / Purpur 1.21.x or higher.
- **Java Version**: Java 21+.
- **Optional Dependencies**:
  * [MythicMobs](https://mythiccraft.io/): Required for spawning custom MM entities and resolving base display models.
