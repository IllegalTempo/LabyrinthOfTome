package com.yourfault.weapon.ThouserHand;

import com.yourfault.Attachments.Godhand;
import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.weapon.WeaponAttachment;
import com.yourfault.weapon.WeaponListener;
import com.yourfault.weapon.WeaponType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class Thouser_Main extends WeaponAttachment {
    public Thouser_Main(GamePlayer player) {
        super(WeaponType.ThouserHand,player);
        addAdditionalHand();
    }
    private static final AnimationInfo ANIMATION_LC = new AnimationInfo("animation_lc",15L);
    private static final AnimationInfo ANIMATION_RC = new AnimationInfo("animation_rc",10L);
    private static final AnimationInfo ANIMATION_FC = new AnimationInfo("animation_fc",40L);
    public List<Godhand> hands = new ArrayList<>();
    public void addAdditionalHand()
    {
        hands.add(new Godhand(player,new Vector(generateOffset(hands.size()),3,-3)));
    }

    private int generateOffset(int i) {
        if (i == 0) return 0;
        return (i % 2 == 0) ? i / 2 : -(i + 1) / 2;
    }

    @Override
    public void onSwitchorRemoveWeapon() {
        for(Godhand hand : hands)
        {
            hand.destroy();
        }
        hands.clear();
    }

    @Override
    public void LC() {
        
    }

    @Override
    public void RC() {

    }

    @Override
    public void FC() {

    }
}
