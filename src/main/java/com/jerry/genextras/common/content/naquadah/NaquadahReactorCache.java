package com.jerry.genextras.common.content.naquadah;

import mekanism.common.lib.multiblock.MultiblockCache;
import mekanism.common.lib.multiblock.MultiblockCache.RejectContents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class NaquadahReactorCache extends MultiblockCache<NaquadahReactorMultiblockData> {
   private double plasmaTemperature = -1.0;
   private int injectionRate = -1;
   private boolean burning;

   private int getInjectionRate() {
      return this.injectionRate == -1 ? 2 : this.injectionRate;
   }

   public void merge(MultiblockCache<NaquadahReactorMultiblockData> mergeCache, RejectContents rejectContents) {
      super.merge(mergeCache, rejectContents);
      this.plasmaTemperature = Math.max(this.plasmaTemperature, ((NaquadahReactorCache)mergeCache).plasmaTemperature);
      this.injectionRate = Math.max(this.injectionRate, ((NaquadahReactorCache)mergeCache).injectionRate);
      this.burning = this.burning | ((NaquadahReactorCache)mergeCache).burning;
   }

   public void apply(NaquadahReactorMultiblockData data, TransactionContext transaction) {
      super.apply(data, transaction);
      if (this.plasmaTemperature >= 0.0) {
         data.setPlasmaTemp(this.plasmaTemperature, transaction);
      }

      data.setInjectionRate(this.getInjectionRate(), transaction);
      data.setBurning(this.burning);
      data.updateTemperatures();
   }

   public void sync(NaquadahReactorMultiblockData data, TransactionContext transaction) {
      super.sync(data, transaction);
      this.plasmaTemperature = data.getPlasmaTemp();
      this.injectionRate = data.getInjectionRate();
      this.burning = data.isBurning();
   }

   public void load(ValueInput input) {
      super.load(input);
      this.plasmaTemperature = input.getDoubleOr("plasma_temp", this.plasmaTemperature);
      this.injectionRate = input.getIntOr("injection_rate", this.injectionRate);
      this.burning = input.getBooleanOr("burning", this.burning);
   }

   public void save(ValueOutput output) {
      super.save(output);
      output.putDouble("plasma_temp", this.plasmaTemperature);
      output.putInt("injection_rate", this.getInjectionRate());
      output.putBoolean("burning", this.burning);
   }
}
