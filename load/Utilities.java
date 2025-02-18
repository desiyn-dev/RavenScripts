// Alert queue key
static String ALERT_QUEUE_KEY;

// Render state
static final int RENDER_SECONDS = 0;
static final int RENDER_PROGRESS = 1;
static final int RENDER_WIDTH = 2;
static final int RENDER_HEIGHT = 3;
static final float[] renderData = new float[4];
static final Map<String, Object> renderInfo = new HashMap<>();

// Display settings
float padding = 4;
float barHeight = 2;
float displayX, displayY;

// Combat tracking
Entity target;
Entity lastTarget;
Entity player;
String targetName = "";
String lastTargetName = "";
float targetCurrentHealth = 0;
float targetPreviousHealth = 0;
float healthDifference = 0;
int targetCheckCounter = 0;
int targetSentCounter = 0;
int killMessageSent = 1;
int missCheckCounter = 0;
int hurtTimeCounter = 0;

// Lagback state
long lastLagbackTime = 0;
boolean wasBhopEnabled = false;
boolean shouldReEnable = false;
int disableDuration = 3000; // Default 3 seconds

// Module alerts
List<String> enabledModules = new ArrayList<>();
boolean playSounds = false;
int alertDelay = 3000;

/* Initialization */
void onLoad() {
    if (!modules.isEnabled("Client")) {
        modules.enable("Client");
    }
    
    ALERT_QUEUE_KEY = (String) bridge.get("Client.ALERT_QUEUE_KEY");
    if (ALERT_QUEUE_KEY == null) {
        client.print("&cError: Alert system not initialized properly");
        return;
    }
    
    initializeDisplaySettings();
    registerModules();
}

void initializeDisplaySettings() {
    int[] displaySize = client.getDisplaySize();
    String sampleText = "Bhop will be re-enabled in 3.0s";
    float textWidth = render.getFontWidth(sampleText);
    float defaultWidth = textWidth + (padding * 2);
    float defaultHeight = render.getFontHeight() + (padding * 3) + barHeight;
    
    // Round to nearest whole number
    displayX = Math.round((displaySize[0] - defaultWidth) / 2);
    displayY = Math.round(displaySize[1] * 0.833f);
}

void registerModules() {
    modules.registerDescription("> TargetStrafe");
    modules.registerButton("On SPACE key", false);
    modules.registerButton("Require Bhop", false);
    
    modules.registerDescription("> Bhop");
    modules.registerButton("Disable on lagback", true);
    modules.registerSlider("Disable duration", "s", 3, 1, 10, 0.5);
    modules.registerButton("Show HUD Notification", true);
    modules.registerButton("Show Chat Notification", true);
    modules.registerSlider("Display X", "", displayX, 0, client.getDisplaySize()[0], 1);
    modules.registerSlider("Display Y", "", displayY, 0, client.getDisplaySize()[1], 1);
    
    modules.registerDescription("> KillAura");
    modules.registerButton("Enable Aura Hitlog", true);
    
    modules.registerDescription("by @desiyn");
    
    modules.registerDescription("> Module Alerts");
    modules.registerButton("Enable Alerts", true);
    modules.registerButton("Play Sounds", false);
    modules.registerSlider("Alert Duration", "s", 3, 0, 10, 1);
}

/* Event Handlers */
void onPreUpdate() {
    updateColors();
    handleModuleAlerts();
    handleTargetStrafe();
    handleCombat();
}

void onRenderTick(float partialTicks) {
    updateColors();
    
    if (!shouldRenderLagbackNotification()) return;
    
    long timeSinceLagback = client.time() - lastLagbackTime;
    
    if (isLagbackCooldownComplete(timeSinceLagback)) {
        handleLagbackCooldownComplete();
    } else if (modules.getButton(scriptName, "Show HUD Notification")) {
        renderCountdown(timeSinceLagback);
    }
}

boolean onPacketReceived(Object packet) {
    if (packet instanceof S08) {
        S08 s08 = (S08) packet;
        if (s08.pitch != 0) {
            handleLagback();
        }
    }
    return true;
}

/* Rendering */
void renderCountdown(long timeSinceLagback) {
    updateDisplayPosition();
    calculateRenderData(timeSinceLagback);
    renderBackground();
    renderText();
    renderProgressBar();
}

void calculateRenderData(long timeSinceLagback) {
    renderData[RENDER_SECONDS] = (disableDuration - timeSinceLagback) / 1000f;
    renderData[RENDER_PROGRESS] = timeSinceLagback / (float)disableDuration;
    
    renderInfo.put("text", String.format("Bhop will be re-enabled in %.1fs", renderData[RENDER_SECONDS]));
    float textWidth = render.getFontWidth((String)renderInfo.get("text"));
    renderInfo.put("textWidth", textWidth);
    
    renderData[RENDER_WIDTH] = textWidth + (padding * 2);
    renderData[RENDER_HEIGHT] = render.getFontHeight() + barHeight + (padding * 3);
}

void renderBackground() {
    render.rect(displayX, displayY, 
               displayX + renderData[RENDER_WIDTH], 
               displayY + renderData[RENDER_HEIGHT], 
               getBackgroundColor());
}

void renderText() {
    float textX = displayX + padding;
    float textY = displayY + padding;
    render.text((String)renderInfo.get("text"), 
                textX, textY, 1, getCurrentColor(0), true);
}

void renderProgressBar() {
    float textX = displayX + padding;
    float barY = displayY + renderData[RENDER_HEIGHT] - barHeight - padding;
    float barWidth = (float)renderInfo.get("textWidth") * (1 - renderData[RENDER_PROGRESS]);
    render.rect(textX, barY,
               textX + barWidth, barY + barHeight,
               getCurrentColor(0));
}

void updateDisplayPosition() {
    displayX = (float) modules.getSlider(scriptName, "Display X");
    displayY = (float) modules.getSlider(scriptName, "Display Y");
}

/* Combat Handling */
void handleCombat() {
    if (target != null) {
        targetName = target.getDisplayName();
    } else {
        handleTargetLost();
        resetCombatCounters();
    }
    processCombatEvents();
}

void processCombatEvents() {
    if (!shouldProcessCombat()) return;

    double distance = target.getPosition().distanceTo(player.getPosition());
    double swingProgress = player.getSwingProgress();

    handleMissDetection(distance, swingProgress);
    updateCombatState(distance, swingProgress);
    processTargetChange();
    updateHealthTracking();
}

void handleMissDetection(double distance, double swingProgress) {
    if (shouldRegisterMiss(distance, swingProgress)) {
        registerMiss();
    }

    if (target.getHurtTime() != 0) {
        missCheckCounter = 0;
    }
}

void updateCombatState(double distance, double swingProgress) {
    if (lastTarget != target) {
        killMessageSent = 0;
        lastTarget = target;
        lastTargetName = target.getDisplayName();
    }

    targetPreviousHealth = target.getHealth();

    if (targetCheckCounter == 0 && targetSentCounter == 0) {
        targetCurrentHealth = target.getHealth();
        targetCheckCounter = 1;
        targetSentCounter = 1;
    }

    if (targetPreviousHealth < targetCurrentHealth) {
        healthDifference = targetPreviousHealth - targetCurrentHealth;
        targetCurrentHealth = targetPreviousHealth;
        client.print("&7[&dR&7]&7 " + lastTargetName + " &c-" + 
            String.format("%.2f", Math.abs(healthDifference)) + 
            " \u2764 &7(" + String.format("%.2f", targetCurrentHealth) + " \u2764 remaining)");
    }
}

/* Target Management */
void handleTargetStrafe() {
    updateTargetAndPlayer();
    if (player == null) return;

    boolean requireBhop = modules.getButton(scriptName, "Require Bhop");
    
    if (shouldDisableTargetStrafe(requireBhop)) {
        modules.disable("TargetStrafe");
    } else if (target != null) {
        modules.enable("TargetStrafe");
    }
}

void updateTargetAndPlayer() {
    target = modules.getKillAuraTarget();
    player = client.getPlayer();
}

void handleTargetLost() {
    if (lastTarget != null && lastTarget.isDead() && killMessageSent == 0) {
        sendKillMessage();
    }
}

void processTargetChange() {
    if (target != null) {
        targetName = target.getDisplayName();
    } else {
        targetName = "";
    }
}

void updateHealthTracking() {
    targetPreviousHealth = target.getHealth();
}

/* Lagback Handling */
void handleLagback() {
    if (!modules.getButton(scriptName, "Disable on lagback")) return;
    
    if (modules.isEnabled("Bhop")) {
        disableBhopOnLagback();
    } else if (shouldReEnable) {
        extendLagbackTimer();
    }
}

void disableBhopOnLagback() {
    wasBhopEnabled = true;
    shouldReEnable = true;
    modules.disable("Bhop");
    updateLagbackState();
    sendLagbackMessage("&cLagback detected! Disabling Bhop for " + 
                      String.format("%.1f", disableDuration/1000f) + " seconds");
}

void extendLagbackTimer() {
    lastLagbackTime = client.time();
    sendLagbackMessage("&cLagback detected during cooldown! Extending timer");
}

void updateLagbackState() {
    lastLagbackTime = client.time();
    disableDuration = (int)(modules.getSlider(scriptName, "Disable duration") * 1000);
}

void handleLagbackCooldownComplete() {
    if (!modules.isEnabled("Bhop")) {
        modules.enable("Bhop");
        if (modules.getButton(scriptName, "Show Chat Notification")) {
            client.print("&7[&dR&7] &aRe-enabling Bhop after cooldown");
        }
    }
    resetLagbackState();
}

/* Utility Methods */
boolean shouldProcessCombat() {
    return modules.getButton(scriptName, "Show Aura Hitlog") && 
           target != null && 
           player != null;
}

boolean shouldRegisterMiss(double distance, double swingProgress) {
    return swingProgress != 0 && 
           targetPreviousHealth == targetCurrentHealth && 
           target.getHurtTime() == 0 && 
           distance < 3 && 
           missCheckCounter == 0 && 
           hurtTimeCounter >= 50;
}

boolean shouldDisableTargetStrafe(boolean requireBhop) {
    return !modules.isEnabled("KillAura") || 
           !keybinds.isKeyDown(57) ||  // Space key
           (requireBhop && !modules.isEnabled("Bhop"));
}

boolean shouldRenderLagbackNotification() {
    return lastLagbackTime != 0 && shouldReEnable;
}

boolean isLagbackCooldownComplete(long timeSinceLagback) {
    return timeSinceLagback > disableDuration;
}

void resetLagbackState() {
    lastLagbackTime = 0;
    shouldReEnable = false;
    wasBhopEnabled = false;
}

void resetCombatCounters() {
    targetCheckCounter = 0;
    targetSentCounter = 0;
}

void registerMiss() {
    missCheckCounter = 1;
    hurtTimeCounter = 0;
    client.print("&7[&dR&7]&c Missed swing on " + lastTargetName + "&c due to &ccorrection.");
}

void sendKillMessage() {
    client.print("&7[&dR&7] " + lastTargetName + "&7 killed.");
    killMessageSent = 1;
}

void sendLagbackMessage(String message) {
    if (modules.getButton(scriptName, "Show Chat Notification")) {
        client.print("&7[&dR&7] " + message);
    }
}

void handleModuleAlerts() {
    if (!modules.getButton(scriptName, "Enable Alerts")) return;
    if (!modules.isEnabled("Client")) {
        modules.enable("Client");
    }
    
    playSounds = modules.getButton(scriptName, "Play Sounds");
    alertDelay = (int)(modules.getSlider(scriptName, "Alert Duration") * 1000);
    
    if (!bridge.has("clientAlert")) {
        checkModuleStates();
    }
}

void checkModuleStates() {
    for (String moduleName : TRACKED_MODULES) {
        boolean isEnabled = modules.isEnabled(moduleName);
        boolean wasEnabled = enabledModules.contains(moduleName);
        
        if (isEnabled && !wasEnabled) {
            enabledModules.add(moduleName);
            if (playSounds) client.ping();
            sendModuleAlert("Module Enabled", moduleName, true);
        } else if (!isEnabled && wasEnabled) {
            enabledModules.remove(moduleName);
            if (playSounds) client.ping();
            sendModuleAlert("Module Disabled", moduleName, false);
        }
    }
}

void sendModuleAlert(String title, String message, boolean enabled) {
    Map<String, Object> alert = new HashMap<>();
    alert.put("title", title);
    alert.put("message", message);
    alert.put("duration", alertDelay);
    alert.put("type", enabled ? 2 : 3);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> queue = (List<Map<String, Object>>) bridge.get(ALERT_QUEUE_KEY);
    if (queue == null) {
        queue = new ArrayList<>();
    }
    queue.add(alert);
    bridge.add(ALERT_QUEUE_KEY, queue);
} 