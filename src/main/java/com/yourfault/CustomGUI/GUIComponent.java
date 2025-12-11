package com.yourfault.CustomGUI;


import org.bukkit.Bukkit;
import org.bukkit.util.Vector;

public class GUIComponent {
    private final int size;
    private String character;
    private final int x;
    private final int y;
    
    public GUIComponent(int size, String character, int x, int y)
    {
        this.size = size;
        this.character = character;
        this.x = x;
        this.y = y;
    }
    public void setCharacter(String character) {
        this.character = character;
    }
    public int getSize() {
        return size*character.length();

    }
    public String getCharacter() {
        return character;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }




}
