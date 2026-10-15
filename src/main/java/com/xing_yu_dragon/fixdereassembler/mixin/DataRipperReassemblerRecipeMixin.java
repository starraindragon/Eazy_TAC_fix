package com.xing_yu_dragon.fixdereassembler.mixin;

import com.fish_dan_.data_energistics.recipe.reassembler.DataRipperReassemblerIngredient;
import com.fish_dan_.data_energistics.recipe.reassembler.DataRipperReassemblerRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Relaxed item-input matching for the Data Reassembler.
 *
 * <p>The original {@code findMatchingItemInputs} builds a max-flow network and then enforces that
 * EVERY non-empty input slot is fully consumed by the recipe ({@code inputCapacityEdge.flow() > 0}).
 * When you batch-craft thousands of items the AE/Trinity push fills a slot beyond its 512 cap and
 * the excess must spill into extra slots, which the original hard check then rejects, so the machine
 * stops. That behaviour is intentional upstream (to guard against accepting a recipe from a subset of
 * inputs) but it is hostile to batch crafting.
 *
 * <p>This overwrite keeps the two safety checks that matter:
 * <ul>
 *   <li>every non-empty input slot must still be recognised by at least one recipe requirement;</li>
 *   <li>all recipe requirements must still be satisfiable (missing materials are still rejected).</li>
 * </ul>
 * but it no longer requires every slot to be fully consumed. Excess material in a slot is simply
 * left alone and no longer blocks the machine.
 */
@Mixin(DataRipperReassemblerRecipe.class)
public abstract class DataRipperReassemblerRecipeMixin {

    @Shadow
    private ObjectList<DataRipperReassemblerIngredient> getItemInputsForMatching() {
        throw new AssertionError("shadowed");
    }

    /**
     * @author fix-data-reassembler
     * @reason relax the "every input slot must be fully consumed" check so batch crafting works
     *         when a slot's material exceeds the 512-slot cap and spills into extra slots.
     */
    @Overwrite
    public ObjectList<DataRipperReassemblerRecipe.ItemInputAssignment> findMatchingItemInputs(List<ItemStack> inputItems) {
        ObjectList<Integer> inputIndexes = new ObjectArrayList<>();
        ObjectList<ItemStack> nonEmpty = new ObjectArrayList<>();
        for (int index = 0; index < inputItems.size(); index++) {
            ItemStack stack = inputItems.get(index);
            if (!stack.isEmpty()) {
                inputIndexes.add(index);
                nonEmpty.add(stack.copy());
            }
        }
        ObjectList<DataRipperReassemblerIngredient> requirements = getItemInputsForMatching();
        if (nonEmpty.isEmpty() || requirements.isEmpty()) {
            return nonEmpty.isEmpty() && requirements.isEmpty() ? ObjectLists.emptyList() : null;
        }
        // Safety check 1: every non-empty input must be recognised by at least one requirement.
        for (ItemStack input : nonEmpty) {
            boolean recognized = false;
            for (DataRipperReassemblerIngredient requirement : requirements) {
                if (requirement.ingredient().test(input)) {
                    recognized = true;
                    break;
                }
            }
            if (!recognized) {
                return null;
            }
        }
        // Greedy allocation: satisfy every requirement across the non-empty slots.
        // Excess material in a slot is allowed to remain unconsumed (no longer blocks the machine).
        int[] remaining = new int[nonEmpty.size()];
        for (int i = 0; i < nonEmpty.size(); i++) {
            remaining[i] = nonEmpty.get(i).getCount();
        }
        ObjectList<Object2IntOpenHashMap<Integer>> consumedPerRequirement = new ObjectArrayList<>();
        for (DataRipperReassemblerIngredient requirement : requirements) {
            int need = requirement.count();
            Object2IntOpenHashMap<Integer> consumed = new Object2IntOpenHashMap<>();
            for (int i = 0; i < nonEmpty.size() && need > 0; i++) {
                if (requirement.ingredient().test(nonEmpty.get(i))) {
                    int take = Math.min(need, remaining[i]);
                    if (take > 0) {
                        consumed.addTo(i, take);
                        remaining[i] -= take;
                        need -= take;
                    }
                }
            }
            if (need > 0) {
                // Safety check 2: missing material -> still reject.
                return null;
            }
            consumedPerRequirement.add(consumed);
        }
        ObjectList<DataRipperReassemblerRecipe.ItemInputAssignment> assignments = new ObjectArrayList<>();
        for (Object2IntOpenHashMap<Integer> consumed : consumedPerRequirement) {
            for (Object2IntMap.Entry<Integer> entry : consumed.object2IntEntrySet()) {
                assignments.add(new DataRipperReassemblerRecipe.ItemInputAssignment(
                        inputIndexes.get(entry.getKey()), entry.getIntValue()));
            }
        }
        return ObjectLists.unmodifiable(assignments);
    }
}
