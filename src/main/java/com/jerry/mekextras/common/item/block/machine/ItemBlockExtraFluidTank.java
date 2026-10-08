package com.jerry.mekextras.common.item.block.machine;

import java.util.function.Consumer;
import mekanism.api.resource.LargeResourceStack;
import mekanism.api.security.IItemSecurityUtils;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import com.jerry.mekextras.common.block.attribute.ExtraAttribute;
import com.jerry.mekextras.common.block.basic.BlockExtraFluidTank;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.proxy.AutomatedResourceHandler;
import mekanism.common.component.containers.type.ContainerType;
import com.jerry.mekextras.common.item.block.ItemBlockExtraTooltip;
import mekanism.common.item.interfaces.IModeItem.DisplayChange;
import mekanism.common.item.interfaces.IModeItem.IAttachmentBasedModeItem;
import mekanism.common.lib.security.ItemSecurityUtils;
import mekanism.common.lib.transaction.TransactionHelper;
import mekanism.common.registries.MekanismDataComponents;
import com.jerry.mekextras.common.tier.FTTier;
import mekanism.common.tile.interfaces.IFluidContainerManager.ContainerEditMode;
import mekanism.common.util.ItemAccessUtils;
import mekanism.common.util.text.TextUtils;
import mekanism.common.util.text.BooleanStateDisplay.OnOff;
import mekanism.common.util.text.BooleanStateDisplay.YesNo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.TypedInstance;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Fail;
import net.minecraft.world.InteractionResult.Pass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class ItemBlockExtraFluidTank extends ItemBlockExtraTooltip<BlockTile<?, ?>> implements IAttachmentBasedModeItem<Boolean> {
   private final FTTier tier;

   public ItemBlockExtraFluidTank(BlockExtraFluidTank block, Properties properties) {
      this.tier = (FTTier)ExtraAttribute.getAdvancedTier(block, FTTier.class);
      super(block, true, properties.component(MekanismDataComponents.BUCKET_MODE, false).component(MekanismDataComponents.EDIT_MODE, ContainerEditMode.BOTH));
   }

   public FTTier getAdvancedTier() {
      return this.tier;
   }

   protected void addStats(
      ItemStack stack, ItemAccess itemAccess, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag
   ) {
      LargeResourceStack<FluidResource> fluidStack = ContainerType.FLUID.getStoredContentsFromAttachment(itemAccess);
      if (fluidStack.isEmpty()) {
         tooltipAdder.accept(MekanismLang.EMPTY.translateColored(EnumColor.DARK_RED));
      } else {
         tooltipAdder.accept(MekanismLang.GENERIC_STORED_MB.translateColored(EnumColor.PINK,
             fluidStack.resource(), EnumColor.GRAY, TextUtils.format(fluidStack.amount())));
      }
      tooltipAdder.accept(MekanismLang.CAPACITY_MB.translateColored(EnumColor.INDIGO,
          EnumColor.GRAY, TextUtils.format(tier.getStorage())));
   }
   protected void addTypeDetails(
      ItemStack stack, ItemAccess itemAccess, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag flag
   ) {
      tooltipAdder.accept(MekanismLang.BUCKET_MODE.translateColored(EnumColor.INDIGO, new Object[]{YesNo.of((Boolean)this.getMode(itemAccess), true)}));
      super.addTypeDetails(stack, itemAccess, context, tooltipDisplay, tooltipAdder, flag);
   }

   public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
      if ((Boolean)this.getMode(stack) && !entity.isBaby()) {
         Level level = player.level();
         Transaction transaction = TransactionHelper.openTransactionSafe();

         InteractionResult var15;
         label85: {
            label86: {
               InteractionResult var16;
               label87: {
                  label95: {
                     try {
                        ItemAccess itemAccess = ItemAccess.forStack(stack);
                        if (ItemSecurityUtils.get().tryClaimItem(level, player, itemAccess, transaction)) {
                           transaction.commit();
                           var15 = InteractionResult.SUCCESS.heldItemTransformedTo(ItemAccessUtils.asStack(itemAccess));
                           break label85;
                        }

                        if (!IItemSecurityUtils.INSTANCE.canAccessOrDisplayError(player, itemAccess)) {
                           var15 = InteractionResult.FAIL;
                           break label86;
                        }

                        SoundEvent milkSound = this.getMilkSound(entity);
                        if (milkSound != null) {
                           itemAccess = ItemAccess.forPlayerInteraction(player, hand);
                           ResourceHandler<FluidResource> fluidHandler = getOneByOneFluidHandler(itemAccess);
                           if (fluidHandler != null) {
                              if (fluidHandler.insert(FluidResource.of(NeoForgeMod.MILK), 1000, transaction) == 0) {
                                 var16 = InteractionResult.FAIL;
                                 break label87;
                              }

                              player.playSound(milkSound, 1.0F, 1.0F);
                              transaction.commit();
                              var16 = InteractionResult.SUCCESS.heldItemTransformedTo(ItemAccessUtils.asStack(itemAccess));
                              break label95;
                           }
                        }
                     } catch (Throwable var12) {
                        if (transaction != null) {
                           try {
                              transaction.close();
                           } catch (Throwable var11) {
                              var12.addSuppressed(var11);
                           }
                        }

                        throw var12;
                     }

                     if (transaction != null) {
                        transaction.close();
                     }

                     return InteractionResult.PASS;
                  }

                  if (transaction != null) {
                     transaction.close();
                  }

                  return var16;
               }

               if (transaction != null) {
                  transaction.close();
               }

               return var16;
            }

            if (transaction != null) {
               transaction.close();
            }

            return var15;
         }

         if (transaction != null) {
            transaction.close();
         }

         return var15;
      } else {
         return InteractionResult.PASS;
      }
   }

   private @Nullable SoundEvent getMilkSound(LivingEntity entity) {
      if (entity instanceof Goat goat) {
         return goat.isScreamingGoat() ? SoundEvents.GOAT_SCREAMING_MILK : SoundEvents.GOAT_MILK;
      } else if (entity instanceof MushroomCow) {
         return SoundEvents.MOOSHROOM_MILK;
      } else {
         return entity instanceof Cow ? SoundEvents.COW_MILK : null;
      }
   }

   public InteractionResult useOn(UseOnContext context) {
      if (context.getPlayer() == null) {
         return InteractionResult.PASS;
      } else {
         return (InteractionResult)(this.getMode(context.getItemInHand()) ? InteractionResult.PASS : super.useOn(context));
      }
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemAccess itemAccess = ItemAccessUtils.playerHandAccess(player, hand);
      if (!(Boolean)this.getMode(itemAccess.getResource())) {
         return InteractionResult.PASS;
      } else {
         Transaction transaction = TransactionHelper.openTransactionSafe();

         InteractionResult var15;
         label134: {
            label135: {
               Pass var16;
               label136: {
                  InteractionResult var20;
                  label137: {
                     label138: {
                        label139: {
                           label140: {
                              Fail fluidHandler;
                              try {
                                 if (ItemSecurityUtils.get().tryClaimItem(level, player, itemAccess, transaction)) {
                                    transaction.commit();
                                    var15 = InteractionResult.SUCCESS.heldItemTransformedTo(ItemAccessUtils.asStack(itemAccess));
                                    break label134;
                                 }

                                 if (!IItemSecurityUtils.INSTANCE.canAccessOrDisplayError(player, itemAccess)) {
                                    var15 = InteractionResult.FAIL;
                                    break label135;
                                 }

                                 BlockHitResult result = getPlayerPOVHitResult(level, player, player.isShiftKeyDown() ? Fluid.NONE : Fluid.SOURCE_ONLY);
                                 if (result.getType() != Type.BLOCK) {
                                    var16 = InteractionResult.PASS;
                                    break label136;
                                 }

                                 BlockPos pos = result.getBlockPos();
                                 Direction direction = result.getDirection();
                                 BlockPos directionOffsetPos = pos.relative(direction);
                                 if (level.mayInteract(player, pos) && player.mayUseItemAt(directionOffsetPos, direction, ItemAccessUtils.asStack(itemAccess))) {
                                    if (player.isCreative()) {
                                       itemAccess = ItemAccess.forInfiniteMaterials(player, ItemAccessUtils.asStack(itemAccess));
                                    }

                                    ResourceHandler<FluidResource> fluidHandlerx = getOneByOneFluidHandler(itemAccess);
                                    if (fluidHandlerx == null) {
                                       var20 = InteractionResult.FAIL;
                                       break label137;
                                    }

                                    if (player.isShiftKeyDown()) {
                                       if (FluidUtil.tryPlaceFluid(fluidHandlerx, player, level, pos, true, transaction).isEmpty()
                                          && FluidUtil.tryPlaceFluid(fluidHandlerx, player, level, directionOffsetPos, true, transaction).isEmpty()) {
                                          var20 = InteractionResult.FAIL;
                                          break label138;
                                       }
                                    } else if (FluidUtil.tryPickupFluid(fluidHandlerx, player, level, pos, transaction).isEmpty()) {
                                       var20 = InteractionResult.FAIL;
                                       break label139;
                                    }

                                    transaction.commit();
                                    var20 = InteractionResult.SUCCESS.heldItemTransformedTo(ItemAccessUtils.asStack(itemAccess));
                                    break label140;
                                 }

                                 fluidHandler = InteractionResult.FAIL;
                              } catch (Throwable var13) {
                                 if (transaction != null) {
                                    try {
                                       transaction.close();
                                    } catch (Throwable var12) {
                                       var13.addSuppressed(var12);
                                    }
                                 }

                                 throw var13;
                              }

                              if (transaction != null) {
                                 transaction.close();
                              }

                              return fluidHandler;
                           }

                           if (transaction != null) {
                              transaction.close();
                           }

                           return var20;
                        }

                        if (transaction != null) {
                           transaction.close();
                        }

                        return var20;
                     }

                     if (transaction != null) {
                        transaction.close();
                     }

                     return var20;
                  }

                  if (transaction != null) {
                     transaction.close();
                  }

                  return var20;
               }

               if (transaction != null) {
                  transaction.close();
               }

               return var16;
            }

            if (transaction != null) {
               transaction.close();
            }

            return var15;
         }

         if (transaction != null) {
            transaction.close();
         }

         return var15;
      }
   }

   private static @Nullable ResourceHandler<FluidResource> getOneByOneFluidHandler(ItemAccess itemAccess) {
      return AutomatedResourceHandler.manual((ResourceHandler)Capabilities.FLUID.getCapability(itemAccess.oneByOne()));
   }

   public DataComponentType<Boolean> getModeDataType() {
      return (DataComponentType<Boolean>)MekanismDataComponents.BUCKET_MODE.get();
   }

   public Boolean getDefaultMode() {
      return Boolean.FALSE;
   }

   public void changeMode(Player player, ItemAccess itemAccess, int shift, DisplayChange displayChange, TransactionContext transaction) {
      if (Math.abs(shift) % 2 == 1) {
         boolean newState = !(Boolean)this.getMode(itemAccess);
         if (this.setMode(itemAccess, player, newState, transaction)) {
            displayChange.sendMessage(player, newState, s -> MekanismLang.BUCKET_MODE.translate(new Object[]{OnOff.of(s, true)}));
         }
      }
   }

   public <ITEM extends TypedInstance<Item> & DataComponentGetter> Component getScrollTextComponent(ITEM instance) {
      return MekanismLang.BUCKET_MODE.translateColored(EnumColor.GRAY, new Object[]{OnOff.of((Boolean)this.getMode(instance), true)});
   }

   public static class FluidTankCauldronInteraction implements CauldronInteraction {
      public static final ItemBlockExtraFluidTank.FluidTankCauldronInteraction INSTANCE = new ItemBlockExtraFluidTank.FluidTankCauldronInteraction();

      private FluidTankCauldronInteraction() {
      }

      public final InteractionResult interact(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
         if (stack.getItem() instanceof ItemBlockExtraFluidTank tank && (Boolean)tank.getMode(stack)) {
            ItemAccess itemAccess = ItemAccessUtils.playerHandAccess(player, hand, true);
            ResourceHandler<FluidResource> fluidHandler = ItemBlockExtraFluidTank.getOneByOneFluidHandler(itemAccess);
            if (fluidHandler == null || fluidHandler.size() == 0) {
               return InteractionResult.TRY_WITH_EMPTY_HAND;
            } else {
               ResourceHandler<FluidResource> cauldronHandler = (ResourceHandler<FluidResource>)Capabilities.FLUID
                  .getCapabilityIfLoaded(level, pos, state, null, Direction.UP);
               if (cauldronHandler == null) {
                  return InteractionResult.TRY_WITH_EMPTY_HAND;
               } else {
                  Item usedItem = stack.getItem();
                  Transaction transaction = TransactionHelper.openTransactionSafe();

                  InteractionResult var32;
                  label143: {
                     InteractionResult var30;
                     label161: {
                        try {
                           FluidResource targetFluidType = FluidResource.EMPTY;
                           int amountToTransfer = 0;
                           int i = 0;

                           for (int size = cauldronHandler.size(); i < size; i++) {
                              FluidResource resource = (FluidResource)cauldronHandler.getResource(i);
                              if (!resource.isEmpty()) {
                                 Transaction simulation = Transaction.open(transaction);

                                 label117: {
                                    try {
                                       amountToTransfer = fluidHandler.insert(resource, cauldronHandler.getAmountAsInt(i), simulation);
                                       if (amountToTransfer > 0) {
                                          targetFluidType = resource;
                                          break label117;
                                       }
                                    } catch (Throwable var25) {
                                       if (simulation != null) {
                                          try {
                                             simulation.close();
                                          } catch (Throwable var23) {
                                             var25.addSuppressed(var23);
                                          }
                                       }

                                       throw var25;
                                    }

                                    if (simulation != null) {
                                       simulation.close();
                                    }
                                    continue;
                                 }

                                 if (simulation != null) {
                                    simulation.close();
                                 }
                                 break;
                              }
                           }

                           if (!targetFluidType.isEmpty()) {
                              var30 = this.tryTransfer(
                                 level, pos, player, usedItem, targetFluidType, amountToTransfer, fluidHandler, cauldronHandler, false, transaction
                              );
                              break label161;
                           }

                           FluidResource resource = (FluidResource)fluidHandler.getResource(0);
                           if (!resource.isEmpty()) {
                              Transaction simulation = Transaction.open(transaction);

                              try {
                                 amountToTransfer = fluidHandler.extract(resource, fluidHandler.getAmountAsInt(0), simulation);
                              } catch (Throwable var24) {
                                 if (simulation != null) {
                                    try {
                                       simulation.close();
                                    } catch (Throwable var22) {
                                       var24.addSuppressed(var22);
                                    }
                                 }

                                 throw var24;
                              }

                              if (simulation != null) {
                                 simulation.close();
                              }

                              if (amountToTransfer > 0) {
                                 var32 = this.tryTransfer(
                                    level, pos, player, usedItem, resource, amountToTransfer, fluidHandler, cauldronHandler, true, transaction
                                 );
                                 break label143;
                              }
                           }
                        } catch (Throwable var26) {
                           if (transaction != null) {
                              try {
                                 transaction.close();
                              } catch (Throwable var21) {
                                 var26.addSuppressed(var21);
                              }
                           }

                           throw var26;
                        }

                        if (transaction != null) {
                           transaction.close();
                        }

                        return InteractionResult.TRY_WITH_EMPTY_HAND;
                     }

                     if (transaction != null) {
                        transaction.close();
                     }

                     return var30;
                  }

                  if (transaction != null) {
                     transaction.close();
                  }

                  return var32;
               }
            }
         } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }
      }

      private InteractionResult tryTransfer(
         Level level,
         BlockPos pos,
         Player player,
         Item usedItem,
         FluidResource fluid,
         int amountToTransfer,
         ResourceHandler<FluidResource> fluidHandler,
         ResourceHandler<FluidResource> cauldronHandler,
         boolean filledCauldron,
         Transaction transaction
      ) {
         ResourceHandler<FluidResource> handlerToFill = filledCauldron ? cauldronHandler : fluidHandler;
         ResourceHandler<FluidResource> handlerToDrain = filledCauldron ? fluidHandler : cauldronHandler;
         int inserted = handlerToFill.insert(fluid, amountToTransfer, transaction);
         if (inserted > 0 && handlerToDrain.extract(fluid, inserted, transaction) == inserted) {
            if (!level.isClientSide()) {
               player.awardStat(filledCauldron ? Stats.FILL_CAULDRON : Stats.USE_CAULDRON);
               player.awardStat(Stats.ITEM_USED.get(usedItem));
               SoundEvent sound = fluid.getFluidType().getSound(player, level, pos, filledCauldron ? SoundActions.BUCKET_EMPTY : SoundActions.BUCKET_FILL);
               if (sound != null) {
                  level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
               }

               level.gameEvent(null, filledCauldron ? GameEvent.FLUID_PLACE : GameEvent.FLUID_PICKUP, pos);
               transaction.commit();
            }

            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
         }
      }
   }

   public static class FluidTankItemDispenseBehavior extends DefaultDispenseItemBehavior {
      public static final ItemBlockExtraFluidTank.FluidTankItemDispenseBehavior INSTANCE = new ItemBlockExtraFluidTank.FluidTankItemDispenseBehavior();

      private FluidTankItemDispenseBehavior() {
      }

      public ItemStack execute(BlockSource source, ItemStack stack) {
         if (stack.getItem() instanceof ItemBlockExtraFluidTank tank && (Boolean)tank.getMode(stack)) {
            ItemStacksResourceHandler containingHandler = new ItemStacksResourceHandler(2);
            containingHandler.set(0, ItemResource.of(stack), stack.getCount());
            ResourceHandler<FluidResource> resourceHandler = ItemBlockExtraFluidTank.getOneByOneFluidHandler(ItemAccess.forHandlerIndex(containingHandler, 0));
            if (resourceHandler == null) {
               return super.execute(source, stack);
            } else {
               Level level = source.level();
               BlockPos pos = source.pos().relative((Direction)source.state().getValue(DispenserBlock.FACING));
               FluidState fluidState = level.getFluidState(pos);
               Transaction transaction = TransactionHelper.openTransactionSafe();

               ItemStack var13;
               label72: {
                  try {
                     FluidStack result;
                     if (!fluidState.isEmpty() && fluidState.isSource()) {
                        result = FluidUtil.tryPickupFluid(resourceHandler, null, level, pos, transaction);
                     } else {
                        result = FluidUtil.tryPlaceFluid(resourceHandler, null, level, pos, true, transaction);
                     }

                     if (!result.isEmpty()) {
                        transaction.commit();
                        ItemStack stack0 = ItemUtil.getStack(containingHandler, 0);
                        ItemStack stack1 = ItemUtil.getStack(containingHandler, 1);
                        stack0.grow(1);
                        var13 = this.consumeWithRemainder(source, stack0, stack1);
                        break label72;
                     }
                  } catch (Throwable var15) {
                     if (transaction != null) {
                        try {
                           transaction.close();
                        } catch (Throwable var14) {
                           var15.addSuppressed(var14);
                        }
                     }

                     throw var15;
                  }

                  if (transaction != null) {
                     transaction.close();
                  }

                  return super.execute(source, stack);
               }

               if (transaction != null) {
                  transaction.close();
               }

               return var13;
            }
         } else {
            return super.execute(source, stack);
         }
      }
   }
}
