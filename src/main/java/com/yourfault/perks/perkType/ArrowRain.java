package com.yourfault.perks.perkType;

import com.yourfault.perks.PerkCategory;
import com.yourfault.perks.PerkType;

import java.util.Collections;
import java.util.List;

public class ArrowRain extends PerkType {
    protected ArrowRain() {
        super("Arrow Rain", Collections.singletonList("More Projectiles!"), PerkCategory.LEVEL, 99, 0, 0, '\u0007');
    }
}
