package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.ai.goal.target.NearestAttackableWitchTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestHealableRaiderTargetGoal;
import net.minecraft.world.entity.monster.Witch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("AlibabaLowerCamelCaseVariableNaming")
@Mixin(Witch.class)
public interface AccessorWitch {
    @Accessor("healRaidersGoal")
    void mob_controller$setHealRaidersGoal(NearestHealableRaiderTargetGoal<?> goal);

    @Accessor("attackPlayersGoal")
    void mob_controller$setAttackPlayersGoal(NearestAttackableWitchTargetGoal<?> goal);
}
