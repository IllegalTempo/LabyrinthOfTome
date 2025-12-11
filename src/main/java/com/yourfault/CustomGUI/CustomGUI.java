package com.yourfault.CustomGUI;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.ARGBLike;
import net.minecraft.network.chat.ClickEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CustomGUI {
    private final List<GUIComponent> actionBarComponents;
    public CustomGUI(List<GUIComponent> components)
    {
        this.actionBarComponents = components;

    }
    public void addActionBarComponent(GUIComponent component)
    {
        actionBarComponents.add(component);
    }
    public GUIComponent getActionBarComponent(int index)
    {
        return actionBarComponents.get(index);
    }

    public void DisplayActionBar(Player p)
    {
        int totalSize = actionBarComponents.stream().mapToInt(c -> (int)c.getSize()).sum();
        Component barSegment = Component.text("").font(Key.key("minecraft:bitmaps")).shadowColor(ShadowColor.shadowColor(0));
        int accum_centerx = -totalSize/2 - actionBarComponents.getFirst().getSize()/2;


        for(int i = 0 ; i < actionBarComponents.size(); i++)
        {
            accum_centerx += actionBarComponents.get(i).getSize();
            GUIComponent component = actionBarComponents.get(i);
            int offsetx = ((-accum_centerx + component.getX())*510/1920) + 127;
            int offsety = (component.getY()*510/1080) + 127;

            Component segment = Component.text(component.getCharacter()).color(TextColor.color(78,offsetx,offsety));
            barSegment = barSegment.append(segment);


        }

        p.sendActionBar(barSegment);

    }

}
