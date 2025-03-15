// Add modules to exclude
static final List<String> EXCLUDED_MODULES = Arrays.asList(
    "ArrayList", "Example 1", "Example 2"
);

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
final List<Map<String, Object>> mods = new ArrayList<>();
final Map<String, Boolean> excludedCategories = new HashMap<>();
Map<String, Map<String, Object>> suffixConfigs = new HashMap<>();
Map<String, String> moduleSuffixes = new HashMap<>();
Map<String, String> suffixCache = new HashMap<>();
Map<String, String> displayTextCache = new HashMap<>();

/* Settings */
int direction;
int animationMode;
int outlineMode;
int alignmentMode;
float gap = 1;
float textScale;
float xPosition;
float yPosition;
float moduleHeight;
int animationDuration;
boolean lowercase;
boolean showBackground = true;
int resetTicks = 0;

final float LINE_GAP = 1;

void onLoad() {
    modules.registerDescription("> Categories");
    for (String category : modules.getCategories().keySet()) {
        if (!category.equalsIgnoreCase("profiles")) {
            excludedCategories.put(category, false);
            modules.registerButton("Show " + category, true);
        }
    }
    
    modules.registerDescription("> Settings");
    modules.registerSlider("Direction", "", 1, new String[]{"Up", "Down"});
    modules.registerSlider("Animations", "", 0, new String[]{"Scale Right", "Scale Center", "Scale Left"});
    modules.registerSlider("Alignment", "", 0, new String[]{"Right", "Left"});
    modules.registerSlider("Animation Speed", "ms", 100, 0, 2000, 10);
    modules.registerButton("Lowercase", false);
    modules.registerButton("Show Background", true);
    modules.registerSlider("Scale", "", 1, 0.5, 2, 0.1);
    
    int[] displaySize = client.getDisplaySize();
    modules.registerSlider("X Offset", "px", 5, 0, displaySize[0], 1);
    modules.registerSlider("Y Offset", "px", 5, 0, displaySize[1], 1);
    modules.registerSlider("Outline Mode", "", 0, new String[]{"Disabled", "Left", "Right", "Full"});
    
    configureSuffixFromArray("KillAura", "Autoblock", new String[]{"Manual", "Vanilla", "Damage", "Fake", "Partial", "Swap", "Interact A", "Interact B", "Interact C"});
    configureSuffixFromSliders("Velocity", "%h% %v%", new String[]{"Horizontal", "Vertical"});
    configureSuffixFromSliders("AntiKnockback", "%h% %v%", new String[]{"Horizontal", "Vertical"});
    configureSuffixFromArray("Bhop", "Mode", new String[]{"Strafe", "Ground", "Glide", "7 tick", "8 tick strafe", "9 tick", "9 tick strafe", "Hurt time"});
    configureSuffixFromArray("NoFall", "Mode", new String[]{"Spoof", "Single", "Extra", "NoGround A", "NoGround B", "Precision", "Position"});
    configureStaticSuffix("AntiVoid", "Blink");
    
    modules.registerDescription("by @desiyn");
}

void configureSuffixFromArray(String moduleName, String settingName, String[] values) {
    Map<String, Object> config = new HashMap<>();
    config.put("type", "array");
    config.put("setting", settingName);
    config.put("values", values);
    suffixConfigs.put(moduleName, config);
}

void configureSuffixFromSlider(String moduleName, String format, String sliderName) {
    Map<String, Object> config = new HashMap<>();
    config.put("type", "slider");
    config.put("format", format);
    config.put("slider", sliderName);
    suffixConfigs.put(moduleName, config);
}

void configureSuffixFromSliders(String moduleName, String format, String[] sliderNames) {
    Map<String, Object> config = new HashMap<>();
    config.put("type", "sliders");
    config.put("format", format);
    config.put("sliders", sliderNames);
    suffixConfigs.put(moduleName, config);
}

void configureStaticSuffix(String moduleName, String suffix) {
    Map<String, Object> config = new HashMap<>();
    config.put("type", "static");
    config.put("suffix", suffix);
    suffixConfigs.put(moduleName, config);
}

String calculateSuffix(String moduleName) {
    if (suffixCache.containsKey(moduleName)) {
        return suffixCache.get(moduleName);
    }
    
    if (!suffixConfigs.containsKey(moduleName)) {
        return "";
    }
    
    Map<String, Object> config = suffixConfigs.get(moduleName);
    String type = (String) config.get("type");
    String suffix = "";
    
    switch (type) {
        case "array":
            String setting = (String) config.get("setting");
            String[] values = (String[]) config.get("values");
            int index = (int) modules.getSlider(moduleName, setting);
            if (index >= 0 && index < values.length) {
                suffix = values[index];
            }
            break;
            
        case "slider":
            String format = (String) config.get("format");
            String sliderName = (String) config.get("slider");
            double value = modules.getSlider(moduleName, sliderName);
            suffix = format.replace("%v", formatValue(value));
            break;
            
        case "sliders":
            format = (String) config.get("format");
            String[] sliders = (String[]) config.get("sliders");
            for (int i = 0; i < sliders.length; i++) {
                String placeholder = "";
                switch (i) {
                    case 0: placeholder = "%h"; break;
                    case 1: placeholder = "%v"; break;
                    default: placeholder = "%" + i;
                }
                value = modules.getSlider(moduleName, sliders[i]);
                format = format.replace(placeholder, formatValue(value));
            }
            suffix = format;
            break;
            
        case "static":
            suffix = (String) config.get("suffix");
            break;
    }
    
    return suffix;
}

String formatValue(double value) {
    if (value == (int) value) {
        return String.valueOf((int) value);
    } else {
        return String.valueOf(value);
    }
}

void clearModuleFromCache(String moduleName) {
    displayTextCache.remove(moduleName + "_0_U");
    displayTextCache.remove(moduleName + "_0_L");
    displayTextCache.remove(moduleName + "_1_U");
    displayTextCache.remove(moduleName + "_1_L");
}

String getDisplayText(String moduleName, boolean showSuffixes) {
    String cacheKey = moduleName + "_" + (showSuffixes ? "1" : "0") + "_" + (lowercase ? "L" : "U");
    
    String cachedText = displayTextCache.get(cacheKey);
    if (cachedText != null) {
        return cachedText;
    }
    
    String suffix = "";
    if (showSuffixes) {
        if (moduleSuffixes.containsKey(moduleName)) {
            suffix = moduleSuffixes.get(moduleName);
        } 
        else if (suffixCache.containsKey(moduleName)) {
            suffix = suffixCache.get(moduleName);
        }
    }
    
    String displayText = moduleName;
    
    if (!suffix.isEmpty()) {
        displayText += " " + util.colorSymbol + "7" + suffix;
    }
    
    if (lowercase) {
        displayText = displayText.toLowerCase();
    }
    
    displayTextCache.put(cacheKey, displayText);
    return displayText;
}

void onEnable() {
    mods.clear();
    resetTicks = 0;
    displayTextCache.clear();
    suffixCache.clear();
    moduleSuffixes.clear();
    
    Map<String, List<String>> categories = modules.getCategories();
    
    for (String category : categories.keySet()) {
        if (category.equalsIgnoreCase("profiles")) continue;
        
        boolean categoryEnabled = modules.getButton(scriptName, "Show " + category);
        excludedCategories.put(category, !categoryEnabled);
        
        if (!categoryEnabled) continue;
        
        List<String> modulesList = categories.get(category);
        for (String module : modulesList) {
            if (EXCLUDED_MODULES.contains(module)) continue;
            
            Map<String, Object> modData = new HashMap<>();
            modData.put("name", module);
            modData.put("visibility", false);
            modData.put("offset", 0);
            modData.put("scale", 0);
            modData.put("animating", false);
            modData.put("animatingUp", false);
            modData.put("animationStart", 0L);
            modData.put("animationProgress", 0);
            mods.add(modData);
            
            if (suffixConfigs.containsKey(module)) {
                String suffix = calculateSuffix(module);
                if (!suffix.isEmpty()) {
                    suffixCache.put(module, suffix);
                }
            }
        }
    }

    updateButtonStates();
    updateSliders();
    updateEnabledModules();
    updateSuffixes();
    sortModules();
}

void onPreUpdate() {
    resetTicks++;
    
    boolean needsRebuild = false;
    for (String category : excludedCategories.keySet()) {
        boolean currentSetting = !modules.getButton(scriptName, "Show " + category);
        if (currentSetting != excludedCategories.get(category)) {
            needsRebuild = true;
            break;
        }
    }
    
    if (needsRebuild) {
        onEnable();
        return;
    }
    
    boolean newLowercase = modules.getButton(scriptName, "Lowercase");
    if (lowercase != newLowercase) {
        lowercase = newLowercase;
        displayTextCache.clear();
        sortModules();
    }
    
    updateEnabledModules();
    moduleHeight = (float) render.getFontHeight() + gap;
    
    xPosition = (float) modules.getSlider(scriptName, "X Offset");
    yPosition = (float) modules.getSlider(scriptName, "Y Offset");

    if (resetTicks == 1 || resetTicks % 5 == 0) {
        updateSliders();
        updateSuffixes();
    }
}

void updateSliders() {
    lowercase = modules.getButton(scriptName, "Lowercase");
    showBackground = modules.getButton(scriptName, "Show Background");
    direction = (int) modules.getSlider(scriptName, "Direction");
    textScale = (float) modules.getSlider(scriptName, "Scale");
    animationDuration = (int) modules.getSlider(scriptName, "Animation Speed");
    animationMode = (int) modules.getSlider(scriptName, "Animations");
    outlineMode = (int) modules.getSlider(scriptName, "Outline Mode");
    alignmentMode = (int) modules.getSlider(scriptName, "Alignment");
    xPosition = (float) modules.getSlider(scriptName, "X Offset");
    yPosition = (float) modules.getSlider(scriptName, "Y Offset");

    updateColors();
    sortModules();
}

void updateSuffixes() {
    boolean suffixesChanged = false;
    
    Map<String, String> newSuffixes = new HashMap<>();
    
    for (String moduleName : suffixConfigs.keySet()) {
        boolean moduleEnabled = false;
        for (Map<String, Object> mod : mods) {
            if (mod.get("name").equals(moduleName) && (boolean) mod.get("visibility")) {
                moduleEnabled = true;
                break;
            }
        }
        
        String suffix = calculateSuffix(moduleName);
        
        if (!suffix.isEmpty()) {
            newSuffixes.put(moduleName, suffix);
        }
        
        String currentSuffix = moduleSuffixes.get(moduleName);
        if ((currentSuffix == null && !suffix.isEmpty()) || 
            (currentSuffix != null && !currentSuffix.equals(suffix))) {
            suffixesChanged = true;
        }
        
        if (!suffix.isEmpty()) {
            suffixCache.put(moduleName, suffix);
        }
    }
    
    if (suffixesChanged) {
        for (String moduleName : suffixConfigs.keySet()) {
            String oldSuffix = moduleSuffixes.get(moduleName);
            String newSuffix = newSuffixes.get(moduleName);
            
            if ((oldSuffix == null && newSuffix != null) ||
                (oldSuffix != null && newSuffix == null) ||
                (oldSuffix != null && newSuffix != null && !oldSuffix.equals(newSuffix))) {
                clearModuleFromCache(moduleName);
            }
        }
        
        moduleSuffixes.clear();
        moduleSuffixes.putAll(newSuffixes);
        
        sortModules();
    }
}

void onRenderTick(float partialTicks) {
    int[] displaySize = client.getDisplaySize();
    float y = yPosition;
    
    List<Map<String, Object>> visibleMods = new ArrayList<>();
    List<Float> moduleY1Positions = new ArrayList<>();
    List<Float> moduleY2Positions = new ArrayList<>();
    List<Float> moduleX1Positions = new ArrayList<>();
    List<Float> moduleX2Positions = new ArrayList<>();
    List<Integer> moduleColors = new ArrayList<>();

    updateAnimations();

    long index = 0;
    for (Map<String, Object> mod : mods) {
        boolean animating = (boolean) mod.get("animating");
        if (!(boolean) mod.get("visibility") && !animating) {
            continue;
        }

        String moduleName = (String) mod.get("name");
        
        String textToDisplay = getDisplayText(moduleName, true);

        float scale = (float) mod.get("scale") * textScale;
        float textWidth = (float) render.getFontWidth(util.strip(textToDisplay)) * textScale;
        float scaledTextWidth = textWidth * scale;
        float finalXPosition;

        boolean isLeftAligned = alignmentMode == 1;
        
        if (isLeftAligned) {
            switch (animationMode) {
                case 2: // Scale Left
                    finalXPosition = xPosition;
                    break;
                case 1: // Scale Center
                    finalXPosition = xPosition + (textWidth / 2f) - ((textWidth * scale) / (2f * textScale));
                    break;
                default: // Scale Right
                    finalXPosition = xPosition + textWidth - (scaledTextWidth / textScale);
                    break;
            }
        } else {
            switch (animationMode) {
                case 2: // Scale Left
                    finalXPosition = displaySize[0] - xPosition - textWidth + (textWidth - scaledTextWidth) / textScale;
                    break;
                case 1: // Scale Center
                    finalXPosition = displaySize[0] - xPosition - (textWidth / 2f) - ((textWidth * scale) / (2f * textScale));
                    break;
                default: // Scale Right
                    finalXPosition = displaySize[0] - xPosition - (scaledTextWidth / textScale);
                    break;
            }
        }

        float outlineThickness = textScale;
        float sidePadding = LINE_GAP * textScale + outlineThickness;
        float verticalPadding = sidePadding / 2;

        float x1 = finalXPosition - scale;
        float y1 = y;
        float x2 = finalXPosition + (textWidth / textScale) * scale + scale;
        float y2 = y + render.getFontHeight() * scale + scale;

        float paddedX1 = x1 - sidePadding;
        float paddedY1 = y1 - verticalPadding;
        float paddedX2 = x2 + sidePadding;
        float paddedY2 = y2 + verticalPadding;

        visibleMods.add(mod);
        moduleY1Positions.add(paddedY1);
        moduleY2Positions.add(paddedY2);
        moduleX1Positions.add(paddedX1);
        moduleX2Positions.add(paddedX2);
        moduleColors.add(getCurrentColor(index));

        if (showBackground) {
            render.rect(paddedX1, paddedY1, paddedX2, paddedY2, getBackgroundColor());
        }

        int color = getCurrentColor(index);
        
        render.text(textToDisplay, finalXPosition, y1 + scale, scale, color, true);

        if (outlineMode > 0) {
            if (outlineMode == 1 || outlineMode == 3) {
                render.rect(paddedX1, paddedY1, paddedX1 + outlineThickness, paddedY2, color);
            }
            
            if (outlineMode == 2 || outlineMode == 3) {
                render.rect(paddedX2 - outlineThickness, paddedY1, paddedX2, paddedY2, color);
            }
        }

        y = paddedY2 + (direction == 0 ? 0 : verticalPadding);
        index += (direction == 0) ? 100 * scale : -100 * scale;
    }
    
    if (visibleMods.size() > 0 && outlineMode > 0) {
        boolean isLeftAligned = alignmentMode == 1;
        float outlineThickness = textScale;
        
        // Draw connecting lines between modules
        for (int i = 0; i < visibleMods.size() - 1; i++) {
            float currentY2 = moduleY2Positions.get(i);
            float nextY1 = moduleY1Positions.get(i + 1);
            int color = moduleColors.get(i);
            
            // For right-aligned modules with left outline
            if (!isLeftAligned && (outlineMode == 1 || outlineMode == 3)) {
                float leftX = moduleX1Positions.get(i);
                float nextLeftX = moduleX1Positions.get(i + 1);
                
                // Draw vertical connecting line
                render.rect(leftX, currentY2 - outlineThickness, leftX + outlineThickness, nextY1, color);
                
                if (nextLeftX > leftX) {
                    // Draw horizontal line at the top of the next module
                    render.rect(leftX, nextY1, nextLeftX, nextY1 + outlineThickness, color);
                }
            }
            
            // For left-aligned modules with right outline
            if (isLeftAligned && (outlineMode == 2 || outlineMode == 3)) {
                float rightX = moduleX2Positions.get(i) - outlineThickness;
                float nextRightX = moduleX2Positions.get(i + 1) - outlineThickness;
                
                // Draw vertical connecting line
                render.rect(rightX, currentY2 - outlineThickness, rightX + outlineThickness, nextY1, color);
                
                if (nextRightX < rightX) {
                    // Draw horizontal line at the top of the next module
                    render.rect(nextRightX, nextY1, rightX, nextY1 + outlineThickness, color);
                }
            }
        }
        
        // Add enclosing lines for full outline mode
        if (outlineMode == 3 && visibleMods.size() > 0) {
            // Get first and last module positions
            float firstY1 = moduleY1Positions.get(0);
            float lastY2 = moduleY2Positions.get(visibleMods.size() - 1);
            int firstColor = moduleColors.get(0);
            int lastColor = moduleColors.get(visibleMods.size() - 1);
            
            if (!isLeftAligned) {
                float leftX = moduleX1Positions.get(0);
                float rightX = moduleX2Positions.get(0) - outlineThickness;
                float lastLeftX = moduleX1Positions.get(visibleMods.size() - 1);
                float lastRightX = moduleX2Positions.get(visibleMods.size() - 1) - outlineThickness;
                
                // Top line
                render.rect(leftX + outlineThickness, firstY1, rightX, firstY1 + outlineThickness, firstColor);
                
                // Bottom line
                render.rect(lastLeftX + outlineThickness, lastY2 - outlineThickness, lastRightX, lastY2, lastColor);
            } else {
                float leftX = moduleX1Positions.get(0);
                float rightX = moduleX2Positions.get(0) - outlineThickness;
                float lastLeftX = moduleX1Positions.get(visibleMods.size() - 1);
                float lastRightX = moduleX2Positions.get(visibleMods.size() - 1) - outlineThickness;
                
                // Top line
                render.rect(leftX, firstY1, rightX, firstY1 + outlineThickness, firstColor);
                
                // Bottom line
                render.rect(lastLeftX, lastY2 - outlineThickness, lastRightX, lastY2, lastColor);
            }
        }
    }
}

void updateEnabledModules() {
    long now = client.time();
    List<String> previousEnabledModules = new ArrayList<>();

    if (resetTicks < 60 || resetTicks % 20 == 0) {
        updateButtonStates();
    }

    for (Map<String, Object> mod : mods) {
        String moduleName = (String) mod.get("name");
        boolean currentlyVisible = (boolean) mod.get("visibility");
        boolean shouldBeVisible = (boolean) mod.getOrDefault("buttonEnabled", false) && modules.isEnabled(moduleName);

        if (currentlyVisible) {
            previousEnabledModules.add(moduleName);
        }

        if (shouldBeVisible != currentlyVisible) {
            mod.put("visibility", shouldBeVisible);
            mod.put("animating", true);
            mod.put("animatingUp", !shouldBeVisible);

            float animationProgress = ((Number) mod.get("animationProgress")).floatValue();
            animationProgress = (animationProgress >= 1f) ? 0f : (animationProgress > 0f ? 1f - animationProgress : animationProgress);

            long adjustedStartTime = now - (long) (animationDuration * animationProgress);
            mod.put("animationStart", adjustedStartTime);
            
            clearModuleFromCache(moduleName);
        }
    }
}

void updateAnimations() {
    long currentTime = client.time();

    for (Map<String, Object> mod : mods) {
        if ((boolean) mod.get("animating")) {
            long startTime = (long) mod.get("animationStart");
            float elapsed = (float) (currentTime - startTime) / (float) animationDuration;

            if (elapsed >= 1f) {
                elapsed = 1f;
                mod.put("animating", false);
            }

            mod.put("animationProgress", elapsed);

            float easedOffset = quadInOut(elapsed) * moduleHeight;
            float easedScale = quadInOut(elapsed);
            if ((boolean) mod.get("animatingUp")) {
                mod.put("offset", moduleHeight - easedOffset);
                mod.put("scale", 1f - easedScale);
            } else {
                mod.put("offset", easedOffset);
                mod.put("scale", easedScale);
            }
        }
    }
}

float quadInOut(float t) {
    if (t < 0.5f) {
        return 2 * t * t;
    } else {
        return -1 + (4 - 2 * t) * t;
    }
}

void updateButtonStates() {
    for (Map<String, Object> mod : mods) {
        String moduleName = (String) mod.get("name");
        boolean isButtonEnabled = !modules.isHidden(moduleName);
        mod.put("buttonEnabled", isButtonEnabled);
    }

    sortModules();
}

void sortModules() {
    mods.sort((a, b) -> {
        String aName = (String) a.get("name");
        String bName = (String) b.get("name");
        
        String aDisplayText = getDisplayText(aName, true);
        String bDisplayText = getDisplayText(bName, true);
        
        String aStripped = util.strip(aDisplayText);
        String bStripped = util.strip(bDisplayText);
        
        int aWidth = render.getFontWidth(aStripped);
        int bWidth = render.getFontWidth(bStripped);
        
        return Integer.compare(bWidth, aWidth);
    });
}

String formatDoubleStr(double val) {
    return val == (long) val ? Long.toString((long) val) : Double.toString(val);
}