static final String[] CLIENT_PROFILES = {
    "Raven B4",
    "BlowsyWare", 
    "Custom Profile 1",
    "Custom Profile 2",
    "Custom Profile 3",
    "Custom Profile 4",
    "Custom Profile 5"
};

static final String BUILD_VERSION = "Custom"; // Used when setting Build Info Version to "Custom"
static final String BUILD_USER = null;      // Use null for default values (Raven client username)
static final String BUILD_NUMBER = null;    // Use null for default values (Raven client UID)

Color startColor, endColor, staticColor;
int colorMode;
float waveSpeed;

void updateColors() {
    colorMode = (int) modules.getSlider("Client", "Mode");
    waveSpeed = (float) modules.getSlider("Client", "Wave Speed");

    int color1Red = (int) modules.getSlider("Client", "Color 1 - Red");
    int color1Green = (int) modules.getSlider("Client", "Color 1 - Green");
    int color1Blue = (int) modules.getSlider("Client", "Color 1 - Blue");
    int color2Red = (int) modules.getSlider("Client", "Color 2 - Red");
    int color2Green = (int) modules.getSlider("Client", "Color 2 - Green");
    int color2Blue = (int) modules.getSlider("Client", "Color 2 - Blue");

    staticColor = new Color(color1Red, color1Green, color1Blue);

    if (colorMode == 2) {
        startColor = staticColor;
        endColor = new Color(color2Red, color2Green, color2Blue);
    }
}

int getRainbow(float seconds, float saturation, float brightness, long index) {
    float hue = ((client.time() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
    return Color.HSBtoRGB(hue, saturation, brightness);
}

double getWaveRatio(float seconds, long index) {
    float time = ((client.time() + index) % (int)(seconds * 1000)) / (float)(seconds * 1000);
    double waveRatio = (time <= 0.5) ? (time * 2) : (2 - time * 2);
    return waveRatio;
}

Color blendColors(Color color1, Color color2, double ratio) {
    int r = clamp((int) (color1.getRed() * ratio + color2.getRed() * (1 - ratio)), 0, 255);
    int g = clamp((int) (color1.getGreen() * ratio + color2.getGreen() * (1 - ratio)), 0, 255);
    int b = clamp((int) (color1.getBlue() * ratio + color2.getBlue() * (1 - ratio)), 0, 255);
    return new Color(r, g, b);
}

int clamp(int val, int min, int max) {
    return Math.max(min, Math.min(max, val));
}

int getCurrentColor(long index) {
    switch(colorMode) {
        case 1: // Rainbow
            return getRainbow(waveSpeed, 1f, 1f, index);
        case 2: // Gradient
            double ratio = getWaveRatio(waveSpeed, index);
            return blendColors(startColor, endColor, ratio).getRGB();
        default: // Static
            return staticColor.getRGB();
    }
}

int getBackgroundColor() {
    int alpha = (int) modules.getSlider("Client", "Background Alpha");
    return (alpha << 24);
}

/* State */
Entity player;
NetworkPlayer networkPlayer;
Map<String, Integer> potionColors;
Map<String, String> potionNameMap;
int background;
static final int white = 0xFFFFFFFF;
static final int gray = 0xFFB4B4B4;
int ping = 100;
String DEFAULT_BUILD_USER;
String DEFAULT_BUILD_NUMBER;

/* Game */
void onLoad() {
    initializeMaps();
    initializePlayer();
    registerModules();
    DEFAULT_BUILD_USER = client.getUser();
    DEFAULT_BUILD_NUMBER = String.valueOf(client.getUID());
}

void onRenderTick(float partialTicks) {
    if (!updatePlayerState()) return;
    updateColors();
    background = getBackgroundColor();
    int[] size = client.getDisplaySize();
    
    renderWatermark(size);

    if (!client.getScreen().isEmpty()) return;
    renderInfo(size, player);
}

/* Initialization */
void initializeMaps() {
    potionColors = new HashMap<>();
    potionNameMap = new HashMap<>();
    initializePotionColors();
}

void initializePlayer() {
    player = client.getPlayer();
    networkPlayer = player.getNetworkPlayer();
}

void registerModules() {
    modules.registerSlider("Watermark Style", "", 1, new String[]{"None", "Default", "CSGO"});
    modules.registerButton("Show Extra Info in CSGO", false);
    modules.registerSlider("Client Name", "", 0, CLIENT_PROFILES);
    modules.registerButton("Show Build Info", true);
    modules.registerSlider("Build Info Version", "", 0, new String[]{"Release", "Beta", "Alpha", "Development", "Custom"});
    modules.registerButton("Show FPS", true);
    modules.registerButton("Show BPS", true);
    modules.registerButton("Show Ping", true);
    modules.registerButton("Show Potion Effects", true);
    modules.registerButton("Show Coordinates", true);
    modules.registerDescription("by @desiyn");
}

/* Potions */
void initializePotionColors() {
    addPotionMapping("moveSpeed", "Speed", 0xFF7AB5F0);
    addPotionMapping("moveSlowdown", "Slowness", 0xFF695F59);
    addPotionMapping("digSpeed", "Haste", 0xFFDBA951);
    addPotionMapping("digSlowDown", "Mining Fatigue", 0xFF90D4CB);
    addPotionMapping("damageBoost", "Strength", 0xFF590C0C);
    addPotionMapping("jump", "Jump Boost", 0xFF86ADAD);
    addPotionMapping("confusion", "Nausea", 0xFF59783A);
    addPotionMapping("regeneration", "Regeneration", 0xFFC4497A);
    addPotionMapping("resistance", "Resistance", 0xFF556063);
    addPotionMapping("fireResistance", "Fire Resistance", 0xFFC44310);
    addPotionMapping("waterBreathing", "Water Breathing", 0xFF3D6DCC);
    addPotionMapping("invisibility", "Invisibility", 0xFFFFFFFF);
    addPotionMapping("blindness", "Blindness", 0xFF3B3736);
    addPotionMapping("nightVision", "Night Vision", 0xFF131A45);
    addPotionMapping("hunger", "Hunger", 0xFF5C3E2E);
    addPotionMapping("weakness", "Weakness", 0xFF594E48);
    addPotionMapping("poison", "Poison", 0xFF68733D);
    addPotionMapping("wither", "Wither", 0xFF000000);
    addPotionMapping("healthBoost", "Health Boost", 0xFFDB4242);
    addPotionMapping("absorption", "Absorption", 0xFFDBAD42);
    addPotionMapping("saturation", "Saturation", 0xFF7D4B1F);
}

void addPotionMapping(String internalName, String displayName, int color) {
    potionColors.put(internalName, color);
    potionNameMap.put(internalName, displayName);
}

String formatPotionDuration(int ticks) {
    if (ticks == -1) return "**:**";
    int seconds = ticks / 20;
    return String.format("%d:%02d", seconds / 60, seconds % 60);
}

String formatPotionName(String potionEffectId) {
    String name = getPotionNameFromId(potionEffectId);
    return name;
}

String getPotionNameFromId(String id) {
    if (id == null) return null;
    return POTION_ID_MAP.get(id);
}

String formatPotionNameFromString(String potionName) {
    if (potionName == null) return null;
    
    String formattedName = POTION_NAME_MAP.getOrDefault(potionName, potionName);
    formattedName = formattedName.replace("potion.", "");
    return Character.toUpperCase(formattedName.charAt(0)) + formattedName.substring(1);
}

/* Rendering */
void renderWatermark(int[] size) {
    String selectedBuildVersion = getSelectedBuildVersion();
    int watermarkStyle = (int) modules.getSlider(scriptName, "Watermark Style");
    
    if (watermarkStyle == 0) return;
    if (watermarkStyle == 1) renderDefaultWatermark();
    if (watermarkStyle == 2) renderCSGOWatermark(selectedBuildVersion);
}

String getSelectedBuildVersion() {
    String[] versions = {"Release", "Beta", "Alpha", "Development", "Custom"};
    int buildVerIndex = (int) modules.getSlider(scriptName, "Build Info Version");
    if (buildVerIndex == 4) {
        return BUILD_VERSION;
    }
    return buildVerIndex < versions.length ? versions[buildVerIndex] : versions[0];
}

void renderDefaultWatermark() {
    int clientNameIndex = (int) modules.getSlider(scriptName, "Client Name");
    if (clientNameIndex >= CLIENT_PROFILES.length) return;
    
    String name = CLIENT_PROFILES[clientNameIndex];
    if (name == null || name.isEmpty()) return;

    char firstLetter = name.charAt(0);
    String restOfName = name.substring(1);
    
    float x = 5f;
    render.text(String.valueOf(firstLetter), x, 5f, 1f, getCurrentColor(0), true);
    x += render.getFontWidth(String.valueOf(firstLetter));
    render.text(restOfName, x, 5f, 1f, white, true);
}

void renderCSGOWatermark(String selectedBuildVersion) {
    int clientNameIndex = (int) modules.getSlider(scriptName, "Client Name");
    if (clientNameIndex >= CLIENT_PROFILES.length) return;
    
    String watermarkname = CLIENT_PROFILES[clientNameIndex];
    boolean showExtraInfo = modules.getButton(scriptName, "Show Extra Info in CSGO");
    
    if (showExtraInfo) {
        renderCSGOWithExtraInfo(watermarkname, selectedBuildVersion);
    } else {
        renderCSGOBasic(watermarkname, selectedBuildVersion);
    }
}

void renderCSGOWithExtraInfo(String watermarkname, String selectedBuildVersion) {
    if (watermarkname == null || selectedBuildVersion == null) return;
    
    // Reset and build first part
    stringBuilder.setLength(0);
    csgoTextParts[0] = stringBuilder
        .append(watermarkname)
        .append(" [")
        .append(selectedBuildVersion.toUpperCase())
        .append("]")
        .toString();
    
    // Build ping part
    stringBuilder.setLength(0);
    csgoTextParts[2] = stringBuilder
        .append("ping: ")
        .append(ping)
        .append("ms")
        .toString();
    
    // Use default build user if BUILD_USER is null
    csgoTextParts[1] = BUILD_USER != null ? BUILD_USER : DEFAULT_BUILD_USER;
    csgoTextParts[3] = "version: 1.8.9";
    
    float totalWidth = calculateTotalWidth(csgoTextParts);
    float rectWidth = totalWidth + 15;
    
    renderCSGOBackground(5, rectWidth);
    renderCSGOText(10f, 11f, 10f, 19f, csgoTextParts);
}

void renderCSGOBasic(String watermarkname, String selectedBuildVersion) {
    stringBuilder.setLength(0);
    String watermarktext = stringBuilder
        .append(watermarkname)
        .append(" [")
        .append(selectedBuildVersion.toUpperCase())
        .append("]")
        .toString();
        
    float rectWidth = render.getFontWidth(watermarktext) + 10;
    
    renderCSGOBackground(5, rectWidth);
    render.text(watermarktext, 10f, 11f, 1f, white, true);
}

float calculateTotalWidth(String[] textParts) {
    if (textParts == null) return 0;
    
    float width = 0;
    float spaceWidth = render.getFontWidth(" ");
    
    for (String part : textParts) {
        if (part == null) continue;
        width += render.getFontWidth(part) + spaceWidth;
    }
    return width + (spaceWidth * 3);
}

void renderCSGOBackground(float startX, float width) {
    render.rect(startX, 5, startX + width, 22, background);
    
    float lineWidth = width / 20f;
    for (int i = 0; i < 20; i++) {
        render.rect(startX + (i * lineWidth), 5, 
                   startX + ((i + 1) * lineWidth), 7, 
                   getCurrentColor(i * 50L));
    }
}

void renderCSGOText(float x, float y, float lineY1, float lineY2, String[] textParts) {
    for (int i = 0; i < textParts.length; i++) {
        render.text(textParts[i], x, y, 1f, white, true);
        x += render.getFontWidth(textParts[i]) + render.getFontWidth(" ");
        
        if (i < textParts.length - 1) {
            renderSeparator(x, lineY1, lineY2);
            x += render.getFontWidth("  ");
        }
    }
}

void renderSeparator(float x, float lineY1, float lineY2) {
    render.line2D(x + 2, lineY1 + 1, x + 2, lineY2 + 1, 1f, 0xFF404040);
    render.line2D(x + 1, lineY1, x + 1, lineY2, 1f, 0xFFFFFFFF);
}

/* Info Rendering */
void renderInfo(int[] size, Entity player) {
    if (size == null || player == null) return;
    
    float margin = 3f;
    float lineHeight = render.getFontHeight() + 1f;
    float baseY = size[1] - margin - lineHeight;
    
    renderLeftSideInfo(margin, baseY, lineHeight, player);
    renderRightSideInfo(size[0] - margin, baseY, lineHeight, player);
}

void renderLeftSideInfo(float x, float baseY, float lineHeight, Entity player) {
    if (modules.getButton(scriptName, "Show Coordinates") && player != null) {
        String coords = String.format("%.0f, %.0f, %.0f", 
            player.getPosition().x, 
            player.getPosition().y, 
            player.getPosition().z);
        render.text(coords, x, baseY, 1f, white, true);
        baseY -= lineHeight;
    }

    if (modules.getButton(scriptName, "Show BPS") && player != null) {
        String bps = String.format("%.2f blocks/sec", player.getBPS());
        render.text(bps, x, baseY, 1f, white, true);
        baseY -= lineHeight;
    }

    if (modules.getButton(scriptName, "Show FPS")) {
        render.text("FPS: " + client.getFPS(), x, baseY, 1f, white, true);
    }
}

void renderRightSideInfo(float rightX, float baseY, float lineHeight, Entity player) {
    if (modules.getButton(scriptName, "Show Build Info")) {
        renderBuildInfo(rightX, baseY);
        baseY -= lineHeight;
    }

    if (modules.getButton(scriptName, "Show Ping")) {
        renderPingInfo(rightX, baseY);
        baseY -= lineHeight;
    }

    if (modules.getButton(scriptName, "Show Potion Effects")) {
        renderPotionEffects(rightX, baseY, lineHeight, player);
    }
}

void renderBuildInfo(float rightX, float baseY) {
    String selectedBuildVersion = getSelectedBuildVersion();
    String buildUser = BUILD_USER != null ? BUILD_USER : DEFAULT_BUILD_USER;
    String buildNumber = BUILD_NUMBER != null ? BUILD_NUMBER : DEFAULT_BUILD_NUMBER;
    
    String fullText = util.color("&7" + selectedBuildVersion + " - &r" + 
                                buildNumber + " &7- " + buildUser);
    
    float textWidth = render.getFontWidth(fullText);
    render.text(fullText, rightX - textWidth, baseY, 1f, white, true);
}

void renderPingInfo(float rightX, float baseY) {
    String fullText = util.color("&fPing: &7" + ping + "ms");
    float textWidth = render.getFontWidth(fullText);
    render.text(fullText, rightX - textWidth, baseY, 1f, white, true);
}

void renderPotionEffects(float rightX, float baseY, float lineHeight, Entity player) {
    if (player == null) return;
    
    List<Object[]> potionEffects = player.getPotionEffects();
    if (potionEffects == null || potionEffects.isEmpty()) return;
    
    for (Object[] effect : potionEffects) {
        if (effect == null || effect.length < 4) continue;
        try {
            renderPotionEffect(rightX, baseY, effect);
            baseY -= lineHeight;
        } catch (Exception e) {
            client.print("Error with effect: " + e.toString());
        }
    }
}

void renderPotionEffect(float rightX, float baseY, Object[] effect) {
    String rawName = ((String)effect[1]).replace("potion.", "");
    int amplifier = (int)effect[2];
    int duration = (int)effect[3];
    
    String displayName = potionNameMap.getOrDefault(rawName, rawName);
    String amplifierText = amplifier > 0 ? " " + (amplifier + 1) : "";
    String durationText = formatPotionDuration(duration);
    
    int potionColor = potionColors.getOrDefault(rawName, white);
    float x = rightX;
    
    x -= render.getFontWidth(durationText);
    render.text(durationText, x, baseY, 1f, gray, true);
    
    x -= render.getFontWidth(" - ");
    render.text(" - ", x, baseY, 1f, gray, true);
    
    String effectText = displayName + amplifierText;
    x -= render.getFontWidth(effectText);
    render.text(effectText, x, baseY, 1f, potionColor, true);
}

/* Utility Methods */
boolean updatePlayerState() {
    player = client.getPlayer();
    if (player == null) return false;
    
    networkPlayer = player.getNetworkPlayer();
    if (networkPlayer != null) {
        ping = networkPlayer.getPing();
    }
    
    return true;
}

static final Map<String, String> POTION_ID_MAP = new HashMap<>();
static final Map<String, String> POTION_NAME_MAP = new HashMap<>();
final String[] csgoTextParts = new String[4];
final StringBuilder stringBuilder = new StringBuilder(64);

static {
    // Initialize POTION_ID_MAP
    POTION_ID_MAP.put("1", "Speed");
    POTION_ID_MAP.put("2", "Slowness");
    POTION_ID_MAP.put("3", "Haste");
    POTION_ID_MAP.put("4", "Mining Fatigue");
    POTION_ID_MAP.put("5", "Strength");
    POTION_ID_MAP.put("8", "Jump Boost");
    POTION_ID_MAP.put("9", "Nausea");
    POTION_ID_MAP.put("10", "Regeneration");
    POTION_ID_MAP.put("11", "Resistance");
    POTION_ID_MAP.put("12", "Fire Resistance");
    POTION_ID_MAP.put("13", "Water Breathing");
    POTION_ID_MAP.put("14", "Invisibility");
    POTION_ID_MAP.put("15", "Blindness");
    POTION_ID_MAP.put("16", "Night Vision");
    POTION_ID_MAP.put("17", "Hunger");
    POTION_ID_MAP.put("18", "Weakness");
    POTION_ID_MAP.put("19", "Poison");
    POTION_ID_MAP.put("20", "Wither");
    POTION_ID_MAP.put("21", "Health Boost");
    POTION_ID_MAP.put("22", "Absorption");
    POTION_ID_MAP.put("23", "Saturation");

    // Initialize POTION_NAME_MAP
    POTION_NAME_MAP.put("potion.confusion", "Nausea");
    POTION_NAME_MAP.put("potion.waterBreathing", "Water Breathing");
    POTION_NAME_MAP.put("potion.jump", "Jump Boost");
    POTION_NAME_MAP.put("potion.nightVision", "Night Vision");
    POTION_NAME_MAP.put("potion.fireResistance", "Fire Resistance");
    POTION_NAME_MAP.put("potion.moveSpeed", "Speed");
    POTION_NAME_MAP.put("potion.digSlowDown", "Mining Fatigue");
    POTION_NAME_MAP.put("potion.damageBoost", "Strength");
    POTION_NAME_MAP.put("potion.healthBoost", "Health Boost");
    POTION_NAME_MAP.put("potion.digSpeed", "Haste");
}
