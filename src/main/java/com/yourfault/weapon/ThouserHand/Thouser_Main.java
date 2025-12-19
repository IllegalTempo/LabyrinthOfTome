package com.yourfault.weapon.ThouserHand;

import com.yourfault.system.GeneralPlayer.GamePlayer;
import com.yourfault.utils.AnimationInfo;
import com.yourfault.weapon.WeaponListener;
import com.yourfault.weapon.WeaponType;

public class Thouser_Main extends WeaponListener {
    public Thouser_Main() {
        super(WeaponType.ThouserHand,5f,10f,20f);
    }
    private static final AnimationInfo ANIMATION_LC = new AnimationInfo("animation_lc",15L);
    private static final AnimationInfo ANIMATION_RC = new AnimationInfo("animation_rc",10L);
    private static final AnimationInfo ANIMATION_FC = new AnimationInfo("animation_fc",40L);


    @Override
    public void LC(GamePlayer player) {
        
    }

    @Override
    public void RC(GamePlayer player) {

    }

    @Override
    public void FC(GamePlayer player) {

    }
}
