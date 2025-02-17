void onLoad() {
    modules.registerDescription("by @desiyn");
}

// Enemy tracking
Entity enemy;
String enemyName = "";

// Combat tracking
int auratargetcheck = 0;
int auratargetsent = 0;
int enemykilledsent = 1;
float enemy_currenthealth = 0;
float enemy_previoushealth = 0;
float healthdifference = 0;
Entity lastTarget;
String lastTargetName = "";
int misssentcheck = 0;
int hurttimecounter = 0;

// Self tracking
Entity self;

void onPreUpdate() {
    // Get current entities
    enemy = modules.getKillAuraTarget();
    self = client.getPlayer();
    if (self == null) return;

    // Process enemy data
    if (enemy != null) {
        // Update enemy info
        enemyName = enemy.getDisplayName();
    } else {
        // Handle enemy death/disappearance
        if (lastTarget != null && lastTarget.isDead() && enemykilledsent == 0) {
            client.print("&7[&dR&7] " + lastTargetName + "&7 killed.");
            enemykilledsent = 1;
        }
        
        // Reset tracking
        auratargetcheck = 0;
        auratargetsent = 0;
    }

    // Call processCombat to handle combat logging
    processCombat();
}

void processCombat() {
    if (enemy == null || self == null) return;

    double distance = enemy.getPosition().distanceTo(self.getPosition());
    double swingProgress = self.getSwingProgress();

    // Miss detection
    if (swingProgress != 0 && 
        enemy_previoushealth == enemy_currenthealth && 
        enemy.getHurtTime() == 0 && 
        distance < 3 && 
        misssentcheck == 0 && 
        hurttimecounter >= 50) {
        
        misssentcheck = 1;
        hurttimecounter = 0;
        client.print("&7[&dR&7]&c Missed swing on " + lastTargetName + "&c due to &ccorrection.");
    }

    // Reset miss check on hurt
    if (enemy.getHurtTime() != 0) {
        misssentcheck = 0;
    }

    // Increment hurt counter
    if (enemy.getHurtTime() == 0 && swingProgress != 0 && distance < 2.99) {
        hurttimecounter = Math.min(hurttimecounter + 1, 100); // Cap counter
    }

    // Target change detection
    if (lastTarget != enemy) {
        enemykilledsent = 0;
        lastTarget = enemy;
        lastTargetName = enemy.getDisplayName();
    }

    // Health tracking
    enemy_previoushealth = enemy.getHealth();

    if (auratargetcheck == 0 && auratargetsent == 0) {
        enemy_currenthealth = enemy.getHealth();
        auratargetcheck = 1;
        auratargetsent = 1;
    }

    // Damage detection
    if (enemy_previoushealth < enemy_currenthealth) {
        healthdifference = enemy_previoushealth - enemy_currenthealth;
        enemy_currenthealth = enemy_previoushealth;
        client.print("&7[&dR&7]&7 " + lastTargetName + " &c-" + 
            String.format("%.2f", Math.abs(healthdifference)) + 
            " \u2764 &7(" + String.format("%.2f", enemy_currenthealth) + " \u2764 remaining)");
    }
}
