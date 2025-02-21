/* Constants */
static final String[] SLOT_NAMES = {util.color("&cDisabled"), "1", "2", "3", "4", "5", "6", "7", "8", "9"};
static final HashSet<String> PICKAXE_TYPES = new HashSet<>(Arrays.asList(
    "wooden_pickaxe", "iron_pickaxe", "golden_pickaxe", "diamond_pickaxe"
));
static final HashSet<String> AXE_TYPES = new HashSet<>(Arrays.asList(
    "wooden_axe", "stone_axe", "iron_axe", "diamond_axe"
));

/* Item Registry */
static final Map<String, String> ITEM_DISPLAY_NAMES = new HashMap<>();
static final List<String> ITEMS = new ArrayList<>();

/* State Management */
Map<String, Integer> locations = new HashMap<>();
Map<String, Long> purchases = new HashMap<>();
Map<String, Boolean> keyStates = new HashMap<>();
List<Integer[]> clickQueue = new ArrayList<>();
boolean sentInventoryPacket = false;

/* Key Mappings */
static final String[] KEY_NAMES = {
    "None",
    // Numbers (1-0) 
    "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
    // Letters (A-Z)
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
    "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    // Function keys
    "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9", "F10", "F11", "F12",
    // Mouse buttons
    "LMB", "RMB", "MMB",
    // Special keys
    "Tab", "Caps", "Space", "LShift", "LControl", "LAlt", "RAlt",
    "Enter", "Backspace", "Insert", "Delete", "Home", "End", "PgUp", "PgDown",
    "Left", "Right", "Up", "Down"
};

static final int[] KEY_CODES = {
    0,  // None
    // Numbers (1-0)
    2, 3, 4, 5, 6, 7, 8, 9, 10, 11,
    // Letters (A-Z)
    30, 48, 46, 32, 18, 33, 34, 35, 23, 36, 37, 38, 50,
    49, 24, 25, 16, 19, 31, 20, 22, 47, 17, 45, 21, 44,
    // Function keys
    59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 87, 88,
    // Mouse buttons
    -100, -99, -98,
    // Special keys
    15, 58, 57, 42, 29, 56, 184,
    28, 14, 210, 211, 199, 207, 201, 209,
    203, 205, 200, 208
};

/* Initialization */
void onLoad() {
    initializeItems();
    registerModules();
}

/* Item Registry */
void initializeItems() {
    // Quick buy items
    registerItem("wool", "Wool");
    registerItem("stone_sword", "Stone Sword");
    registerItem("iron_sword", "Iron Sword");
    registerItem("golden_apple", "Golden Apple");
    registerItem("fire_charge", "Fireball");
    registerItem("tnt", "TNT");
    registerItem("ender_pearl", "Ender Pearl");
    
    // Tools and armor
    registerItem("pickaxe", "Pickaxe");
    registerItem("axe", "Axe");
    registerItem("shears", "Shears");
    registerItem("chainmail_boots", "Chainmail Armor");
    registerItem("iron_boots", "Iron Armor");
    
    // Diamond upgrades
    registerItem("upg iron_sword", "Sharpness");
    registerItem("upg iron_chestplate", "Protection");
    registerItem("upg iron_pickaxe", "Mining Fatigue");
    registerItem("upg golden_pickaxe", "Haste");
    registerItem("upg diamond_boots", "Feather Falling");
    
    // Other items
    registerItem("diamond_sword", "Diamond Sword");
    registerItem("stick", "Knockback Stick");
    registerItem("arrow", "Arrows");
    registerItem("diamond_boots", "Diamond Armor");
}

void registerModules() {
    // Blocks and basic items
    modules.registerDescription(util.color("&f       ----- BASIC ITEMS -----"));
    registerItemModules("wool");
    registerItemModules("stone_sword");
    registerItemModules("iron_sword");
    registerItemModules("golden_apple");
    registerItemModules("fire_charge");
    registerItemModules("tnt");
    registerItemModules("ender_pearl");
    
    // Tools and armor
    modules.registerDescription(util.color("&f    ----- TOOLS AND ARMOR -----"));
    registerItemModules("pickaxe");
    registerItemModules("axe");
    registerItemModules("shears");
    registerItemModules("chainmail_boots");
    registerItemModules("iron_boots");
    
    // Diamond upgrades
    modules.registerDescription(util.color("&f     ----- TEAM UPGRADES -----"));
    registerItemModules("upg iron_sword");
    registerItemModules("upg iron_chestplate");
    registerItemModules("upg iron_pickaxe");
    registerItemModules("upg golden_pickaxe");
    registerItemModules("upg diamond_boots");
    
    // Other items
    modules.registerDescription(util.color("&f      ----- OTHER ITEMS -----"));
    registerItemModules("diamond_sword");
    registerItemModules("stick");
    registerItemModules("arrow");
    registerItemModules("diamond_boots");

    modules.registerDescription(util.color("&7by @desiyn"));
}

void registerItemModules(String item) {
    String displayName = ITEM_DISPLAY_NAMES.get(item);
    boolean isUpgrade = item.startsWith("upg ");
    boolean isBoots = item.contains("boots");
    
    if (!isUpgrade && !isBoots) {
        modules.registerDescription(util.color("&7> " + displayName));
        modules.registerSlider(displayName + " Quickslot", "Select hotbar slot (1-9)", 0, SLOT_NAMES);
        modules.registerSlider(displayName + " Key", "Select key to buy " + displayName.toLowerCase(), 0, KEY_NAMES);
        modules.registerButton(displayName + " Turbo", false);
    } else {
        modules.registerDescription(util.color("&7> " + displayName));
        modules.registerSlider(displayName + " Key", "Select key to buy " + displayName.toLowerCase(), 0, KEY_NAMES);
    }
}

void registerItem(String item, String displayName) {
    ITEM_DISPLAY_NAMES.put(item, displayName);
    ITEMS.add(item);
}

/* Event Handlers */

/**
 * Processes inventory updates and click queue
 */
void onPreUpdate() {
    if (client.getScreen().equals("GuiChest")) {
        Map<Integer, ItemStack> inventory = createInventoryMap();
        processInventory(inventory);
        processClicks();
    } else {
        resetState();
    }
}

/**
 * Resets the inventory packet flag
 */
void onPostMotion() {
    sentInventoryPacket = false;
}

/**
 * Tracks inventory packet sending
 */
boolean onPacketSent(CPacket packet) {
    if (packet instanceof C0E) {
        sentInventoryPacket = true;
    }
    return true;
}

/* Inventory Processing */
void processInventory(Map<Integer, ItemStack> inv) {
    String chestTitle = inventory.getChest();
    boolean isQuickBuy = chestTitle.equals("Quick Buy");
    boolean isUpgrades = chestTitle.equals("Upgrades & Traps");
    
    if (!isQuickBuy && !isUpgrades) return;
    
    int chestSize = inventory.getChestSize();
    updateLocations(inv, isQuickBuy, isUpgrades, chestSize);
    processKeybinds(isQuickBuy, isUpgrades);
}

void updateLocations(Map<Integer, ItemStack> inv, boolean isQuickBuy, boolean isUpgrades, int chestSize) {
    for (String item : ITEMS) {
        if (!shouldProcessItem(item, isQuickBuy, isUpgrades)) continue;
        
        String searchItem = item.startsWith("upg ") ? item.substring(4) : item;
        HashSet<String> itemTypes = getItemTypes(searchItem, isQuickBuy);
        
        findItemLocation(inv, item, searchItem, itemTypes, isQuickBuy, chestSize);
    }
}

void processKeybinds(boolean isQuickBuy, boolean isUpgrades) {
    long now = client.time();
    
    for (String item : ITEMS) {
        if (!shouldProcessItem(item, isQuickBuy, isUpgrades)) continue;
        
        String displayName = ITEM_DISPLAY_NAMES.get(item);
        if (!shouldProcessKeybind(item, displayName, now)) continue;
        
        int hotbarSlot = (int) modules.getSlider(scriptName, displayName + " Quickslot") - 1;
        queueClick(item, hotbarSlot);
    }
}

/* Utility Methods */
void resetState() {
    locations.clear();
    clickQueue.clear();
}

boolean shouldProcessItem(String item, boolean isQuickBuy, boolean isUpgrades) {
    if (isQuickBuy && item.startsWith("upg ")) return false;
    if (isUpgrades && !item.startsWith("upg ")) return false;
    return true;
}

HashSet<String> getItemTypes(String searchItem, boolean isQuickBuy) {
    if (!isQuickBuy) return null;
    if (searchItem.equals("pickaxe")) return PICKAXE_TYPES;
    if (searchItem.equals("axe")) return AXE_TYPES;
    return null;
}

boolean shouldProcessKeybind(String item, String displayName, long now) {
    int keyIndex = (int)modules.getSlider(scriptName, displayName + " Key");
    if (keyIndex == 0) return false;  // None selected
    
    int keyCode = KEY_CODES[keyIndex];
    boolean keyDown = keyCode > 0 && keybinds.isKeyDown(keyCode);
    boolean turbo = !item.startsWith("upg ") && modules.getButton(scriptName, displayName + " Turbo");
    long cooldown = item.startsWith("upg ") ? 300 : 90;
    long lastTime = purchases.getOrDefault(item, 0L);
    
    if (!keyDown) {
        keyStates.put(item, false);
        return false;
    }
    
    if (!turbo && keyStates.getOrDefault(item, false)) return false;
    if (now - lastTime < cooldown) return false;
    
    purchases.put(item, now);
    keyStates.put(item, true);
    return true;
}

void queueClick(String itemName, int hotbarSlot) {
    Integer slot = locations.get(itemName);
    if (slot != null) {
        clickQueue.add(new Integer[]{slot, hotbarSlot});
    }
}

void processClicks() {
    if (client.getPlayer().getTicksExisted() % 2 == 0 && !sentInventoryPacket && !clickQueue.isEmpty()) {
        Integer[] click = clickQueue.remove(0);
        int slot = click[0];
        int hotbarSlot = click[1];
        
        if (hotbarSlot >= 0) {
            inventory.click(slot, hotbarSlot, 2);
        } else {
            inventory.click(slot, 0, 0);
        }
    }
}

Map<Integer, ItemStack> createInventoryMap() {
    Map<Integer, ItemStack> inv = new HashMap<>();
    String screen = client.getScreen();
    int inventorySize = inventory.getSize() - 4;
    int slot = 0;

    if (screen.equals("GuiInventory")) {
        for (int i = 0; i < 5; i++) inv.put(slot++, null);
        Entity player = client.getPlayer();
        for (int i = 3; i >= 0; i--) inv.put(slot++, player.getArmorInSlot(i));
    } else if (screen.equals("GuiChest") && !inventory.getChest().isEmpty()) {
        int chestSize = inventory.getChestSize();
        for (int i = 0; i < chestSize; i++) {
            inv.put(slot++, inventory.getStackInChestSlot(i));
        }
    }

    for (int i = 9; i < inventorySize + 9; i++) {
        inv.put(slot++, inventory.getStackInSlot(i % inventorySize));
    }

    return inv;
}

void findItemLocation(Map<Integer, ItemStack> inv, String item, String searchItem, HashSet<String> itemTypes, boolean isQuickBuy, int chestSize) {
    int start = isQuickBuy ? 18 : 9;
    int end = isQuickBuy ? chestSize - 9 : 27;
    
    for (int i = start; i < end; i++) {
        ItemStack stack = inv.get(i);
        if (stack == null) continue;
        
        if (itemTypes != null) {
            if (itemTypes.contains(stack.name)) {
                locations.put(item, i);
                return;
            }
        } else if (stack.name.equals(searchItem)) {
            locations.put(item, i);
            return;
        }
    }
}