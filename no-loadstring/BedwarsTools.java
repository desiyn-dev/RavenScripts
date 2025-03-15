/* Alert System */
static String ALERT_QUEUE_KEY;
static final String ALERT_TITLE_ITEMS = "Item Alerts";
static final String ALERT_TITLE_BED = "Bed Alert";
static final String CHAT_PREFIX = util.color("&7[&dR&7] ");

/* Item Registry */
Map<String, Map<String, Object>> playerItems = new HashMap<>();
Map<String, String> itemDisplayColors = new HashMap<>();
HashSet<String> armorPieceNames = new HashSet<>();
HashSet<String> heldItemNames = new HashSet<>();
HashSet<String> heldItemDisplayNames = new HashSet<>();
HashSet<String> checkedPlayers = new HashSet<>();

/* Bed Protection */
Vec3 bedPosition = null;
double bedDistance = 0;
int range = 20;
String myTeamColor = "";
String defaultColor = util.color("&7");

/* State */
boolean showDistance = true;
boolean showTeammates = false;
boolean debug = false;
String myName = "";
int status = -1;
int DELAY_INTERVAL = 15000;

void onLoad() {
    ALERT_QUEUE_KEY = (String) bridge.get("Client.ALERT_QUEUE_KEY");
    if (ALERT_QUEUE_KEY == null) {
        client.print("&cError: Alert system not initialized properly");
        return;
    }

    modules.registerDescription("> Item Alerts");
    modules.registerButton("Show Distance", true);
    modules.registerButton("Show Teammates", false);
    modules.registerSlider("Alert Delay", "s", 15, 0, 60, 1);
    modules.registerButton("Debug Mode", false);

    modules.registerDescription("> Bed Protection");
    modules.registerSlider("Detection Range", "", 20, 5, 60, 1);
    modules.registerButton("Show Alerts", true);

    registerItems();
    
    modules.registerDescription("by @desiyn");
}

void registerItems() {
    // Armor
    addDisplayColor("chainmail_leggings", "&fChainmail Armor");
    addDisplayColor("iron_leggings", "&fIron Armor");
    addDisplayColor("diamond_leggings", "&bDiamond Armor");
    
    // Weapons
    addDisplayColor("iron_sword", "&fIron Sword");
    addDisplayColor("diamond_sword", "&bDiamond Sword");
    addDisplayColor("diamond_pickaxe", "&bDiamond Pickaxe");
    
    // Special items
    addDisplayColor("ender_pearl", "&3Ender Pearl");
    addDisplayColor("egg", "&eBridge Egg");
    addDisplayColor("fire_charge", "&6Fireball");
    addDisplayColor("Bow", "&2Bow");
    addDisplayColor("obsidian", "&5Obsidian");
    addDisplayColor("tnt", "&cT&fN&cT");
    addDisplayColor("prismarine_shard", "&3Block Zapper");
    
    // Potions
    addDisplayColor("Speed II Potion (45 seconds)", "&bSpeed Potion");
    addDisplayColor("Jump V Potion (45 seconds)", "&aJump Boost Potion");
    addDisplayColor("Invisibility Potion (30 seconds)", "&fInvisibility Potion");
    
    // Special weapons/items
    addDisplayColor("Machine Gun Bow", "&4Machine Gun Bow");
    addDisplayColor("&cDream Defender", "&fIron Golem");
    addDisplayColor("Charlie the Unicorn", "&dCharlie the Unicorn");
    addDisplayColor("Ice Bridge", "&bIce Bridge");
    addDisplayColor("Sleeping Dust", "&cSleeping Dust");
    addDisplayColor("Devastator Bow", "&2Devastator Bow");
    addDisplayColor("Miracle of the Stars", "&eMiracle of the Stars");

    setupItems();
}

void setupItems() {
    heldItemNames.clear();
    heldItemDisplayNames.clear();
    armorPieceNames.clear();

    addName("iron_sword");
    addName("diamond_sword");
    addName("diamond_pickaxe");
    addName("ender_pearl");
    addName("egg");
    addName("fire_charge");
    addName("tnt");
    addName("prismarine_shard");
    addName("obsidian");
    addDisplayName("Bow");
    addDisplayName("Speed II Potion (45 seconds)");
    addDisplayName("Jump V Potion (45 seconds)");
    addDisplayName("Invisibility Potion (30 seconds)");
    addDisplayName("&cDream Defender");
    addDisplayName("Machine Gun Bow");
    addDisplayName("Charlie the Unicorn");
    addDisplayName("Ice Bridge");
    addDisplayName("Sleeping Dust");
    addDisplayName("Devastator Bow");
    addDisplayName("Miracle of the Stars");
    addArmorPiece("chainmail_leggings");
    addArmorPiece("iron_leggings");
    addArmorPiece("diamond_leggings");

    showDistance = modules.getButton(scriptName, "Show Distance");
    showTeammates = modules.getButton(scriptName, "Show Teammates");
    DELAY_INTERVAL = (int)(modules.getSlider(scriptName, "Alert Delay") * 1000);
    debug = modules.getButton(scriptName, "Debug Mode");
    range = (int) modules.getSlider(scriptName, "Detection Range");
}

/* Event Handlers */
void onPreUpdate() {
    Entity playerEntity = client.getPlayer();
    
    if (playerEntity.getTicksExisted() % 100 == 0) {
        status = getBedwarsStatus();
        NetworkPlayer networkPlayer = playerEntity.getNetworkPlayer();
        myName = networkPlayer == null ? playerEntity.getName() : networkPlayer.getName();
        refreshTeams();
        setupItems();
    }

    if (status == 3) {
        handleItemAlerts();
        handleBedAlerts();
    }
}

void onWorldJoin(Entity en) {
    if (en == client.getPlayer()) {
        playerItems.clear();
        status = getBedwarsStatus();
        
        // Find bed position if game is starting
        if (status == 3) {
            client.async(() -> {
                client.sleep(4000);
                bedPosition = findBed(30);
                if (bedPosition != null) {
                    client.print(CHAT_PREFIX + "&aBed found.");
                }
            });
        } else {
            bedPosition = null;
            bedDistance = 0;
        }
    }
}

/* Item Alerts */
void handleItemAlerts() {
    Entity playerEntity = client.getPlayer();
    long now = client.time();
    checkedPlayers.clear();

    List<Entity> players = world.getPlayerEntities();
    for (Entity player : players) {
        if (!shouldCheckPlayer(player, playerEntity)) continue;

        NetworkPlayer nwp = player.getNetworkPlayer();
        String playerDisplay = player.getDisplayName();
        String uuid = nwp.getUUID();
        
        if (checkedPlayers.contains(uuid)) continue;
        
        processPlayerItems(player, playerDisplay, uuid, now, playerEntity);
        checkedPlayers.add(uuid);
    }
}

void processPlayerItems(Entity player, String playerDisplay, String uuid, long now, Entity playerEntity) {
    ItemStack item = player.getHeldItem();
    String itemName = item == null ? "" : item.name;
    ItemStack pants = player.getArmorInSlot(1);
    Map<String, Object> existingData = playerItems.getOrDefault(uuid, new HashMap<>());

    if (item != null) {
        checkHeldItem(item, itemName, existingData, now, playerDisplay, player, playerEntity);
    }
    existingData.put("lastitem", itemName);

    if (pants != null) {
        checkArmorPiece(pants, existingData, playerDisplay, player, playerEntity);
    }

    playerItems.put(uuid, existingData);
}

/* Bed Protection */
void handleBedAlerts() {
    if (bedPosition == null || !modules.getButton(scriptName, "Show Alerts")) return;

    Entity player = client.getPlayer();
    List<Entity> players = world.getPlayerEntities();
    for (Entity p : players) {
        if (!shouldCheckPlayerForBed(p, player)) continue;

        Vec3 ePos = p.getPosition();
        double dX = ePos.x - bedPosition.x;
        double dZ = ePos.z - bedPosition.z;
        
        if (isPlayerNearBed(dX, dZ)) {
            sendBedAlert(p, Math.sqrt(dX * dX + dZ * dZ));
            break;
        }
    }
}

/* Utility Methods */
void addDisplayColor(String key, String value) {
    itemDisplayColors.put(key, util.color(value));
    modules.registerButton(util.color(value), true);
}

void addName(String key) {
    if (modules.getButton(scriptName, itemDisplayColors.get(key))) {
        heldItemNames.add(key);
    }
}

void addDisplayName(String key) {
    if (modules.getButton(scriptName, itemDisplayColors.get(key))) {
        heldItemDisplayNames.add(util.color(key));
    }
}

void addArmorPiece(String key) {
    if (modules.getButton(scriptName, itemDisplayColors.get(key))) {
        armorPieceNames.add(key);
    }
}

boolean shouldCheckPlayer(Entity player, Entity playerEntity) {
    if (player == playerEntity) return false;
    
    NetworkPlayer nwp = player.getNetworkPlayer();
    if (nwp == null) return false;
    
    String playerDisplay = player.getDisplayName();
    if (!showTeammates && playerDisplay.startsWith(util.color("&" + myTeamColor))) return false;
    if (playerDisplay.startsWith(util.color("&7"))) return false;
    
    return true;
}

void checkHeldItem(ItemStack item, String itemName, Map<String, Object> existingData, long now, String playerDisplay, Entity player, Entity playerEntity) {
    boolean heldRaw = heldItemNames.contains(itemName);
    boolean heldDisplay = heldItemDisplayNames.contains(item.displayName);

    if (heldRaw || heldDisplay) {
        String lastItem = existingData.getOrDefault("lastitem", "").toString();
        String name = heldRaw ? itemName : item.displayName;
        long lastTime = Long.parseLong(existingData.getOrDefault(name, "0").toString());

        if (now > lastTime && !lastItem.equals(itemName)) {
            sendItemAlert(playerDisplay, player, name, playerEntity);
            existingData.put(name, now + DELAY_INTERVAL);
        }
    }
}

void checkArmorPiece(ItemStack pants, Map<String, Object> existingData, String playerDisplay, Entity player, Entity playerEntity) {
    String existingArmor = existingData.getOrDefault("armorpiece", "").toString();
    existingData.put("armorpiece", pants.name);
    
    if (armorPieceNames.contains(pants.name) && !existingArmor.isEmpty() && !existingArmor.equals(pants.name)) {
        sendItemAlert(playerDisplay, player, pants.name, playerEntity);
    }
}

void sendItemAlert(String playerDisplay, Entity player, String itemName, Entity playerEntity) {
    String msg = playerDisplay.substring(0, 2) + player.getName() + " &7is holding&r " + itemDisplayColors.get(itemName) + "&r&7";
    if (showDistance) {
        msg += " &7(&d" + (int)playerEntity.getPosition().distanceTo(player.getPosition()) + "m&7)";
    }

    client.print(CHAT_PREFIX + "&eAlert: " + msg);
    
    Map<String, Object> alert = new HashMap<>();
    alert.put("title", ALERT_TITLE_ITEMS);
    alert.put("message", msg);
    alert.put("duration", 3000);
    alert.put("type", 1);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> queue = (List<Map<String, Object>>) bridge.get(ALERT_QUEUE_KEY);
    if (queue == null) {
        queue = new ArrayList<>();
    }
    queue.add(alert);
    bridge.add(ALERT_QUEUE_KEY, queue);
}

boolean shouldCheckPlayerForBed(Entity p, Entity player) {
    if (p == player) return false;
    
    NetworkPlayer nwp = p.getNetworkPlayer();
    if (nwp == null) return false;
    
    String uuid = p.getUUID();
    if (uuid.charAt(14) != '4' && uuid.charAt(14) != '1') return false;
    
    if (p.getDisplayName().startsWith(myTeamColor)) return false;
    if (p.getDisplayName().startsWith(defaultColor)) return false;
    
    return true;
}

boolean isPlayerNearBed(double dX, double dZ) {
    return Math.abs(dX) <= range && Math.abs(dZ) <= range;
}

void sendBedAlert(Entity player, double distance) {
    String msg = player.getDisplayName() + " &r&7is near bed: &d" + util.round(distance, 1) + "&7m";
    
    client.print(CHAT_PREFIX + msg);
    
    Map<String, Object> alert = new HashMap<>();
    alert.put("title", ALERT_TITLE_BED);
    alert.put("message", msg);
    alert.put("duration", 3000);
    alert.put("type", 1);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> queue = (List<Map<String, Object>>) bridge.get(ALERT_QUEUE_KEY);
    if (queue == null) {
        queue = new ArrayList<>();
    }
    queue.add(alert);
    bridge.add(ALERT_QUEUE_KEY, queue);
}

Vec3 findBed(int range) {
    Vec3 playerPos = client.getPlayer().getBlockPosition();
    int startX = (int)playerPos.x - range;
    int startY = (int)playerPos.y - range;
    int startZ = (int)playerPos.z - range;

    for (int x = startX; x <= playerPos.x + range; x++) {
        for (int y = startY; y <= playerPos.y + range; y++) {
            for (int z = startZ; z <= playerPos.z + range; z++) {
                Block block = world.getBlockAt(new Vec3(x, y, z));
                if (block.name.equalsIgnoreCase("bed")) {
                    return new Vec3(x, y, z);
                }
            }
        }
    }
    return null;
}

void refreshTeams() {
    if (status != 3 || client.allowFlying()) return;
    
    String[] colorKeys = {"c", "9", "a", "e", "b", "f", "d", "8"};
    List<NetworkPlayer> networkPlayers = world.getNetworkPlayers();
    for (NetworkPlayer player : networkPlayers) {
        if (!player.getName().equals(myName)) continue;
        
        for (String color : colorKeys) {
            if (player.getDisplayName().startsWith(util.color("&" + color))) {
                myTeamColor = color;
                break;
            }
        }
    }
}

int getBedwarsStatus() {
    List<String> sidebar = world.getScoreboard();
    if (sidebar == null) {
        if (world.getDimension().equals("The End")) {
            return 0;
        }
        return -1;
    }
    
    int size = sidebar.size();
    if (size < 7) return -1;
    
    if (!util.strip(sidebar.get(0)).startsWith("BED WARS")) {
        return -1;
    }
    
    if (util.strip(sidebar.get(5)).startsWith("R Red:") && 
        util.strip(sidebar.get(6)).startsWith("B Blue:")) {
        return 3;
    }
    
    String six = util.strip(sidebar.get(6));
    if (six.equals("Waiting...") || six.startsWith("Starting in")) {
        return 2;
    }
    
    return -1;
}

boolean onPacketSent(CPacket packet) {
    if (!debug) return true;
    
    if (packet instanceof C02) {
        C02 c02 = (C02) packet;
        if (!c02.action.equals("ATTACK")) return true;
        
        Entity en = (Entity)c02.entity;
        if (!en.type.equals("EntityOtherPlayerMP")) return true;
        
        String msg = CHAT_PREFIX + en.getDisplayName().substring(0, 2) + en.getName() + "&7: ";
        ItemStack item = en.getHeldItem();
        if (item == null) {
            msg += "&r'null' / 'null'";
        } else {
            msg += "&r'" + item.name + "&r' &7/ &r'" + item.displayName + "&r' " + item.stackSize;
        }
        client.print(msg);
    }
    return true;
}
