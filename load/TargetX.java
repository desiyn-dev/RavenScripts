static final int TARGET_DISPLAY_DURATION = 1000;

int hudX = 100;
int hudY = 100;
boolean dragging = false;
int dragOffsetX = 0;
int dragOffsetY = 0;

Entity currentTarget = null;
long targetTime = 0;

float interpolatedHealth = 0;
long lastHealthUpdate = 0;
static final float HEALTH_INTERPOLATION_SPEED = 0.05f;

void onLoad() {
    modules.registerButton("Require KillAura", false);
    modules.registerButton("Only Network Players", true);
    modules.registerSlider("Style", "", 0, new String[]{"Default", "Astolfo", "Myau", util.color("Moon &c(WIP)"), util.color("Exhibition &c(WIP)")});
    modules.registerButton("Debug", false);

    modules.registerDescription("> Myau");
    modules.registerButton("Text Shadow", true);
    modules.registerButton("Show Head", true);
    
    modules.registerDescription("> Astolfo");
    modules.registerButton("Show W/L", true);
    modules.registerButton("Show Health Diff", false);
    
    String savedX = config.get("hudX");
    String savedY = config.get("hudY");
    if (savedX != null && !savedX.isEmpty()) {
        hudX = Integer.parseInt(savedX);
    } else {
        hudX = 100;
    }
    if (savedY != null && !savedY.isEmpty()) {
        hudY = Integer.parseInt(savedY);
    } else {
        hudY = 100;
    }
}

int[] getConvertedMouse() {
    int[] displaySize = client.getDisplaySize();
    int displayHeight = displaySize[1];
    int[] rawMouse = keybinds.getMousePosition();
    int guiX = rawMouse[0] / 2;
    int guiY = (displayHeight * 2 - rawMouse[1]) / 2;
    return new int[]{guiX, guiY};
}

void debug(String message) {
    if (modules.getButton(scriptName, "Debug")) {
        client.print(message);
    }
}

boolean checkNWP(Entity target) {
    if (target.getNetworkPlayer() == null && modules.getButton(scriptName, "Only Network Players")) {
        return false;
    }
    return true;
}

Image getPlayerHead(Entity target) {
    if (target.getNetworkPlayer() != null) {
        String url = "https://crafatar.com/avatars/" + target.getNetworkPlayer().getUUID() + "?overlay=true&size=8";
        return new Image(url, true);
    }
    return new Image("https://crafatar.com/avatars/c06f89064c8a49119c29ea1dbd1aab82?overlay=true&size=8", true);
}

boolean onPacketSent(CPacket packet) {
    if (packet instanceof C02) {
        C02 c02 = (C02) packet;
        if (!"ATTACK".equals(c02.action)) {
            return true;
        }
        
        if (modules.getKillAuraTarget() != null) {
            currentTarget = modules.getKillAuraTarget();
            targetTime = client.time();
            debug("got target (killaura)");
        }
        else if (!modules.getButton(scriptName, "Require KillAura")) {
            currentTarget = c02.entity;
            targetTime = client.time();
            debug("got target (c02)");
        }
    }
    return true;
}

int[] getCurrentHUDDimensions() {
    double style = modules.getSlider(scriptName, "Style");
    if (style == 0) {
        return new int[]{HUD_WIDTH, HUD_HEIGHT};
    } else if (style == 1) {
        return new int[]{ASTOLFO_HUD_WIDTH, ASTOLFO_HUD_HEIGHT};
    } else if (style == 2) {
        boolean showHead = modules.getButton(scriptName, "Show Head");
        int effectiveWidth = showHead ? MYAU_HUD_WIDTH : MYAU_HUD_WIDTH - MYAU_HUD_HEIGHT;
        return new int[]{effectiveWidth, MYAU_HUD_HEIGHT};
    } else if (style == 3) {
        return new int[]{150, 50};
    } else if (style == 4) {
        return new int[]{150, 50};
    }
    return new int[]{HUD_WIDTH, HUD_HEIGHT};
}

void onRenderTick(float partialTicks) {
    updateColors();
    
    boolean isInChat = "GuiChatOF".equals(client.getScreen());
    
    if (isInChat) {
        int[] guiMouse = getConvertedMouse();
        int[] hudDimensions = getCurrentHUDDimensions();
        int hudWidth = hudDimensions[0];
        int hudHeight = hudDimensions[1];
        
        debug("GUI Mouse: " + guiMouse[0] + ", " + guiMouse[1] + " | HUD: " + hudX + ", " + hudY + 
              " | Dimensions: " + hudWidth + "x" + hudHeight);
        
        if (keybinds.isMouseDown(0)) {
            if (!dragging &&
                guiMouse[0] >= hudX && guiMouse[0] <= hudX + hudWidth &&
                guiMouse[1] >= hudY && guiMouse[1] <= hudY + hudHeight) {
                dragging = true;
                dragOffsetX = guiMouse[0] - hudX;
                dragOffsetY = guiMouse[1] - hudY;
                debug("Started dragging");
            }
        } else {
            if (dragging) {
                debug("Stopped dragging");
                config.set("hudX", String.valueOf(hudX));
                config.set("hudY", String.valueOf(hudY));
            }
            dragging = false;
        }
        if (dragging) {
            hudX = guiMouse[0] - dragOffsetX;
            hudY = guiMouse[1] - dragOffsetY;
            debug("Dragging... New HUD pos: " + hudX + ", " + hudY);
        }
        
        long currentTime = client.time();
        if (currentTarget == null || (currentTime - targetTime >= TARGET_DISPLAY_DURATION)) {
            currentTarget = client.getPlayer();
            targetTime = currentTime;
        }
        onRenderTargetHUD(currentTarget, hudX, hudY);
        return;
    } else if (currentTarget != null) {
        if (currentTarget == client.getPlayer()) {
            currentTarget = null;
            return;
        }
        
        long currentTime = client.time();
        if (currentTime - targetTime < TARGET_DISPLAY_DURATION) {
            onRenderTargetHUD(currentTarget, hudX, hudY);
        } else {
            currentTarget = null;
        }
    }
}

void onRenderTargetHUD(Entity target, int x, int y) {
    if (!checkNWP(target)) {
        return;
    }
    
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
    
    double style = modules.getSlider(scriptName, "Style");
    if (style == 0) {
        renderDefault(target, interpolatedHealth, shealth, winning, x, y);
    } else if (style == 1) {
        renderAstolfo(target, interpolatedHealth, shealth, winning, x, y);
    } else if (style == 2) {
        renderMyau(target, interpolatedHealth, shealth, winning, x, y);
    } else if (style == 3) {
        renderMoon(target, interpolatedHealth, shealth, winning, x, y);
    } else if (style == 4) {
        renderExhibition(target, interpolatedHealth, shealth, winning, x, y);
    }
}


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
static final float HEALTH_BAR_RADIUS = 2.0f;
static final int ENTITY_RENDER_SCALE = 32;
static final int ENTITY_VERTICAL_MARGIN = 4;

void renderDefault(Entity target, float thealth, float shealth, int winning, int x, int y) {
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
    render.text(healthText, textAreaX + TEXT_AREA_PADDING, subheaderY, 1f, getCurrentColor(0), true);
    float diff = shealth - target.getHealth();
    String diffText = (diff > 0 ? "+" : "") + String.format("%.1f", diff);
    int diffTextWidth = render.getFontWidth(diffText);
    int diffTextX = textAreaX + textAreaWidth - TEXT_AREA_PADDING - diffTextWidth;
    int diffColor = (diff > 0) ? 0xFF00FF00 : (diff < 0) ? 0xFFFF0000 : 0xFFFFFF00;
    render.text(diffText, diffTextX, subheaderY, 1f, diffColor, true);
    
    float healthPercentage = interpolatedHealth / target.getMaxHealth();
    int healthBarWidth = (int)(healthBarMaxWidth * healthPercentage);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarMaxWidth, healthBarY + HEALTH_BAR_HEIGHT, 0xFF444444);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + HEALTH_BAR_HEIGHT, getCurrentColor(0));
}


static final int ASTOLFO_HUD_WIDTH = 150;
static final int ASTOLFO_HUD_HEIGHT = 55;
static final int ASTOLFO_ENTITY_PADDING = 4;
static final int ASTOLFO_TEXT_PADDING = 6;
static final int ASTOLFO_HEALTHBAR_HEIGHT = 8;
static final int ASTOLFO_ENTITY_SCALE = 25;
static final int ASTOLFO_COLOR = 0xFFFF0000;

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
    
    // Name row with optional W/L indicator
    render.text(target.getDisplayName(), nameX, nameY, 1f, 0xFFFFFFFF, true);
    
    // Add win/loss indicator if enabled
    boolean showWL = modules.getButton(scriptName, "Show W/L");
    if (showWL) {
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
    render.rect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + ASTOLFO_HEALTHBAR_HEIGHT, getCurrentColor(0));
    
    // Health text in middle area
    int midY = (nameY + healthBarY) / 2 - 4;
    render.text(String.format("%.1f \u2764", target.getHealth()), nameX, midY, 2f, getCurrentColor(0), true);
    
    // Add health difference if enabled
    boolean showHealthDiff = modules.getButton(scriptName, "Show Health Diff");
    if (showHealthDiff) {
        float diff = shealth - target.getHealth();
        String diffText = (diff > 0 ? "+" : "") + String.format("%.1f", diff);
        int diffTextWidth = render.getFontWidth(diffText) * 2;
        int diffTextX = x + ASTOLFO_HUD_WIDTH - ASTOLFO_TEXT_PADDING - diffTextWidth;
        int diffColor = (diff > 0) ? 0xFF00FF00 : (diff < 0) ? 0xFFFF0000 : 0xFFFFFF00;
        render.text(diffText, diffTextX, midY, 2f, diffColor, true);
    }
}

static final int MYAU_HUD_WIDTH = 130;
static final int MYAU_HUD_HEIGHT = 30;
static final int MYAU_HUD_PADDING = 2;
static final int MYAU_HUD_COLOR = 0x55000000;
static final int MYAU_HUD_COLOR2 = 0xFFFF0000;
static final int MYAU_HEALTHBAR_HEIGHT = 4;

void renderMyau(Entity target, float thealth, float shealth, int winning, int x, int y) {
    boolean showHead = modules.getButton(scriptName, "Show Head");
    boolean textShadow = modules.getButton(scriptName, "Text Shadow");
    int effectiveHudWidth = showHead ? MYAU_HUD_WIDTH : MYAU_HUD_WIDTH - MYAU_HUD_HEIGHT;
    render.rect(x, y, x + effectiveHudWidth, y + MYAU_HUD_HEIGHT, MYAU_HUD_COLOR);

    int accentColor = getCurrentColor(0);
    
    // border
    render.rect(x - 1, y - 1, x + effectiveHudWidth + 1, y, accentColor);
    render.rect(x - 1, y + MYAU_HUD_HEIGHT, x + effectiveHudWidth + 1, y + MYAU_HUD_HEIGHT + 1, accentColor);
    render.rect(x - 1, y - 1, x, y + MYAU_HUD_HEIGHT + 1, accentColor);
    render.rect(x + effectiveHudWidth, y - 1, x + effectiveHudWidth + 1, y + MYAU_HUD_HEIGHT + 1, accentColor);

    int contentStartX = x;
    if (showHead) {
        Image head = getPlayerHead(target);
        render.image(head, x + MYAU_HUD_PADDING, y + MYAU_HUD_PADDING, 
                    MYAU_HUD_HEIGHT - (MYAU_HUD_PADDING * 2), 
                    MYAU_HUD_HEIGHT - (MYAU_HUD_PADDING * 2));
        contentStartX = x + MYAU_HUD_HEIGHT;
    }

    // first line
    render.text(target.getDisplayName(), 
                contentStartX + MYAU_HUD_PADDING, 
                y + MYAU_HUD_PADDING, 1f, 0xFFFFFFFF, textShadow);
    String winText = (winning == 1) ? util.color("&a&lW")
                    : (winning == 0) ? util.color("&e&lD")
                    : util.color("&c&lL");
    int winTextWidth = render.getFontWidth(winText);
    render.text(winText, 
                x + effectiveHudWidth - winTextWidth - MYAU_HUD_PADDING, 
                y + MYAU_HUD_PADDING, 1f, 0xFFFFFFFF, textShadow);

    // second line
    render.text(String.format("%.1f \u2764", target.getHealth()), 
                contentStartX + MYAU_HUD_PADDING, 
                y + render.getFontHeight() + (MYAU_HUD_PADDING * 2), 
                1f, accentColor, textShadow);
                
    float diff = shealth - target.getHealth();
    String diffText = (diff > 0 ? "+" : "") + String.format("%.1f", diff);
    int diffTextWidth = render.getFontWidth(diffText);
    int diffTextX = x + effectiveHudWidth - MYAU_HUD_PADDING - diffTextWidth;
    int diffTextY = y + render.getFontHeight() + (MYAU_HUD_PADDING * 2);
    int diffColor;
    if(diff > 0) {
        diffColor = 0xFF00FF00;
    } else if(diff < 0) {
        diffColor = 0xFFFF0000;
    } else {
        diffColor = 0xFFFFFF00;
    }
    render.text(diffText, diffTextX, diffTextY, 1f, diffColor, textShadow);

    // health bar
    int healthBarX = contentStartX + MYAU_HUD_PADDING;
    int healthBarMaxWidth = effectiveHudWidth - (showHead ? MYAU_HUD_HEIGHT : 0) - 2 * MYAU_HUD_PADDING;
    int healthBarY = y + MYAU_HUD_HEIGHT - MYAU_HUD_PADDING - MYAU_HEALTHBAR_HEIGHT;
    float healthPercentage = interpolatedHealth / target.getMaxHealth();
    int healthBarWidth = (int)(healthBarMaxWidth * healthPercentage);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarMaxWidth, healthBarY + MYAU_HEALTHBAR_HEIGHT, MYAU_HUD_COLOR);
    render.rect(healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + MYAU_HEALTHBAR_HEIGHT, accentColor);
}

void renderMoon(Entity target, float thealth, float shealth, int winning, int x, int y) {   
    // moon
}

void renderExhibition(Entity target, float thealth, float shealth, int winning, int x, int y) {
    // exhibition
}