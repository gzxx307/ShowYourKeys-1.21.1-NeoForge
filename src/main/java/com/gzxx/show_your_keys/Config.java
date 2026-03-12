package com.gzxx.show_your_keys;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // 开启日志
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .define("logDirtBlock", true);

    // 魔法数字配置
    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    // 魔法数字介绍文本
    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .define("magicNumberIntroduction", "The magic number is... ");

    // 物品列表
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * 验证物品名称是否有效
     * 
     * @param obj 要验证的对象
     * @return 如果对象是有效的物品名称则返回 true
     */
    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
