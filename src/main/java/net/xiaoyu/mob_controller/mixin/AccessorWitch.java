package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableWitchTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.monster.Witch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/**
 * 女巫私有目标字段访问器。
 *
 * <p>用于替换女巫的治疗与攻击目标 AI。</p>
 */
@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
@Mixin(Witch.class)
public interface AccessorWitch {
    /**
     * 设置女巫治疗袭击者目标。
     */
    @Accessor("healRaidersGoal")
    void mob_controller$setHealRaidersGoal(NearestHealableRaiderTargetGoal<?> goal);

    /**
     * 设置女巫攻击玩家目标。
     */
    @Accessor("attackPlayersGoal")
    void mob_controller$setAttackPlayersGoal(NearestAttackableWitchTargetGoal<?> goal);
}
