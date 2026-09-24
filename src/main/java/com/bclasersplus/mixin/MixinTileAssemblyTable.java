package com.bclasersplus.mixin;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.items.IItemHandlerModifiable;

import buildcraft.api.recipes.IngredientStack;
import buildcraft.lib.net.PacketBufferBC;
import buildcraft.lib.tile.item.ItemHandlerSimple;
import buildcraft.silicon.EnumAssemblyRecipeState;
import buildcraft.silicon.tile.TileAssemblyTable;
import buildcraft.silicon.tile.TileAssemblyTable.AssemblyInstruction;
import buildcraft.silicon.tile.TileLaserTableBase;


@Mixin(value = TileAssemblyTable.class, remap = false)
public abstract class MixinTileAssemblyTable extends TileLaserTableBase {
    @Shadow @Final public static int NET_RECIPE_STATE;
    @Shadow @Final public ItemHandlerSimple inv;
    @Shadow public SortedMap<AssemblyInstruction, EnumAssemblyRecipeState> recipesStates;

    @Unique private final SortedMap<AssemblyInstruction, Collection<IngredientStack>> bclasersplus$savedTargets =
        new TreeMap<>();
    @Unique private final Set<EntityPlayer> bclasersplus$guiPlayers =
        Collections.newSetFromMap(new IdentityHashMap<>());
    @Unique private static final int bclasersplus$REFRESH_NONE = 0;
    @Unique private static final int bclasersplus$REFRESH_TARGETS = 1;
    @Unique private static final int bclasersplus$REFRESH_RECIPES = 2;
    @Unique private int bclasersplus$refreshState = bclasersplus$REFRESH_RECIPES;
    @Unique private boolean bclasersplus$recipeFinished;

    @Invoker(value = "updateRecipes")
    public abstract void bclasersplus$invokeUpdateRecipes();

    @Inject(method = "updateRecipes", at = @At("HEAD"), cancellable = true)
    private void bclasersplus$updateCachedTargets(CallbackInfo ci) {
        int refreshState = bclasersplus$refreshState;

        bclasersplus$refreshState = bclasersplus$REFRESH_NONE;
        if (refreshState == bclasersplus$REFRESH_RECIPES) return;

        if (refreshState == bclasersplus$REFRESH_TARGETS && bclasersplus$updateSavedTargets()) {
            sendNetworkGuiUpdate(NET_GUI_DATA);
        }

        ci.cancel();
    }

    @Inject(method = "updateRecipes", at = @At("TAIL"))
    private void bclasersplus$cacheSavedTargets(CallbackInfo ci) {
        bclasersplus$cacheSavedTargets();
    }

    @Inject(method = "update", at = @At("HEAD"), remap = true)
    private void bclasersplus$resetRecipeFinished(CallbackInfo ci) {
        bclasersplus$recipeFinished = false;
    }

    @Inject(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lbuildcraft/silicon/tile/TileAssemblyTable;updateRecipes()V",
            shift = At.Shift.AFTER,
            remap = false
        ),
        cancellable = true,
        remap = true
    )
    private void bclasersplus$checkActiveRecipeInputs(CallbackInfo ci) {
        long target = getTarget();
        if (target <= 0 || power < target || bclasersplus$canCraftActiveRecipe()) return;

        if (bclasersplus$updateSavedTargets()) sendNetworkGuiUpdate(NET_GUI_DATA);

        ci.cancel();
    }

    @Inject(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lbuildcraft/silicon/tile/TileAssemblyTable;activateNextRecipe()V",
            shift = At.Shift.AFTER,
            remap = false
        ),
        remap = true
    )
    private void bclasersplus$updateTargetsAfterCraft(CallbackInfo ci) {
        bclasersplus$updateSavedTargets();
        bclasersplus$recipeFinished = true;
    }

    @Redirect(
        method = "update",
        at = @At(
            value = "INVOKE",
            target = "Lbuildcraft/silicon/tile/TileAssemblyTable;sendNetworkGuiUpdate(I)V",
            remap = false
        ),
        remap = true
    )
    private void bclasersplus$sendGuiStateAfterCraft(TileAssemblyTable table, int id) {
        if (bclasersplus$recipeFinished) table.sendNetworkGuiUpdate(id);
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"), remap = true)
    private void bclasersplus$invalidateCachedTargets(NBTTagCompound nbt, CallbackInfo ci) {
        bclasersplus$savedTargets.clear();
        bclasersplus$refreshState = bclasersplus$REFRESH_RECIPES;
    }

    @Inject(method = "readPayload", at = @At("TAIL"))
    private void bclasersplus$cacheSelectedTarget(int id, PacketBufferBC buffer, Side side, MessageContext ctx,
        CallbackInfo ci) {
        if (side == Side.SERVER && id == NET_RECIPE_STATE) {
            bclasersplus$cacheSavedTargets();
            if (bclasersplus$updateSavedTargets()) sendNetworkGuiUpdate(NET_GUI_DATA);
        }
    }

    @Override
    public void onPlayerOpen(EntityPlayer player) {
        if (!world.isRemote) {
            bclasersplus$refreshState = bclasersplus$REFRESH_RECIPES;
            bclasersplus$invokeUpdateRecipes();
        }

        super.onPlayerOpen(player);

        if (!world.isRemote) bclasersplus$guiPlayers.add(player);
    }

    @Override
    public void onPlayerClose(EntityPlayer player) {
        if (!world.isRemote) bclasersplus$guiPlayers.remove(player);

        super.onPlayerClose(player);
    }

    @Override
    protected void onSlotChange(IItemHandlerModifiable handler, int slot, ItemStack before, ItemStack after) {
        super.onSlotChange(handler, slot, before, after);
        if (handler == inv && bclasersplus$refreshState == bclasersplus$REFRESH_NONE) {
            if (!bclasersplus$guiPlayers.isEmpty()) {
                // If we have someone viewing the GUI, refresh the recipes every time inventory changes,
                // so things stay responsive
                bclasersplus$refreshState = bclasersplus$REFRESH_RECIPES;
            } else if (getTarget() == 0) {
                // Otherwise, we only need to refresh in the (rare) case we have no active target
                // If we have a target, they will naturally refresh when the it finishes processing
                bclasersplus$refreshState = bclasersplus$REFRESH_TARGETS;
            }
        }
    }

    @Unique
    private void bclasersplus$cacheSavedTargets() {
        bclasersplus$savedTargets.clear();
        for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
            if (entry.getValue() != EnumAssemblyRecipeState.POSSIBLE) {
                AssemblyInstruction instruction = entry.getKey();
                bclasersplus$savedTargets.put(instruction, instruction.recipe.getInputsFor(instruction.output));
            }
        }
    }

    @Unique
    private boolean bclasersplus$canCraftActiveRecipe() {
        for (Map.Entry<AssemblyInstruction, EnumAssemblyRecipeState> entry : recipesStates.entrySet()) {
            if (entry.getValue() != EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) continue;

            AssemblyInstruction instruction = entry.getKey();
            Collection<IngredientStack> inputs = bclasersplus$savedTargets.get(instruction);
            if (inputs == null) {
                inputs = instruction.recipe.getInputsFor(instruction.output);
                bclasersplus$savedTargets.put(instruction, inputs);
            }

            return extract(inv, inputs, true, false);
        }

        return false;
    }

    @Unique
    private boolean bclasersplus$updateSavedTargets() {
        boolean findActive = false;
        boolean changed = false;
        for (Map.Entry<AssemblyInstruction, Collection<IngredientStack>> entry : bclasersplus$savedTargets.entrySet()) {
            AssemblyInstruction instruction = entry.getKey();
            EnumAssemblyRecipeState state = recipesStates.get(instruction);
            if (state == null || state == EnumAssemblyRecipeState.POSSIBLE) continue;

            boolean enough = extract(inv, entry.getValue(), true, false);
            if (enough) {
                if (state == EnumAssemblyRecipeState.SAVED) {
                    state = EnumAssemblyRecipeState.SAVED_ENOUGH;
                    changed = true;
                }

            } else if (state != EnumAssemblyRecipeState.SAVED) {
                state = EnumAssemblyRecipeState.SAVED;
                changed = true;
            }

            if (state == EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE) findActive = true;

            recipesStates.put(instruction, state);
        }

        if (!findActive) {
            for (AssemblyInstruction instruction : bclasersplus$savedTargets.keySet()) {
                EnumAssemblyRecipeState state = recipesStates.get(instruction);
                if (state == EnumAssemblyRecipeState.SAVED_ENOUGH) {
                    recipesStates.put(instruction, EnumAssemblyRecipeState.SAVED_ENOUGH_ACTIVE);
                    changed = true;
                    break;
                }
            }
        }

        return changed;
    }
}
