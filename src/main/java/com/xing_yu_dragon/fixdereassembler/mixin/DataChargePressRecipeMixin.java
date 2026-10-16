package com.xing_yu_dragon.fixdereassembler.mixin;

import com.fish_dan_.data_energistics.recipe.chargepress.DataChargePressIngredient;
import com.fish_dan_.data_energistics.recipe.chargepress.DataChargePressRecipe;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * Relaxed item-input matching for the Data Charge Press.
 *
 * <p>The original {@code findMatchingInputSlots} assigns each recipe requirement to a single physical
 * slot and additionally enforces via {@code hasNoUnassignedInputs} that EVERY non-empty input slot is
 * fully assigned. When AE/Trinity batch-crafts and a material exceeds a slot cap, it spills into extra
 * slots; those "extra" slots are then treated as unassigned and the recipe fails, so the machine stops.
 *
 * <p>This overwrite keeps the safety line that every non-empty input slot must be recognised by at
 * least one recipe requirement, but aggregates requirement amounts across all slots and allows excess
 * material to remain unconsumed.
 */
@Mixin(DataChargePressRecipe.class)
public abstract class DataChargePressRecipeMixin {

    @Shadow
    private List<DataChargePressIngredient> inputs() {
        throw new AssertionError("shadowed");
    }

    /**
     * @author fix-data-reassembler
     * @reason relax the "every non-empty input slot must be fully assigned" check so batch crafting
     *         works when a material exceeds a slot cap and spills into extra slots.
     */
    @Overwrite
    public List<DataChargePressRecipe.InputSlot> findMatchingInputSlots(List<ItemStack> inputs) {
        ObjectList<Integer> inputIndexes = new ObjectArrayList<>();
        ObjectList<ItemStack> nonEmpty = new ObjectArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            ItemStack stack = inputs.get(index);
            if (!stack.isEmpty()) {
                inputIndexes.add(index);
                nonEmpty.add(stack.copy());
            }
        }
        List<DataChargePressIngredient> requirements = inputs();
        if (nonEmpty.isEmpty() || requirements.isEmpty()) {
            return ObjectLists.emptyList();
        }
        // Safety: every non-empty input must be recognised by at least one requirement.
        for (ItemStack input : nonEmpty) {
            boolean recognized = false;
            for (DataChargePressIngredient requirement : requirements) {
                if (requirement.ingredient().test(input)) {
                    recognized = true;
                    break;
                }
            }
            if (!recognized) {
                return ObjectLists.emptyList();
            }
        }
        // Greedy allocation across all slots; excess material may remain unconsumed.
        int[] remaining = new int[nonEmpty.size()];
        for (int i = 0; i < nonEmpty.size(); i++) {
            remaining[i] = nonEmpty.get(i).getCount();
        }
        int[] consumed = new int[nonEmpty.size()];
        for (DataChargePressIngredient requirement : requirements) {
            int need = requirement.count();
            for (int i = 0; i < nonEmpty.size() && need > 0; i++) {
                if (requirement.ingredient().test(nonEmpty.get(i))) {
                    int take = Math.min(need, remaining[i]);
                    if (take > 0) {
                        consumed[i] += take;
                        remaining[i] -= take;
                        need -= take;
                    }
                }
            }
            if (need > 0) {
                // Missing material -> still reject.
                return ObjectLists.emptyList();
            }
        }
        ObjectList<DataChargePressRecipe.InputSlot> matches = new ObjectArrayList<>();
        for (int i = 0; i < nonEmpty.size(); i++) {
            if (consumed[i] > 0) {
                matches.add(new DataChargePressRecipe.InputSlot(inputIndexes.get(i), consumed[i]));
            }
        }
        return ObjectLists.unmodifiable(matches);
    }
}
