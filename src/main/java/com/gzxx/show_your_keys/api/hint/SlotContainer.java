package com.gzxx.show_your_keys.api.hint;

import com.gzxx.show_your_keys.api.registry.SlotRegistry;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 单个按键槽位的提示容器。
 *
 * <p>每个 {@code SlotContainer} 对应一个槽位（如 {@link HintSlot#USE}、{@link HintSlot#ATTACK}），
 * 内部存储该槽位下所有的 {@link Hint}。</p>
 *
 * <h3>同键去重规则（解决"同一按键显示多个相互矛盾的动作"问题）</h3>
 * <p>调用 {@link #add(Hint)} 时，以 {@link Hint#keyLabel()} 的文本为键进行去重：</p>
 * <ul>
 *   <li>若容器内尚无该按键的提示 → 直接插入</li>
 *   <li>若已存在相同按键的提示，且新提示的 {@code priority} 更小（优先级更高）→ 替换</li>
 *   <li>否则 → 丢弃新提示（保留已有的高优先级提示）</li>
 * </ul>
 * <p>这确保了同一按键在同一槽位最多只显示一条提示，即优先级最高的那条。</p>
 *
 * <h3>显示顺序</h3>
 * <p>{@link #getHints()} 按 {@link Hint#priority()} 升序返回（数值越小越靠上）。</p>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 * // 单条添加
 * SlotContainer useSlot = new SlotContainer(HintSlot.USE);
 * useSlot.add(Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.interact"));
 *
 * // 工厂方法批量创建
 * SlotContainer useSlot = SlotContainer.of(HintSlot.USE,
 *     Hint.fromMapping(mc.options.keyUse, "hint.show_your_keys.interact")
 * );
 * }</pre>
 */
public final class SlotContainer {

    private final String slotId;

    /**
     * 以按键标签文本为键的提示映射。
     * <p>使用 {@link LinkedHashMap} 保留插入顺序，以保证 priority 相同时先插入者优先。</p>
     */
    private final Map<String, Hint> hintsByKey = new LinkedHashMap<>();

    /**
     * 构造指定槽位的空容器。
     *
     * @param slotId 槽位 ID，见 {@link HintSlot} 内置常量
     */
    public SlotContainer(String slotId) {
        this.slotId = slotId;
    }

    /**
     * 工厂方法：构造容器并批量填入提示。
     *
     * @param slotId 槽位 ID
     * @param hints  初始提示列表
     * @return 填充完毕的容器
     */
    public static SlotContainer of(String slotId, Hint... hints) {
        SlotContainer container = new SlotContainer(slotId);
        for (Hint hint : hints) {
            container.add(hint);
        }
        return container;
    }

    // ── 添加 ──────────────────────────────────────────────────────────────────

    /**
     * 向容器添加一条提示，并按同键去重规则处理冲突。
     *
     * <p>去重规则：</p>
     * <ul>
     *   <li>若该按键尚无提示 → 插入</li>
     *   <li>若已有相同按键且新提示优先级更高（{@code hint.priority() < existing.priority()}）→ 替换</li>
     *   <li>否则 → 忽略（保留更高优先级的已有提示）</li>
     * </ul>
     *
     * @param hint 要添加的提示
     */
    public void add(Hint hint) {
        String key = hint.keyLabel().getString();
        Hint existing = hintsByKey.get(key);
        if (existing == null || hint.priority() < existing.priority()) {
            hintsByKey.put(key, hint);
        }
        // hint.priority() >= existing.priority() 时忽略，保留更高优先级的已有条目
    }

    /**
     * 将另一个 {@code SlotContainer} 的所有提示合并到当前容器。
     * <p>合并时仍遵循同键去重规则，按键冲突时保留两者中优先级更高的那条。</p>
     *
     * @param other 来源容器，其 {@code slotId} 应与当前容器相同
     */
    public void merge(SlotContainer other) {
        // getHints() 返回 priority 升序列表，保证合并时优先级高的先被处理
        for (Hint hint : other.getHints()) {
            add(hint);
        }
    }

    // ── 查询 ──────────────────────────────────────────────────────────────────

    /**
     * 获取槽位 ID。
     *
     * @return 槽位 ID 字符串
     */
    public String getSlotId() {
        return slotId;
    }

    /**
     * 容器是否为空。
     *
     * @return 若无任何提示则返回 {@code true}
     */
    public boolean isEmpty() {
        return hintsByKey.isEmpty();
    }

    /**
     * 返回去重后按优先级升序排列的提示列表（只读）。
     * <p>priority 数值越小，在 HUD 中越靠上显示。</p>
     *
     * @return 不可修改的有序提示列表
     */
    public List<Hint> getHints() {
        return hintsByKey.values().stream()
                .sorted(Comparator.comparingInt(Hint::priority))
                .toList();
    }

    /**
     * 获取此槽位在 HUD 中的全局排序值，由 {@link SlotRegistry} 管理。
     * <p>值越小，槽位在面板中越靠上显示。</p>
     *
     * @return 槽位排序值
     */
    public int getSlotOrder() {
        return SlotRegistry.INSTANCE.getOrder(slotId);
    }
}