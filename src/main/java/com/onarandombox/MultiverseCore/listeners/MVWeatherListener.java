package com.onarandombox.MultiverseCore.listeners;

import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.WeatherListener;

import com.onarandombox.MultiverseCore.MVWorld;
import com.onarandombox.MultiverseCore.MultiverseCore;

public class MVWeatherListener extends WeatherListener {
    private MultiverseCore plugin;

    public MVWeatherListener(MultiverseCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onWeatherChange(WeatherChangeEvent event) {

        MVWorld world = this.plugin.getWorldManager().getMVWorld(event.getWorld().getName());
        if (world != null) {
            // If it's going to start raining and we have weather disabled
            event.setCancelled((event.toWeatherState() && !world.getWeatherEnabled()));
        }
    }

    @Override
    public void onThunderChange(ThunderChangeEvent event) {

        MVWorld world = this.plugin.getWorldManager().getMVWorld(event.getWorld().getName());
        if (world != null) {
            // If it's going to start raining and we have weather disabled
            event.setCancelled((event.toThunderState() && !world.getWeatherEnabled()));
        }
    }
}
