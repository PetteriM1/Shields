package me.petterim1.shields;

import cn.nukkit.item.ItemID;
import cn.nukkit.item.ItemTool;

public class ItemShieldC extends ItemTool {

    public ItemShieldC() {
        this(0, 1);
    }

    public ItemShieldC(Integer meta) {
        this(meta, 1);
    }

    public ItemShieldC(Integer meta, int count) {
        super(ItemID.SHIELD, meta, count, "Shield");
    }

    @Override
    public int getMaxDurability() {
        return 337;
    }
}
