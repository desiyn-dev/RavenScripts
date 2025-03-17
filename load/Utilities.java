/* Render Data and Alert System */
static final Map<String, Object> renderInfo = new HashMap<>();
static String ALERT_QUEUE_KEY;
static final String ALERT_TITLE_MODULES = "Module Alerts";
static final String ALERT_TITLE_QUEUE = "Queue Commands";
static final String ALERT_TITLE_LAGBACK = "Lagback Protection";
static final String ALERT_TITLE_ANTICHEAT = "Hacker Detector";
static final String CHAT_PREFIX = "&7[&dR&7] ";

/* State */
float padding = 4;
float barHeight = 2;
float displayX, displayY;

/* Combat Tracking */
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

/* Lagback Handling */
long lastLagbackTime = 0;
boolean wasBhopEnabled = false;
boolean shouldReEnable = false;
int disableDuration = 3000;

/* Module Tracking */
List<String> enabledModules = new ArrayList<>();
List<String> pendingEnables = new ArrayList<>();
List<String> pendingDisables = new ArrayList<>();
Map<String, Boolean> excludedCategories = new HashMap<>();
long lastModuleChange = 0;
static final long BATCH_DELAY = 50;
boolean playSounds = false;
int alertDelay = 3000;
int abStage = 0;
String gameMode = "";
boolean checkMode = false, scoreboard = false;
int checkModeTicks = 0;
int previousTheme = -1;

/* Vertical Tower */
boolean towerEdge = false, towerOver = false, towerUnder = false, towerGroundCheck = false, towerDamage = false;
int towerStage = 0, towerDelay = 0, towerDamageticks = 0;
double towerFirstX = 0;
C08 towerQueued = null;
final boolean towerAlignGround = true, towerAlignPlaceSecond = true, towerDisableHurt = true;
final int towerAlignTicks = 3, towerDisableHurtTicks = 9;
final double edgingMotion = 0.24;

/* Queue System */
static final Map<String, String> QUEUE_COMMANDS = new HashMap<>();
static final Map<String, String> GAME_NAMES = new HashMap<>();
String lastQueuedGame = "";

/* AntiCheat */
static final String ANTICHEAT_FORMAT = "{entity} has been detected for {flag}.";

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
    initializeQueueCommands();
    registerModules();
}

void initializeDisplaySettings() {
    int[] displaySize = client.getDisplaySize();
    String sampleText = "Bhop will be re-enabled in 3.0s";
    float textWidth = render.getFontWidth(sampleText);
    float defaultWidth = textWidth + (padding * 2);
    float defaultHeight = render.getFontHeight() + (padding * 3) + barHeight;
    
    displayX = Math.round((displaySize[0] - defaultWidth) / 2);
    displayY = Math.round(displaySize[1] * 0.833f);
}

void registerModules() {
    modules.registerDescription("> AntiCheat Alerts");
    modules.registerButton("Enable AntiCheat Alerts", true);
    modules.registerSlider("AC Alert Duration", "s", 5, 3, 10, 1);
    
    modules.registerDescription("> Bhop");
    modules.registerButton("Disable on lagback", true);
    modules.registerSlider("Disable duration", "s", 3, 1, 10, 0.5);
    modules.registerButton("Show HUD Notification", true);
    modules.registerButton("Show Chat Notification", true);
    modules.registerButton("Show Lagback Alerts", true);
    modules.registerButton("Play Lagback Sounds", false);
    modules.registerSlider("Display X", "", displayX, 0, client.getDisplaySize()[0], 1);
    modules.registerSlider("Display Y", "", displayY, 0, client.getDisplaySize()[1], 1);
    
    modules.registerDescription("> TargetStrafe");
    modules.registerButton("On SPACE key", false);
    modules.registerButton("Require Bhop", false);
    
    modules.registerDescription("> KillAura");
    modules.registerButton("Enable Aura Hitlog", true);
    modules.registerButton("Legit autoblock", false);
    
    modules.registerDescription("> Scaffold");
    modules.registerButton("Vertical tower", false);

    modules.registerDescription("> Queue Commands");
    modules.registerButton("Enable Queue Commands", true);
    modules.registerButton("Show Queue Alerts", true);
    
    modules.registerDescription("> Other");
    modules.registerButton("AntiDebuff", false);
    modules.registerButton("Theme sync", false);

    modules.registerDescription("> Module Alerts");
    modules.registerButton("Enable Module Alerts", true);
    modules.registerButton("Play Alert Sounds", false);
    modules.registerSlider("Alert Duration", "s", 3, 0, 10, 1);
    
    modules.registerDescription("Categories to Alert");
    for (String category : modules.getCategories().keySet()) {
        if (!category.equalsIgnoreCase("profiles")) {
            excludedCategories.put(category, false);
            modules.registerButton("Alert " + category + " modules", true);
        }
    }

    modules.registerDescription("by @desiyn");
}

/* Event Handlers */
void onPreUpdate() {
    updateColors();
    handleModuleAlerts();
    handleTargetStrafe();
    handleCombat();
    
    if (modules.getButton(scriptName, "AntiDebuff")) {
        antiDebuff();
    }
    if (modules.getButton(scriptName, "Legit autoblock")) {
        legitAB();
    }
    if (modules.getButton(scriptName, "Theme sync")) {
        themeSync();
    }
    getGameMode();
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

void onPreMotion(PlayerState state) {
    Entity player = client.getPlayer();
    if (modules.getButton(scriptName, "Vertical tower")) {
        verticalTower(player, state);
    }
}

void onPostPlayerInput() {
    if (modules.getButton(scriptName, "Vertical tower")) {
        handleVerticalTowerInput();
    }
}

boolean onChat(String msg) {
    msg = util.strip(msg);
    if (checkMode && msg.startsWith("{")) {
         return parseGameMode(msg);
    }
    return true;
}

/* Rendering */
void renderCountdown(long timeSinceLagback) {
    updateDisplayPosition();
    
    float seconds = (disableDuration - timeSinceLagback) / 1000f;
    float progress = timeSinceLagback / (float)disableDuration;
    String text = String.format("Bhop will be re-enabled in %.1fs", seconds);
    float textWidth = render.getFontWidth(text);
    float height = render.getFontHeight() + barHeight + (padding * 3);
    float width = textWidth + (padding * 2);
    
    renderInfo.put("text", text);
    renderInfo.put("textWidth", textWidth);
    renderInfo.put("progress", progress);
    renderInfo.put("width", width);
    renderInfo.put("height", height);
    
    renderBackground();
    renderText();
    renderProgressBar();
}

void renderBackground() {
    float width = (float)renderInfo.get("width");
    float height = (float)renderInfo.get("height");
    
    render.rect(displayX, displayY, 
               displayX + width, 
               displayY + height, 
               getBackgroundColor());
}

void renderText() {
    String text = (String)renderInfo.get("text");
    float textX = displayX + padding;
    float textY = displayY + padding;
    
    render.text(text, textX, textY, 1, getCurrentColor(0), true);
}

void renderProgressBar() {
    float progress = (float)renderInfo.get("progress");
    float textWidth = (float)renderInfo.get("textWidth");
    float height = (float)renderInfo.get("height");
    
    float textX = displayX + padding;
    float barY = displayY + height - barHeight - padding;
    float barWidth = textWidth * (1 - progress);
    
    render.rect(textX, barY,
               textX + barWidth, 
               barY + barHeight,
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

boolean shouldPlayLagbackSound() {
    boolean moduleAlertsEnabled = modules.getButton(scriptName, "Enable Module Alerts") && 
                                 modules.getButton(scriptName, "Play Alert Sounds") &&
                                 modules.getButton(scriptName, "Alert Combat Modules");
                                   
    return modules.getButton(scriptName, "Play Lagback Sounds") && !moduleAlertsEnabled;
}

void disableBhopOnLagback() {
    wasBhopEnabled = true;
    shouldReEnable = true;
    modules.disable("Bhop");
    updateLagbackState();
    
    if (modules.getButton(scriptName, "Show Chat Notification")) {
        client.print(CHAT_PREFIX + "&cLagback detected! Disabling Bhop for " + 
                    String.format("%.1f", disableDuration/1000f) + " seconds.");
    }
    
    if (shouldPlayLagbackSound()) {
        client.ping();
    }
    
    if (modules.getButton(scriptName, "Show Lagback Alerts")) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", ALERT_TITLE_LAGBACK);
        alert.put("message", "Disabled Bhop for " + String.format("%.1f", disableDuration/1000f) + "s");
        alert.put("duration", 3000);
        alert.put("type", 1);
        bridge.add("clientAlert", alert);
    }
}

void extendLagbackTimer() {
    lastLagbackTime = client.time();
    
    if (modules.getButton(scriptName, "Show Chat Notification")) {
        client.print(CHAT_PREFIX + "&cLagback detected during cooldown! Timer extended.");
    }
    
    if (modules.getButton(scriptName, "Play Lagback Sounds")) {
        client.ping();
    }
    
    if (modules.getButton(scriptName, "Show Lagback Alerts")) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", ALERT_TITLE_LAGBACK);
        alert.put("message", "Extended cooldown timer");
        alert.put("duration", 3000);
        alert.put("type", 1);
        bridge.add("clientAlert", alert);
    }
}

void updateLagbackState() {
    lastLagbackTime = client.time();
    disableDuration = (int)(modules.getSlider(scriptName, "Disable duration") * 1000);
}

void handleLagbackCooldownComplete() {
    if (!modules.isEnabled("Bhop")) {
        modules.enable("Bhop");
        
        if (modules.getButton(scriptName, "Show Chat Notification")) {
            client.print(CHAT_PREFIX + "&aRe-enabling Bhop after cooldown.");
        }
        
        if (shouldPlayLagbackSound()) {
            client.ping();
        }
        
        if (modules.getButton(scriptName, "Show Lagback Alerts")) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("title", ALERT_TITLE_LAGBACK);
            alert.put("message", "Re-enabled Bhop");
            alert.put("duration", 3000);
            alert.put("type", 2);
            bridge.add("clientAlert", alert);
        }
    }
    resetLagbackState();
}

/* Utility Methods */
boolean shouldProcessCombat() {
    return modules.getButton(scriptName, "Enable Aura Hitlog") && 
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
    client.print(CHAT_PREFIX + "&cMissed swing on " + lastTargetName + " due to correction!");
}

void sendKillMessage() {
    client.print(CHAT_PREFIX + lastTargetName + " &7has been killed.");
    killMessageSent = 1;
}

void handleModuleAlerts() {
    if (!modules.getButton(scriptName, "Enable Module Alerts")) return;
    if (!modules.isEnabled("Client")) {
        modules.enable("Client");
    }
    
    playSounds = modules.getButton(scriptName, "Play Alert Sounds");
    alertDelay = (int)(modules.getSlider(scriptName, "Alert Duration") * 1000);
    
    if (!bridge.has("clientAlert")) {
        checkModuleStates();
    }
}

void checkModuleStates() {
    long now = client.time();
    Map<String, List<String>> categories = modules.getCategories();
    
    for (String category : categories.keySet()) {
        if (category.equalsIgnoreCase("profiles")) continue;
        
        if (!modules.getButton(scriptName, "Alert " + category + " modules")) continue;
        
        List<String> modulesList = categories.get(category);
        for (String moduleName : modulesList) {
            if (EXCLUDED_MODULES.contains(moduleName)) continue;
            
            boolean isEnabled = modules.isEnabled(moduleName);
            boolean wasEnabled = enabledModules.contains(moduleName);
            
            if (isEnabled && !wasEnabled) {
                enabledModules.add(moduleName);
                pendingEnables.add(moduleName);
                lastModuleChange = now;
            } else if (!isEnabled && wasEnabled) {
                enabledModules.remove(moduleName);
                pendingDisables.add(moduleName);
                lastModuleChange = now;
            }
        }
    }

    if (!pendingEnables.isEmpty() || !pendingDisables.isEmpty()) {
        if (now - lastModuleChange > BATCH_DELAY) {
            sendBatchedAlerts();
        }
    }
}

void sendBatchedAlerts() {
    boolean shouldPlaySound = playSounds && 
                            (pendingEnables.size() + pendingDisables.size() > 0);
    
    if (!pendingEnables.isEmpty()) {
        String modules = String.join(", ", pendingEnables);
        sendModuleAlert(ALERT_TITLE_MODULES, "Enabled " + modules, true);
        pendingEnables.clear();
    }
    
    if (!pendingDisables.isEmpty()) {
        String modules = String.join(", ", pendingDisables);
        sendModuleAlert(ALERT_TITLE_MODULES, "Disabled " + modules, false);
        pendingDisables.clear();
    }
    
    if (shouldPlaySound) {
        client.ping();
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

/* Queue Commands */
void initializeQueueCommands() {
    // Bedwars commands
    addCommand(new String[]{"1s"}, "bedwars_eight_one");
    addCommand(new String[]{"2s"}, "bedwars_eight_two");
    addCommand(new String[]{"3s"}, "bedwars_four_three");
    addCommand(new String[]{"4s"}, "bedwars_four_four");
    addCommand(new String[]{"4v4"}, "bedwars_two_four");
    addCommand(new String[]{"2r"}, "bedwars_eight_two_rush");
    addCommand(new String[]{"4r"}, "bedwars_four_four_rush");
    addCommand(new String[]{"c"}, "bedwars_castle");

    // Skywars commands
    addCommand(new String[]{"sn"}, "solo_normal");
    addCommand(new String[]{"si"}, "solo_insane");
    addCommand(new String[]{"tn"}, "teams_normal");
    addCommand(new String[]{"ti"}, "teams_insane");

    // Duels commands
    addCommand(new String[]{"bowd"}, "duels_bow_duel");
    addCommand(new String[]{"cd"}, "duels_classic_duel");
    addCommand(new String[]{"opd"}, "duels_op_duel");
    addCommand(new String[]{"uhcd"}, "duels_uhc_duel");
    addCommand(new String[]{"bd"}, "duels_bridge_duel");

    // Other games
    addCommand(new String[]{"pit"}, "pit");
    addCommand(new String[]{"uhc"}, "uhc_solo");
    addCommand(new String[]{"tuhc"}, "uhc_teams");
    addCommand(new String[]{"mm"}, "murder_classic");
    addCommand(new String[]{"ww"}, "wool_wool_wars_two_four");
    addCommand(new String[]{"ctw"}, "wool_capture_the_wool_two_twenty");
    addCommand(new String[]{"sbs"}, "build_battle_speed_builders");

    // Game display names
    GAME_NAMES.put("bedwars_eight_one", "Solo Bedwars");
    GAME_NAMES.put("bedwars_eight_two", "Doubles Bedwars");
    GAME_NAMES.put("bedwars_four_three", "3v3v3v3 Bedwars");
    GAME_NAMES.put("bedwars_four_four", "4v4v4v4 Bedwars");
    GAME_NAMES.put("bedwars_two_four", "4v4 Bedwars");
    GAME_NAMES.put("bedwars_eight_two_rush", "Doubles Rush");
    GAME_NAMES.put("bedwars_four_four_rush", "4v4v4v4 Rush");
    GAME_NAMES.put("bedwars_castle", "Castle");
    GAME_NAMES.put("solo_normal", "Solo Normal");
    GAME_NAMES.put("solo_insane", "Solo Insane");
    GAME_NAMES.put("teams_normal", "Teams Normal");
    GAME_NAMES.put("teams_insane", "Teams Insane");
    GAME_NAMES.put("duels_bow_duel", "Bow Duels");
    GAME_NAMES.put("duels_classic_duel", "Classic Duels");
    GAME_NAMES.put("duels_op_duel", "OP Duels");
    GAME_NAMES.put("duels_uhc_duel", "UHC Duels");
    GAME_NAMES.put("duels_bridge_duel", "Bridge Duels");
    GAME_NAMES.put("pit", "The Pit");
    GAME_NAMES.put("uhc_solo", "UHC Solo");
    GAME_NAMES.put("uhc_teams", "UHC Teams");
    GAME_NAMES.put("murder_classic", "Murder Mystery");
    GAME_NAMES.put("wool_wool_wars_two_four", "Wool Wars");
    GAME_NAMES.put("wool_capture_the_wool_two_twenty", "Capture the Wool");
    GAME_NAMES.put("build_battle_speed_builders", "Speed Builders");
}

void addCommand(String[] aliases, String gameId) {
    for (String alias : aliases) {
        QUEUE_COMMANDS.put(alias.toLowerCase(), gameId.toLowerCase());
    }
}

boolean onPacketSent(CPacket packet) {
    if (!modules.getButton(scriptName, "Enable Queue Commands")) return true;
    
    if (packet instanceof C01) {
        C01 c01 = (C01) packet;
        String message = c01.message.toLowerCase().trim();
        
        if (message.equals("/q")) {
            showQueueHelp();
            return false;
        } else if (message.startsWith("/q ")) {
            String[] parts = message.split(" ");
            if (parts.length > 1) {
                String command = parts[1].trim();
                String gameId = QUEUE_COMMANDS.get(command);
                if (gameId != null) {
                    queueForGame(gameId);
                } else {
                    client.print("&7[&dR&7] &cUnknown queue command. Available commands:");
                    showQueueHelp();
                }
                return false;
            }
        } else if (message.equals("/rq")) {
            handleRequeue();
            return false;
        }
    }
    
    if (modules.getButton(scriptName, "Vertical tower")) {
        handleVerticalTowerPackets(packet);
    }
    
    return true;
}

void showQueueHelp() {
    client.print("&7[&dR&7] Queue Commands:");
    client.print("&7- Bedwars: &f/q 1s&7, &f/q 2s&7, &f/q 3s&7, &f/q 4s&7, &f/q 4v4");
    client.print("&7- Rush: &f/q 2r&7, &f/q 4r");
    client.print("&7- Castle: &f/q c");
    client.print("&7- Skywars: &f/q sn&7, &f/q si&7, &f/q tn&7, &f/q ti");
    client.print("&7- Duels: &f/q cd&7, &f/q opd&7, &f/q uhcd&7, &f/q bd&7, &f/q bowd");
    client.print("&7- Other: &f/q pit&7, &f/q uhc&7, &f/q tuhc&7, &f/q mm&7, &f/q ww&7, &f/q ctw&7, &f/q sbs");
    client.print("&7- Requeue: &f/rq");
}

void handleRequeue() {
    if (lastQueuedGame.isEmpty()) {
        if (modules.getButton(scriptName, "Show Queue Alerts")) {
            sendQueueAlert(ALERT_TITLE_QUEUE, "No previous game to requeue!", 3);
            client.print("&7[&dR&7] &cNo previous game to requeue! Use &f/q&c to see available commands.");
        }
        return;
    }
    queueForGame(lastQueuedGame);
}

void queueForGame(String gameId) {
    lastQueuedGame = gameId;
    String gameName = GAME_NAMES.get(gameId);
    
    client.chat("/play " + gameId);
    if (modules.getButton(scriptName, "Show Queue Alerts")) {
        sendQueueAlert(ALERT_TITLE_QUEUE, "Joining " + (gameName != null ? gameName : gameId), 2);
    }
}

void sendQueueAlert(String title, String message, int type) {
    Map<String, Object> alert = new HashMap<>();
    alert.put("title", title);
    alert.put("message", message);
    alert.put("duration", 3000);
    alert.put("type", type);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> queue = (List<Map<String, Object>>) bridge.get(ALERT_QUEUE_KEY);
    if (queue == null) {
        queue = new ArrayList<>();
    }
    queue.add(alert);
    bridge.add(ALERT_QUEUE_KEY, queue);
}

/* AntiCheat Flag Handler */
void onAntiCheatFlag(String flag, Entity entity) {
    if (!modules.getButton(scriptName, "Enable AntiCheat Alerts")) return;
    if (bridge.has(ALERT_QUEUE_KEY)) return;
    
    String formattedMessage = util.color(ANTICHEAT_FORMAT)
        .replace("{entity}", entity.getName())
        .replace("{flag}", flag);
    
    int duration = (int)(modules.getSlider(scriptName, "AC Alert Duration") * 1000);
    
    Map<String, Object> alert = new HashMap<>();
    alert.put("title", ALERT_TITLE_ANTICHEAT);
    alert.put("message", formattedMessage);
    alert.put("duration", duration);
    alert.put("type", 1); // Warning type

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> queue = (List<Map<String, Object>>) bridge.get(ALERT_QUEUE_KEY);
    if (queue == null) {
        queue = new ArrayList<>();
    }
    queue.add(alert);
    bridge.add(ALERT_QUEUE_KEY, queue);
}

// Legit autoblock
void legitAB() {
    if (abConditions()) {
        switch (abStage) {
            case 0:
                client.sendPacketNoEvent(new C07(new Vec3(0, 0, 0), "RELEASE_USE_ITEM", "DOWN"));
                keybinds.setPressed("use", false);
                abStage++;
                break;
            case 1:
                client.swing(false);
                if (getTarget() != null) {
                    client.sendPacketNoEvent(new C0A());
                    client.sendPacketNoEvent(new C02(modules.getKillAuraTarget(), "ATTACK", null));
                }
                abStage++;
                break;
            case 2:
                client.sendPacketNoEvent(new C08(client.getPlayer().getHeldItem(), new Vec3(-1, -1, -1), 255, new Vec3(0, 0, 0)));
                keybinds.setPressed("use", true);
                abStage = 0;
                break;
        }
    } else {
        abStage = 0;
    }
}

boolean abConditions() {
    return (
        modules.isEnabled("KillAura") && 
        modules.getSlider("KillAura", "Autoblock") == 0 && 
        modules.getKillAuraTarget() != null && 
        !modules.isEnabled("Blink") && 
        keybinds.isMouseDown(1) && 
        holding("sword")
    );
}

Entity getTarget() {
    Entity target = null;
    double maxSq = 11.55;
    for (Entity en : world.getPlayerEntities()) {
        if (en == client.getPlayer()) continue;
        double distSq = client.getPlayer().getPosition().distanceToSq(en.getPosition());
        if (distSq < maxSq) {
            maxSq = distSq;
            target = en;
        }
    }
    return target;
}

void antiDebuff() {
    int[] effectIDs = {2, 15};
    for (int effect : effectIDs) {
        client.removePotionEffect(effect);
    }
}

boolean holding(String itemType) {
    Entity player = client.getPlayer();
    if (player.getHeldItem() != null) {
        if (itemType.equals("blocks")) {
            return player.getHeldItem().isBlock;
        } else {
            return player.getHeldItem().type.toLowerCase().contains(itemType);
        }
    }
    return false;
}

void themeSync() {
    if ((int) modules.getSlider("TargetHUD", "Theme") != previousTheme ||
        (int) modules.getSlider("HUD", "Theme") != previousTheme ||
        (int) modules.getSlider("BedESP", "Theme") != previousTheme) {
        int newTheme = (int) modules.getSlider("TargetHUD", "Theme");
        if ((int) modules.getSlider("HUD", "Theme") != previousTheme) {
            newTheme = (int) modules.getSlider("HUD", "Theme");
        } else if ((int) modules.getSlider("BedESP", "Theme") != previousTheme) {
            newTheme = (int) modules.getSlider("BedESP", "Theme");
        }
        modules.setSlider("TargetHUD", "Theme", newTheme);
        modules.setSlider("HUD", "Theme", newTheme);
        modules.setSlider("BedESP", "Theme", newTheme);
        previousTheme = newTheme;
    }
}

void verticalTower(Entity player, PlayerState state) {
    if (towerDamage && ++towerDamageticks >= towerDisableHurtTicks) {
        towerDamage = towerGroundCheck = towerUnder = towerOver = towerEdge = false;
        towerDamageticks = towerStage = towerDelay = 0;
    }
    Vec3 position = player.getPosition();
    if (towerQueued != null) {
        client.sendPacketNoEvent(towerQueued);
        client.sendPacketNoEvent(new C0A());
        towerQueued = null;
    }
    double towerSimpleX = (int) Math.round(position.x);
    if (verticalTowerConditions() && jumpLvl() == 0) {
        if (player.onGround()) towerGroundCheck = true;
        if (!towerGroundCheck) return;
        if (towerDelay == 0) {
            towerFirstX = towerSimpleX;
            towerDelay = 1;
            client.setSpeed(0);
            if (towerSimpleX > position.x) towerOver = true;
            if (towerSimpleX < position.x) towerUnder = true;
        }
        if ((!towerEdge && towerDelay >= 1 && !towerAlignGround) || (!towerEdge && towerDelay >= 1 && player.onGround() && towerAlignGround)) {
            Vec3 motion = client.getMotion();
            if (towerUnder) client.setMotion(edgingMotion, motion.y, motion.z);
            if (towerOver) client.setMotion(-edgingMotion, motion.y, motion.z);
        }
        if ((towerSimpleX > towerFirstX || towerSimpleX < towerFirstX) && ++towerDelay == towerAlignTicks) {
            towerEdge = true;
        }
    }
    if (!verticalTowerConditions() || jumpLvl() != 0) {
        towerStage = towerDelay = 0;
        towerEdge = towerOver = towerUnder = towerGroundCheck = false;
        if (!keybinds.isPressed("jump")) modules.setButton("Scaffold", "Delay on jump", true);
        return;
    }
    if (towerAlignGround && !towerEdge) return;
    modules.setButton("Scaffold", "Delay on jump", false);
    
    if ((towerEdge && towerUnder) || (!towerEdge && towerOver)) state.yaw = 90;
    if ((towerEdge && towerOver) || (!towerEdge && towerUnder)) state.yaw = 270;
    state.pitch = towerEdge ? 88 : 83;
    if (towerDisableHurt && towerDamage) return;
    if (towerEdge) client.setSpeed(0);
    int valY = (int) Math.round((state.y % 1) * 10000);
    Vec3 motion = client.getMotion();
    
    if (valY == 0) {
        if (towerEdge) client.setMotion(motion.x, 0.42f, motion.z);
        else if (towerUnder) client.setMotion(edgingMotion, 0.42f, motion.z);
        else if (towerOver) client.setMotion(-edgingMotion, 0.42f, motion.z);
    } else if (valY > 4000 && valY < 4300) {
        if (towerEdge) client.setMotion(motion.x, 0.33, motion.z);
        else if (towerUnder) client.setMotion(edgingMotion, 0.33, motion.z);
        else if (towerOver) client.setMotion(-edgingMotion, 0.33, motion.z);
    } else if (valY > 7000) {
        if (towerEdge) client.setMotion(motion.x, 1 - state.y % 1, motion.z);
        else if (towerUnder) client.setMotion(edgingMotion, 1 - state.y % 1, motion.z);
        else if (towerOver) client.setMotion(-edgingMotion, 1 - state.y % 1, motion.z);
    }
}

void handleVerticalTowerPackets(CPacket packet) {
    if (!towerAlignPlaceSecond || towerEdge) {
        if (verticalTowerConditions()) {
            if (packet instanceof C08) {
                C08 c08 = (C08) packet;
                if (c08.direction != 255) {
                    Vec3 position = c08.position;
                    position.y += 1;
                    if (towerUnder) towerQueued = new C08(c08.itemStack, position, 5, new Vec3(0, 0.51424, 0.493123));
                    if (towerOver) towerQueued = new C08(c08.itemStack, position, 4, new Vec3(0, 0.51424, 0.493123));
                }
            }
        }
    }
}

void handleVerticalTowerInput() {
    if (verticalTowerConditions()) {
        if (!towerDisableHurt || !towerDamage) {
            client.setJump(false);
        }
    }
}

boolean verticalTowerConditions() {
    return !bridge.has("serverTeleport") && modules.isEnabled("Scaffold") && keybinds.isKeyDown(57)
           && !keybinds.isKeyDown(30) && !keybinds.isKeyDown(31) && !keybinds.isKeyDown(32)
           && !keybinds.isKeyDown(17) && holding("blocks");
}

double speedLvl() {
    for (Object[] effect : client.getPlayer().getPotionEffects()) {
        String name = (String) effect[1];
        int amplifier = (int) effect[2];
        if (name.equals("potion.moveSpeed")) {
            return amplifier;
        }
        return 0;
    }
    return 0;
}
double jumpLvl() {
    for (Object[] effect : client.getPlayer().getPotionEffects()) {
        String pot = (String) effect[1];
        double amplifier = ((Number) effect[2]).doubleValue();
        if (pot.contains("jump")) {
            return amplifier + 1;
        }
    }
    return 0;
}

void getGameMode() {
    if (checkMode && !scoreboard && world.getScoreboard() != null) scoreboard = true;
    if (scoreboard && ++checkModeTicks == 45 && client.getServerIP().toLowerCase().contains("hypixel.net")) {
        client.chat("/locraw");
    }
}

boolean parseGameMode(String msg) {
    checkMode = false;
    try {
        if (!msg.contains("REPLAY") && !msg.equals("{\"server\":\"limbo\"}"))
            gameMode = msg.split("mode\":\"")[1].split("\"")[0];
    } catch (Exception e) {
    }
    return false;
}