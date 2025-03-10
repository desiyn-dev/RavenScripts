// =========================
// CONSTANTS
// =========================
final int WHITE = 0xFFFFFF;
final int AREA_COLOR = 0x33FFFFFF;
final int AREA_ACTIVE_COLOR = 0x6644AAFF;

final int ICON_SIZE = 18;
final int TEXT_PADDING = 20;
final int PADDING = 4;
final int ARMOR_SPACING = 18;
final int ARMOR_ITEM_SIZE = 16;

// =========================
// VARIABLES
// =========================
boolean chatOpened = false;
Map<String, Boolean> isDraggingMap = new HashMap<>();
Map<String, Integer> dragOffsetXMap = new HashMap<>();
Map<String, Integer> dragOffsetYMap = new HashMap<>();
String currentlyDraggingItem = null;

Map<String, String> potionNameMap = new HashMap<>();
Map<String, Image> potionImageMap = new HashMap<>();

Map<String, int[]> hudPositionsCache = new HashMap<>();
int cachedPotionHUDWidth = -1;
int cachedPotionHUDHeight = -1;

boolean moveInChatEnabled = false;

boolean armorHUDEnabled = false;
double armorHUDPosition = 0;

boolean potionHUDEnabled = false;
boolean potionHUDSync = false;

boolean fpsHUDEnabled = false;
boolean fpsHUDSync = false;

boolean bpsHUDEnabled = false;
boolean bpsHUDSync = false;

boolean watermarkHUDEnabled = false;
double watermarkStyle = 0;
boolean watermarkSync = false;
String watermarkText = "Raven B4 &7($USER)";
int watermarkX = 5;
int watermarkY = 5;
boolean watermarkDragging = false;
String[] watermarkProfiles = new String[5];
String[] watermarkProfileNames = {"Watermark 1", "Watermark 2", "Watermark 3", "Watermark 4", "Watermark 5"};
int selectedWatermarkProfile = 0;

boolean coordinatesHUDEnabled = false;
boolean coordinatesHUDSync = false;

boolean buildInfoHUDEnabled = false;

boolean targetHUDEnabled = false;
double targetHUDStyle = 0;
boolean targetHUDSync = false;

String buildVersion = "Release"; // Default version
String cachedUID = "";
String cachedUsername = "";

// =========================
// TARGET HUD VARIABLES
// =========================
Entity currentTarget = null;
long targetTime = 0;
static final int TARGET_DISPLAY_DURATION = 1000;
float interpolatedHealth = 0;
long lastHealthUpdate = 0;
static final float HEALTH_INTERPOLATION_SPEED = 0.05f;

static final int HUD_WIDTH = 140;
static final int HUD_HEIGHT = 60;
static final int HUD_COLOR = 0xAA000000;
static final int ENTITY_AREA_WIDTH = 40;
static final int TEXT_AREA_WIDTH = HUD_WIDTH - ENTITY_AREA_WIDTH;
static final int TEXT_AREA_PADDING = 4;
static final int ROW_GAP = 3;
static final int ITEM_TEXT_GAP = 4;
static final int NUM_ITEM_SLOTS = 5;
static final int ITEM_SPACING = 3;
static final int HEALTH_BAR_HEIGHT = 6;
static final int HEALTH_BAR_PADDING_BOTTOM = 4;
static final int HEALTH_BAR_RADIUS = 2;
static final int ENTITY_RENDER_SCALE = 32;
static final int ENTITY_VERTICAL_MARGIN = 4;

static final int ASTOLFO_HUD_WIDTH = 150;
static final int ASTOLFO_HUD_HEIGHT = 55;
static final int ASTOLFO_ENTITY_PADDING = 4;
static final int ASTOLFO_TEXT_PADDING = 6;
static final int ASTOLFO_HEALTHBAR_HEIGHT = 8;
static final int ASTOLFO_ENTITY_SCALE = 25;

static final int MYAU_HUD_WIDTH = 130;
static final int MYAU_HUD_HEIGHT = 30;
static final int MYAU_HUD_PADDING = 2;
static final int MYAU_HUD_COLOR = 0x55000000;
static final int MYAU_HEALTHBAR_HEIGHT = 4;

boolean targetRequireKillAura = false;
boolean targetOnlyNetworkPlayers = true;
boolean targetShowWL = true;
boolean targetShowHealthDiff = false;
boolean targetTextShadow = true;
boolean targetShowHead = true;

// =========================
// HELPER METHODS
// =========================

/**
 * Returns the normalized potion name (removes the "potion." prefix if present).
 */
String normalizePotionName(String name) {
    return name.startsWith("potion.") ? name.substring(7) : name;
}

/**
 * Checks if a string represents a valid integer.
 */
boolean isNumeric(String str) {
    try {
        Integer.parseInt(str);
        return true;
    } catch (NumberFormatException e) {
        return false;
    }
}

/**
 * Retrieves the HUD position for a given config key.
 * Uses a global cache (hudPositionsCache) to avoid repeated config lookups.
 */
int[] getHudPosition(String configName, int defaultX, int defaultY) {
    if (hudPositionsCache.containsKey(configName)) {
        return hudPositionsCache.get(configName);
    }
    int posX = defaultX;
    int posY = defaultY;
    String savedX = config.get(scriptName + "_" + configName + "_x");
    String savedY = config.get(scriptName + "_" + configName + "_y");
    if (savedX != null && savedY != null) {
        try {
            posX = Integer.parseInt(savedX);
            posY = Integer.parseInt(savedY);
        } catch (NumberFormatException e) {
            debug("Failed to parse saved position for " + configName);
        }
    }
    int[] pos = new int[]{posX, posY};
    hudPositionsCache.put(configName, pos);
    return pos;
}

/**
 * Updates the HUD position in both config and our cache.
 */
void updateHudPosition(String configName, int posX, int posY) {
    config.set(scriptName + "_" + configName + "_x", String.valueOf(posX));
    config.set(scriptName + "_" + configName + "_y", String.valueOf(posY));
    hudPositionsCache.put(configName, new int[]{posX, posY});
}

/**
 * Converts the raw mouse coordinates to GUI coordinates.
 */
int[] getConvertedMouse() {
    int[] displaySize = client.getDisplaySize();
    int displayHeight = displaySize[1];
    int[] rawMouse = keybinds.getMousePosition();
    int guiX = rawMouse[0] / 2;
    int guiY = (displayHeight * 2 - rawMouse[1]) / 2;
    return new int[]{guiX, guiY};
}

/**
 * Loads watermark profiles from config
 */
void loadWatermarkProfiles() {
    for (int i = 0; i < watermarkProfiles.length; i++) {
        String configKey = scriptName + "_watermark_profile_" + i;
        String savedValue = config.get(configKey);
        if (savedValue != null && !savedValue.isEmpty()) {
            watermarkProfiles[i] = savedValue;
        }
    }
}

/**
 * Saves watermark profiles to config
 */
void saveWatermarkProfiles() {
    for (int i = 0; i < watermarkProfiles.length; i++) {
        String configKey = scriptName + "_watermark_profile_" + i;
        config.set(configKey, watermarkProfiles[i]);
    }
}

/**
 * Print and debug messages
 */
void print(String message) {
    client.print("&7[&dR&7] &r" + message);
}

void debug(String message) {
    if (!modules.getButton(scriptName, "Debug")) return;
    client.print("&7[&dR&7] &eDebug: &r" + message);
}
/**
 * Reset settings
 */
void resetSettings() {
    // Script Settings
    modules.setButton(scriptName, "Debug", false);
    modules.setButton(scriptName, "Move in chat", false);

    // ArmorHUD
    modules.setButton(scriptName, "Enable ArmorHUD", true);
    modules.setSlider(scriptName, "Position", 0);

    // PotionHUD
    modules.setButton(scriptName, "Enable PotionHUD", true);
    modules.setButton(scriptName, "Sync PotionHUD Colors", false);

    // FPS
    modules.setButton(scriptName, "Enable FPS", true);
    modules.setButton(scriptName, "Sync FPS Colors", false);

    // BPS
    modules.setButton(scriptName, "Enable BPS", true);
    modules.setButton(scriptName, "Sync BPS Colors", false);

    // Coordinates
    modules.setButton(scriptName, "Enable Coordinates", true);
    modules.setButton(scriptName, "Sync Coordinates Colors", false);

    // Watermark
    modules.setButton(scriptName, "Enable Watermark", true);
    modules.setSlider(scriptName, "Watermark Style", 0);
    modules.setButton(scriptName, "Sync Watermark Colors", true);
    
    // Build Info
    buildVersion = "Release";
    config.set(scriptName + "_build_version", buildVersion);
    modules.setButton(scriptName, "Enable Build Info", true);

    // TargetHUD
    modules.setButton(scriptName, "Enable TargetHUD", true);
    modules.setSlider(scriptName, "TargetHUD Style", 0);
    modules.setButton(scriptName, "Require KillAura", false);
    modules.setButton(scriptName, "Only Network Players", true);
    modules.setButton(scriptName, "Sync TargetHUD Colors", true);
    modules.setButton(scriptName, "Text Shadow", true);
    modules.setButton(scriptName, "Show Head", true);
    modules.setButton(scriptName, "Show W/L", true);
    modules.setButton(scriptName, "Show Health Diff", false);
    
    int[] displaySize = client.getDisplaySize();
    int displayWidth = displaySize[0];
    int displayHeight = displaySize[1];
    int edgePadding = 5;
    int fontPadding = 1;
    int fontHeight = render.getFontHeight();
    
    updateHudPosition("armorHUD", displayWidth - 20, displayHeight - 20 - fontHeight - edgePadding);
    updateHudPosition("potionHUD", edgePadding, displayHeight / 2);
    updateHudPosition("fpsHUD", edgePadding, displayHeight - fontHeight * 3 - edgePadding - fontPadding * 2);
    updateHudPosition("bpsHUD", edgePadding, displayHeight - fontHeight * 2 - edgePadding - fontPadding);
    updateHudPosition("coordinatesHUD", edgePadding, displayHeight - fontHeight - edgePadding);
    updateHudPosition("watermarkHUD", edgePadding, 5);
    updateHudPosition("targetHUD", displayWidth / 2, displayHeight / 2);
    updateHudPosition("buildInfoHUD", 0, displayHeight - fontHeight - edgePadding);
    updateBuildInfoPosition();
    updateHudPosition("watermark", 5, 5);

    for (int i = 0; i < watermarkProfiles.length; i++) {
        watermarkProfiles[i] = "Raven B4 &7($USER)";
    }

    saveWatermarkProfiles();
    updateSettings();
}

// =========================
// INITIALIZATION
// =========================

void onLoad() {
    modules.registerButton("Debug", false);
    modules.registerButton("Move in chat", true);

    // ArmorHUD
    modules.registerDescription("> ArmorHUD");
    modules.registerButton("Enable ArmorHUD", true);
    modules.registerSlider("Position", "", 0, new String[] {"Vertical", "Horizontal"});

    // PotionHUD
    modules.registerDescription("> PotionHUD");
    modules.registerButton("Enable PotionHUD", true);
    modules.registerButton("Sync PotionHUD Colors", false);
    
    // FPS
    modules.registerDescription("> FPS");
    modules.registerButton("Enable FPS", true);
    modules.registerButton("Sync FPS Colors", false);

    // BPS
    modules.registerDescription("> BPS");
    modules.registerButton("Enable BPS", true);
    modules.registerButton("Sync BPS Colors", false);
    
    // Coordinates
    modules.registerDescription("> Coordinates");
    modules.registerButton("Enable Coordinates", true);
    modules.registerButton("Sync Coordinates Colors", false);

    // Watermark
    modules.registerDescription("> Watermark");
    modules.registerButton("Enable Watermark", true);
    modules.registerSlider("Watermark Style", "", 0, new String[] {"Simple", "CS:GO"});
    modules.registerSlider("Watermark Profile", "", 0, watermarkProfileNames);
    modules.registerButton("Sync Watermark Colors", false);

    // Build Info
    modules.registerDescription("> Build Info");
    modules.registerButton("Enable Build Info", true);

    // TargetHUD
    modules.registerDescription("> TargetHUD");
    modules.registerButton("Enable TargetHUD", true);
    modules.registerSlider("TargetHUD Style", "", 0, new String[] {"Default", "Astolfo", "Myau"});
    modules.registerButton("Require KillAura", false);
    modules.registerButton("Only Network Players", true);
    modules.registerButton("Sync TargetHUD Colors", false);
    modules.registerDescription(util.color("&7Myau Style"));
    modules.registerButton("Text Shadow", true);
    modules.registerButton("Show Head", true);
    modules.registerDescription(util.color("&7Astolfo Style"));
    modules.registerButton("Show W/L", true);
    modules.registerButton("Show Health Diff", false);

    updateSettings();
    loadSavedValues();
    initializePotionNames();

    for (int i = 0; i < watermarkProfiles.length; i++) {
        watermarkProfiles[i] = "Raven B4 &7($USER)";
    }
    
    loadWatermarkProfiles();
}

void onEnable() {
    cachedUID = String.valueOf(client.getUID());
    cachedUsername = client.getUser();
    
    debug("Cached UID: " + cachedUID + ", Username: " + cachedUsername);

    boolean hasConfig = config.get(scriptName + "_has_config") != null;
    if (!hasConfig) {
        config.set(scriptName + "_has_config", "true");
        resetSettings();
    }

    updateSettings();
}

void updateSettings() {
    moveInChatEnabled = modules.getButton(scriptName, "Move in chat");

    armorHUDEnabled = modules.getButton(scriptName, "Enable ArmorHUD");
    armorHUDPosition = modules.getSlider(scriptName, "Position");

    potionHUDEnabled = modules.getButton(scriptName, "Enable PotionHUD");
    potionHUDSync = modules.getButton(scriptName, "Sync PotionHUD Colors");

    fpsHUDEnabled = modules.getButton(scriptName, "Enable FPS");
    fpsHUDSync = modules.getButton(scriptName, "Sync FPS Colors");

    bpsHUDEnabled = modules.getButton(scriptName, "Enable BPS");
    bpsHUDSync = modules.getButton(scriptName, "Sync BPS Colors");
    
    coordinatesHUDEnabled = modules.getButton(scriptName, "Enable Coordinates");
    coordinatesHUDSync = modules.getButton(scriptName, "Sync Coordinates Colors");

    watermarkHUDEnabled = modules.getButton(scriptName, "Enable Watermark");
    watermarkStyle = modules.getSlider(scriptName, "Watermark Style");
    watermarkSync = modules.getButton(scriptName, "Sync Watermark Colors");

    targetHUDEnabled = modules.getButton(scriptName, "Enable TargetHUD");
    targetHUDStyle = modules.getSlider(scriptName, "TargetHUD Style");
    targetHUDSync = modules.getButton(scriptName, "Sync TargetHUD Colors");
    targetRequireKillAura = modules.getButton(scriptName, "Require KillAura");
    targetOnlyNetworkPlayers = modules.getButton(scriptName, "Only Network Players");
    targetTextShadow = modules.getButton(scriptName, "Text Shadow");
    targetShowHead = modules.getButton(scriptName, "Show Head");
    targetShowWL = modules.getButton(scriptName, "Show W/L");
    targetShowHealthDiff = modules.getButton(scriptName, "Show Health Diff");

    buildInfoHUDEnabled = modules.getButton(scriptName, "Enable Build Info");

    selectedWatermarkProfile = (int) modules.getSlider(scriptName, "Watermark Profile");
    selectedWatermarkProfile = clamp(selectedWatermarkProfile, 0, watermarkProfiles.length - 1);
    
    debug("Settings updated");
}

void initializePotionNames() {
    addPotionMapping("moveSpeed", "Speed", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/ce/Speed_JE3_BE2.png/revision/latest?cb=20210224080116");
    addPotionMapping("moveSlowdown", "Slowness", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/8/83/Slowness_JE3_BE2.png/revision/latest?cb=20210224080107");
    addPotionMapping("digSpeed", "Haste", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/c4/Haste_JE3.png/revision/latest?cb=20210224074327");
    addPotionMapping("digSlowDown", "Mining Fatigue", "hhttps://static.wikia.nocookie.net/minecraft_gamepedia/images/b/b4/Mining_Fatigue_JE3_BE2.png/revision/latest?cb=20210224074750");
    addPotionMapping("damageBoost", "Strength", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/cb/Strength_JE3_BE2.png/revision/latest?cb=20210224080132");
    addPotionMapping("jump", "Jump Boost", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/7d/Jump_Boost_JE2_BE2.png/revision/latest?cb=20210224074810");
    addPotionMapping("confusion", "Nausea", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/f/fc/Nausea_JE3_BE2.png/revision/latest?cb=20210224075452");
    addPotionMapping("regeneration", "Regeneration", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/6/66/Regeneration_JE3_BE2.png/revision/latest?cb=20210312153727");
    addPotionMapping("resistance", "Resistance", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/1/11/Resistance_JE2_BE2.png/revision/latest?cb=20210224075420");
    addPotionMapping("fireResistance", "Fire Resistance", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/1/1f/Fire_Resistance_JE2_BE2.png/revision/latest?cb=20210224074305");
    addPotionMapping("waterBreathing", "Water Breathing", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/1/12/Water_Breathing_JE2_BE2.png/revision/latest?cb=20210224080138");
    addPotionMapping("invisibility", "Invisibility", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/2/26/Invisibility_JE2_BE2.png/revision/latest?cb=20210224074815");
    addPotionMapping("blindness", "Blindness", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/b/b6/Blindness_JE2_BE2.png/revision/latest?cb=20210224074253");
    addPotionMapping("nightVision", "Night Vision", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/c/cb/Night_Vision_JE2_BE2.png/revision/latest?cb=20210224075446");
    addPotionMapping("hunger", "Hunger", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/70/Hunger_JE2_BE2.png/revision/latest?cb=20210224074831");
    addPotionMapping("weakness", "Weakness", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/e/e6/Weakness_JE3_BE2.png/revision/latest?cb=20210224080150");
    addPotionMapping("poison", "Poison", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/9/91/Poison_JE3_BE2.png/revision/latest?cb=20210224075436");
    addPotionMapping("wither", "Wither", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/b/b3/Wither_%28effect%29_JE2_BE2.png/revision/latest?cb=20210224080155");
    addPotionMapping("healthBoost", "Health Boost", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/b/bc/Health_Boost_JE3_BE3.png/revision/latest?cb=20210224074333");
    addPotionMapping("absorption", "Absorption", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/7/79/Absorption_JE3_BE3.png/revision/latest?cb=20210223123750");
    addPotionMapping("saturation", "Saturation", "https://static.wikia.nocookie.net/minecraft_gamepedia/images/2/21/Saturation_JE1.png/revision/latest?cb=20210224075415");
}

void addPotionMapping(String internalName, String displayName, String imageUrl) {
    potionNameMap.put(internalName, displayName);
    try {
        client.log("Attempting to load image for " + internalName + " from URL: " + imageUrl);
        Image potionImage = new Image(imageUrl, false);
        if (potionImage == null) {
            client.log("Failed to load image for " + internalName + ": Image is null or not loaded");
        } else {
            client.log("Successfully loaded image for " + internalName);
            potionImageMap.put(internalName, potionImage);
        }
    } catch (Exception e) {
        client.log("Exception loading image for " + internalName + ": " + e.getMessage());
    }
}

// =========================
// EVENT HANDLERS
// =========================
void onRenderTick(float partialTicks) {
    Entity player = client.getPlayer();
    if (player == null) return;
    String currentScreen = client.getScreen();
    boolean noScreenOpen = currentScreen.isEmpty();
    boolean inChat = chatOpened && moveInChatEnabled;
    long currentTime = client.time();
    updateColors();

    if (targetHUDEnabled) {
        if (inChat) {
            if (currentTarget == null || (currentTime - targetTime >= TARGET_DISPLAY_DURATION)) {
                currentTarget = player;
                targetTime = currentTime;
            }
            
            if (noScreenOpen || inChat) {
                renderTargetHUD(currentTarget);
            }
        } else {
            if (currentTarget != null) {
                if (currentTarget == player) {
                    currentTarget = null;
                } else if (currentTime - targetTime < TARGET_DISPLAY_DURATION) {
                    if (noScreenOpen || inChat) {
                        renderTargetHUD(currentTarget);
                    }
                } else {
                    currentTarget = null;
                }
            }
        }
    }
    
    if (noScreenOpen || inChat) {
        if (fpsHUDEnabled) fpsHUD();
        if (armorHUDEnabled) armorHUD(player);
        if (potionHUDEnabled) potionHUD(player);
        if (bpsHUDEnabled) bpsHUD(player);
        if (buildInfoHUDEnabled) buildInfoHUD();
        if (watermarkHUDEnabled) watermarkHUD();
        if (coordinatesHUDEnabled) coordinatesHUD(player);
    }
    
    int[] mousePos = getConvertedMouse();
    int mouseX = mousePos[0];
    int mouseY = mousePos[1];
    
    if (inChat) {
        // ArmorHUD
        if (armorHUDEnabled) {
            boolean isVertical = (armorHUDPosition == 0);
            int armorWidth = isVertical ? ARMOR_ITEM_SIZE : (4 * ARMOR_SPACING);
            int armorHeight = isVertical ? (4 * ARMOR_SPACING) : ARMOR_ITEM_SIZE;
            
            if (isVertical) {
                int armorCount = 0;
                for (int i = 0; i <= 3; i++) {
                    if (player.getArmorInSlot(i) != null) {
                        armorCount++;
                    }
                }
                int[] pos = getHudPosition("armorHUD", 10, 10);
                int bootsY = pos[1];  
                int totalHeight = (armorCount - 1) * ARMOR_SPACING;
                int helmetY = bootsY - totalHeight;
                handleDraggingArmor(pos[0], helmetY, armorWidth, armorHeight, mouseX, mouseY);
            } else {
                handleDragging(10, 10, armorWidth, armorHeight, "armorHUD", mouseX, mouseY);
            }
        }
        
        // PotionHUD
        if (potionHUDEnabled) {
            int potionWidth = 100;
            int potionHeight = 100;
            String widthStr = config.get(scriptName + "_potionHUD_width");
            String heightStr = config.get(scriptName + "_potionHUD_height");
            if (widthStr != null && heightStr != null) {
                try {
                    potionWidth = Integer.parseInt(widthStr);
                    potionHeight = Integer.parseInt(heightStr);
                    potionWidth = Math.max(potionWidth, 50);
                    potionHeight = Math.max(potionHeight, 20);
                } catch (NumberFormatException e) { /* Use defaults */ }
            }
            handleDragging(30, 30, potionWidth, potionHeight, "potionHUD", mouseX, mouseY);
        }

        // FPS
        if (fpsHUDEnabled) {
            int fpsTextWidth = render.getFontWidth("FPS: " + client.getFPS());
            int fpsTextHeight = render.getFontHeight();
            handleDragging(10, 10, fpsTextWidth, fpsTextHeight, "fpsHUD", mouseX, mouseY);
        }
        
        // BPS
        if (bpsHUDEnabled) {
            int bpsTextWidth = render.getFontWidth(String.format("%.2f blocks/sec", player.getBPS()));
            int bpsTextHeight = render.getFontHeight();
            handleDragging(10, 10, bpsTextWidth, bpsTextHeight, "bpsHUD", mouseX, mouseY);
        }

        // Coordinates
        if (coordinatesHUDEnabled) {
            int coordinatesTextWidth = render.getFontWidth(String.format("%.0f, %.0f, %.0f", player.getPosition().x, player.getPosition().y, player.getPosition().z));
            int coordinatesTextHeight = render.getFontHeight();
            handleDragging(10, 10, coordinatesTextWidth, coordinatesTextHeight, "coordinatesHUD", mouseX, mouseY);
        }
        
        // Watermark
        if (watermarkHUDEnabled) {
            watermarkHUD();
        }
        
        // Build Info
        if (buildInfoHUDEnabled) {
            String buildInfoText = "&7" + buildVersion + " - &r" + cachedUID + " &7- " + cachedUsername;
            String strippedText = stripColorCodes(buildInfoText);
            int buildInfoWidth = render.getFontWidth(strippedText);
            int buildInfoHeight = render.getFontHeight();
            
            handleDragging(10, 10, buildInfoWidth, buildInfoHeight, "buildInfoHUD", mouseX, mouseY);
        }

        // TargetHUD
        if (targetHUDEnabled) {
            int[] hudDimensions = getTargetHUDDimensions();
            int hudWidth = hudDimensions[0];
            int hudHeight = hudDimensions[1];
            
            handleDragging(10, 10, hudWidth, hudHeight, "targetHUD", mouseX, mouseY);
        }
    }
}

/**
 * Special handling for armor HUD dragging when in vertical mode.
 */
void handleDraggingArmor(int visualX, int visualY, int width, int height, int mouseX, int mouseY) {
    String configName = "armorHUD";
    int[] pos = getHudPosition(configName, 10, 10);
    int posX = pos[0];
    int posY = pos[1];
    int offsetY = posY - visualY;

    if (!isDraggingMap.containsKey(configName)) {
        isDraggingMap.put(configName, false);
        dragOffsetXMap.put(configName, 0);
        dragOffsetYMap.put(configName, 0);
    }
    
    boolean isItemDragging = isDraggingMap.get(configName);

    if (!isItemDragging && keybinds.isMouseDown(0) &&
        mouseX >= visualX && mouseX <= visualX + width &&
        mouseY >= visualY && mouseY <= visualY + height &&
        (currentlyDraggingItem == null || currentlyDraggingItem.equals(configName))) {
        
        isDraggingMap.put(configName, true);
        dragOffsetXMap.put(configName, mouseX - visualX);
        dragOffsetYMap.put(configName, mouseY - visualY);
        currentlyDraggingItem = configName;
    }
    
    if (isItemDragging) {
        if (keybinds.isMouseDown(0)) {
            int newVisualX = mouseX - dragOffsetXMap.get(configName);
            int newVisualY = mouseY - dragOffsetYMap.get(configName);
            int newBootsX = newVisualX;
            int newBootsY = newVisualY + offsetY;
            
            updateHudPosition(configName, newBootsX, newBootsY);
            render.rect(newVisualX, newVisualY, newVisualX + width, newVisualY + height, AREA_ACTIVE_COLOR);
        } else {
            isDraggingMap.put(configName, false);
            currentlyDraggingItem = null;
        }
    } else {
        if (mouseX >= visualX && mouseX <= visualX + width &&
            mouseY >= visualY && mouseY <= visualY + height) {
            render.rect(visualX, visualY, visualX + width, visualY + height, AREA_ACTIVE_COLOR);
        } else {
            render.rect(visualX, visualY, visualX + width, visualY + height, AREA_COLOR);
        }
    }
    
    debug("ArmorHUD - Visual Position: " + visualX + ", " + visualY + 
          " | Boots Position: " + posX + ", " + posY +
          " | Size: " + width + "x" + height +
          " | Mouse: " + mouseX + ", " + mouseY);
}

void handleDragging(int defaultX, int defaultY, int width, int height, String configName, int mouseX, int mouseY) {
    int[] pos = getHudPosition(configName, defaultX, defaultY);
    int posX = pos[0];
    int posY = pos[1];

    if (!isDraggingMap.containsKey(configName)) {
        isDraggingMap.put(configName, false);
        dragOffsetXMap.put(configName, 0);
        dragOffsetYMap.put(configName, 0);
    }
    
    boolean isItemDragging = isDraggingMap.get(configName);
    if (!isItemDragging && keybinds.isMouseDown(0) &&
        mouseX >= posX && mouseX <= posX + width &&
        mouseY >= posY && mouseY <= posY + height &&
        (currentlyDraggingItem == null || currentlyDraggingItem.equals(configName))) {
        
        isDraggingMap.put(configName, true);
        dragOffsetXMap.put(configName, mouseX - posX);
        dragOffsetYMap.put(configName, mouseY - posY);
        currentlyDraggingItem = configName;
    }
    
    if (isItemDragging) {
        if (keybinds.isMouseDown(0)) {
            posX = mouseX - dragOffsetXMap.get(configName);
            posY = mouseY - dragOffsetYMap.get(configName);
            updateHudPosition(configName, posX, posY);
            render.rect(posX, posY, posX + width, posY + height, AREA_ACTIVE_COLOR);
        } else {
            isDraggingMap.put(configName, false);
            currentlyDraggingItem = null;
        }
    } else {
        if (mouseX >= posX && mouseX <= posX + width &&
            mouseY >= posY && mouseY <= posY + height) {
            render.rect(posX, posY, posX + width, posY + height, AREA_ACTIVE_COLOR);
        } else {
            render.rect(posX, posY, posX + width, posY + height, AREA_COLOR);
        }
    }
    
    debug("Position: " + posX + ", " + posY + " | Size: " + width + "x" + height +
          " | Mouse: " + mouseX + ", " + mouseY + " | Config: " + configName +
          " | Dragging: " + (currentlyDraggingItem != null ? currentlyDraggingItem : "none"));
}

// =========================
// RENDER FUNCTIONS
// =========================

void armorHUD(Entity player) {
    int[] pos = getHudPosition("armorHUD", 10, 10);
    int x = pos[0];
    int y = pos[1];
    
    boolean isVertical = (armorHUDPosition == 0);
    
    if (isVertical) {
        int armorCount = 0;
        for (int i = 0; i <= 3; i++) {
            if (player.getArmorInSlot(i) != null) {
                armorCount++;
            }
        }
        
        int totalHeight = (armorCount - 1) * ARMOR_SPACING;
        int startY = y - totalHeight;
        int currentY = startY;
        for (int i = 3; i >= 0; i--) {
            ItemStack armor = player.getArmorInSlot(i);
            if (armor == null) continue;
            
            render.item(armor, x, currentY, 1);
            currentY += ARMOR_SPACING;
        }
    } else {
        int currentX = x;
        for (int i = 3; i >= 0; i--) {
            ItemStack armor = player.getArmorInSlot(i);
            if (armor == null) continue;
            render.item(armor, currentX, y, 1);
            currentX += ARMOR_SPACING;
        }
    }
}

void potionHUD(Entity player) {
    int[] pos = getHudPosition("potionHUD", 30, 30);
    int x = pos[0];
    int y = pos[1];
    
    List<Object[]> effects = player.getPotionEffects();
    if (effects.isEmpty()) return;
    
    final int fontHeight = render.getFontHeight();
    final int effectHeight = Math.max((fontHeight * 2) + PADDING, ICON_SIZE + PADDING);
    
    int maxWidth = 0;
    int totalHeight = effects.size() * effectHeight;
    
    List<String> displayNames = new ArrayList<>();
    List<String> durationTexts = new ArrayList<>();
    List<String> internalNamesList = new ArrayList<>();
    
    for (Object[] effect : effects) {
        String internalName = normalizePotionName((String) effect[1]);
        int amplifier = (int) effect[2];
        String displayName = potionNameMap.getOrDefault(internalName, internalName);
        if (amplifier > 0) {
            displayName += " " + (amplifier + 1);
        }
        displayNames.add(displayName);
        internalNamesList.add(internalName);
        
        int duration = (int) effect[3];
        int totalSeconds = duration / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        String durationText = String.format("%d:%02d", minutes, seconds);
        durationTexts.add(durationText);
        
        int textWidth = render.getFontWidth(displayName);
        maxWidth = Math.max(maxWidth, textWidth + TEXT_PADDING);
    }
    
    maxWidth += 10;
    
    if (maxWidth != cachedPotionHUDWidth || totalHeight != cachedPotionHUDHeight) {
        config.set(scriptName + "_potionHUD_width", String.valueOf(maxWidth));
        config.set(scriptName + "_potionHUD_height", String.valueOf(totalHeight));
        cachedPotionHUDWidth = maxWidth;
        cachedPotionHUDHeight = totalHeight;
    }
    
    int currentColor = WHITE;
    if (potionHUDSync) {
        currentColor = getCurrentColor(0);
    }

    int currentY = y;
    for (int i = 0; i < effects.size(); i++) {
        String internalName = internalNamesList.get(i);
        String displayName = displayNames.get(i);
        String durationText = durationTexts.get(i);
        
        Image potionImage = potionImageMap.get(internalName);
        if (potionImage != null) {
            try {
                render.image(potionImage, x, currentY, ICON_SIZE, ICON_SIZE);
            } catch (Exception e) {
                debug("Exception rendering image for " + internalName + ": " + e.getMessage());
                render.rect(x, currentY, x + ICON_SIZE, currentY + ICON_SIZE, WHITE);
            }
        } else {
            render.rect(x, currentY, x + ICON_SIZE, currentY + ICON_SIZE, WHITE);
        }
        
        render.text(displayName, x + TEXT_PADDING, currentY, 1, currentColor, true);
        render.text(durationText, x + TEXT_PADDING, currentY + fontHeight + 1, 1, WHITE, true);
        currentY += effectHeight;
    }
}

void fpsHUD() {
    int[] pos = getHudPosition("fpsHUD", 10, 10);

    int currentColor = WHITE;
    if (fpsHUDSync) {
        currentColor = getCurrentColor(0);
    }
    render.text("FPS: " + client.getFPS(), pos[0], pos[1], 1, currentColor, true);
}

void bpsHUD(Entity player) {
    int[] pos = getHudPosition("bpsHUD", 10, 10);

    int currentColor = WHITE;
    if (bpsHUDSync) {
        currentColor = getCurrentColor(0);
    }
    render.text(String.format("%.2f blocks/sec", player.getBPS()), pos[0], pos[1], 1, currentColor, true);
}

void buildInfoHUD() {
    if (!buildInfoHUDEnabled) return;
    
    int[] pos = getHudPosition("buildInfoHUD", 10, 10);
    String buildInfoText = "&7" + buildVersion + " - &r" + cachedUID + " &7- " + cachedUsername;
    render.text(util.color(buildInfoText), pos[0], pos[1], 1, WHITE, true);
}

void watermarkHUD() {
    int watermarkStyle = (int) modules.getSlider(scriptName, "Watermark Style");
    boolean syncWatermarkColors = modules.getButton(scriptName, "Sync Watermark Colors");
    
    selectedWatermarkProfile = (int) modules.getSlider(scriptName, "Watermark Profile");
    selectedWatermarkProfile = clamp(selectedWatermarkProfile, 0, watermarkProfiles.length - 1);
    watermarkText = watermarkProfiles[selectedWatermarkProfile];
    String processedWatermarkText = processWatermarkPlaceholders(watermarkText);
    if (processedWatermarkText == null || processedWatermarkText.isEmpty()) return;
    processedWatermarkText = util.color(processedWatermarkText);
    int[] mouse = getConvertedMouse();
    
    float width, height;
    if (watermarkStyle == 0) {
        width = render.getFontWidth(stripColorCodes(processedWatermarkText));
        height = render.getFontHeight();
    } else {
        width = calculateTextWidthWithSeparators(stripColorCodes(processedWatermarkText)) + 10;
        height = 17;
    }
    
    if (chatOpened && moveInChatEnabled) {
        handleDragging(5, 5, (int)width, (int)height, "watermark", mouse[0], mouse[1]);
    }
    
    int[] position = getHudPosition("watermark", 5, 5);
    watermarkX = position[0];
    watermarkY = position[1];
    
    if (watermarkStyle == 0) {
        renderSimpleWatermark(processedWatermarkText, syncWatermarkColors, watermarkX, watermarkY);
    } else if (watermarkStyle == 1) {
        renderCSGOWatermark(processedWatermarkText, syncWatermarkColors, watermarkX, watermarkY);
    }
}

void renderSimpleWatermark(String text, boolean syncColors, int x, int y) {
    if (text == null || text.isEmpty()) return;
    if (text.contains("$SEPARATOR")) {
        renderTextWithSeparators(text, x, y, y - 1, y + 9);
        return;
    }
    
    char firstLetter = text.charAt(0);
    String restOfText = text.substring(1);
    
    float currentX = x;
    int firstColor = syncColors ? getCurrentColor(0) : WHITE;
    render.text(String.valueOf(firstLetter), currentX, y, 1f, firstColor, true);
    currentX += render.getFontWidth(String.valueOf(firstLetter));
    render.text(restOfText, currentX, y, 1f, WHITE, true);
}

void renderCSGOWatermark(String text, boolean syncColors, int x, int y) {
    if (text == null || text.isEmpty()) return;
    
    float textWidth = calculateTextWidthWithSeparators(text);
    float rectWidth = textWidth + 10;
    int bgColor = getBackgroundColor();

    render.rect(x, y, x + rectWidth, y + 17, bgColor);
    float lineWidth = rectWidth / 20f;
    for (int i = 0; i < 20; i++) {
        render.rect(x + (i * lineWidth), y, 
                   x + ((i + 1) * lineWidth), y + 2, 
                   getCurrentColor(i * 50L));
    }
    
    renderTextWithSeparators(text, x + 5, y + 6, y + 5, y + 14);
}

float calculateTextWidthWithSeparators(String text) {
    if (text == null) return 0;
    
    String[] parts = text.split("\\$SEPARATOR");
    float width = 0;
    
    for (int i = 0; i < parts.length; i++) {
        width += render.getFontWidth(parts[i]);
        
        if (i < parts.length - 1) {
            width += render.getFontWidth(" ") / 2;
            renderSeparator(width, 0, 0);
            width += render.getFontWidth(" ") / 2 + 1;
        }
    }
    
    return width;
}

void renderSeparator(float x, float lineY1, float lineY2) {
    render.line2D(x + 1, lineY1 + 1, x + 1, lineY2 + 1, 1f, 0xFF404040);
    render.line2D(x, lineY1, x, lineY2, 1f, 0xFFFFFFFF);
}

void renderTextWithSeparators(String text, float x, float y, float lineY1, float lineY2) {
    if (text == null) return;
    
    String[] parts = text.split("\\$SEPARATOR");
    float currentX = x;
    
    for (int i = 0; i < parts.length; i++) {
        if (i == 0 && parts[i].isEmpty()) continue;
        render.text(parts[i], currentX, y, 1f, WHITE, true);
        currentX += render.getFontWidth(parts[i]);
        if (i < parts.length - 1) {
            currentX += render.getFontWidth(" ") / 2;
            renderSeparator(currentX, lineY1, lineY2);
            currentX += render.getFontWidth(" ") / 2 + 1;
        }
    }
}

String processWatermarkPlaceholders(String text) {
    if (text == null) return null;
    
    Entity player = client.getPlayer();
    if (player == null) return text;
    
    text = text.replace("$USER", cachedUsername);
    text = text.replace("$UID", cachedUID);
    text = text.replace("$BUILD", buildVersion);

    text = text.replace("$FPS", String.valueOf(client.getFPS()));
    NetworkPlayer nwp = player.getNetworkPlayer();
    if (nwp != null) {
        int pingValue = nwp.getPing();
        text = text.replace("$PING", String.valueOf(pingValue));
    } else {
        text = text.replace("$PING", "N/A");
    }
    
    if (text.contains("$SERVER")) {
        text = text.replace("$SERVER", client.getServerIP());
    }

    if (text.contains("$BPS")) {
        text = text.replace("$BPS", String.format("%.2f", player.getBPS()));
    }
    debug("Processed watermark text: " + text);
    return text;
}

void coordinatesHUD(Entity player) {
    if (!coordinatesHUDEnabled) return;

    int[] pos = getHudPosition("coordinatesHUD", 10, 10);

    int currentColor = WHITE;
    if (coordinatesHUDSync) {
        currentColor = getCurrentColor(0);
    }
    Vec3 vec = player.getPosition();
    render.text(String.format("%.0f, %.0f, %.0f", vec.x, vec.y, vec.z), pos[0], pos[1], 1, currentColor, true);
}

void onGuiUpdate(String name, boolean opened) {
    if (name.equals("GuiChatOF")) {
        chatOpened = opened;
        debug(opened ? "&aChat opened." : "&cChat closed.");
    }
    if (name.equals("GuiRaven")) {
        updateSettings();
    }
}


// =========================
// COMMAND HANDLING
// =========================

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        String message = c01.message.trim();

        if (message.startsWith(".")) {
            String[] parts = message.substring(1).split(" ");
            String command = parts[0].toLowerCase();
            
            boolean handled = handleCommand(command, parts);
            
            if (handled) {
                debug("&aCommand executed: " + message);
                return false;
            } else {
                return true;
            }
        }
        
        return true;
    }
    
    if (targetHUDEnabled && packet instanceof C02) {
        C02 c02 = (C02) packet;
        if ("ATTACK".equals(c02.action)) {
            Entity potentialTarget = null;
            
            if (modules.getKillAuraTarget() != null) {
                potentialTarget = modules.getKillAuraTarget();
                debug("Got KillAura target: " + potentialTarget.getDisplayName());
            }
            else if (!targetRequireKillAura && c02.entity != null) {
                potentialTarget = c02.entity;
                debug("Got direct attack target: " + potentialTarget.getDisplayName());
            }
            if (potentialTarget != null && (!targetOnlyNetworkPlayers || potentialTarget.getNetworkPlayer() != null)) {
                currentTarget = potentialTarget;
                targetTime = client.time();
            }
        }
    }
    
    return true;
}

/**
 * Handles commands with their arguments.
 */
boolean handleCommand(String command, String[] args) {
    switch (command) {
        case "watermark":
            return handleWatermarkCommand(args);
        
        case "buildinfo":
        case "version":
            return handleBuildInfoCommand(args);
        
        case "reset":
            resetSettings();
            print("&aAll settings have been reset to default values.");
            return true;
            
        case "help":
            showHelp();
            return true;

        default:
            return false;
    }
}

/**
 * Handles watermark-related commands.
 */
boolean handleWatermarkCommand(String[] args) {
    if (args.length < 2) {
        print("Usage: .watermark <set/reset/list> [profile] [value]");
        return true;
    }
    
    String subCommand = args[1].toLowerCase();
    
    if (subCommand.equals("list")) {
        listWatermarkProfiles();
        return true;
    }
    
    int profileIndex = selectedWatermarkProfile;
    int argOffset = 0;
    
    if (args.length >= 3 && isNumeric(args[2])) {
        profileIndex = Integer.parseInt(args[2]) - 1;
        profileIndex = clamp(profileIndex, 0, watermarkProfiles.length - 1);
        argOffset = 1;
    }
    
    switch (subCommand) {
        case "set":
            if (args.length < 3 + argOffset) {
                print("Usage: .watermark set [profile] <text>");
                return true;
            }
            
            StringBuilder sb = new StringBuilder();
            for (int i = 2 + argOffset; i < args.length; i++) {
                sb.append(args[i]);
                if (i < args.length - 1) sb.append(" ");
            }
            
            watermarkProfiles[profileIndex] = sb.toString();
            saveWatermarkProfiles();
            print("Set watermark for profile " + (profileIndex + 1) + " (" + 
                  watermarkProfileNames[profileIndex] + ") to: " + watermarkProfiles[profileIndex]);
            return true;
        case "reset":
            watermarkProfiles[profileIndex] = "Raven B4 &7($USER)";
            print("Reset watermark for profile " + (profileIndex + 1) + " (" + 
                  watermarkProfileNames[profileIndex] + ") to default");
            return true;
        default:
            print("Unknown watermark command.");
            return true;
    }
}

/**
 * Handles build info related commands.
 */
boolean handleBuildInfoCommand(String[] args) {
    if (args.length < 2) {
        print("&cUsage: .buildinfo set <version>");
        return true;
    }
    
    String subCommand = args[1].toLowerCase();
    
    switch (subCommand) {
        case "set":
            if (args.length < 3) {
                print("&cPlease specify a version.");
                return true;
            }
            
            StringBuilder newVersion = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) newVersion.append(" ");
                newVersion.append(args[i]);
            }
            
            buildVersion = newVersion.toString();
            config.set(scriptName + "_build_version", buildVersion);
            
            updateBuildInfoPosition();
            
            print("&aBuild version set to: &r" + buildVersion);
            return true;
            
        default:
            print("&cUnknown buildinfo subcommand. Available: 'set'");
            return true;
    }
}

/**
 * Strips all Minecraft color codes from a string (removes all instances of & followed by 0-9, a-f, k-r)
 */
String stripColorCodes(String text) {
    if (text == null) return "";
    
    return text.replaceAll("&[0-9a-fk-or]", "");
}

/**
 * Updates the position of the buildInfoHUD based on the current text width
 * to maintain proper alignment with the right edge of the screen.
 */
void updateBuildInfoPosition() {
    int[] displaySize = client.getDisplaySize();
    int displayWidth = displaySize[0];
    int edgePadding = 5;
    
    String buildInfoText = "&7" + buildVersion + " - &r&l" + cachedUID + " &7- " + cachedUsername;
    
    String strippedText = stripColorCodes(buildInfoText);
    
    int buildInfoWidth = render.getFontWidth(strippedText);
    
    int[] currentPos = getHudPosition("buildInfoHUD", 10, 10);
    int currentY = currentPos[1];
    
    int newX = displayWidth - buildInfoWidth - edgePadding;
    
    updateHudPosition("buildInfoHUD", newX, currentY);
    
    debug("Updated buildInfoHUD position: X=" + newX + ", width=" + buildInfoWidth + 
          ", stripped text: '" + strippedText + "'");
}

/**
 * Displays available commands and their usage.
 */
void showHelp() {
    print("&e.watermark set/reset/list <profile> <text> &7- Set custom watermark text");
    print("&7Placeholders: &e$UID&7, &e$USER&7, &e$FPS&7, &e$SERVER&7, &e$SEPARATOR&7, &e$PING&7, &e$BPS&7, &e$BUILD&7");
    print("&e.buildinfo set <version> &7- Set custom build version");
    print("&e.reset &7- Reset all settings to default values");
    print("&e.help &7- Show this help message");
}

/**
 * Load saved values during initialization.
 * Add this to your onLoad() method.
 */
void loadSavedValues() {
    String savedBuildVersion = config.get(scriptName + "_build_version");
    if (savedBuildVersion != null && !savedBuildVersion.isEmpty()) {
        buildVersion = savedBuildVersion;
    }

    int[] watermarkPos = getHudPosition("watermark", 5, 5);
    watermarkX = watermarkPos[0];
    watermarkY = watermarkPos[1];
}

/**
 * Get dimensions for the TargetHUD based on the current style
 */
int[] getTargetHUDDimensions() {
    if (targetHUDStyle == 1) { // Astolfo
        return new int[]{ASTOLFO_HUD_WIDTH, ASTOLFO_HUD_HEIGHT};
    } else if (targetHUDStyle == 2) { // Myau
        boolean showHead = targetShowHead;
        int effectiveWidth = showHead ? MYAU_HUD_WIDTH : MYAU_HUD_WIDTH - MYAU_HUD_HEIGHT;
        return new int[]{effectiveWidth, MYAU_HUD_HEIGHT};
    } else { // Default style
        return new int[]{HUD_WIDTH, HUD_HEIGHT};
    }
}

/**
 * Checks if an entity is a valid network player target
 */
boolean checkNetworkPlayer(Entity target) {
    if (target.getNetworkPlayer() == null && targetOnlyNetworkPlayers) {
        return false;
    }
    return true;
}

/**
 * Gets a player head image based on UUID
 */
Image getPlayerHead(Entity target) {
    if (target.getNetworkPlayer() != null) {
        String url = "https://crafatar.com/avatars/" + target.getNetworkPlayer().getUUID() + "?overlay=true&size=8";
        return new Image(url, true);
    }
    return new Image("https://crafatar.com/avatars/c06f89064c8a49119c29ea1dbd1aab82?overlay=true&size=8", true);
}

/**
 * Render the target HUD
 */
void renderTargetHUD(Entity target) {
    if (!checkNetworkPlayer(target)) {
        return;
    }
    
    int[] pos = getHudPosition("targetHUD", 10, 10);
    int x = pos[0];
    int y = pos[1];
    
    float thealth = target.getHealth();
    float shealth = client.getPlayer().getHealth();
    int winning = (shealth > thealth) ? 1 : (shealth == thealth) ? 0 : -1;
    
    long currentTime = client.time();
    if (currentTarget != target || currentTime - lastHealthUpdate > 1000) {
        interpolatedHealth = thealth;
    } else {
        interpolatedHealth += (thealth - interpolatedHealth) * HEALTH_INTERPOLATION_SPEED;
    }
    lastHealthUpdate = currentTime;
    
    if (targetHUDStyle == 1) {
        renderAstolfo(target, interpolatedHealth, shealth, winning, x, y);
    } else if (targetHUDStyle == 2) {
        renderMyau(target, interpolatedHealth, shealth, winning, x, y);
    } else {
        renderDefaultTarget(target, interpolatedHealth, shealth, winning, x, y);
    }
}

void renderDefaultTarget(Entity target, float thealth, float shealth, int winning, int x, int y) {
    render.rect(x, y, x + HUD_WIDTH, y + HUD_HEIGHT, HUD_COLOR);
    
    int entityCenterX = x + (ENTITY_AREA_WIDTH / 2);
    int entityBottomY = y + HUD_HEIGHT - ENTITY_VERTICAL_MARGIN;
    gl.color(1f, 1f, 1f, 1f);
    render.entityGui(target, entityCenterX, entityBottomY, 0, 0, ENTITY_RENDER_SCALE - 5);
    
    int textAreaX = x + ENTITY_AREA_WIDTH;
    int textAreaWidth = TEXT_AREA_WIDTH;
    int fontHeight = render.getFontHeight();
    
    int healthBarY = y + HUD_HEIGHT - HEALTH_BAR_PADDING_BOTTOM - HEALTH_BAR_HEIGHT;
    int healthBarX = textAreaX + TEXT_AREA_PADDING;
    int healthBarMaxWidth = textAreaWidth - 2 * TEXT_AREA_PADDING;
    int itemSlotSize = (healthBarMaxWidth - (NUM_ITEM_SLOTS - 1) * ITEM_SPACING) / NUM_ITEM_SLOTS;
    
    int itemsRowY = y + TEXT_AREA_PADDING;
    for (int i = 0; i < NUM_ITEM_SLOTS; i++) {
        int slotX = healthBarX + i * (itemSlotSize + ITEM_SPACING);
        int slotY = itemsRowY;
        render.rect(slotX, slotY, slotX + itemSlotSize, slotY + itemSlotSize, 0xFF222222);
        if (i < 4) {
            int armorSlot = 3 - i;
            ItemStack armorItem = target.getArmorInSlot(armorSlot);
            if (armorItem != null) {
                render.item(armorItem, slotX, slotY, 1.0f);
            }
        } else {
            ItemStack heldItem = target.getHeldItem();
            if (heldItem != null) {
                render.item(heldItem, slotX, slotY, 1.0f);
            }
        }
    }
    
    int headerY = itemsRowY + itemSlotSize + ITEM_TEXT_GAP;
    render.text(target.getDisplayName(), textAreaX + TEXT_AREA_PADDING, headerY, 1f, 0xFFFFFFFF, true);
    String winText = (winning == 1) ? util.color("&a&lW")
                     : (winning == 0) ? util.color("&e&lD")
                     : util.color("&c&lL");
    int winTextWidth = render.getFontWidth(winText);
    int winTextX = textAreaX + textAreaWidth - TEXT_AREA_PADDING - winTextWidth;
    render.text(winText, winTextX, headerY, 1f, 0xFFFFFFFF, true);
    
    int subheaderY = headerY + fontHeight + ROW_GAP;
    String healthText = String.format("%.1f \u2764", target.getHealth());
    int textColor = targetHUDSync ? getCurrentColor(0) : 0xFFFFFFFF;
    render.text(healthText, textAreaX + TEXT_AREA_PADDING, subheaderY, 1f, textColor, true);
    
    float diff = shealth - target.getHealth();
    String diffText = (diff > 0 ? "+" : "") + String.format("%.1f", diff);
    int diffTextWidth = render.getFontWidth(diffText);
    int diffTextX = textAreaX + textAreaWidth - TEXT_AREA_PADDING - diffTextWidth;
    int diffColor = (diff > 0) ? 0xFF00FF00 : (diff < 0) ? 0xFFFF0000 : 0xFFFFFF00;
    render.text(diffText, diffTextX, subheaderY, 1f, diffColor, true);
    
    float healthPercentage = interpolatedHealth / target.getMaxHealth();
    int healthBarWidth = (int)(healthBarMaxWidth * healthPercentage);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarMaxWidth, healthBarY + HEALTH_BAR_HEIGHT, 0xFF444444);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + HEALTH_BAR_HEIGHT, textColor);
}

void renderAstolfo(Entity target, float thealth, float shealth, int winning, int x, int y) {
    render.rect(x, y, x + ASTOLFO_HUD_WIDTH, y + ASTOLFO_HUD_HEIGHT, HUD_COLOR);

    int entityAreaWidth = ASTOLFO_HUD_HEIGHT / 2;
    int entityCenterX = x + entityAreaWidth / 2 + 4;
    int entityBottomY = y + ASTOLFO_HUD_HEIGHT - ASTOLFO_ENTITY_PADDING;
    gl.color(1f, 1f, 1f, 1f);
    render.entityGui(target, entityCenterX, entityBottomY, -200, 0, ASTOLFO_ENTITY_SCALE);

    int textAreaX = x + entityAreaWidth + 2;  
    int textAreaWidth = ASTOLFO_HUD_WIDTH - entityAreaWidth;

    int nameX = textAreaX + ASTOLFO_TEXT_PADDING;
    int nameY = y + ASTOLFO_TEXT_PADDING;
    
    render.text(target.getDisplayName(), nameX, nameY, 1f, 0xFFFFFFFF, true);
    
    if (targetShowWL) {
        String winText = (winning == 1) ? util.color("&a&lW")
                       : (winning == 0) ? util.color("&e&lD")
                       : util.color("&c&lL");
        int winTextWidth = render.getFontWidth(winText);
        int winTextX = x + ASTOLFO_HUD_WIDTH - ASTOLFO_TEXT_PADDING - winTextWidth;
        render.text(winText, winTextX, nameY, 1f, 0xFFFFFFFF, true);
    }
    
    int healthBarY = y + ASTOLFO_HUD_HEIGHT - ASTOLFO_TEXT_PADDING - ASTOLFO_HEALTHBAR_HEIGHT;
    int healthBarX = textAreaX + ASTOLFO_TEXT_PADDING;
    int healthBarMaxWidth = textAreaWidth - 2 * ASTOLFO_TEXT_PADDING;
    
    float healthPercentage = interpolatedHealth / target.getMaxHealth();
    int healthBarWidth = (int)(healthBarMaxWidth * healthPercentage);
    
    render.rect(healthBarX, healthBarY, healthBarX + healthBarMaxWidth, healthBarY + ASTOLFO_HEALTHBAR_HEIGHT, 0xFF444444);
    int textColor = targetHUDSync ? getCurrentColor(0) : 0xFFFF0000;
    render.rect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + ASTOLFO_HEALTHBAR_HEIGHT, textColor);
    
    int midY = (nameY + healthBarY) / 2 - 4;
    render.text(String.format("%.1f \u2764", target.getHealth()), nameX, midY, 2f, textColor, true);
    
    if (targetShowHealthDiff) {
        float diff = shealth - target.getHealth();
        String diffText = (diff > 0 ? "+" : "") + String.format("%.1f", diff);
        int diffTextWidth = render.getFontWidth(diffText) * 2;
        int diffTextX = x + ASTOLFO_HUD_WIDTH - ASTOLFO_TEXT_PADDING - diffTextWidth;
        int diffColor = (diff > 0) ? 0xFF00FF00 : (diff < 0) ? 0xFFFF0000 : 0xFFFFFF00;
        render.text(diffText, diffTextX, midY, 2f, diffColor, true);
    }
}

void renderMyau(Entity target, float thealth, float shealth, int winning, int x, int y) {
    int effectiveHudWidth = targetShowHead ? MYAU_HUD_WIDTH : MYAU_HUD_WIDTH - MYAU_HUD_HEIGHT;
    render.rect(x, y, x + effectiveHudWidth, y + MYAU_HUD_HEIGHT, MYAU_HUD_COLOR);

    int accentColor = targetHUDSync ? getCurrentColor(0) : 0xFFFF0000;
    
    // border
    render.rect(x - 1, y - 1, x + effectiveHudWidth + 1, y, accentColor);
    render.rect(x - 1, y + MYAU_HUD_HEIGHT, x + effectiveHudWidth + 1, y + MYAU_HUD_HEIGHT + 1, accentColor);
    render.rect(x - 1, y - 1, x, y + MYAU_HUD_HEIGHT + 1, accentColor);
    render.rect(x + effectiveHudWidth, y - 1, x + effectiveHudWidth + 1, y + MYAU_HUD_HEIGHT + 1, accentColor);

    int contentStartX = x;
    if (targetShowHead) {
        Image head = getPlayerHead(target);
        render.image(head, x + MYAU_HUD_PADDING, y + MYAU_HUD_PADDING, 
                    MYAU_HUD_HEIGHT - (MYAU_HUD_PADDING * 2), 
                    MYAU_HUD_HEIGHT - (MYAU_HUD_PADDING * 2));
        contentStartX = x + MYAU_HUD_HEIGHT;
    }

    // first line
    render.text(target.getDisplayName(), 
                contentStartX + MYAU_HUD_PADDING, 
                y + MYAU_HUD_PADDING, 1f, 0xFFFFFFFF, targetTextShadow);
    String winText = (winning == 1) ? util.color("&a&lW")
                    : (winning == 0) ? util.color("&e&lD")
                    : util.color("&c&lL");
    int winTextWidth = render.getFontWidth(winText);
    render.text(winText, 
                x + effectiveHudWidth - winTextWidth - MYAU_HUD_PADDING, 
                y + MYAU_HUD_PADDING, 1f, 0xFFFFFFFF, targetTextShadow);

    // second line
    render.text(String.format("%.1f \u2764", target.getHealth()), 
                contentStartX + MYAU_HUD_PADDING, 
                y + render.getFontHeight() + (MYAU_HUD_PADDING * 2), 
                1f, accentColor, targetTextShadow);
                
    float diff = shealth - target.getHealth();
    String diffText = (diff > 0 ? "+" : "") + String.format("%.1f", diff);
    int diffTextWidth = render.getFontWidth(diffText);
    int diffTextX = x + effectiveHudWidth - MYAU_HUD_PADDING - diffTextWidth;
    int diffTextY = y + render.getFontHeight() + (MYAU_HUD_PADDING * 2);
    int diffColor = (diff > 0) ? 0xFF00FF00 : (diff < 0) ? 0xFFFF0000 : 0xFFFFFF00;
    render.text(diffText, diffTextX, diffTextY, 1f, diffColor, targetTextShadow);

    // health bar
    int healthBarX = contentStartX + MYAU_HUD_PADDING;
    int healthBarMaxWidth = effectiveHudWidth - (targetShowHead ? MYAU_HUD_HEIGHT : 0) - 2 * MYAU_HUD_PADDING;
    int healthBarY = y + MYAU_HUD_HEIGHT - MYAU_HUD_PADDING - MYAU_HEALTHBAR_HEIGHT;
    float healthPercentage = interpolatedHealth / target.getMaxHealth();
    int healthBarWidth = (int)(healthBarMaxWidth * healthPercentage);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarMaxWidth, healthBarY + MYAU_HEALTHBAR_HEIGHT, MYAU_HUD_COLOR);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + MYAU_HEALTHBAR_HEIGHT, accentColor);
}

void listWatermarkProfiles() {
    print("&eWatermark Profiles:");
    for (int i = 0; i < watermarkProfiles.length; i++) {
        print("&e" + (i + 1) + ". &f" + "\"" + watermarkProfiles[i] + "\"");
    }
    print("&eCurrent profile: &f" + (selectedWatermarkProfile + 1));
}