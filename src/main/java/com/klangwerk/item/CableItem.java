package com.klangwerk.item;

import com.klangwerk.Channel;
import com.klangwerk.Config;
import com.klangwerk.block.SpeakerBlock;
import com.klangwerk.block.TurntableBlock;
import com.klangwerk.blockentity.TurntableBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/**
 * Audiokabel (wiederverwendbar):
 * 1) Rechtsklick auf einen Ausgang an der Rueckseite des Plattenspielers (Shift = Ausgang leeren),
 * 2) Rechtsklick auf eine Box (Shift = Box wieder trennen).
 */
public class CableItem extends Item {
    public CableItem(Properties p) { super(p); }

    private static void msg(Player p, String key, Object... args) {
        p.displayClientMessage(Component.translatable("message.klangwerk." + key, args), true);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState st = level.getBlockState(pos);
        Player pl = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        if (pl == null) return InteractionResult.PASS;
        boolean turntable = st.getBlock() instanceof TurntableBlock;
        boolean speaker = st.getBlock() instanceof SpeakerBlock;
        if (!turntable && !speaker) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (turntable) {
            Channel ch = TurntableBlock.portAt(st, pos, ctx.getClickLocation(), ctx.getClickedFace());
            if (ch == null) { msg(pl, "cable.back"); return InteractionResult.CONSUME; }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TurntableBE tb) {
                if (pl.isShiftKeyDown()) {
                    tb.clear(ch);
                    msg(pl, "cable.cleared", ch.label());
                } else {
                    final int chIdx = ch.ordinal();
                    final String dim = level.dimension().location().toString();
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                        tag.putLong("Pos", pos.asLong());
                        tag.putInt("Ch", chIdx);
                        tag.putString("Dim", dim);
                    });
                    msg(pl, "cable.selected", ch.label());
                }
            }
            return InteractionResult.CONSUME;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = data == null ? null : data.copyTag();
        if (tag == null || !tag.contains("Pos")) { msg(pl, "cable.nothing"); return InteractionResult.CONSUME; }
        BlockPos tp = BlockPos.of(tag.getLong("Pos"));
        int r = Config.CABLE_RANGE.get();
        if (!tag.getString("Dim").equals(level.dimension().location().toString()) || tp.distSqr(pos) > (double) r * r) {
            msg(pl, "cable.far", r);
            return InteractionResult.CONSUME;
        }
        if (!(level.getBlockEntity(tp) instanceof TurntableBE tb)) {
            stack.remove(DataComponents.CUSTOM_DATA);
            msg(pl, "cable.lost");
            return InteractionResult.CONSUME;
        }
        int idx = tag.getInt("Ch");
        Channel ch = Channel.values()[Math.max(0, Math.min(idx, Channel.values().length - 1))];
        if (pl.isShiftKeyDown()) {
            msg(pl, tb.unlinkAll(pos) ? "cable.unlinked" : "cable.notlinked");
        } else if (tb.link(ch, pos)) {
            msg(pl, "cable.linked", ch.label());
        } else {
            msg(pl, "cable.full");
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains("Pos");
    }

    @Override
    public void appendHoverText(ItemStack s, Item.TooltipContext ctx, List<Component> tip, TooltipFlag f) {
        tip.add(Component.translatable("tooltip.klangwerk.cable1").withStyle(net.minecraft.ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.klangwerk.cable2").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
