/* Constants */
// Alert dimensions
static final float ALERT_WIDTH = 250;
static final float ICON_SIZE = 20;
static final float ICON_PADDING = 4;
static final float BAR_HEIGHT = 2.5f;
static final float DEFAULT_PADDING = 4;

// Alert types
static final int ALERT_TYPE_DEFAULT = 0;
static final int ALERT_TYPE_WARNING = 1;
static final int ALERT_TYPE_CONFIRMATION = 2;
static final int ALERT_TYPE_ERROR = 3;

// Colors
static final int TEXT_COLOR = 0xFFFFFFFF;
static final int WARNING_COLOR = 0xFFFFAA00;
static final int CONFIRM_COLOR = 0xFF55FF55;
static final int ERROR_COLOR = 0xFFFF5555;

/* State */
// Color system
Color startColor, endColor, staticColor;
int colorMode;
float waveSpeed;

// Display settings
boolean useBlur = true;
float bottomPadding = 15;
float scale = 1;
int[] displaySize;

// Animation settings
float animateIn = 100;
float animateOut = 100;
float animateClick = 25;
float animateHover = 100;

// Mouse tracking
boolean clicking = false;
boolean lastclicking = false;
float[] mousePosition;
float[] lastClickPosition;

// Alert management
List<Map<String, Object>> alerts = new ArrayList<>();
Image warningIcon;
Image confirmationIcon;
Image errorIcon;

/* Initialization */
void onLoad() {
    initializeState();
    registerModules();
    loadResources();
}

void initializeState() {
    displaySize = client.getDisplaySize();
    int[] mp = keybinds.getMousePosition();
    float guiScale = displaySize[2];
    mousePosition = new float[]{ mp[0] / guiScale, displaySize[1] - (mp[1] / guiScale) };
    bridge.remove("clientAlert");
    updateColors();
}

void registerModules() {
    registerThemeSettings();
    registerAlertSettings();
    modules.registerDescription("by @desiyn");
}

void registerThemeSettings() {
    modules.registerDescription("> Theme");
    modules.registerButton("Show Preview", true);
    modules.registerSlider("Color 1 - Red", "", 255, 0, 255, 1);
    modules.registerSlider("Color 1 - Green", "", 0, 0, 255, 1);
    modules.registerSlider("Color 1 - Blue", "", 0, 0, 255, 1);
    modules.registerSlider("Color 2 - Red", "", 255, 0, 255, 1);
    modules.registerSlider("Color 2 - Green", "", 255, 0, 255, 1);
    modules.registerSlider("Color 2 - Blue", "", 255, 0, 255, 1);
    modules.registerSlider("Background Alpha", "", 70, 0, 255, 5);
    modules.registerSlider("Mode", "", 0, new String[]{"Static", util.color("&cR&6a&ei&an&bb&do&5w"), util.color("&4G&cr&5a&bd&3i&9e&1n&1t")});
    modules.registerSlider("Wave Speed", "s", 5, 0.1, 10, 0.1);
}

void registerAlertSettings() {
    modules.registerDescription("> Alerts");
    modules.registerSlider("Scale", "", 1.0, 0.5, 1.5, 0.1);
    modules.registerSlider("Animate In", "ms", 100, 0, 2000, 10);
    modules.registerSlider("Animate Out", "ms", 100, 0, 2000, 10);
    modules.registerSlider("Bottom Padding", "", 15, 5, 50, 1);
    modules.registerButton("Use Blur", false);
    modules.registerButton("Use Alert Colors", true);
}

void loadResources() {
    String baseUrl = "https://raw.githubusercontent.com/desiyn-dev/RavenScripts/main/assets/";
    warningIcon = new Image(baseUrl + "warning.png", true);
    confirmationIcon = new Image(baseUrl + "confirm.png", true);
    errorIcon = new Image(baseUrl + "error.png", true);
}

/* Event Handlers */
void onRenderTick(float partialTicks) {
    if (client.getScreen().equals("GuiRaven")) {
        updateColors();
        if (modules.getButton(scriptName, "Show Preview")) {
            renderColorPreviews();
        }
    }
    renderAlerts(partialTicks);
}

void onPostMotion() {
    updateAlertState();
    processAlerts();
}

/* Color System Methods */
void updateColors() {
    colorMode = (int) modules.getSlider(scriptName, "Mode");
    waveSpeed = (float) modules.getSlider(scriptName, "Wave Speed");
    
    int color1Red = (int) modules.getSlider(scriptName, "Color 1 - Red");
    int color1Green = (int) modules.getSlider(scriptName, "Color 1 - Green");
    int color1Blue = (int) modules.getSlider(scriptName, "Color 1 - Blue");
    int color2Red = (int) modules.getSlider(scriptName, "Color 2 - Red");
    int color2Green = (int) modules.getSlider(scriptName, "Color 2 - Green");
    int color2Blue = (int) modules.getSlider(scriptName, "Color 2 - Blue");

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
    return (time <= 0.5) ? (time * 2) : (2 - time * 2);
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

int getCurrentColor(long index, int alertType) {
    boolean useAlertColors = modules.getButton(scriptName, "Use Alert Colors");
    
    if (useAlertColors) {
        switch(alertType) {
            case ALERT_TYPE_WARNING:
                return WARNING_COLOR;
            case ALERT_TYPE_CONFIRMATION:
                return CONFIRM_COLOR;
            case ALERT_TYPE_ERROR:
                return ERROR_COLOR;
        }
    }
    
    // Use theme colors if alert colors are disabled or for default type
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
    int alpha = (int) modules.getSlider(scriptName, "Background Alpha");
    return (alpha << 24);
}

void renderColorPreviews() {
    if (!client.getScreen().equals("GuiRaven")) return;
    
    int[] displaySize = client.getDisplaySize();
    int screenWidth = displaySize[0];
    int rectY = 20;
    int rectSize = 50;

    render.roundedRect(screenWidth / 2 - rectSize - 10, rectY, 
                      screenWidth / 2 - 10, rectY + rectSize, 
                      10, staticColor.getRGB());

    render.roundedRect(screenWidth / 2 + 10, rectY,
                      screenWidth / 2 + rectSize + 10, rectY + rectSize,
                      10, endColor.getRGB());
}

/* Alert State Management */
void updateAlertState() {
    long now = client.time();
    scale = (float) modules.getSlider(scriptName, "Scale");
    displaySize = client.getDisplaySize();
    animateIn = (float) modules.getSlider(scriptName, "Animate In");
    animateOut = (float) modules.getSlider(scriptName, "Animate Out");
    bottomPadding = (float) modules.getSlider(scriptName, "Bottom Padding");
    useBlur = modules.getButton(scriptName, "Use Blur");
    
    lastclicking = clicking;
    clicking = keybinds.isMouseDown(0);
    
    updateMousePosition();
    processClickState();
}

void updateMousePosition() {
    int[] size = client.getDisplaySize();
    float guiScale = size[2];
    int[] mp = keybinds.getMousePosition();
    mousePosition = new float[]{ mp[0] / guiScale, size[1] - (mp[1] / guiScale) };
    
    if (clicking && !lastclicking) {
        lastClickPosition = new float[]{ mousePosition[0], mousePosition[1] };
    }
}

void processClickState() {
    if (clicking && !lastclicking) {
        lastClickPosition = new float[]{ mousePosition[0], mousePosition[1] };
    }
}

void processAlerts() {
    @SuppressWarnings("unchecked")
    Map<String, Object> alert = (Map<String, Object>) bridge.get("clientAlert");
    if (alert != null) {
        bridge.remove("clientAlert");
        processNewAlert(alert);
    }

    processExistingAlerts();
}

void processNewAlert(Map<String, Object> alert) {
    String title = (String) alert.get("title");
    String message = (String) alert.get("message");
    Long duration = ((Number) alert.get("duration")).longValue();

    if (title != null && message != null && duration != null) {
        String command = (String) alert.getOrDefault("command", "");
        int type = (int) alert.getOrDefault("type", ALERT_TYPE_DEFAULT);
        
        // Remove the auto-bold formatting, just color the title if it doesn't have formatting
        if (!title.startsWith("&")) {
            title = "&f" + title; // Default to white if no color specified
        }
        title = util.color(title);
        
        // Format message
        if (!message.startsWith("&")) {
            message = "&7" + message;
        }
        message = util.color(message);
        
        createAlert(title, message, command, duration, type);
    }
}

void createAlert(String title, String message, String command, long duration, int type) {
    long now = client.time();
    Map<String, Object> alertData = new HashMap<>();
    alertData.put("title", title);
    alertData.put("message", message);
    alertData.put("command", command);
    alertData.put("added", now);
    alertData.put("animationStart", now);
    alertData.put("duration", duration + animateIn);
    alertData.put("originalDuration", duration + animateIn);
    alertData.put("removing", false);
    alertData.put("hoverState", false);
    alertData.put("startHover", -1L);
    alertData.put("stopHover", -1L);
    alertData.put("clicking", false);
    alertData.put("clickStart", -1L);
    alertData.put("type", type);
    alerts.add(alertData);
}

void processExistingAlerts() {
    long now = client.time();
    for (Map<String, Object> alertData : alerts) {
        boolean isHovered = (boolean) alertData.get("hoverState");
        boolean isClicking = (boolean) alertData.get("clicking");
        alertData.put("lastClicking", isClicking);
        
        if (isHovered && clicking && !isClicking) {
            alertData.put("clicking", true);
            alertData.put("clickStart", now);
        } else if (!clicking && isClicking) {
            alertData.put("clicking", false);
            alertData.put("clickStart", now);
        }
    }
}

void processHoverState(Map<String, Object> alertData, boolean empty, float currentX, float currentY, long now) {
    float width = ALERT_WIDTH * scale;
    float height = getAlertHeight();
    
    boolean isHovered = !empty && mousePosition[0] >= currentX && mousePosition[0] <= currentX + width &&
                        mousePosition[1] >= currentY && mousePosition[1] <= currentY + height;

    updateAlertDuration(alertData, isHovered, now);
    updateHoverAnimation(alertData, isHovered, now);
}

void updateAlertDuration(Map<String, Object> alertData, boolean isHovered, long now) {
    long lastCheck = (long) alertData.getOrDefault("lastCheck", now);
    long elapsed = now - lastCheck;
    if (!isHovered) {
        long currentDuration = ((Number) alertData.get("duration")).longValue();
        alertData.put("duration", currentDuration - elapsed);
    }
    alertData.put("lastCheck", now);
}

void updateHoverAnimation(Map<String, Object> alertData, boolean isHovered, long now) {
    if (isHovered && !(boolean) alertData.get("hoverState")) {
        alertData.put("hoverState", true);
        alertData.put("startHover", now);
    } else if (!isHovered && (boolean) alertData.get("hoverState")) {
        alertData.put("hoverState", false);
        alertData.put("stopHover", now);
    }
}

/* Alert Rendering */
void renderAlerts(float partialTicks) {
    if (alerts.isEmpty() || displaySize == null || mousePosition == null) return;

    updateColors();
    boolean empty = client.getScreen().isEmpty();
    long now = client.time();
    float offsetY = 0;
    float maxSlideOffsetY = 0;
    boolean foundRemoving = false;

    for (Iterator<Map<String, Object>> iterator = alerts.iterator(); iterator.hasNext();) {
        Map<String, Object> alertData = iterator.next();
        if (alertData == null) continue;
        
        boolean result = renderAlert(alertData, empty, now, offsetY, maxSlideOffsetY, foundRemoving, iterator);
        if (result) {
            offsetY += getAlertHeight() + (DEFAULT_PADDING * scale);
        }
        if ((boolean) alertData.getOrDefault("removing", false)) {
            foundRemoving = true;
        }
    }
}

boolean renderAlert(Map<String, Object> alertData, boolean empty, long now, float offsetY, float maxSlideOffsetY, boolean foundRemoving, Iterator<Map<String, Object>> iterator) {
    try {
        // Process alert state
        boolean removing = (boolean) alertData.getOrDefault("removing", false);
        long alertDurationMillis = ((Number) alertData.getOrDefault("duration", 0L)).longValue();
        
        if (alertDurationMillis <= 0) {
            if (!removing) {
                alertData.put("removing", true);
                alertData.put("animationStart", now);
            }
            removing = true;
        }

        // Calculate animation progress
        float animationProgress = calculateAnimationProgress(alertData, removing, now);
        if (removing && animationProgress <= 0) {
            iterator.remove();
            return false;
        }

        // Calculate positions
        float[] positions = calculateAlertPositions(animationProgress, offsetY, maxSlideOffsetY, foundRemoving);
        if (positions == null) return false;
        
        float currentX = positions[0];
        float currentY = positions[1];

        // Process hover state
        processHoverState(alertData, empty, currentX, currentY, now);

        // Render alert
        renderAlertBackground(alertData, currentX, currentY);
        renderAlertContent(alertData, currentX, currentY, now);

        return true;
    } catch (Exception e) {
        client.print("Error rendering alert: " + e.toString());
        return false;
    }
}

float calculateAnimationProgress(Map<String, Object> alertData, boolean removing, long now) {
    long animationStart = (long) alertData.get("animationStart");
    if (removing) {
        float progress = (now - animationStart) / animateOut;
        return progress >= 1f ? 0 : (1f - progress);
    }
    return Math.min((now - animationStart) / animateIn, 1f);
}

float[] calculateAlertPositions(float progress, float offsetY, float maxSlideOffsetY, boolean foundRemoving) {
    float scaledPadding = DEFAULT_PADDING * scale;
    float width = ALERT_WIDTH * scale;
    float height = getAlertHeight();
    
    float endX = displaySize[0] - width - scaledPadding;
    float currentX = (displaySize[0] + width) - ((displaySize[0] + width) - endX) * progress;
    float startY = displaySize[1] - height - bottomPadding - offsetY;
    float currentY = startY;

    if (foundRemoving) {
        currentY += maxSlideOffsetY;
    }

    return new float[]{currentX, currentY};
}

float getAlertHeight() {
    float scaledFontHeight = render.getFontHeight() * scale;
    float scaledPadding = DEFAULT_PADDING * scale;
    float contentHeight = (scaledFontHeight * 2) + (scaledPadding * 3); // Two lines of text with padding between
    return contentHeight + (scaledPadding * 2); // Add padding for top and bottom edges
}

void renderAlertBackground(Map<String, Object> alertData, float currentX, float currentY) {
    float width = ALERT_WIDTH * scale;
    float height = getAlertHeight();
    
    if (useBlur) {
        render.blur.prepare();
        render.rect(currentX, currentY, 
                   currentX + width, 
                   currentY + height, -1);
        render.blur.apply(2, 3);
    }
    
    render.rect(currentX, currentY, 
               currentX + width, 
               currentY + height, 
               getBackgroundColor());
}

void renderAlertContent(Map<String, Object> alertData, float currentX, float currentY, long now) {
    float scaledPadding = DEFAULT_PADDING * scale;
    float scaledFontHeight = render.getFontHeight() * scale;
    
    // Start text position with more padding from top
    float textY = currentY + (scaledPadding * 1.5f); // Match AlertsBackend padding
    
    // Render icon if needed
    float textX = currentX + scaledPadding;
    int type = (int) alertData.getOrDefault("type", ALERT_TYPE_DEFAULT);
    if (type != ALERT_TYPE_DEFAULT) {
        textX = renderAlertIcon(alertData, currentX, currentY, textY);
    }

    // Render text with proper spacing
    render.text((String)alertData.get("title"), 
               textX, textY, 
               scale, TEXT_COLOR, true);
    render.text((String)alertData.get("message"), 
               textX, textY + scaledFontHeight + scaledPadding,
               scale, -1, true);

    // Render progress bar
    renderProgressBar(alertData, currentX, currentY);
}

float renderAlertIcon(Map<String, Object> alertData, float currentX, float currentY, float textY) {
    float scaledPadding = DEFAULT_PADDING * scale;
    float iconSize = ICON_SIZE * scale;
    float textX = currentX + scaledPadding + iconSize + (ICON_PADDING * scale);
    
    // Align icon with the text
    float textHeight = (render.getFontHeight() * scale * 2) + (DEFAULT_PADDING * scale);
    float iconY = textY + (textHeight - iconSize) / 2;
    
    Image icon = null;
    switch ((int)alertData.getOrDefault("type", ALERT_TYPE_DEFAULT)) {
        case ALERT_TYPE_WARNING:
            icon = warningIcon;
            break;
        case ALERT_TYPE_CONFIRMATION:
            icon = confirmationIcon;
            break;
        case ALERT_TYPE_ERROR:
            icon = errorIcon;
            break;
    }
    
    if (icon != null) {
        render.image(icon, 
                    currentX + scaledPadding,
                    iconY,
                    iconSize,
                    iconSize);
    }
    
    return textX;
}

void renderProgressBar(Map<String, Object> alertData, float currentX, float currentY) {
    float scaledPadding = DEFAULT_PADDING * scale;
    float width = ALERT_WIDTH * scale;
    float height = getAlertHeight();
    
    float progressBarHeight = BAR_HEIGHT * scale;
    float timeRemaining = Math.max(0, ((Number) alertData.get("duration")).longValue());
    float progressBarWidth = (width - (scaledPadding * 2)) * 
        (timeRemaining / ((Number) alertData.get("originalDuration")).floatValue());

    int type = (int) alertData.getOrDefault("type", ALERT_TYPE_DEFAULT);
    int barColor = getCurrentColor(alerts.indexOf(alertData) * 50L, type);

    float barY = currentY + height - progressBarHeight - scaledPadding;
    render.rect(currentX + scaledPadding, 
               barY,
               currentX + scaledPadding + progressBarWidth,
               barY + progressBarHeight,
               barColor);
}