package dev.arakiel.twilightsparksdelightfabric.event;

import java.util.ArrayList;
import java.util.List;

import dev.arakiel.twilightsparksdelightfabric.TwilightSparksDelightFabric;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Spreads Twilight Forest moss onto the soils of this mod. */
public final class TSDMossEvents {
    private static final ResourceLocation MOSS_ID =
            ResourceLocation.fromNamespaceAndPath("twilightforest", "moss_patch");
    private static final TagKey<Block> MOSS_SPREAD_SOILS =
            TagKey.create(Registries.BLOCK, TwilightSparksDelightFabric.id("moss_spread_soils"));
    private static final ResourceLocation ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(TwilightSparksDelightFabric.MOD_ID, "moss_spread");

    private TSDMossEvents() {
    }

    /** Called from the random tick mixin for every randomly ticking block. */
    public static void onRandomTick(ServerLevel level, BlockPos picked) {
        if (isMoss(level.getBlockState(picked))) {
            spread(level, picked, false);
        } else if (isSoil(level.getBlockState(picked))
                && isMoss(level.getBlockState(picked.above()))) {
            spread(level, picked.above(), false);
        }
    }

    /** Called from the bone meal mixin. */
    public static boolean trySpreadWithBonemeal(Level level, BlockPos pos) {
        return level instanceof ServerLevel serverLevel && isMoss(level.getBlockState(pos))
                && spread(serverLevel, pos, true);
    }

    private static boolean spread(ServerLevel level, BlockPos mossPos, boolean particles) {
        BlockState moss = level.getBlockState(mossPos);
        if (!isMoss(moss) || !isSoil(level.getBlockState(mossPos.below()))) {
            return false;
        }

        List<BlockPos> candidates = new ArrayList<>();
        BlockPos soilPos = mossPos.below();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos candidate = soilPos.offset(dx, 0, dz);
                if (isSoil(level.getBlockState(candidate))
                        && level.getBlockState(candidate.above()).isAir()) {
                    candidates.add(candidate);
                }
            }
        }
        if (candidates.isEmpty()) {
            return false;
        }

        for (int index = candidates.size() - 1; index > 0; index--) {
            int swap = level.getRandom().nextInt(index + 1);
            BlockPos selected = candidates.get(index);
            candidates.set(index, candidates.get(swap));
            candidates.set(swap, selected);
        }
        Block mossBlock = BuiltInRegistries.BLOCK.get(MOSS_ID);
        int count = Math.min(2, candidates.size());
        for (int index = 0; index < count; index++) {
            BlockPos target = candidates.get(index).above();
            level.setBlock(target, mossBlock.defaultBlockState(), Block.UPDATE_ALL);
            if (particles) {
                level.levelEvent(2005, target, 0);
            }
            awardNearby(level, target);
        }
        if (particles) {
            level.levelEvent(2005, mossPos, 0);
        }
        return true;
    }

    private static void awardNearby(ServerLevel level, BlockPos pos) {
        var advancement = level.getServer().getAdvancements().get(ADVANCEMENT_ID);
        if (advancement == null) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 256.0D) {
                player.getAdvancements().award(advancement, "moss_spread");
            }
        }
    }

    private static boolean isMoss(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(MOSS_ID);
    }

    private static boolean isSoil(BlockState state) {
        return state.is(MOSS_SPREAD_SOILS);
    }
}
