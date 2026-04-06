package script.systems.npcii;

public class npcii_profile extends script.base_script
{
    public npcii_profile()
    {
    }

    public static final int BASELINE_COMBAT_LEVEL = 10;
    public static final int MAX_PROGRESSIVE_COMBAT_LEVEL = 30;
    public static final int FORAGE_SUCCESS_PER_LEVEL = 20;
    public static final int PATROL_SUCCESS_PER_LEVEL = 25;
    public static final int COMBAT_WIN_PER_LEVEL = 6;

    public static final float TICK_SECONDS = 5.0f;
    public static final float ROAM_RADIUS = 24.0f;
    public static final float PATROL_STEP_RADIUS = 10.0f;
    public static final float FORAGE_SCAN_RADIUS = 18.0f;
    public static final float COMBAT_SCAN_RADIUS = 26.0f;
    public static final float LEASH_MAX_DISTANCE = 60.0f;
    public static final int CHASE_TIMEOUT_SECONDS = 35;

    public static final int MAX_BEHAVIOR_WEIGHT = 55;
    public static final int MIN_BEHAVIOR_WEIGHT = 5;
    public static final int FORAGE_WEIGHT_CAP = 45;
    public static final int PATROL_WEIGHT_CAP = 50;
    public static final int COMBAT_WEIGHT_CAP = 40;

    public static final String[] APPROVED_FACTIONS =
    {
        "rebel",
        "imperial",
        "pirate",
        "droid"
    };

    public static final String[] PROTECTED_SCRIPTS =
    {
        "npc.static_quest.quest_npc",
        "theme_park.quest_npc",
        "theme_park.static_spawn.egg"
    };

    public static final String[] SCRIPTED_PROTECTED_OBJVARS =
    {
        "quest.isQuestActor",
        "quest.noCombat",
        "theme_park.noAttack",
        "ai.invulnerable"
    };

    public static final String ACTIVITY_FORAGE = "forage";
    public static final String ACTIVITY_PATROL = "patrol";
    public static final String ACTIVITY_COMBAT = "combat";

    public static final int[][] FORAGE_WINDOWS =
    {
        {6, 9},
        {12, 14},
        {18, 20}
    };

    public static final int[][] COMBAT_WINDOWS =
    {
        {0, 3},
        {20, 23}
    };
}
