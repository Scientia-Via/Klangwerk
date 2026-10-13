package com.klangwerk;

import com.klangwerk.block.GramophoneBlock;
import com.klangwerk.block.SpeakerBlock;
import com.klangwerk.block.TurntableBlock;
import com.klangwerk.blockentity.GramophoneBE;
import com.klangwerk.blockentity.TurntableBE;
import com.klangwerk.item.CableItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Klangwerk.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Klangwerk.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Klangwerk.MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Klangwerk.MODID);

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion();
    }

    private static DeferredHolder<Block, Block> block(String name, Supplier<Block> sup) {
        DeferredHolder<Block, Block> b = BLOCKS.register(name, sup);
        ITEMS.register(name, () -> new BlockItem(b.get(), new Item.Properties()));
        return b;
    }

    public static final DeferredHolder<Block, Block> TURNTABLE = block("turntable", () -> new TurntableBlock(props()));
    public static final DeferredHolder<Block, Block> GRAMOPHONE = block("gramophone", () -> new GramophoneBlock(props()));
    public static final DeferredHolder<Block, Block> SPEAKER_TREBLE = block("speaker_treble", () -> new SpeakerBlock(SpeakerType.TREBLE, props()));
    public static final DeferredHolder<Block, Block> SPEAKER_MID = block("speaker_mid", () -> new SpeakerBlock(SpeakerType.MID, props()));
    public static final DeferredHolder<Block, Block> SPEAKER_BASS = block("speaker_bass", () -> new SpeakerBlock(SpeakerType.BASS, props()));

    public static final DeferredHolder<Item, Item> CABLE = ITEMS.register("audio_cable", () -> new CableItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TurntableBE>> TURNTABLE_BE = BES.register("turntable",
            () -> BlockEntityType.Builder.of(TurntableBE::new, TURNTABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GramophoneBE>> GRAMOPHONE_BE = BES.register("gramophone",
            () -> BlockEntityType.Builder.of(GramophoneBE::new, GRAMOPHONE.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.klangwerk"))
            .icon(() -> new ItemStack(TURNTABLE.get()))
            .displayItems((params, out) -> {
                out.accept(GRAMOPHONE.get());
                out.accept(TURNTABLE.get());
                out.accept(SPEAKER_TREBLE.get());
                out.accept(SPEAKER_MID.get());
                out.accept(SPEAKER_BASS.get());
                out.accept(CABLE.get());
            }).build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BES.register(bus);
        TABS.register(bus);
        bus.addListener(ModRegistry::addToVanillaTabs);
    }

    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            e.accept(GRAMOPHONE.get());
            e.accept(TURNTABLE.get());
            e.accept(SPEAKER_TREBLE.get());
            e.accept(SPEAKER_MID.get());
            e.accept(SPEAKER_BASS.get());
        } else if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(CABLE.get());
        }
    }
}
