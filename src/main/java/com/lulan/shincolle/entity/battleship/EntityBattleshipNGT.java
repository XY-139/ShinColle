package com.lulan.shincolle.entity.battleship;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackOnCollide;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveTowardsTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIOwnerHurtByTarget;
import net.minecraft.entity.ai.EntityAIOwnerHurtTarget;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIRestrictOpenDoor;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIWatchClosest2;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.ai.EntityAIShipAttackOnCollide;
import com.lulan.shincolle.ai.EntityAIShipFlee;
import com.lulan.shincolle.ai.EntityAIShipFollowOwner;
import com.lulan.shincolle.ai.EntityAIShipInRangeTarget;
import com.lulan.shincolle.ai.EntityAIShipRangeAttack;
import com.lulan.shincolle.ai.EntityAIShipFloating;
import com.lulan.shincolle.ai.EntityAIShipSit;
import com.lulan.shincolle.ai.EntityAIShipWatchClosest;
import com.lulan.shincolle.client.inventory.ContainerShipInventory;
import com.lulan.shincolle.entity.BasicEntityShipSmall;
import com.lulan.shincolle.entity.ExtendShipProps;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.ParticleHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

/**�S��heavy attack:
 * ��StateEmotion[ID.S.Phase]���x�s�������q
 * Phase 1:���� 2:�z�� 3:���� 
 */
public class EntityBattleshipNGT extends BasicEntityShipSmall {

	public EntityBattleshipNGT(World world) {
		super(world);
		this.setSize(0.8F, 1.6F);	//�I���j�p ��ҫ��j�p�L��
//		this.setCustomNameTag(StatCollector.translateToLocal("entity.shincolle.EntityBattleshipNGT.name"));
		this.ShipType = ID.ShipType.BATTLESHIP;
		this.ShipID = ID.S_BattleshipNagato;
		this.ModelPos = new float[] {0F, 15F, 0F, 40F};
		ExtProps = (ExtendShipProps) getExtendedProperties(ExtendShipProps.SHIP_EXTPROP_NAME);
		
		this.initTypeModify();
	}
	
	//for morph
	@Override
	public float getEyeHeight() {
		return this.height;
	}
	
	//equip type: 1:cannon+misc 2:cannon+airplane+misc 3:airplane+misc
	@Override
	public int getEquipType() {
		return 1;
	}
	
	@Override
	public void setAIList() {
		super.setAIList();

		//use range attack (light)
		this.tasks.addTask(11, new EntityAIShipRangeAttack(this));			   //0011
	}
    
    //check entity state every tick
  	@Override
  	public void onLivingUpdate() {
  		super.onLivingUpdate();
          
  		if(worldObj.isRemote) {
  			if(this.ticksExisted % 5 == 0) {
  				if(getStateEmotion(ID.S.Phase) > 0) {
   	  				//�ͦ���u�S��
  	  				ParticleHelper.spawnAttackParticleAtEntity(this, 0.1D, 1D, 0D, (byte)1);
  				}
			
  				if(getStateEmotion(ID.S.State) >= ID.State.EQUIP00) {
  					double smokeY = posY + 1.6D;
  					if(this.isSitting()) smokeY = posY + 0.9D;
  					
  					//�p�������m
  	  				float[] partPos = ParticleHelper.rotateParticleByAxis(-0.55F, 0F, (this.renderYawOffset % 360) / 57.2957F, 1F);
  	  				//�ͦ��˳ƫ_�ϯS��
  	  				ParticleHelper.spawnAttackParticleAt(posX+partPos[1], smokeY, posZ+partPos[0], 0D, 0D, 0D, (byte)20);
  				}	
  			}
  		}    
  	}
  	
  	@Override
  	public boolean interact(EntityPlayer player) {	
		ItemStack itemstack = player.inventory.getCurrentItem();  //get item in hand
		
		//use cake to change state
		if(itemstack != null) {
			if(itemstack.getItem() == Items.cake) {
				switch(getStateEmotion(ID.S.State)) {
				case ID.State.NORMAL:
					setStateEmotion(ID.S.State, ID.State.EQUIP00, true);
					break;
				case ID.State.EQUIP00:
					setStateEmotion(ID.S.State, ID.State.NORMAL, true);
					break;			
				}
				return true;
			}
		}
		
		super.interact(player);
		return false;
  	}
  	
  	//�ק�����S��
  	@Override
  	public boolean attackEntityWithAmmo(Entity target) {
  		//get attack value
		float atk = StateFinal[ID.ATK];
		//set knockback value (testing)
		float kbValue = 0.05F;
		//update entity look at vector (for particle spawn)
        //����k��getLook�٥��T (client sync���D)
        float distX = (float) (target.posX - this.posX);
        float distY = (float) (target.posY - this.posY);
        float distZ = (float) (target.posZ - this.posZ);
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        distX = distX / distSqrt;
        distY = distY / distSqrt;
        distZ = distZ / distSqrt;
      
        //�o�g�̷����S��
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 19, this.posX, this.posY+0.3D, this.posZ, distX, 1F, distZ, true), point);

		//play cannon fire sound at attacker
        playSound(Reference.MOD_ID+":ship-firesmall", ConfigHandler.fireVolume, 0.7F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        //play entity attack sound
        if(this.rand.nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //experience++
  		addShipExp(2);
  		
  		//grudge--
  		decrGrudgeNum(1);
        
        //light ammo -1
        if(!decrAmmoNum(0)) {		//not enough ammo
        	atk = atk * 0.125F;	//reduce damage to 12.5%
        }

        //calc miss chance, if not miss, calc cri/multi hit
        float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.N.ShipLevel];
        missChance -= EffectEquip[ID.EF_MISS];		//equip miss reduce
        if(missChance > 0.35F) missChance = 0.35F;	//max miss chance
        
        if(this.rand.nextFloat() < missChance) {
        	atk = 0;	//still attack, but no damage
        	//spawn miss particle
    		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
        }
        else {
        	//roll cri -> roll double hit -> roll triple hit (triple hit more rare)
        	//calc critical
        	if(this.rand.nextFloat() < EffectEquip[ID.EF_CRI]) {
        		atk *= 1.5F;
        		//spawn critical particle
        		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, false), point);
        	}
        	else {
        		//calc double hit
            	if(this.rand.nextFloat() < EffectEquip[ID.EF_DHIT]) {
            		atk *= 2F;
            		//spawn double hit particle
            		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 12, false), point);
            	}
            	else {
            		//calc double hit
                	if(this.rand.nextFloat() < EffectEquip[ID.EF_THIT]) {
                		atk *= 3F;
                		//spawn triple hit particle
                		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 13, false), point);
                	}
            	}
        	}
        }
        
        //vs player = 25% dmg
  		if(target instanceof EntityPlayer) {
  			atk *= 0.25F;
  			
  			//check friendly fire
    		if(!ConfigHandler.friendlyFire) {
    			atk = 0F;
    		}
    		else if(atk > 59F) {
    			atk = 59F;	//same with TNT
    		}
  		}
  		
	    //�Natk��attacker�ǵ��ؼЪ�attackEntityFrom��k, �b�ؼ�class���p��ˮ`
	    //�åB�^�ǬO�_���\�ˮ`��ؼ�
	    boolean isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this).setProjectile(), atk);

	    //if attack success
	    if(isTargetHurt) {
	    	//calc kb effect
	        if(kbValue > 0) {
	            target.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
	            motionX *= 0.6D;
	            motionZ *= 0.6D;
	        }
	        
        	//display hit particle on target
	        TargetPoint point1 = new TargetPoint(this.dimension, target.posX, target.posY, target.posZ, 64D);
			CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(target, 9, false), point1);
        }

	    return isTargetHurt;
	}
  	
  	/**Type 91 Armor-Piercing Fist
  	 * �ݭn�i��4���q����(3���q�ǳ�), ��ؼгy����������4���ˮ`, �B�~�l�[8x8�d��1���ˮ`
  	 */
  	@Override
  	public boolean attackEntityWithHeavyAmmo(Entity target) {
  		//get attack value
		float atk1 = StateFinal[ID.ATK_H] * 4F;
		float atk2 = StateFinal[ID.ATK_H];
		
		//set knockback value (testing)
		float kbValue = 0.15F;
		
		boolean isTargetHurt = false;

		//�p��ؼжZ��
		float tarX = (float)target.posX;	//for miss chance calc
		float tarY = (float)target.posY;
		float tarZ = (float)target.posZ;
		float distX = tarX - (float)this.posX;
		float distY = tarY - (float)this.posY;
		float distZ = tarZ - (float)this.posZ;
        float distSqrt = MathHelper.sqrt_float(distX*distX + distY*distY + distZ*distZ);
        float dX = distX / distSqrt;
        float dY = distY / distSqrt;
        float dZ = distZ / distSqrt;
		
		//experience++
		addShipExp(16);
		
		//grudge--
		decrGrudgeNum(1);
		
		//heavy ammo -1
        if(!decrAmmoNum(1)) {	//not enough ammo
        	atk1 = atk1 * 0.125F;	//reduce damage to 12.5%
        	atk2 = atk2 * 0.125F;	//reduce damage to 12.5%
        }
	
		//play cannon fire sound at attacker
		int atkPhase = getStateEmotion(ID.S.Phase);
		
        switch(atkPhase) {
        case 0:
        case 2:
        	this.playSound(Reference.MOD_ID+":ship-ap_phase1", ConfigHandler.fireVolume, 1F);
        	break;
        case 1:
        	this.playSound(Reference.MOD_ID+":ship-ap_phase2", ConfigHandler.fireVolume, 1F);
        	break;
        case 3:
        	this.playSound(Reference.MOD_ID+":ship-ap_attack", ConfigHandler.fireVolume, 1F);
        	break;
    	default:
    		this.playSound(Reference.MOD_ID+":ship-ap_phase1", ConfigHandler.fireVolume, 1F);
    		break;
        }
        
        //play entity attack sound
        if(this.getRNG().nextInt(10) > 7) {
        	this.playSound(Reference.MOD_ID+":ship-hitsmall", ConfigHandler.shipVolume, 1F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        }
        
        //phase++
        TargetPoint point = new TargetPoint(this.dimension, this.posX, this.posY, this.posZ, 64D);
        atkPhase++;
      
        if(atkPhase > 3) {	//�����ǳƧ���, �p������ˮ`
        	//display hit particle on target
	        CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 21, posX, posY, posZ, target.posX, target.posY, target.posZ, true), point);
        	
        	//calc miss chance, miss: atk1 = 0, atk2 = 50%
            float missChance = 0.2F + 0.15F * (distSqrt / StateFinal[ID.HIT]) - 0.001F * StateMinor[ID.N.ShipLevel];
            missChance -= EffectEquip[ID.EF_MISS];	//equip miss reduce
            if(missChance > 0.35F) missChance = 0.35F;	//max miss chance = 30%
           
            if(this.rand.nextFloat() < missChance) {	//MISS
            	atk1 = 0F;
            	atk2 *= 0.5F;
            	//spawn miss particle
            	CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 10, false), point);
            }
            else if(this.rand.nextFloat() < EffectEquip[ID.EF_CRI]) {	//CRI
        		atk1 *= 1.5F;
        		atk2 *= 1.5F;
        		//spawn critical particle
        		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 11, false), point);
            }
            
            //vs player = 25% dmg
      		if(target instanceof EntityPlayer) {
      			atk1 *= 0.25F;
      			atk2 *= 0.25F;
      			
      			//check friendly fire
        		if(!ConfigHandler.friendlyFire) {
        			atk1 = 0F;
        			atk2 = 0F;
        		}
        		else if(atk2 > 40F) {
        			atk2 = 40F;		//TNT
        		}
      		}
      		
      		//�糧��y��atk1�ˮ`
      		isTargetHurt = target.attackEntityFrom(DamageSource.causeMobDamage(this), atk1);
      		
  			this.motionX = 0D;
  			this.motionY = 0D;
  			this.motionZ = 0D;
  			this.posX = tarX+dX*2F;
  			this.posY = tarY;
  			this.posZ = tarZ+dZ*2F;
  			this.setPosition(posX, posY, posZ);
      		
      		//��d��y��atk2�ˮ`
            EntityLivingBase hitEntity = null;
            AxisAlignedBB impactBox = this.boundingBox.expand(3.5D, 3.5D, 3.5D); 
            List hitList = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, impactBox);
            float atkTemp = atk2;
            
            //�j�Mlist, ��X�Ĥ@�ӥi�H�P�w���ؼ�, �Y�ǵ�onImpact
            if(hitList != null && !hitList.isEmpty()) {
                for(int i=0; i<hitList.size(); ++i) {
                	atkTemp = atk2;
                	hitEntity = (EntityLivingBase)hitList.get(i);
                	
                	//�ؼФ���O�ۤv or �D�H
                	if(hitEntity != this && hitEntity.canBeCollidedWith() && EntityHelper.checkNotSameEntityID(this, hitEntity)) {
                		//calc miss and cri
                		if(this.rand.nextFloat() < missChance) {	//MISS
                        	atkTemp *= 0.5F;
                        
                        }
                        else if(this.rand.nextFloat() < EffectEquip[ID.EF_CRI]) {	//CRI
                    		atkTemp *= 1.5F;
                        }
                		
                		//�Y������P�}��entity (ex: owner), �h�ˮ`�]��0 (���O�̵MĲ�o�����S��)
                		if(EntityHelper.checkSameOwner(this.getOwner(), hitEntity)) {
                			atkTemp = 0F;
                    	}
                		
                		//�Y�����쪱�a, �̤j�ˮ`�T�w��TNT�ˮ` (non-owner)
                    	if(hitEntity instanceof EntityPlayer) {
                    		atkTemp *= 0.25F;
                    		
                    		if(atkTemp > 59F) {
                    			atkTemp = 59F;	//same with TNT
                    		}
                    		
                    		//check friendly fire
                    		if(EntityHelper.checkOwnerIsPlayer(hitEntity) && !ConfigHandler.friendlyFire) {
                    			atkTemp = 0F;
                    		}
                    	}

                		//if attack success
                	    if(hitEntity.attackEntityFrom(DamageSource.causeMobDamage(this), atkTemp)) {
                	    	//calc kb effect
                	        if(kbValue > 0) {
                	        	hitEntity.addVelocity((double)(-MathHelper.sin(rotationYaw * (float)Math.PI / 180.0F) * kbValue), 
                	                   0.1D, (double)(MathHelper.cos(rotationYaw * (float)Math.PI / 180.0F) * kbValue));
                	            motionX *= 0.6D;
                	            motionZ *= 0.6D;
                	        }             	 
                	    }
                	}//end can be collided with
                }//end hit target list for loop
            }//end hit target list != null
        	
        	this.setStateEmotion(ID.S.Phase, 0, true);
        }
        else {
        	if(atkPhase == 2) {
        		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 23, this.posX, this.posY, this.posZ, 1D, 0D, 0D, true), point);
        	}
        	else {
        		CommonProxy.channelP.sendToAllAround(new S2CSpawnParticle(this, 22, this.posX, this.posY, this.posZ, 1D, 0D, 0D, true), point);
        	}
    		
        	this.setStateEmotion(ID.S.Phase, atkPhase, true);
        }
        
        return isTargetHurt;
	}

	@Override
	public int getKaitaiType() {
		return 1;
	}
	
	@Override
	public double getMountedYOffset() {
		if(this.isSitting()) {
			if(getStateEmotion(ID.S.State) > ID.State.NORMAL) {
				return (double)this.height * 0.4F;
			}
			else {
				if(getStateEmotion(ID.S.Emotion) == ID.Emotion.BORED) {
					return (double)this.height * -0.1F;
	  			}
	  			else {
	  				return (double)this.height * 0.3F;
	  			}
			}
  		}
  		else {
  			return (double)this.height * 0.8F;
  		}
	}


}

