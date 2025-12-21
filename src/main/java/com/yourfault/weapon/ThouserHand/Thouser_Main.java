package com.yourfault.weapon.ThouserHand;

import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.weapon.WeaponAttachment;
import com.yourfault.weapon.WeaponListener;
import com.yourfault.weapon.WeaponType;

public class Thouser_Main extends WeaponAttachment {
    public Thouser_Main(GamePlayer player) {
        super(WeaponType.ThouserHand,player);
    }
    private static final AnimationInfo ANIMATION_LC = new AnimationInfo("animation_lc",15L);
    private static final AnimationInfo ANIMATION_RC = new AnimationInfo("animation_rc",10L);
    private static final AnimationInfo ANIMATION_FC = new AnimationInfo("animation_fc",40L);


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
