package com.onarandombox.MultiverseCore;

import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.util.Vector;

import com.onarandombox.utils.BlockSafety;
import com.onarandombox.utils.InvalidDestination;
import com.onarandombox.utils.LocationManipulation;
import com.onarandombox.utils.MVDestination;

public class MVTeleport {

    MultiverseCore plugin;

    BlockSafety bs;

    public MVTeleport(MultiverseCore plugin) {
        this.plugin = plugin;
        this.bs = new BlockSafety();
    }

    /**
     * This method will be specific to beds, and check on top of the bed then around it.
     *
     * @return
     */
    public Location getSafeBedDestination(Location bedLocation) {
        // System.out.print(bedLocation);
        Location idealLocation = bedLocation;
        idealLocation.setY(idealLocation.getY() + 1);
        idealLocation.setX(idealLocation.getX() + .5);
        idealLocation.setZ(idealLocation.getZ() + .5);
        // System.out.print(idealLocation);
        if (this.bs.playerCanSpawnHereSafely(idealLocation)) {
            // System.out.print(idealLocation);
            return bedLocation;
        }
        return null;
    }

    private Location getSafeLocation(Location l) {

        // Check around the player first in a configurable radius:
        // TODO: Make this configurable
        Location safe = checkAboveAndBelowLocation(l, 6, 9);
        if (safe != null) {
            safe.setX(safe.getBlockX() + .5);
            safe.setZ(safe.getBlockZ() + .5);
            this.plugin.log(Level.FINE, "Hey! I found one: " + LocationManipulation.strCoordsRaw(safe));
        } else {
            this.plugin.log(Level.FINE, "Uh oh! No safe place found!");
        }
        return safe;
    }

    private Location checkAboveAndBelowLocation(Location l, int tolerance, int radius) {

        // Tolerance must be an even number:
        if (tolerance % 2 != 0) {
            tolerance += 1;
        }
        // We want half of it, so we can go up and down
        tolerance /= 2;
        this.plugin.log(Level.FINER, "Given Location of: " + LocationManipulation.strCoordsRaw(l));
        this.plugin.log(Level.FINER, "Checking +-" + tolerance + " with a radius of " + radius);

        // For now this will just do a straight up block.
        Location locToCheck = l.clone();
        // Check the main level
        Location safe = this.checkAroundLocation(locToCheck, radius);
        if (safe != null) {
            return safe;
        }
        // We've already checked zero right above this.
        int currentLevel = 1;
        while (currentLevel <= tolerance) {
            // Check above
            locToCheck = l.clone();
            locToCheck.add(0, currentLevel, 0);
            safe = this.checkAroundLocation(locToCheck, radius);
            if (safe != null) {
                return safe;
            }

            // Check below
            locToCheck = l.clone();
            locToCheck.subtract(0, currentLevel, 0);
            safe = this.checkAroundLocation(locToCheck, radius);
            if (safe != null) {
                return safe;
            }
            currentLevel++;
        }

        return null;
    }

    /**
     * For my crappy algorithm, radius MUST be odd
     *
     * @param l
     * @param radius
     * @return
     */
    private Location checkAroundLocation(Location l, int diameter) {
        if (diameter % 2 == 0) {
            diameter += 1;
        }
        Location checkLoc = l.clone();

        // Start at 3, the min diameter around a block
        int loopcounter = 3;
        while (loopcounter <= diameter) {
            boolean foundSafeArea = checkAroundSpecificDiameter(checkLoc, loopcounter);
            // If a safe area was found:
            if (foundSafeArea) {
                // Return the checkLoc, it is the safe location.
                return checkLoc;
            }
            // Otherwise, let's reset our location
            checkLoc = l.clone();
            // And increment the radius
            loopcounter += 2;
        }
        return null;
    }

    private boolean checkAroundSpecificDiameter(Location checkLoc, int circle) {
        // Adjust the circle to get how many blocks to step out.
        // A radius of 3 makes the block step 1
        // A radius of 5 makes the block step 2
        // A radius of 7 makes the block step 3
        // ...
        int adjustedCircle = ((circle - 1) / 2);
        checkLoc.add(adjustedCircle, 0, 0);
        if (this.bs.playerCanSpawnHereSafely(checkLoc)) {
            return true;
        }
        // Now we go to the right that adjustedCircle many
        for (int i = 0; i < adjustedCircle; i++) {
            checkLoc.add(0, 0, 1);
            if (this.bs.playerCanSpawnHereSafely(checkLoc)) {
                return true;
            }
        }

        // Then down adjustedCircle *2
        for (int i = 0; i < adjustedCircle * 2; i++) {
            checkLoc.add(-1, 0, 0);
            if (this.bs.playerCanSpawnHereSafely(checkLoc)) {
                return true;
            }
        }

        // Then left adjustedCircle *2
        for (int i = 0; i < adjustedCircle * 2; i++) {
            checkLoc.add(0, 0, -1);
            if (this.bs.playerCanSpawnHereSafely(checkLoc)) {
                return true;
            }
        }

        // Then up Then left adjustedCircle *2
        for (int i = 0; i < adjustedCircle * 2; i++) {
            checkLoc.add(1, 0, 0);
            if (this.bs.playerCanSpawnHereSafely(checkLoc)) {
                return true;
            }
        }

        // Then finish up by doing adjustedCircle - 1
        for (int i = 0; i < adjustedCircle - 1; i++) {
            checkLoc.add(0, 0, 1);
            if (this.bs.playerCanSpawnHereSafely(checkLoc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deprectated. use safelyTeleport(Entity e, Destination d)
     *
     * @param e
     * @param l
     * @return
     */
    @Deprecated
    public boolean safelyTeleport(Entity e, Location l) {
        Location safeLoc = this.getSafeLocation(e, l);
        if (safeLoc != null) {
            e.teleport(safeLoc);
            return true;
        }
        return false;
    }

    /**
     *
     * @param e
     * @param l
     * @return
     */
    @Deprecated
    public Location getSafeLocation(Entity e, Location l) {
        if (this.bs.playerCanSpawnHereSafely(l)) {
            plugin.log(Level.FINE, "The first location you gave me was safe.");
            return l;
        }
        if (e instanceof Minecart) {
            Minecart m = (Minecart) e;
            if (!this.bs.canSpawnCartSafely(m)) {
                return null;
            }
        }
        else if (e instanceof Vehicle) {
            Vehicle v = (Vehicle) e;
            if (!this.bs.canSpawnVehicleSafely(v)) {
                return null;
            }
        }
        Location safeLocation = this.getSafeLocation(l);
        if (safeLocation != null) {
            // Add offset to account for a vehicle on dry land!
            if (e instanceof Minecart && !this.bs.isEntitiyOnTrack(e, safeLocation)) {
                safeLocation.setY(safeLocation.getBlockY() + .5);
                this.plugin.log(Level.FINER, "Player was inside a minecart. Offsetting Y location.");
            }
            this.plugin.log(Level.FINE, "Had to look for a bit, but I found a safe place for ya!");
            return safeLocation;
        }
        if (e instanceof Player) {
            Player p = (Player) e;
            p.sendMessage("No safe locations found!");
            this.plugin.log(Level.FINER, "No safe location found for " + p.getName());
        }
        else if (e.getPassenger() instanceof Player) {
            Player p = (Player) e.getPassenger();
            p.sendMessage("No safe locations found!");
            this.plugin.log(Level.FINER, "No safe location found for " + p.getName());
        }
        this.plugin.log(Level.FINE, "Sorry champ, you're basically trying to teleport into a minefield. I should just kill you now.");
        return null;
    }

    /**
     * Safely teleport the entity to the MVDestination. This will perform checks to see if the place is safe, and if it's not, will adjust the final destination accordingly.
     * @param e Entity to teleport
     * @param d Destination to teleport them to
     * @return true for success, false for failure
     */
    public boolean safelyTeleport(Entity e, MVDestination d) {
        if(d instanceof InvalidDestination) {
            this.plugin.log(Level.FINER, "Entity tried to teleport to an invalid destination");
            return false;
        }

        Location safeLoc = d.getLocation(e);
        if(d.useSafeTeleporter()) {
            safeLoc = this.getSafeLocation(e, d);
        }

        if (safeLoc != null) {
            if(e.teleport(safeLoc)) {
                if (!d.getVelocity().equals(new Vector(0, 0, 0))) {
                    e.setVelocity(d.getVelocity());
                }
            return true;
            }
        }
        return false;
    }

    /**
     * Returns a safe location for the entity to spawn at.
     *
     * @param e The entity to spawn
     * @param d The MVDestination to take the entity to.
     * @return A new location to spawn the entity at.
     */
    public Location getSafeLocation(Entity e, MVDestination d) {
        Location l = d.getLocation(e);
        if (this.bs.playerCanSpawnHereSafely(l)) {
            plugin.log(Level.FINE, "The first location you gave me was safe.");
            return l;
        }
        if (e instanceof Minecart) {
            Minecart m = (Minecart) e;
            if (!this.bs.canSpawnCartSafely(m)) {
                return null;
            }
        }
        else if (e instanceof Vehicle) {
            Vehicle v = (Vehicle) e;
            if (!this.bs.canSpawnVehicleSafely(v)) {
                return null;
            }
        }
        Location safeLocation = this.getSafeLocation(l);
        if (safeLocation != null) {
            // Add offset to account for a vehicle on dry land!
            if (e instanceof Minecart && !this.bs.isEntitiyOnTrack(e, safeLocation)) {
                safeLocation.setY(safeLocation.getBlockY() + .5);
                this.plugin.log(Level.FINER, "Player was inside a minecart. Offsetting Y location.");
            }
            this.plugin.log(Level.FINE, "Had to look for a bit, but I found a safe place for ya!");
            return safeLocation;
        }
        if (e instanceof Player) {
            Player p = (Player) e;
            p.sendMessage("No safe locations found!");
            this.plugin.log(Level.FINER, "No safe location found for " + p.getName());
        }
        else if (e.getPassenger() instanceof Player) {
            Player p = (Player) e.getPassenger();
            p.sendMessage("No safe locations found!");
            this.plugin.log(Level.FINER, "No safe location found for " + p.getName());
        }
        this.plugin.log(Level.FINE, "Sorry champ, you're basically trying to teleport into a minefield. I should just kill you now.");
        return null;
    }

}
