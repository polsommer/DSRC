package script.systems.npcii;

import script.*;
import script.library.create;
import script.library.locations;
import script.library.utils;

public class npcii_spawner extends script.base_script
{
    public npcii_spawner()
    {
    }

    public static final String OBJVAR_GUARD_SPAWNED = "systems.npcii.spawned";
    public static final String OBJVAR_GUARD_NPC_ID = "systems.npcii.npcId";
    public static final String OBJVAR_GUARD_RESPAWN_PENDING = "systems.npcii.respawnPending";
    public static final String OBJVAR_GLOBAL_NPC_ID = "systems.npcii.globalNpcId";
    public static final String OBJVAR_NPC_MANAGER = "systems.npcii.manager";
    public static final String OBJVAR_NPC_MARKER = "systems.npcii.isManagedNpc";
    public static final String SCRIPT_NPCII_BRAIN = "systems.npcii.npcii_brain";
    public static final String SCRIPT_NPCII_BAZAAR_AGENT = "systems.npcii.npcii_bazaar_agent";
    public static final String SCRIPT_NPCII_ACTIVITY_CONTROLLER = "systems.npcii.npcii_activity_controller";
    public static final float INITIALIZE_DELAY = 1.0f;
    public static final float RESPAWN_DELAY_SECONDS = 30.0f;
    public static final String OBJVAR_CREATURE_LEVEL = "creature_attribs.level";
    public static final String SCRIPTVAR_AI_LEVEL = "ai.level";
    public static final String OBJVAR_SPAWN_LOCATION = "npcII.spawner.location";
    public static final String OBJVAR_SPAWN_LOCATIONS = "npcII.spawner.locations";
    public static final String OBJVAR_PATROL_ANCHOR = "systems.npcii.bazaar.patrolAnchor";
    public static final String OBJVAR_ACTIVITY_HOME = "systems.npcii.profile.home";
    public static final String CONFIG_ANCHOR_TABLE = "npciiSpawnAnchorTable";
    public static final String DEFAULT_ANCHOR_TABLE = "datatables/systems/npcii/spawn_anchors.iff";
    public static final float RESOLVE_ANCHOR_RETRY_DELAY_SECONDS = 3.0f;
    public static final float HUB_VALIDATION_RADIUS = 96.0f;
    public static final float GROUND_HEIGHT_DELTA_TOLERANCE = 5.0f;

    public int OnAttach(obj_id self) throws InterruptedException
    {
        if (isManagedNpc(self))
        {
            applyNpcIICombatLevelFields(self);
            return SCRIPT_CONTINUE;
        }
        if (!isManagedNpc(self))
        {
            messageTo(self, "initializeNpcIISpawner", null, INITIALIZE_DELAY, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        if (isManagedNpc(self))
        {
            applyNpcIICombatLevelFields(self);
            return SCRIPT_CONTINUE;
        }
        if (!isManagedNpc(self))
        {
            messageTo(self, "initializeNpcIISpawner", null, INITIALIZE_DELAY, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int initializeNpcIISpawner(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self) || isManagedNpc(self))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id guard = getGuardObject(self);
        if (!isIdValid(guard) || !exists(guard))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id tracker = getGlobalTrackerObject(self, guard);
        if (!isIdValid(tracker) || !exists(tracker))
        {
            tracker = guard;
        }

        obj_id globalNpc = getTrackedNpcFromObject(tracker);
        if (isIdValid(globalNpc) && exists(globalNpc))
        {
            syncLocalGuardToGlobal(self, guard, tracker, globalNpc);
            return SCRIPT_CONTINUE;
        }
        if (isIdValid(globalNpc) && !exists(globalNpc))
        {
            LOG("npcii", "NPCII_GLOBAL_CLEAR_STALE: manager=" + self + " tracker=" + tracker + " staleGlobalNpc=" + globalNpc + " -> 0");
            removeObjVar(tracker, OBJVAR_GLOBAL_NPC_ID);
        }

        if (hasObjVar(guard, OBJVAR_GUARD_NPC_ID))
        {
            obj_id existing = getObjIdObjVar(guard, OBJVAR_GUARD_NPC_ID);
            if (isIdValid(existing) && exists(existing))
            {
                setObjVar(tracker, OBJVAR_GLOBAL_NPC_ID, existing);
                LOG("npcii", "NPCII_GLOBAL_ADOPT_LOCAL: manager=" + self + " guard=" + guard + " tracker=" + tracker + " globalNpc=0 -> " + existing);
                syncLocalGuardToGlobal(self, guard, tracker, existing);
                return SCRIPT_CONTINUE;
            }
            removeObjVar(guard, OBJVAR_GUARD_NPC_ID);
            removeObjVar(guard, OBJVAR_GUARD_SPAWNED);
        }

        if (hasObjVar(guard, OBJVAR_GUARD_RESPAWN_PENDING))
        {
            return SCRIPT_CONTINUE;
        }

        int retryCount = params != null && params.containsKey("anchorRetryCount") ? params.getInt("anchorRetryCount") : 0;
        int alternateAnchorIndex = params != null && params.containsKey("anchorAlternateIndex") ? params.getInt("anchorAlternateIndex") : 0;

        spawnNpcII(self, guard, tracker, retryCount, alternateAnchorIndex);
        return SCRIPT_CONTINUE;
    }

    public int handleNpcIIDestroyed(obj_id self, dictionary params) throws InterruptedException
    {
        if (!isIdValid(self) || !exists(self))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id guard = getGuardObject(self);
        if (!isIdValid(guard) || !exists(guard))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id tracker = getGlobalTrackerObject(self, guard);
        if (!isIdValid(tracker) || !exists(tracker))
        {
            tracker = guard;
        }

        obj_id destroyedNpc = null;
        if (params != null && params.containsKey("npcId"))
        {
            destroyedNpc = params.getObjId("npcId");
        }

        if (hasObjVar(guard, OBJVAR_GUARD_NPC_ID))
        {
            obj_id trackedNpc = getObjIdObjVar(guard, OBJVAR_GUARD_NPC_ID);
            if (!isIdValid(destroyedNpc) || trackedNpc == destroyedNpc)
            {
                removeObjVar(guard, OBJVAR_GUARD_NPC_ID);
                removeObjVar(guard, OBJVAR_GUARD_SPAWNED);
            }
        }

        if (hasObjVar(tracker, OBJVAR_GLOBAL_NPC_ID))
        {
            obj_id globalNpc = getObjIdObjVar(tracker, OBJVAR_GLOBAL_NPC_ID);
            if (!isIdValid(destroyedNpc) || globalNpc == destroyedNpc)
            {
                LOG("npcii", "NPCII_GLOBAL_CLEAR: manager=" + self + " tracker=" + tracker + " globalNpc=" + globalNpc + " -> 0 destroyedNpc=" + destroyedNpc);
                removeObjVar(tracker, OBJVAR_GLOBAL_NPC_ID);
            }
            else
            {
                LOG("npcii", "NPCII_GLOBAL_KEEP: manager=" + self + " tracker=" + tracker + " globalNpc=" + globalNpc + " destroyedNpc=" + destroyedNpc);
            }
        }

        if (!hasObjVar(guard, OBJVAR_GUARD_RESPAWN_PENDING))
        {
            setObjVar(guard, OBJVAR_GUARD_RESPAWN_PENDING, 1);
            messageTo(self, "initializeNpcIISpawner", null, RESPAWN_DELAY_SECONDS, false);
        }
        return SCRIPT_CONTINUE;
    }

    public int OnDestroy(obj_id self) throws InterruptedException
    {
        if (!isManagedNpc(self))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id manager = getObjIdObjVar(self, OBJVAR_NPC_MANAGER);
        if (!isIdValid(manager) || !exists(manager))
        {
            return SCRIPT_CONTINUE;
        }

        dictionary d = new dictionary();
        d.put("npcId", self);
        messageTo(manager, "handleNpcIIDestroyed", d, RESPAWN_DELAY_SECONDS, false);
        return SCRIPT_CONTINUE;
    }

    public int OnUnloadedFromMemory(obj_id self) throws InterruptedException
    {
        if (!isManagedNpc(self))
        {
            return SCRIPT_CONTINUE;
        }

        obj_id manager = getObjIdObjVar(self, OBJVAR_NPC_MANAGER);
        if (!isIdValid(manager) || !exists(manager))
        {
            return SCRIPT_CONTINUE;
        }

        dictionary d = new dictionary();
        d.put("npcId", self);
        messageTo(manager, "handleNpcIIDestroyed", d, 0.0f, false);
        return SCRIPT_CONTINUE;
    }

    private void spawnNpcII(obj_id manager, obj_id guard, obj_id tracker, int retryCount, int alternateAnchorIndex) throws InterruptedException
    {
        location spawnLocation = resolveSpawnAnchor(manager, alternateAnchorIndex);
        if (spawnLocation == null)
        {
            if (retryCount < 2)
            {
                dictionary retry = new dictionary();
                retry.put("anchorRetryCount", retryCount + 1);
                retry.put("anchorAlternateIndex", alternateAnchorIndex + 1);
                LOG("npcii", "NPCII_SPAWN_ANCHOR_RETRY: manager=" + manager + " retry=" + (retryCount + 1) + " alternateIndex=" + (alternateAnchorIndex + 1));
                messageTo(manager, "initializeNpcIISpawner", retry, RESOLVE_ANCHOR_RETRY_DELAY_SECONDS, false);
                return;
            }
            spawnLocation = getLocation(manager);
            LOG("npcii", "NPCII_SPAWN_ANCHOR_FALLBACK_MANAGER_LOC: manager=" + manager + " loc=" + spawnLocation);
        }

        obj_id npc = create.staticObject("npcII", spawnLocation);
        if (!isIdValid(npc) || !exists(npc))
        {
            removeObjVar(guard, OBJVAR_GUARD_SPAWNED);
            removeObjVar(guard, OBJVAR_GUARD_NPC_ID);
            removeObjVar(guard, OBJVAR_GUARD_RESPAWN_PENDING);
            return;
        }

        obj_id currentGlobal = getTrackedNpcFromObject(tracker);
        if (isIdValid(currentGlobal) && exists(currentGlobal) && currentGlobal != npc)
        {
            LOG("npcii", "NPCII_DUPLICATE_CULL_NEW: manager=" + manager + " tracker=" + tracker + " existingGlobalNpc=" + currentGlobal + " duplicateNpc=" + npc);
            destroyObject(npc);
            setObjVar(guard, OBJVAR_GUARD_SPAWNED, 1);
            setObjVar(guard, OBJVAR_GUARD_NPC_ID, currentGlobal);
            removeObjVar(guard, OBJVAR_GUARD_RESPAWN_PENDING);
            return;
        }

        setObjVar(guard, OBJVAR_GUARD_SPAWNED, 1);
        setObjVar(guard, OBJVAR_GUARD_NPC_ID, npc);
        setObjVar(tracker, OBJVAR_GLOBAL_NPC_ID, npc);
        LOG("npcii", "NPCII_GLOBAL_SET: manager=" + manager + " tracker=" + tracker + " globalNpc=" + currentGlobal + " -> " + npc);
        removeObjVar(guard, OBJVAR_GUARD_RESPAWN_PENDING);

        setObjVar(npc, OBJVAR_NPC_MANAGER, manager);
        setObjVar(npc, OBJVAR_NPC_MARKER, 1);
        setObjVar(npc, OBJVAR_PATROL_ANCHOR, spawnLocation);
        setObjVar(npc, OBJVAR_ACTIVITY_HOME, spawnLocation);
        applyNpcIICombatLevelFields(npc);
        if (!hasScript(npc, "systems.npcii.npcii_spawner"))
        {
            attachScript(npc, "systems.npcii.npcii_spawner");
        }
        if (!hasScript(npc, SCRIPT_NPCII_BRAIN))
        {
            attachScript(npc, SCRIPT_NPCII_BRAIN);
        }
        if (!hasScript(npc, SCRIPT_NPCII_BAZAAR_AGENT))
        {
            attachScript(npc, SCRIPT_NPCII_BAZAAR_AGENT);
        }
        if (!hasScript(npc, SCRIPT_NPCII_ACTIVITY_CONTROLLER))
        {
            attachScript(npc, SCRIPT_NPCII_ACTIVITY_CONTROLLER);
        }
    }

    private location resolveSpawnAnchor(obj_id manager, int alternateAnchorIndex) throws InterruptedException
    {
        location explicit = getExplicitManagerSpawnAnchor(manager, alternateAnchorIndex);
        if (isValidSpawnAnchor(explicit))
        {
            return explicit;
        }

        location configAnchor = getZoneConfigSpawnAnchor(manager, alternateAnchorIndex);
        if (isValidSpawnAnchor(configAnchor))
        {
            return configAnchor;
        }

        location fallback = getLocation(manager);
        if (isValidSpawnAnchor(fallback))
        {
            return fallback;
        }
        return null;
    }

    private location getExplicitManagerSpawnAnchor(obj_id manager, int alternateAnchorIndex) throws InterruptedException
    {
        if (hasObjVar(manager, OBJVAR_SPAWN_LOCATIONS))
        {
            location[] anchors = getLocationArrayObjVar(manager, OBJVAR_SPAWN_LOCATIONS);
            if (anchors != null && anchors.length > 0)
            {
                return anchors[Math.abs(alternateAnchorIndex) % anchors.length];
            }
        }
        if (hasObjVar(manager, OBJVAR_SPAWN_LOCATION))
        {
            return getLocationObjVar(manager, OBJVAR_SPAWN_LOCATION);
        }
        return null;
    }

    private location getZoneConfigSpawnAnchor(obj_id manager, int alternateAnchorIndex) throws InterruptedException
    {
        location managerLoc = getLocation(manager);
        String area = managerLoc != null ? toLower(managerLoc.area) : "";
        int cityId = managerLoc != null ? getCityAtLocation(managerLoc, 256) : -1;
        String city = cityId > 0 ? toLower(cityGetName(cityId)) : "";

        String keyedAnchor = getConfigSetting("GameServer", "npciiSpawnAnchor." + area + (city.length() > 0 ? "." + city : ""));
        if (keyedAnchor == null || keyedAnchor.length() <= 0)
        {
            keyedAnchor = getConfigSetting("GameServer", "npciiSpawnAnchor." + area);
        }
        location parsed = parseAnchorConfigValue(keyedAnchor, managerLoc, alternateAnchorIndex);
        if (parsed != null)
        {
            return parsed;
        }

        String table = getConfigSetting("GameServer", CONFIG_ANCHOR_TABLE);
        if (table == null || table.length() <= 0)
        {
            table = DEFAULT_ANCHOR_TABLE;
        }
        if (dataTableGetNumRows(table) <= 0)
        {
            return null;
        }

        location match = null;
        for (int i = 0; i < dataTableGetNumRows(table); i++)
        {
            dictionary row = dataTableGetRow(table, i);
            if (row == null)
            {
                continue;
            }
            String rowArea = row.containsKey("zone") ? toLower(row.getString("zone")) : "";
            String rowCity = row.containsKey("city") ? toLower(row.getString("city")) : "";
            if (rowArea.length() <= 0 || !area.equals(rowArea))
            {
                continue;
            }
            if (city.length() > 0 && rowCity.length() > 0 && !city.equals(rowCity))
            {
                continue;
            }

            float x = row.containsKey("x") ? row.getFloat("x") : managerLoc.x;
            float y = row.containsKey("y") ? row.getFloat("y") : getHeightAtLocation(x, row.containsKey("z") ? row.getFloat("z") : managerLoc.z);
            float z = row.containsKey("z") ? row.getFloat("z") : managerLoc.z;
            location candidate = new location(x, y, z, managerLoc.area);
            if (isValidSpawnAnchor(candidate))
            {
                if (alternateAnchorIndex <= 0)
                {
                    return candidate;
                }
                alternateAnchorIndex--;
                match = candidate;
            }
        }
        return match;
    }

    private location parseAnchorConfigValue(String configValue, location managerLoc, int alternateAnchorIndex) throws InterruptedException
    {
        if (configValue == null || configValue.length() <= 0 || managerLoc == null)
        {
            return null;
        }

        String[] tokens = split(configValue, '|');
        if (tokens == null || tokens.length <= 0)
        {
            tokens = split(configValue, ';');
        }
        if (tokens == null || tokens.length <= 0)
        {
            tokens = new String[]{configValue};
        }

        String selected = tokens[Math.abs(alternateAnchorIndex) % tokens.length];
        String[] xyz = split(selected, ',');
        if (xyz == null || xyz.length < 2)
        {
            return null;
        }

        float x = utils.stringToFloat(xyz[0]);
        float z = utils.stringToFloat(xyz[xyz.length - 1]);
        float y = xyz.length > 2 ? utils.stringToFloat(xyz[1]) : getHeightAtLocation(x, z);
        location parsed = new location(x, y, z, managerLoc.area);
        return isValidSpawnAnchor(parsed) ? parsed : null;
    }

    private boolean isValidSpawnAnchor(location candidate) throws InterruptedException
    {
        if (candidate == null || candidate.area == null || candidate.area.length() <= 0)
        {
            return false;
        }

        location adjusted = locations.getGoodLocationAroundLocation(candidate, 1.0f, 1.0f, 6.0f, 6.0f, false, true);
        if (adjusted == null)
        {
            return false;
        }

        float groundY = getHeightAtLocation(adjusted.x, adjusted.z);
        if (Math.abs(adjusted.y - groundY) > GROUND_HEIGHT_DELTA_TOLERANCE)
        {
            return false;
        }

        obj_id[] nearby = getObjectsInRange(adjusted, HUB_VALIDATION_RADIUS);
        if (nearby == null)
        {
            return false;
        }
        for (int i = 0; i < nearby.length; i++)
        {
            obj_id obj = nearby[i];
            if (!isIdValid(obj) || !exists(obj))
            {
                continue;
            }
            int got = getGameObjectType(obj);
            if (isGameObjectTypeOf(got, GOT_terminal_bazaar) || hasScript(obj, "terminal.npc_vendor") || hasScript(obj, "terminal.nonvendor") || hasObjVar(obj, "gcw.cityPatrol.route"))
            {
                return true;
            }
        }
        return false;
    }

    private obj_id getGuardObject(obj_id self) throws InterruptedException
    {
        location here = getLocation(self);
        return getPlanetByName(here.area);
    }

    private obj_id getGlobalTrackerObject(obj_id self, obj_id fallbackGuard) throws InterruptedException
    {
        obj_id tracker = getPlanetByName("tatooine");
        if (!isIdValid(tracker) || !exists(tracker))
        {
            tracker = fallbackGuard;
        }
        return tracker;
    }

    private obj_id getTrackedNpcFromObject(obj_id tracker) throws InterruptedException
    {
        if (!isIdValid(tracker) || !exists(tracker) || !hasObjVar(tracker, OBJVAR_GLOBAL_NPC_ID))
        {
            return null;
        }
        return getObjIdObjVar(tracker, OBJVAR_GLOBAL_NPC_ID);
    }

    private void syncLocalGuardToGlobal(obj_id manager, obj_id guard, obj_id tracker, obj_id globalNpc) throws InterruptedException
    {
        obj_id localNpc = null;
        if (hasObjVar(guard, OBJVAR_GUARD_NPC_ID))
        {
            localNpc = getObjIdObjVar(guard, OBJVAR_GUARD_NPC_ID);
        }

        if (isIdValid(localNpc) && exists(localNpc) && localNpc != globalNpc)
        {
            LOG("npcii", "NPCII_DUPLICATE_CULL_LOCAL: manager=" + manager + " guard=" + guard + " tracker=" + tracker + " localNpc=" + localNpc + " globalNpc=" + globalNpc);
            destroyObject(localNpc);
        }

        setObjVar(guard, OBJVAR_GUARD_SPAWNED, 1);
        setObjVar(guard, OBJVAR_GUARD_NPC_ID, globalNpc);
        removeObjVar(guard, OBJVAR_GUARD_RESPAWN_PENDING);
        LOG("npcii", "NPCII_UNIQUENESS_SKIP_GLOBAL: manager=" + manager + " guard=" + guard + " tracker=" + tracker + " globalNpc=" + globalNpc + " planet=" + getLocation(manager).area);
    }

    private boolean isManagedNpc(obj_id self) throws InterruptedException
    {
        return hasObjVar(self, OBJVAR_NPC_MARKER) && getIntObjVar(self, OBJVAR_NPC_MARKER) == 1;
    }

    private void applyNpcIICombatLevelFields(obj_id npc) throws InterruptedException
    {
        int level = npcii_profile.BASELINE_COMBAT_LEVEL;
        // Keep runtime and persistence keys aligned for creature combat systems.
        // Expected keys for npcII level state:
        // - current level via setLevel/getLevel
        // - objvar: creature_attribs.level
        // - scriptvar: ai.level
        setLevel(npc, level);
        setObjVar(npc, OBJVAR_CREATURE_LEVEL, level);
        utils.setScriptVar(npc, SCRIPTVAR_AI_LEVEL, level);
    }
}
