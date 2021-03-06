// FCMOD

package net.minecraft.src;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FCEntityVillager extends EntityVillager
{
	public static int m_iNumProfessionTypes = 5;
	
	protected static final int m_iInLoveDataWatcherID = 22;	
	protected static final int m_iTradeLevelDataWatcherID = 23;
	
	// data watcher 24 used by EntityCreature parent to indicate possession
	
	protected static final int m_iTradeExperienceDataWatcherID = 25; 
	protected static final int m_iDirtyPeasantDataWatcherID = 26;
	
    protected int m_iAIFullTickCountdown;
    protected int m_iUpdateTradesCountdown;

    public FCEntityVillager( World world )
    {
        this( world, 0 );
    }
    
    public FCEntityVillager( World world, int iProfession )
    {
        super( world, iProfession );
        
        tasks.RemoveAllTasksOfClass( EntityAIAvoidEntity.class );
        
        tasks.addTask( 1, new EntityAIAvoidEntity( this, FCEntityZombie.class, 8.0F, 0.3F, 0.35F ) );
        tasks.addTask( 1, new EntityAIAvoidEntity( this, FCEntityWolf.class, 8.0F, 0.3F, 0.35F ) );
        
        tasks.RemoveAllTasksOfClass( EntityAIVillagerMate.class );
        
        tasks.addTask( 1, new FCEntityAIVillagerMate( this ) );
        tasks.addTask( 2, new EntityAITempt( this, 0.3F, Item.diamond.itemID, false ) );
        
        experienceValue = 50; // set experience when slain
        
        m_iUpdateTradesCountdown = 0;        
        m_iAIFullTickCountdown = 0;
    }
    
	@Override
    protected void updateAITick()
    {
		m_iAIFullTickCountdown--;
		
        if ( m_iAIFullTickCountdown <= 0 )
        {
        	m_iAIFullTickCountdown = 70 + rand.nextInt(50); // schedule the next village position update
            
            worldObj.villageCollectionObj.addVillagerPosition( MathHelper.floor_double( posX ), MathHelper.floor_double( posY ), MathHelper.floor_double( posZ ) );
            
            villageObj = worldObj.villageCollectionObj.findNearestVillage(
            	MathHelper.floor_double( posX ), MathHelper.floor_double( posY ), MathHelper.floor_double( posZ ), 32 );

            if ( villageObj == null )
            {
                detachHome();
            }
            else
            {
                ChunkCoordinates var1 = villageObj.getCenter();
                
                setHomeArea( var1.posX, var1.posY, var1.posZ, (int)( (float)villageObj.getVillageRadius() * 0.6F ) );
            }
        }

        if ( !isTrading() )
        {
        	if ( GetCurrentTradeLevel() == 0 )
        	{
        		// this indicates a newly created villager or an old one that was created before I revamped the trading system
        		
        		SetTradeLevel( 1 );
        		
        		buyingList = null;
        		m_iUpdateTradesCountdown = 0;
        		
        		CheckForNewTrades( 1 );
        	}
        	else if ( m_iUpdateTradesCountdown > 0 )
        	{
        		m_iUpdateTradesCountdown--;
	
	            if ( m_iUpdateTradesCountdown <= 0 )
	            {
                	// remove all trades which have exceeded their maximum uses
                	
                    Iterator tradeListIterator = this.buyingList.iterator();

                    while ( tradeListIterator.hasNext() )
                    {
                        MerchantRecipe tempRecipe = (MerchantRecipe)tradeListIterator.next();

                        if (tempRecipe.func_82784_g()) // check for toolUses >= this.maxTradeUses;
                        {
                        	tradeListIterator.remove();
                        }
                    }
                    
                    int iDesiredNumTrades = GetCurrentMaxNumTrades();
                    
                    if ( buyingList.size() < iDesiredNumTrades )
                    {
	                    CheckForNewTrades( iDesiredNumTrades - buyingList.size() );
	                    
	                    worldObj.setEntityState( this, (byte)14 ); // triggers "happy villager" particles
	
		                addPotionEffect( new PotionEffect( Potion.regeneration.id, 200, 0 ) );
                    }
	            }
        	}
        	else
        	{
        		// schedule periodic checks of the trade list so it'll never jam up
        		
        		m_iUpdateTradesCountdown = 600 + rand.nextInt( 600 ); // 30 seconds to a minute
        	}
        }
    }
	
	@Override
    public boolean interact( EntityPlayer player )
    {
    	if ( CustomInteract( player ) )
		{
    		return true;
		}
    	
    	if ( GetInLove() > 0 )
    	{
    		return EntityAgeableInteract( player );
    	}
    	
    	return super.interact( player );    		
    }

	@Override
    protected void entityInit()
    {
        super.entityInit();
                
        dataWatcher.addObject( m_iInLoveDataWatcherID, new Integer( 0 ) );
        dataWatcher.addObject( m_iTradeLevelDataWatcherID, new Integer( 0 ) );
        dataWatcher.addObject( m_iTradeExperienceDataWatcherID, new Integer( 0 ) );
        dataWatcher.addObject( m_iDirtyPeasantDataWatcherID, new Integer( 0 ) );
    }

	@Override
    public void writeEntityToNBT( NBTTagCompound tag )
    {
        super.writeEntityToNBT( tag );
        
    	tag.setInteger( "FCInLove", GetInLove() );
    	
    	tag.setInteger( "FCTradeLevel", GetCurrentTradeLevel() );
    	tag.setInteger( "FCTradeXP", GetCurrentTradeXP() );
    	
    	tag.setInteger( "FCDirty", GetDirtyPeasant() );
    }
    
	@Override
    public void readEntityFromNBT( NBTTagCompound tag )
    {
        super.readEntityFromNBT(tag);
        
	    if ( tag.hasKey( "FCInLove" ) )
	    {
	    	SetInLove( tag.getInteger( "FCInLove" ) );
	    }
	    
	    if ( tag.hasKey( "FCTradeLevel" ) )
	    {
	    	SetTradeLevel( tag.getInteger( "FCTradeLevel" ) );
	    }
	    
	    if ( tag.hasKey( "FCTradeXP" ) )
	    {
	    	SetTradeExperience( tag.getInteger( "FCTradeXP" ) );
	    }
	    
	    if ( tag.hasKey( "FCDirty" ) )
	    {
	    	SetDirtyPeasant( tag.getInteger( "FCDirty" ) );
	    }	    
	    
	    CheckForInvalidTrades();
    }
    
	@Override
    public void setRevengeTarget( EntityLiving attackingEntity )
    {
        entityLivingToAttack = attackingEntity;
        
        if ( attackingEntity != null )
        {
        	revengeTimer = 100;
        	
	        if ( villageObj != null )
	        {
	            villageObj.addOrRenewAgressor( attackingEntity );
	        }
	        
            if ( isEntityAlive() )
            {
                worldObj.setEntityState( this, (byte)13 );
            }
        }
        else
    	{
        	 revengeTimer = 0;
    	}
    }
	
	@Override
    public void useRecipe( MerchantRecipe recipe )
    {
        recipe.incrementToolUses();

        m_iUpdateTradesCountdown = 10;
        
        // special trade reactions
        
        if ( recipe.getItemToBuy().itemID == FCBetterThanWolves.fcCompanionCube.blockID )
        {
            worldObj.playSoundEffect( posX, posY, posZ, 
        		"mob.wolf.hurt", 5.0F, (rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F);            
        }
        else if ( recipe.getItemToBuy().itemID == FCBetterThanWolves.fcBlockLightningRod.blockID ) 
        {
            worldObj.playSoundEffect( posX, posY, posZ, 
        		"random.classic_hurt", 1F, 
        		getSoundPitch() * 2.0F);
        }
        else if ( recipe.getItemToBuy().itemID == FCBetterThanWolves.fcItemSoap.itemID )
        {
        	worldObj.playSoundEffect( posX, posY, posZ, "mob.slime.attack", 1.0F, ( rand.nextFloat() - rand.nextFloat()) * 0.2F + 1.0F );

        	SetDirtyPeasant( 0 );
        }
        else if ( recipe.getItemToSell().itemID == FCBetterThanWolves.fcAnvil.blockID )
        {
        	worldObj.playSoundEffect( posX, posY, posZ, "random.anvil_land", 0.3F, rand.nextFloat() * 0.1F + 0.9F );
        	worldObj.playSoundEffect( posX, posY, posZ, "ambient.cave.cave4", 0.5F, rand.nextFloat() * 0.05F + 0.5F );
        }

		if ( worldObj.getWorldInfo().getGameType() != EnumGameType.CREATIVE )
		{
	        if ( recipe.m_iTradeLevel < 0 ) // negative trade levels represent a level up trade
	        {
	        	int iVillagerTradeLevel = GetCurrentTradeLevel();
	        	
	        	if ( iVillagerTradeLevel < 5 && 
	        		GetCurrentTradeXP() == GetCurrentTradeMaxXP() && 
	        		GetCurrentTradeLevel() == -( recipe.m_iTradeLevel ) )
	        	{
	        		iVillagerTradeLevel++;
	        		
	        		SetTradeLevel( iVillagerTradeLevel );
	            	SetTradeExperience( 0 );
	            	
	            	if ( this.getProfession() == 2 && GetCurrentTradeLevel() >= 5 )
	            	{
	                    worldObj.playSoundAtEntity( this, "mob.enderdragon.growl", 1.0F, 0.5F );
	                    
	                    worldObj.playSoundAtEntity( this, "ambient.weather.thunder", 1.0F, rand.nextFloat() * 0.4F + 0.8F );
	                    
	            		worldObj.playSoundAtEntity( this, "random.levelup", 0.75F + ( rand.nextFloat() * 0.25F ), 0.5F );
	            	}
	            	else
	            	{
	            		worldObj.playSoundAtEntity( this, "random.levelup", 0.5F + ( rand.nextFloat() * 0.25F ), 1.5F );
	            	}
	        	}
	        }
	        else if ( recipe.m_iTradeLevel >= GetCurrentTradeLevel() )
	        {    		
	        	int iCurrentXP = GetCurrentTradeXP() + 1;
	        	int iMaxXP = GetCurrentTradeMaxXP();
	        	
	        	if ( iCurrentXP > iMaxXP )
	        	{
	        		iCurrentXP = iMaxXP;
	        	}
	        	
	        	SetTradeExperience( iCurrentXP );
	        }
		}
    }
    
	@Override
    public MerchantRecipeList getRecipes( EntityPlayer player )
    {
        if ( buyingList == null )
        {
            CheckForNewTrades( 1 );
        }

        return buyingList;
    }
    
	@Override
    public void initCreature()
    {
    	setProfession( worldObj.rand.nextInt( m_iNumProfessionTypes ) );
    }
    

	@Override
    public void onLivingUpdate()
    {
        super.onLivingUpdate();
        
        if ( !worldObj.isRemote )
        {
            if ( isEntityAlive() )
            {
    	        CheckForLooseMilk();
            }
        }
        else
        {
        	UpdateStatusParticles();
        }
    }

	@Override
	protected void dropFewItems( boolean bKilledByPlayer, int iLootingModifier )
    {
		if ( !HasHeadCrabbedSquid() )
        {
			int iDropItemID = FCBetterThanWolves.fcItemRawMysteryMeat.itemID;
			
			if ( isBurning() )
			{
				iDropItemID = FCBetterThanWolves.fcItemCookedMysteryMeat.itemID;
			}
	    	
	        int iNumDropped = rand.nextInt( 2 ) + 1 + rand.nextInt( 1 + iLootingModifier );
	
	        for ( int iTempCount = 0; iTempCount < iNumDropped; ++iTempCount )
	        {        	
	            dropItem( iDropItemID, 1 );
	        }
        }
    }
    
    @Override
    protected float getSoundPitch()
    {
    	float fPitch = super.getSoundPitch();

    	if ( IsPossessed() || ( getProfession() == 2 && GetCurrentTradeLevel() == 5 ) )
    	{
    		fPitch *= 0.60F;
    	}
    	
        return fPitch;
    }

    @Override
    protected boolean GetCanCreatureTypeBePossessed()
    {
    	return true;
    }
    
	@Override
    protected void OnFullPossession()
    {
        worldObj.playAuxSFX( FCBetterThanWolves.m_iPossessedVillagerTransformToWitchAuxFXID, 
    		MathHelper.floor_double( posX ), MathHelper.floor_double( posY ), MathHelper.floor_double( posZ ), 
    		0 );
        
        setDead();
        
        FCEntityWitch entityWitch = new FCEntityWitch( worldObj );
        
        entityWitch.setLocationAndAngles( posX, posY, posZ, rotationYaw, rotationPitch );
        entityWitch.renderYawOffset = renderYawOffset;
        
        entityWitch.SetPersistent( true );
        
        worldObj.spawnEntityInWorld( entityWitch );
    }
	
    @Override
    public boolean IsValidZombieSecondaryTarget( EntityZombie zombie )
    {
    	return true;
    }
    
    @Override
    public boolean IsSecondaryTargetForSquid()
    {
    	return true;
    }
    
    @Override
    public double getMountedYOffset()
    {
		return (double)height;
    }
    
    @Override
    public FCEntityVillager func_90012_b( EntityAgeable otherParent )
    {
    	// creates new villager when breeding
    	
        FCEntityVillager child = new FCEntityVillager( worldObj );
        
        child.initCreature();
        
        return child;
    }

    //------------- Class Specific Methods ------------//
    
    protected void CheckForNewTrades( int iMaxNumTradesToAdd )
    {
		if ( iMaxNumTradesToAdd <= 0 )
		{
			return;
		}
    	
    	if ( GetCurrentTradeMaxXP() == GetCurrentTradeXP() )
    	{
    		// we've maxed out the xp at this level, offer a level up trade
    		
    		if ( CheckForLevelUpTrade() )
    		{
    			iMaxNumTradesToAdd--;
    			
    			if ( iMaxNumTradesToAdd <= 0 )
    			{
    				return;
    			}
    		}
    	}
    	
    	iMaxNumTradesToAdd = CheckForMandatoryTrades( iMaxNumTradesToAdd );
    	
		if ( iMaxNumTradesToAdd <= 0 )
		{
			return;
		}
    	
        MerchantRecipeList newTradeList = new MerchantRecipeList();
        
        switch ( getProfession() )
        {
            case 0: // peasant
            	
            	CheckForPeasantTrades( newTradeList );
            	
            	break;
            	
            case 1: // librarian
            	
            	CheckForLibrarianTrades( newTradeList );
            	
                break;

            case 2: // priest
            	
            	CheckForPriestTrades( newTradeList );
            	
            	break;
            	
            case 3: // blacksmith
            	
            	CheckForBlacksmithTrades( newTradeList );          	
            	
                break;

            case 4: // butcher
            	
            	CheckForButcherTrades( newTradeList );            	
                
                break;
        }

        if ( newTradeList.isEmpty() )
        {
        	AddDefaultTradeToList( newTradeList );
        }
        else
        {
            Collections.shuffle( newTradeList );
        }

        if ( buyingList == null )
        {
            buyingList = new MerchantRecipeList();
        }

        for ( int iTempTradeCount = 0; iTempTradeCount < iMaxNumTradesToAdd && iTempTradeCount < newTradeList.size(); ++iTempTradeCount )
        {
            buyingList.addToListWithCheck( (MerchantRecipe)newTradeList.get( iTempTradeCount ) );
        }
    }
    
    private boolean CheckForLevelUpTrade()
    {
    	MerchantRecipe levelUpRecipe = null;
    	
        switch ( getProfession() )
        {
            case 0: // peasant
        
            	levelUpRecipe = GetPeasantLevelUpRecipe();
            	
            	break;
            	
            case 1: // librarian
            	
            	levelUpRecipe = GetLibrarianLevelUpRecipe();
            	
                break;

            case 2: // priest
            	
            	levelUpRecipe = GetPriestLevelUpRecipe();
            	
            	break;
            	
            case 3: // blacksmith
            	
            	levelUpRecipe = GetBlacksmithLevelUpRecipe();
            	
                break;

            case 4: // butcher
            	
            	levelUpRecipe = GetButcherLevelUpRecipe();
            	
                break;
        }
        
        if ( levelUpRecipe != null )
        {
        	if ( !DoesRecipeListAlreadyContainRecipe( levelUpRecipe ) )
    		{
                buyingList.add( levelUpRecipe );
                
                return true;
    		}        	
        }        
        
    	return false;
    }
   
    private int CheckForMandatoryTrades( int iMaxNumTradesToAdd )
    {
    	// some classes have a trade that is central to progression and should always be available once unlocked.
    	
    	MerchantRecipe mandatoryRecipe = null;
    	int iCurrentTradeLevel = GetCurrentTradeLevel();    	
    	
        switch ( getProfession() )
        {
            case 0: // peasant
        
            	iMaxNumTradesToAdd = CheckForPeasantMandatoryTrades( iMaxNumTradesToAdd );
            	
            	break;
            	
            case 1: // librarian
            	
            	iMaxNumTradesToAdd = CheckForLibrarianMandatoryTrades( iMaxNumTradesToAdd );
            	
                break;

            case 2: // priest
            	
            	iMaxNumTradesToAdd = CheckForPriestMandatoryTrades( iMaxNumTradesToAdd );
            	
            	break;
            	
            case 3: // blacksmith
            	
            	iMaxNumTradesToAdd = CheckForBlacksmithMandatoryTrades( iMaxNumTradesToAdd );
            	
                break;

            case 4: // butcher
            	
            	iMaxNumTradesToAdd = CheckForButcherMandatoryTrades( iMaxNumTradesToAdd );
            	
                break;
        }

        return iMaxNumTradesToAdd;
    }
    
    private boolean AttemptToAddTradeToBuyingList( MerchantRecipe recipe )
    {
        if ( recipe != null )
        {
        	if ( !DoesRecipeListAlreadyContainRecipe( recipe ) )
    		{
                buyingList.add( recipe );
                
                return true;
    		}        	
        }
        
        return false;
    }
    
    private void CheckForPeasantTrades( MerchantRecipeList tradeList )
    {
    	// Level 1
    	
    	CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcBlockDirtLoose.blockID, 0.2F, 48, 64, 1 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Block.wood.blockID, 0, 0.15F, 32, 48, 1 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Block.wood.blockID, 1, 0.15F, 32, 48, 1 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Block.wood.blockID, 2, 0.15F, 32, 48, 1 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Block.wood.blockID, 3, 0.15F, 32, 48, 1 );    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemWool.itemID, 3, 0.2F, 16, 24, 1 ); // 3 = brown wool
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Item.dyePowder.itemID, 15, 0.2F, 32, 48, 1 ); // 15 = bone meal    	
    	
        // Level 2
        
    	CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemFlour.itemID, 0.2F, 24, 32, 2 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Item.sugar.itemID, 0.2F, 10, 20, 2 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemCocoaBeans.itemID, 0.2F, 10, 16, 2 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemMushroomBrown.itemID, 0.2F, 10, 16, 2 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemHempSeeds.itemID, 0.2F, 24, 32, 2 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Item.egg.itemID, 0.2F, 12, 12, 2 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Block.thinGlass.blockID, 0.2F, 16, 32, 2 );
    	
    	CheckForWishToBuySingleItemTrade( tradeList, Item.bucketMilk.itemID, 0.05F, 1, 2, 2 );
    	
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemWheat.itemID, 0.2F, 8, 16, 2 );
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.appleRed.itemID, 0.1F, 2, 4, 2 );
        
        // Level 3
        
        if ( GetDirtyPeasant() > 0 )
        {
        	CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemSoap.itemID, 0.2F, 1, 2, 3 );
        }
        
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Block.melon.blockID, 0.2F, 8, 10, 3 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcBlockPumpkinFresh.blockID, 0.2F, 10, 16, 3 );    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemStumpRemover.itemID, 0.2F, 8, 12, 3 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemChocolate.itemID, 0.2F, 1, 2, 3 );
        
        CheckForWishToBuySingleItemTrade( tradeList, Item.shears.itemID, 0.1F, 1, 1, 3 );
        CheckForWishToBuySingleItemTrade( tradeList, Item.flintAndSteel.itemID, 0.1F, 1, 1, 3 );
        
        CheckForComplexTrade( tradeList, new ItemStack( FCBetterThanWolves.fcItemStake, 2 ),
        	new ItemStack( Item.silk, rand.nextInt( 17 ) + 16 ),
        	new ItemStack( Item.emerald, 1 ), 0.1F, 3 );
        
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.bread.itemID, 0.2F, 4, 6, 3 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemCookedMushroomOmelet.itemID, 0.1F, 8, 12, 3 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemCookedScrambledEggs.itemID, 0.1F, 8, 12, 3 );
        
        // Level 4
        
    	CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemBucketCement.itemID, 0.2F, 2, 4, 4 );
    	
    	CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcLightBulbOff.blockID, 0.2F, 2, 4, 4 );
    	
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.cookie.itemID, 0.2F, 8, 16, 4 );
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.pumpkinPie.itemID, 0.2F, 1, 2, 4 );        
        CheckForWishToSellSingleItemTrade( tradeList, Item.cake.itemID, 0.2F, 2, 4, 4 );
        
        // Level 5        

        CheckForWishToSellSingleItemTrade( tradeList, Block.mycelium.blockID, 0.2F, 10, 20, 5 );
        
        CheckForArcaneScrollTrade( tradeList, Enchantment.looting.effectId, 0.2F, 16, 32, 5 );               
    }
    
    private MerchantRecipe GetPeasantLevelUpRecipe()
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
    	switch ( iCurrentTradeLevel )
    	{
			case 1:
			
		        return new MerchantRecipe( new ItemStack( Item.hoeIron, 1 ), 
		        	new ItemStack( Item.emerald.itemID, 1, 0 ), 
		        	-iCurrentTradeLevel );
	        
			case 2:
			
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcMillStone, 1 ), 
					new ItemStack( Item.emerald.itemID, 2, 0 ), 
					-iCurrentTradeLevel );
				
			case 3:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcItemWaterWheel, 1 ), 
					new ItemStack( Item.emerald.itemID, 3, 0 ), 
					-iCurrentTradeLevel );
				
			case 4:
				
				return new MerchantRecipe( 
					new ItemStack( FCBetterThanWolves.fcBlockPlanterSoil, rand.nextInt( 5 ) + 8 ), 
					new ItemStack( Item.emerald.itemID, 4, 0 ), 
					-iCurrentTradeLevel );	        
    	}
    	
    	return null;
    }
    
    private int CheckForPeasantMandatoryTrades( int iMaxNumTradesToAdd )
    {
    	return iMaxNumTradesToAdd;
    }
    
    private void CheckForLibrarianTrades( MerchantRecipeList tradeList )
    {
    	// Level 1
    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.paper.itemID, 0.2F, 27, 38, 1 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.dyePowder.itemID, 0, 0.2F, 27, 38, 1 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.feather.itemID, 0.2F, 27, 38, 1 );
        
        // Level 2        
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.book.itemID, 0.2F, 1, 3, 2 );        
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.writableBook.itemID, 0.2F, 1, 1, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Block.bookShelf.blockID, 0.2F, 1, 1, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.netherStalkSeeds.itemID, 0.2F, 16, 24, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.lightStoneDust.itemID, 0.2F, 24, 32, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemNitre.itemID, 0.2F, 32, 48, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemBatWing.itemID, 0.2F, 14, 16, 2 );        
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.spiderEye.itemID, 0.2F, 4, 8, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.redstone.itemID, 0.2F, 32, 48, 2 );        
        
        // Level 3
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemWitchWart.itemID, 0.2F, 6, 10, 3 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemMysteriousGland.itemID, 0.2F, 14, 16, 3 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.fermentedSpiderEye.itemID, 0.2F, 4, 8, 3 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.ghastTear.itemID, 0.2F, 4, 6, 3 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.magmaCream.itemID, 0.2F, 8, 12, 3 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.blazePowder.itemID, 0.2F, 4, 6, 3 );
        
        // Level 4
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcBlockDetector.blockID, 0.2F, 2, 3, 4 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcBuddyBlock.blockID, 0.2F, 2, 3, 4 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcBlockDispenser.blockID, 0.2F, 2, 3, 4 );
        
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcLens.blockID, 0.2F, 2, 3, 4 );
        
        // Level 5
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemBrimstone.itemID, 0.2F, 16, 32, 5 );        
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcAestheticVegetation.blockID, FCBlockAestheticVegetation.m_iSubtypeBloodWoodSapling, 0.2F, 8, 16, 5 );        
        
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemBloodMossSpores.itemID, 0.2F, 2, 3, 5 );
    	
        CheckForArcaneScrollTrade( tradeList, Enchantment.power.effectId, 0.2F, 32, 48, 5 );
        
        // FCTODO: Code temporarily disabled for release
        /*
        CheckForComplexTrade( tradeList, new ItemStack( Item.enchantedBook, 1 ),        	
        	new ItemStack( Item.emerald, rand.nextInt( 3 ) + 3 ), 
        	new ItemStack( FCBetterThanWolves.fcItemAncientProphecy, 1 ), 0.2F, 5 );
    	*/
        // END FCTODO
    }
    
    private MerchantRecipe GetLibrarianLevelUpRecipe()
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
    	switch ( iCurrentTradeLevel )
    	{
			case 1:
			
				return new MerchantRecipe( new ItemStack( Item.enchantedBook, 1 ), 
					new ItemStack( Item.emerald.itemID, 2, 0 ), 
					-iCurrentTradeLevel );
				
			case 2:
				
				return new MerchantRecipe( new ItemStack( Item.brewingStand, 1 ), 
					new ItemStack( Item.emerald.itemID, 2, 0 ), 
					-iCurrentTradeLevel );
				
			case 3:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcBlockDispenser, 1 ), 
					new ItemStack( Item.emerald.itemID, 4, 0 ), 
					-iCurrentTradeLevel );
				
			case 4:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcItemEnderSpectacles, 1 ), 
					new ItemStack( Item.emerald.itemID, 3, 0 ), 
					-iCurrentTradeLevel );				
    	}
    	
    	return null;
    }
    
    private int CheckForLibrarianMandatoryTrades( int iMaxNumTradesToAdd )
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
        if ( iCurrentTradeLevel >= 5 )
        {
        	if ( AttemptToAddTradeToBuyingList( new MerchantRecipe( 
            	new ItemStack( Item.enderPearl, 1 ),
            	new ItemStack( Item.emerald, rand.nextInt( 3 ) + 6 ), 
            	new ItemStack( Item.eyeOfEnder, 1 ),              	
				4 ) // one level lower than available so doesn't provide xp
        		)
    		)
    		{
        		iMaxNumTradesToAdd--;
    		}
        		
        }
    
    	return iMaxNumTradesToAdd;
    }
    
    private void CheckForPriestTrades( MerchantRecipeList tradeList )
    {
    	// Level 1
    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemHemp.itemID, 0.2F, 18, 22, 1 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemMushroomRed.itemID, 0.2F, 10, 16, 1 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Block.cactus.blockID, 0.2F, 32, 64, 1 );
        CheckForWishToBuySingleItemTrade( tradeList, Item.painting.itemID, 0.1F, 2, 3, 1 );
        CheckForWishToBuySingleItemTrade( tradeList, Item.flintAndSteel.itemID, 0.1F, 1, 1, 1 );
        
    	// Level 2
        
        // FCTODO: Buy lapis, diamonds, and gold    	
        
        CheckForItemEnchantmentForCostTrade( tradeList, Item.swordIron, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.axeIron, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.pickaxeIron, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.helmetIron, 0.05F, 2, 4, 2 );        
        CheckForItemEnchantmentForCostTrade( tradeList, Item.plateIron, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.legsIron, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.bootsIron, 0.05F, 2, 4, 2 );
        
        CheckForItemEnchantmentForCostTrade( tradeList, Item.swordDiamond, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.axeDiamond, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.pickaxeDiamond, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.helmetDiamond, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.plateDiamond, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.legsDiamond, 0.05F, 2, 4, 2 );
        CheckForItemEnchantmentForCostTrade( tradeList, Item.bootsDiamond, 0.05F, 2, 4, 2 );
        
    	// Level 3
    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemCandle.itemID, rand.nextInt( 16 ), 0.2F, 4, 8, 3 ); // random colored candle
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.skull.itemID, 0, 0.2F, 3, 5, 3 ); // skeleton
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.skull.itemID, 2, 0.2F, 2, 4, 3 ); // zombie
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.skull.itemID, 4, 0.2F, 2, 4, 3 ); // creeper
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcAestheticOpaque.blockID, 
        	FCBlockAestheticOpaque.m_iSubtypeBone, 0.2F, 4, 6, 3 );
        
    	// Level 4    	

        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemSoulUrn.itemID, 0.2F, 2, 3, 4 );
        
    	// Level 5    	
        
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemCanvas.itemID, 0.2F, 2, 3, 5 );
        
        CheckForArcaneScrollTrade( tradeList, Enchantment.fortune.effectId, 0.2F, 48, 64, 5 );        
    }
    
    private MerchantRecipe GetPriestLevelUpRecipe()
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
    	switch ( iCurrentTradeLevel )
    	{
			case 1:
			
				return new MerchantRecipe( new ItemStack( Block.enchantmentTable, 1 ), 
					new ItemStack( Item.emerald.itemID, 2, 0 ), 
					-iCurrentTradeLevel );
	        
			case 2:
			
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcBlockArcaneVessel, 1 ), 
					new ItemStack( Item.emerald.itemID, 2, 0 ), 
					-iCurrentTradeLevel );
				
			case 3:
				
				return new MerchantRecipe( new ItemStack( Item.skull.itemID, 1, 1 ), 
					new ItemStack( Item.emerald.itemID, 3, 0 ), 
					-iCurrentTradeLevel );
				
			case 4:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcInfernalEnchanter, 1 ), 
					new ItemStack( Item.emerald.itemID, 4, 0 ), 
					-iCurrentTradeLevel );	        
    	}
    	
    	return null;
    }
    
    private int CheckForPriestMandatoryTrades( int iMaxNumTradesToAdd )
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
        if ( iCurrentTradeLevel >= 4 )
        {
            // runed skull (1) to infused skull (5) trade
        	
        	if ( AttemptToAddTradeToBuyingList( new MerchantRecipe( 
            	new ItemStack( Item.skull, 1, 1 ),
            	new ItemStack( Item.emerald, rand.nextInt( 3 ) + 6 ), 
            	new ItemStack( Item.skull, 1, 5 ),	                	
				3 ) // one level lower than available so doesn't provide xp
        		)
    		)
        	{
        		iMaxNumTradesToAdd--;
        	}
        	
        	if ( iMaxNumTradesToAdd >= 0 )
        	{
            	if ( AttemptToAddTradeToBuyingList( new MerchantRecipe( 
                	new ItemStack( Item.netherStar ),
                	new ItemStack( FCBetterThanWolves.fcBlockSoulforgeDormant ), 
                	new ItemStack( FCBetterThanWolves.fcAnvil ),	                	
    				3 ) // one level lower than available so doesn't provide xp
            		)
        		)
            	{
            		iMaxNumTradesToAdd--;
            	}
        	}
        }
    
    	return iMaxNumTradesToAdd;
    }
    
    private void CheckForBlacksmithTrades( MerchantRecipeList tradeList )
    {
    	// Level 1
    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.coal.itemID, 0.2F, 16, 24, 1 );
    	CheckForWishToBuyMultipleItemsTrade( tradeList, Block.wood.blockID, 2, 0.2F, 32, 48, 1 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemNuggetIron.itemID, 0.2F, 18, 27, 1 );
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcBlockFurnaceBrickIdle.blockID, 0.2F, 1, 1, 1 );
        
        CheckForWishToSellSingleItemTrade( tradeList, Item.swordIron.itemID, 0.2F, 4, 6, 1 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.axeIron.itemID, 0.2F, 4, 6, 1 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.pickaxeIron.itemID, 0.2F, 6, 9, 1 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.shovelIron.itemID, 0.2F, 2, 3, 1 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.hoeIron.itemID, 0.2F, 2, 3, 1 );
        
    	// Level 2
    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemNethercoal.itemID, 0.2F, 12, 20, 2 );
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcBBQ.blockID, 0.2F, 2, 3, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemCreeperOysters.itemID, 0.2F, 14, 16, 2 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.goldNugget.itemID, 0.2F, 18, 27, 2 );
       
        CheckForWishToBuySingleItemTrade( tradeList, Item.diamond.itemID, 0.2F, 2, 3, 2 );
        
        CheckForWishToSellSingleItemTrade( tradeList, Item.bootsIron.itemID, 0.2F, 4, 6, 2 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.helmetIron.itemID, 0.2F, 10, 15, 2 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.plateIron.itemID, 0.2F, 16, 24, 2 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.legsIron.itemID, 0.2F, 14, 21, 2 );        
        
        // Level 3
        
        CheckForWishToSellSingleItemTrade( tradeList, Item.swordDiamond.itemID, 0.2F, 8, 12, 3 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.axeDiamond.itemID, 0.2F, 8, 12, 3 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.pickaxeDiamond.itemID, 0.2F, 12, 18, 3 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.shovelDiamond.itemID, 0.2F, 4, 6, 3 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.hoeDiamond.itemID, 0.2F, 4, 6, 3 );
        
        // Level 4
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemSoulUrn.itemID, 0, 0.2F, 2, 3, 4 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemHaft.itemID, 0, 0.2F, 6, 8, 4 );
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcBlockMiningCharge.blockID, 0, 0.2F, 4, 6, 4 );
        
        CheckForWishToSellSingleItemTrade( tradeList, Item.bootsDiamond.itemID, 0.2F, 8, 12, 4 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.helmetDiamond.itemID, 0.2F, 20, 30, 4 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.plateDiamond.itemID, 0.2F, 32, 48, 4 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.legsDiamond.itemID, 0.2F, 28, 42, 4 );
        
        // Level 5
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemSoulFlux.itemID, 0, 0.2F, 16, 24, 5 );
        
        CheckForWishToSellSingleItemTrade( tradeList, Item.bootsChain.itemID, 0.2F, 4, 6, 5 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.helmetChain.itemID, 0.2F, 10, 15, 5 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.plateChain.itemID, 0.2F, 16, 24, 5 );
        CheckForWishToSellSingleItemTrade( tradeList, Item.legsChain.itemID, 0.2F, 14, 21, 5 );
    
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemRefinedAxe.itemID, 0.2F, 16, 24, 5 );
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemRefinedHoe.itemID, 0.2F, 16, 24, 5 );
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemRefinedPickAxe.itemID, 0.2F, 24, 36, 5 );
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemRefinedShovel.itemID, 0.2F, 8, 12, 5 );
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemRefinedSword.itemID, 0.2F, 16, 24, 5 );
        
        CheckForArcaneScrollTrade( tradeList, Enchantment.unbreaking.effectId, 0.2F, 32, 48, 5 );        
    }
    
    private MerchantRecipe GetBlacksmithLevelUpRecipe()
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
    	switch ( iCurrentTradeLevel )
    	{
			case 1:
			
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcBBQ, 1 ), 
					new ItemStack( Item.emerald.itemID, 1, 0 ), 
					-iCurrentTradeLevel );
	        
			case 2:
			
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcBellows, 1 ), 
					new ItemStack( Item.emerald.itemID, 2, 0 ), 
					-iCurrentTradeLevel );
				
			case 3:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcCrucible, 1 ), 
					new ItemStack( Item.emerald.itemID, 3, 0 ), 
					-iCurrentTradeLevel );
				
			case 4:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcItemSteel, 8 ), 
					new ItemStack( Item.emerald.itemID, 4, 0 ), 
					-iCurrentTradeLevel );	        
    	}
    	
    	return null;
    }
    
    private int CheckForBlacksmithMandatoryTrades( int iMaxNumTradesToAdd )
    {
    	return iMaxNumTradesToAdd;
    }
    
    private void CheckForButcherTrades( MerchantRecipeList tradeList )
    {
    	// level 1
    	
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.arrow.itemID, 0.2F, 24, 32, 1 );
        
        CheckForWishToBuySingleItemTrade( tradeList, Item.shears.itemID, 0.1F, 1, 1, 1 );
        CheckForWishToBuySingleItemTrade( tradeList, Item.fishingRod.itemID, 0.1F, 1, 1, 1 );
        
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.beefRaw.itemID, 0.2F, 7, 9, 1 );
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.porkRaw.itemID, 0.2F, 8, 11, 1 );
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.chickenRaw.itemID, 0.2F, 9, 12, 1 );
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.fishRaw.itemID, 0.2F, 10, 13, 1 );
        CheckForWishToSellMultipleItemsTrade( tradeList, Item.leather.itemID, 0.2F, 4, 6, 1 );        
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemMuttonRaw.itemID, 0.2F, 10, 13, 1 );
        
        // level 2
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemDung.itemID, 0.2F, 10, 16, 2 );        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemWolfRaw.itemID, 0.2F, 6, 8, 2 );        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemBark.itemID, 1, 0.2F, 48, 64, 2 );        
        
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemSteakAndPotatoes.itemID, 0.2F, 4, 8, 2 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemHamAndEggs.itemID, 0.2F, 4, 8, 2 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemTastySandwich.itemID, 0.2F, 4, 8, 2 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemFishSoup.itemID, 0.2F, 10, 12, 2 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemCookedKebab.itemID, 0.2F, 4, 6, 2 );
        
        // level 3
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.carrot.itemID, 0.2F, 10, 16, 3 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, Item.potato.itemID, 0.2F, 10, 16, 3 );
        
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemBeastLiverRaw.itemID, 0.2F, 1, 2, 3 );
        
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemTannedLeatherCut.itemID, 0.2F, 4, 8, 3 );
        
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemSteakDinner.itemID, 0.2F, 4, 6, 3 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemPorkDinner.itemID, 0.2F, 4, 6, 3 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemChickenSoup.itemID, 0.2F, 4, 6, 3 );
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemWolfDinner.itemID, 0.2F, 4, 6, 3 );
        
        CheckForWishToSellSingleItemTrade( tradeList, Item.saddle.itemID, 0.2F, 6, 8, 3 );
        
        // level 4
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemRawMysteryMeat.itemID, 0.2F, 2, 4, 4 );
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemDynamite.itemID, 0.2F, 4, 6, 5 );
        
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemScrew.itemID, 0.2F, 2, 3, 4 );        
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemCompositeBow.itemID, 0.2F, 2, 3, 4 );
        
        CheckForWishToSellMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemHeartyStew.itemID, 0.2F, 3, 4, 4 );
        
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemArmorTannedBoots.itemID, 0.1F, 2, 3, 4 );
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemArmorTannedChest.itemID, 0.1F, 6, 8, 4 );
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemArmorTannedHelm.itemID, 0.1F, 3, 4, 4 );
        CheckForWishToSellSingleItemTrade( tradeList, FCBetterThanWolves.fcItemArmorTannedLeggings.itemID, 0.1F, 4, 6, 4 );
        
        // level 5
                
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcItemBattleAxe.itemID, 0.2F, 4, 5, 5 );
        
        CheckForWishToBuySingleItemTrade( tradeList, FCBetterThanWolves.fcCompanionCube.blockID, 0.2F, 1, 2, 5 );
        
        CheckForWishToBuyMultipleItemsTrade( tradeList, FCBetterThanWolves.fcItemBroadheadArrow.itemID, 0.2F, 6, 12, 5 );
        
        CheckForComplexTrade( tradeList, 
        	new ItemStack( FCBetterThanWolves.fcBlockLightningRod, 1 ),
        	new ItemStack( FCBetterThanWolves.fcItemSoap, 1 ),
        	new ItemStack( Item.emerald, rand.nextInt( 3 ) + 3 ), 0.1F, 5 );
        
        CheckForArcaneScrollTrade( tradeList, Enchantment.sharpness.effectId, 0.2F, 32, 48, 5 );
    }
    
    private MerchantRecipe GetButcherLevelUpRecipe()
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
    	switch ( iCurrentTradeLevel )
    	{
			case 1:
			
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcCauldron, 1 ), 
					new ItemStack( Item.emerald.itemID, 1, 0 ), 
					-iCurrentTradeLevel );
	        
			case 2:
			
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcSaw, 1 ), 
					new ItemStack( Item.emerald.itemID, 2, 0 ), 
					-iCurrentTradeLevel );
				
			case 3:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcItemBreedingHarness, 1 ), 
					new ItemStack( Item.emerald.itemID, 3, 0 ), 
					-iCurrentTradeLevel );
				
			case 4:
				
				return new MerchantRecipe( new ItemStack( FCBetterThanWolves.fcAestheticOpaque, 1, FCBlockAestheticOpaque.m_iSubtypeChoppingBlockDirty ), 
					new ItemStack( Item.emerald.itemID, 4, 0 ), 
					-iCurrentTradeLevel );	        
    	}
    	
    	return null;
    }
    
    private int CheckForButcherMandatoryTrades( int iMaxNumTradesToAdd )
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	
        if ( iCurrentTradeLevel >= 4 )
        {
            // skeleton (0) to runed skull (1) trade
        	
        	if ( AttemptToAddTradeToBuyingList( new MerchantRecipe( 
            	new ItemStack( Item.skull, 1, 0 ),
            	new ItemStack( Item.emerald, rand.nextInt( 3 ) + 6 ), 
            	new ItemStack( Item.skull, 1, 1 ),	                	
				3 ) // one level lower than available so doesn't provide xp
        		)
    		)
        	{
        		iMaxNumTradesToAdd--;
        	}
        }
    
    	return iMaxNumTradesToAdd;
    }
    
	private void AddDefaultTradeToList( MerchantRecipeList newTradeList )
	{		
		// default recipe add if no others found
		
        switch ( getProfession() )
        {
            case 0: // peasant
        
        	    newTradeList.add( new MerchantRecipe( 
        	    	new ItemStack( FCBetterThanWolves.fcBlockDirtLoose.blockID, rand.nextInt( 17 ) + 48, 0 ), 
        	    	new ItemStack( Item.emerald.itemID, 1, 0 ), 
        	    	1 ) );        	    
            	
            	break;
            	
            case 1: // librarian
            	
        	    newTradeList.add( new MerchantRecipe( 
        	    	new ItemStack( Item.paper.itemID, rand.nextInt( 12 ) + 27, 0 ), 
        	    	new ItemStack( Item.emerald.itemID, 1, 0 ), 
        	    	1 ) );
            	
                break;

            case 2: // priest
            	
        	    newTradeList.add( new MerchantRecipe( 
        	    	new ItemStack( FCBetterThanWolves.fcItemHemp.itemID, rand.nextInt( 5 ) + 18, 0 ), 
        	    	new ItemStack( Item.emerald.itemID, 1, 0 ), 
        	    	1 ) );
            	
            	break;
            	
            case 3: // blacksmith
            	
        	    newTradeList.add( new MerchantRecipe( 
        	    	new ItemStack( Item.coal.itemID, rand.nextInt( 9 ) + 16, 0 ), 
        	    	new ItemStack( Item.emerald.itemID, 1, 0 ), 
        	    	1 ) );        	    
            	
                break;

            case 4: // butcher
            	
        	    newTradeList.add( new MerchantRecipe( 
        	    	new ItemStack( Item.emerald.itemID, 1, 0 ), 
        	    	new ItemStack( Item.beefRaw.itemID, rand.nextInt( 3 ) + 7, 0 ), 
        	    	1 ) );        	    
            	
                break;
        }        
	}
    
    private float ComputeAdjustedChanceOfTrade( float fBaseFrequency, int iTradeLevel )
    {
    	float fLevelModifier = 1F;
    	int iVillagerTradeLevel = GetCurrentTradeLevel();
    	
    	if ( iTradeLevel > 0 && iVillagerTradeLevel > 0 )
    	{
    		fLevelModifier = (float)iTradeLevel / (float)iVillagerTradeLevel;
    	}
    	
    	return fBaseFrequency * fLevelModifier;
    }
    
    private void CheckForWishToBuyMultipleItemsTrade( MerchantRecipeList tradeList, int iItemID, float fFrequency, int iMinItemCount, int iMaxItemCount, int iTradeLevel )
    {
    	CheckForWishToBuyMultipleItemsTrade( tradeList, iItemID, 0, fFrequency, iMinItemCount, iMaxItemCount, iTradeLevel );
    }
    
    private void CheckForWishToBuyMultipleItemsTrade( MerchantRecipeList tradeList, int iItemID, int iItemMetadata, float fFrequency, int iMinItemCount, int iMaxItemCount, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
        	int iItemCount = MathHelper.getRandomIntegerInRange( rand, iMinItemCount, iMaxItemCount );
        	
        	AddWishToBuyTradeToList( tradeList, iItemID, iItemCount, iItemMetadata, 1, iTradeLevel );
        }
    }
    
    private void CheckForWishToBuySingleItemTrade( MerchantRecipeList tradeList, int iItemID, float fFrequency, int iMinEmeraldCount, int iMaxEmeraldCount, int iTradeLevel )
    {
    	CheckForWishToBuySingleItemTrade( tradeList, iItemID, 0, fFrequency, iMinEmeraldCount, iMaxEmeraldCount, iTradeLevel );
    }
    
    private void CheckForWishToBuySingleItemTrade( MerchantRecipeList tradeList, int iItemID, int iItemMetadata, float fFrequency, int iMinEmeraldCount, int iMaxEmeraldCount, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
        	int iEmeraldCount = MathHelper.getRandomIntegerInRange( rand, iMinEmeraldCount, iMaxEmeraldCount );        	
        	
        	AddWishToBuyTradeToList( tradeList, iItemID, 1, iItemMetadata, iEmeraldCount, iTradeLevel );        	
        }
    }
    
    private void AddWishToBuyTradeToList( MerchantRecipeList tradeList, int iItemID, int iItemCount, int iItemMetadata, int iEmeraldCount, int iTradeLevel )
    {
        ItemStack emeraldStack = new ItemStack( Item.emerald.itemID, iEmeraldCount, 0 );
        ItemStack itemToSellStack = new ItemStack( iItemID, iItemCount, iItemMetadata );
        
        tradeList.add( new MerchantRecipe( itemToSellStack, emeraldStack, iTradeLevel ) );
    }
    
    private void CheckForWishToSellSingleItemTrade( MerchantRecipeList tradeList, int iItemID, float fFrequency, int iMinEmeraldCount, int iMaxEmeraldCount, int iTradeLevel )
    {
    	CheckForWishToSellSingleItemTrade( tradeList, iItemID, 0, fFrequency, iMinEmeraldCount, iMaxEmeraldCount, iTradeLevel );
    }
    
    private void CheckForWishToSellSingleItemTrade( MerchantRecipeList tradeList, int iItemID, int iItemMetadata, float fFrequency, int iMinEmeraldCount, int iMaxEmeraldCount, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
        	int iEmeraldCount = MathHelper.getRandomIntegerInRange( rand, iMinEmeraldCount, iMaxEmeraldCount );        	
            
        	AddWishToSellTradeToList( tradeList, iItemID, 1, iItemMetadata, iEmeraldCount, iTradeLevel );
        }
    }
    
    private void CheckForWishToSellMultipleItemsTrade( MerchantRecipeList tradeList, int iItemID, float fFrequency, int iMinItemCount, int iMaxItemCount, int iTradeLevel )
    {
        CheckForWishToSellMultipleItemsTrade( tradeList, iItemID, 0, fFrequency, iMinItemCount, iMaxItemCount, iTradeLevel );
    }
    
    private void CheckForWishToSellMultipleItemsTrade( MerchantRecipeList tradeList, int iItemID, int iItemMetadata, float fFrequency, int iMinItemCount, int iMaxItemCount, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
        	int iItemCount = MathHelper.getRandomIntegerInRange( rand, iMinItemCount, iMaxItemCount );
        	
        	AddWishToSellTradeToList( tradeList, iItemID, iItemCount, iItemMetadata, 1, iTradeLevel );
        }
    }
    
    private void AddWishToSellTradeToList( MerchantRecipeList tradeList, int iItemID, int iItemCount, int iItemMetadata, int iEmeraldCount, int iTradeLevel )
    {
        ItemStack emeraldStack = new ItemStack( Item.emerald.itemID, iEmeraldCount, 0 );
        ItemStack itemToSellStack = new ItemStack( iItemID, iItemCount, iItemMetadata );
        
        tradeList.add( new MerchantRecipe( emeraldStack, itemToSellStack, iTradeLevel ) );
    }
    
    private void CheckForArcaneScrollTrade( MerchantRecipeList tradeList, int iEnchantmentID, float fFrequency, int iMinPrice, int iMaxPrice, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
            int iEmeraldQuantity = MathHelper.getRandomIntegerInRange( rand, iMinPrice, iMaxPrice );
            
            ItemStack outputStack = new ItemStack( FCBetterThanWolves.fcItemArcaneScroll, 1, iEnchantmentID );
            
            tradeList.add( new MerchantRecipe( new ItemStack( Item.paper ), new ItemStack( Item.emerald, iEmeraldQuantity ), outputStack, iTradeLevel ) );
        }
    }
    
    private void CheckForItemEnchantmentForCostTrade( MerchantRecipeList tradeList, Item inputItem, float fFrequency, int iMinPrice, int iMaxPrice, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
            int iEmeraldQuantity = MathHelper.getRandomIntegerInRange( rand, iMinPrice, iMaxPrice );
            
        	tradeList.add( new MerchantRecipe( 
        		new ItemStack( inputItem, 1, 0 ), 
        		new ItemStack( Item.emerald, iEmeraldQuantity, 0 ), 
        		EnchantmentHelper.addRandomEnchantment( rand, new ItemStack( inputItem, 1, 0 ), 5 + rand.nextInt( 15 ) ), 
        		iTradeLevel ) );
        }
    }
    
    private void CheckForItemConversionForCostTrade( MerchantRecipeList tradeList, Item inputItem, Item outputItem, float fFrequency, int iMinPrice, int iMaxPrice, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
            int iEmeraldQuantity = MathHelper.getRandomIntegerInRange( rand, iMinPrice, iMaxPrice );
            
            ItemStack inputStack = new ItemStack( inputItem );
            ItemStack outputStack = new ItemStack( outputItem );
            
            tradeList.add( new MerchantRecipe( inputStack, new ItemStack( Item.emerald, iEmeraldQuantity ), outputStack, iTradeLevel ) );
        }
    }
    
    private void CheckForSkullConversionForCostTrade( MerchantRecipeList tradeList, int iInputSkullType, int iOutputSkullType, float fFrequency, int iMinPrice, int iMaxPrice, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
            int iEmeraldQuantity = MathHelper.getRandomIntegerInRange( rand, iMinPrice, iMaxPrice );
            
            ItemStack inputStack = new ItemStack( Item.skull, 1, iInputSkullType );
            ItemStack outputStack = new ItemStack( Item.skull, 1, iOutputSkullType );
            
            tradeList.add( new MerchantRecipe( inputStack, new ItemStack( Item.emerald, iEmeraldQuantity ), outputStack, iTradeLevel ) );
        }
    }
    
    private void CheckForComplexTrade( MerchantRecipeList tradeList, ItemStack inputStack1, ItemStack inputStack2, ItemStack outputStack, float fFrequency, int iTradeLevel )
    {
        if ( GetCurrentTradeLevel() >= iTradeLevel && rand.nextFloat() < ComputeAdjustedChanceOfTrade( fFrequency, iTradeLevel ) )
        {
        	tradeList.add( new MerchantRecipe( inputStack1, inputStack2, outputStack, iTradeLevel ) );
        }
    }
    
    private boolean DoesRecipeListAlreadyContainRecipe( MerchantRecipe recipe )
    {
        for ( int iTempRecipeIndex = 0; iTempRecipeIndex < buyingList.size(); ++iTempRecipeIndex )
        {
            MerchantRecipe tempRecipe = (MerchantRecipe)buyingList.get(iTempRecipeIndex);

            if ( recipe.hasSameIDsAs( tempRecipe ) )
            {
            	return true;
            }
        }
        
        return false;
    }    
    
    private boolean CustomInteract( EntityPlayer player )
    {
        ItemStack heldStack = player.inventory.getCurrentItem();

        if ( heldStack != null && heldStack.getItem().itemID == Item.diamond.itemID && 
        	getGrowingAge() == 0 && GetInLove() == 0 && !IsPossessed() )
        {
            if ( !player.capabilities.isCreativeMode )
            {
                heldStack.stackSize--;

                if ( heldStack.stackSize <= 0 )
                {
                    player.inventory.setInventorySlotContents( player.inventory.currentItem, (ItemStack)null );
                }
            }

			worldObj.playSoundAtEntity( this, 
        		"random.classic_hurt", 1.0F, 
        		getSoundPitch() * 2.0F);
			
            SetInLove( 1 );
            
            entityToAttack = null;

            return true;
        }
        
        return false;
    }    
    
    private void CheckForInvalidTrades()
    {
    	int iProfession = getProfession();
    	int iCurrentTradeLevel = GetCurrentTradeLevel();  	
    	
    	if ( iProfession == 0 ) // peasant
    	{
    		if ( iCurrentTradeLevel >= 4 )
    		{
		        Iterator tradeListIterator = buyingList.iterator();
		
		        while ( tradeListIterator.hasNext() )
		        {
		            MerchantRecipe tempRecipe = (MerchantRecipe)tradeListIterator.next();
		
		            if ( IsInvalidPeasantTrade( tempRecipe ) )
		            {
		            	ScheduleImmediateTradelistRefresh();
		            	
		            	tradeListIterator.remove();
		            }
		        }
    		}
    	}
    	else if ( iProfession == 2 ) // priest
    	{
    		if ( iCurrentTradeLevel >= 3 )
    		{
		        Iterator tradeListIterator = buyingList.iterator();
		
		        while ( tradeListIterator.hasNext() )
		        {
		            MerchantRecipe tempRecipe = (MerchantRecipe)tradeListIterator.next();
		
		            if ( IsInvalidPriestTrade( tempRecipe ) )
		            {
		            	ScheduleImmediateTradelistRefresh();
		            	
		            	tradeListIterator.remove();
		            }
		        }
    		}
        }
    	else if ( iProfession == 3 ) // Blacksmith
    	{
	        Iterator tradeListIterator = buyingList.iterator();
	
	        while ( tradeListIterator.hasNext() )
	        {
	            MerchantRecipe tempRecipe = (MerchantRecipe)tradeListIterator.next();
	
	            if ( IsInvalidBlacksmithTrade( tempRecipe ) )
	            {
	            	ScheduleImmediateTradelistRefresh();
	            	
	            	tradeListIterator.remove();
	            }
	        }
        }
    	else if ( iProfession == 4 ) // Butcher
    	{
    		if ( iCurrentTradeLevel >= 4 )
    		{
		        Iterator tradeListIterator = buyingList.iterator();
				
		        while ( tradeListIterator.hasNext() )
		        {
		            MerchantRecipe tempRecipe = (MerchantRecipe)tradeListIterator.next();
		
		            if ( IsInvalidButcherTrade( tempRecipe ) )
		            {
		            	ScheduleImmediateTradelistRefresh();
		            	
		            	tradeListIterator.remove();
		            }
		        }
    		}
    	}
    }
    
    private boolean IsInvalidPeasantTrade( MerchantRecipe recipe )
    {
    	// NOTE: Won't get checked unless calling method determines villager of sufficient level
    	
    	if ( recipe.getItemToBuy().itemID == FCBetterThanWolves.fcPlanter.blockID )
    	{
    		// legacy planter trade
    		
    		return true;
    	}
    	
    	return false;
    }
    
    private boolean IsInvalidPriestTrade( MerchantRecipe recipe )
    {
    	// NOTE: Won't get checked unless calling method determines villager of sufficient level
    	
    	if ( recipe.getItemToBuy().itemID == Item.netherStar.itemID )
    	{
    		if ( ( recipe.getSecondItemToBuy() == null && 
				recipe.getItemToSell().itemID == Item.emerald.itemID ) ||
    			( 
    				recipe.getSecondItemToBuy() != null && 
    				recipe.getSecondItemToBuy().itemID == Item.emerald.itemID && 
    				recipe.getItemToSell().itemID == FCBetterThanWolves.fcAnvil.blockID 
				)				
			)
    		{
            	// old anvil trade
    			
        		return true;
    		}
    	}
    	else if ( recipe.getItemToBuy().itemID == Item.bone.itemID &&
    		recipe.getItemToBuy().stackSize > 16 )
    	{
    		// old bone item trade invalidated by stack size changes
    		
    		return true;
    	}
    	
    	return false;
    }
    
    private boolean IsInvalidBlacksmithTrade( MerchantRecipe recipe )
    {
    	// NOTE: Won't get checked unless calling method determines villager of sufficient level
    	
    	int iBuyItemID = recipe.getItemToBuy().itemID;
    		
    	if ( iBuyItemID == FCBetterThanWolves.fcAnvil.blockID &&
    		recipe.getItemToSell().itemID == Item.emerald.itemID )
    	{
        	// old anvil trade
        	
    		return true;
    	}
    	else if ( iBuyItemID == FCBetterThanWolves.fcItemChunkIronOre.itemID ||
    		iBuyItemID == FCBetterThanWolves.fcItemChunkGoldOre.itemID )
    	{
    		// replacing ore trades with nuggets since they can't be converted back 
    		
    		return true;
    	}
    	
    	return false;
    }
    
    private boolean IsInvalidButcherTrade( MerchantRecipe recipe )
    {
    	// NOTE: Won't get checked unless calling method determines villager of sufficient level
    	
        if ( recipe.getItemToBuy().itemID == FCBetterThanWolves.fcAestheticNonOpaque.blockID && 
        	recipe.getItemToBuy().getItemDamage() == 
    		FCBlockAestheticNonOpaque.m_iSubtypeLightningRod )
        {
        	// old lightning rod trade
        	
        	return true;
        }
        else if ( recipe.getItemToBuy().itemID == FCBetterThanWolves.fcItemMould.itemID )
        {
        	// Moulds have been deprecated
        	
        	return true;
        }
        
        return false;
    }
    
    private void UpdateStatusParticles()
    {
    	if ( getProfession() == 2 && GetCurrentTradeLevel() >= 5 ) // top level priest
    	{
    		// enderman particles
    		
            worldObj.spawnParticle( "portal", 
            	posX + ( rand.nextDouble() - 0.5D ) * width, 
            	posY + rand.nextDouble() * height - 0.25D, 
            	posZ + ( rand.nextDouble() - 0.5D ) * width, 
            	( rand.nextDouble() - 0.5D ) * 2D, 
            	-rand.nextDouble(), 
            	( rand.nextDouble() - 0.5D ) * 2D );
    	}
    	
        if ( GetInLove() > 0 )
        {
        	GenerateRandomParticles( "heart" );
        }
    }
    
    protected void GenerateRandomParticles( String sParticle )
    {
        for ( int iTempCount = 0; iTempCount < 5; ++iTempCount )
        {
            double dVelX = rand.nextGaussian() * 0.02D;
            double dVelY = rand.nextGaussian() * 0.02D;
            double dVelZ = rand.nextGaussian() * 0.02D;
            
            worldObj.spawnParticle( sParticle, 
            	posX + ( rand.nextFloat() * width * 2F ) - width, 
            	posY + 1D + ( rand.nextFloat() * height ), 
            	posZ + ( rand.nextFloat() * width * 2F ) - width, 
            	dVelX, dVelY, dVelZ);
        }
    }
    
    public void CheckForLooseMilk()
    {    
	    List collisionList = worldObj.getEntitiesWithinAABB( EntityItem.class, 
    		AxisAlignedBB.getAABBPool().getAABB( 
			posX - 1.0f, posY - 1.0f, posZ - 1.0f, 
			posX + 1.0f, posY + 1.0f, posZ + 1.0f) );
	    
	    if ( !collisionList.isEmpty() )
	    {
            for(int listIndex = 0; listIndex < collisionList.size(); listIndex++)
            {
	    		EntityItem entityItem = (EntityItem)collisionList.get( listIndex );;
	    		
		        if ( entityItem.delayBeforeCanPickup <= 0 && !(entityItem.isDead) )
		        {
		        	int iTempItemID = entityItem.getEntityItem().itemID;
		        	
		    		Item tempItem = Item.itemsList[iTempItemID];
		    		
		    		if ( tempItem.itemID == Item.bucketMilk.itemID )
		    		{
		    			// toss the milk
			            
			            entityItem.setDead();
			            
			            entityItem = new EntityItem( worldObj, posX, posY - 0.30000001192092896D + (double)getEyeHeight(), posZ, 
			            	new ItemStack( Item.bucketMilk, 1, 0 ) );
			            
	    	            float f1 = 0.2F;
	    	            
	    	            entityItem.motionX = -MathHelper.sin((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F) * f1;
	    	            entityItem.motionZ = MathHelper.cos((rotationYaw / 180F) * 3.141593F) * MathHelper.cos((rotationPitch / 180F) * 3.141593F) * f1;
	    	            
	    	            entityItem.motionY = -MathHelper.sin((rotationPitch / 180F) * 3.141593F) * f1 + 0.2F;

	    	            f1 = 0.02F;
	    	            float f3 = rand.nextFloat() * 3.141593F * 2.0F;
	    	            f1 *= rand.nextFloat();
	    	            entityItem.motionX += Math.cos(f3) * (double)f1;
	    	            entityItem.motionY += 0.25F;
	    	            entityItem.motionZ += Math.sin(f3) * (double)f1;
	    	            
			            entityItem.delayBeforeCanPickup = 10;
			            
			            worldObj.spawnEntityInWorld( entityItem );

			            int iFXI = MathHelper.floor_double( entityItem.posX );
			            int iFXJ = MathHelper.floor_double( entityItem.posY );
			            int iFXK = MathHelper.floor_double( entityItem.posZ );
			            
			            int iExtraData = 0;
			            
			        	if ( IsPossessed() || ( getProfession() == 2 && GetCurrentTradeLevel() == 5 ) )
			        	{
			        		iExtraData = 1;
			        	}
			        	
		    	        worldObj.playAuxSFX( FCBetterThanWolves.m_iTossTheMilkAuxFXID, iFXI, iFXJ, iFXK, iExtraData );  	        
		            		
		    			
			        }
	    		}
            }
	    }
    }    
    
    public int GetInLove()
    {
        return dataWatcher.getWatchableObjectInt( m_iInLoveDataWatcherID );
    }
    
    public void SetInLove( int iInLove )
    {
        dataWatcher.updateObject( m_iInLoveDataWatcherID, Integer.valueOf( iInLove ) );
    }
    
    public int GetDirtyPeasant()
    {
        return dataWatcher.getWatchableObjectInt( m_iDirtyPeasantDataWatcherID );
    }
    
    public void SetDirtyPeasant( int iDirtyPeasant )
    {
        dataWatcher.updateObject( m_iDirtyPeasantDataWatcherID, Integer.valueOf( iDirtyPeasant ) );
    }
    
    public int GetCurrentTradeLevel()
    {
        return dataWatcher.getWatchableObjectInt( m_iTradeLevelDataWatcherID );
    }
    
    public void SetTradeLevel( int iTradeLevel )
    {
        dataWatcher.updateObject( m_iTradeLevelDataWatcherID, Integer.valueOf( iTradeLevel ) );
    }
    
    public int GetCurrentTradeXP()
    {
        return dataWatcher.getWatchableObjectInt( m_iTradeExperienceDataWatcherID );
    }
    
    public void SetTradeExperience( int iTradeExperience )
    {
        dataWatcher.updateObject( m_iTradeExperienceDataWatcherID, Integer.valueOf( iTradeExperience ) );
    }
    
    public int GetCurrentTradeMaxXP()
    {
    	int iLevel = GetCurrentTradeLevel();
    	
    	switch ( iLevel )
    	{
    		case 1:
    			
    			return 5;
    			
    		case 2:
    			
    			return 7;
    			
    		case 3:
    			
    			return 10;
    			
    		case 4:
    			
    			return 15;
    			
    		default:
    			
    			return 20;    			
    	}
    }    
    
    public int GetCurrentMaxNumTrades()
    {
    	int iCurrentTradeLevel = GetCurrentTradeLevel();
    	int iMaxTrades = iCurrentTradeLevel;
    	
    	switch ( getProfession() )
    	{
	        case 0: // peasant
	        	
	        	break;
	        	
	        case 1: // librarian
	        	
	            break;
	
	        case 2: // priest
	        	
	        	if ( iCurrentTradeLevel >= 4 )
	        	{
	        		// extra trade to help compensate for the 2 mandatory ones at level 4
	        		
	        		iMaxTrades += 1;
	        	}
	        	
	        	break;
	        	
	        case 3: // blacksmith
	        	
	            break;
	
	        case 4: // butcher
	        	
	            break;
	            
            default:
            	
            	break;
    	}
    	
    	return iMaxTrades;
    }
    
    private void ScheduleImmediateTradelistRefresh()
    {
		m_iUpdateTradesCountdown = 1;
    }
    
	//----------- Client Side Functionality -----------//

    @Override
    public String getTexture()
    {
        switch ( getProfession() )
        {
            case 0: // peasant
            	
            	if ( GetDirtyPeasant() > 0 )
            	{
            		return "/btwmodtex/fcDirtyPeasant.png";
            	}
            	
            	break;

            case 1: // librarian
            	
            	if ( GetCurrentTradeLevel() >= 5 )
            	{
            		return "/btwmodtex/fcLibrarianSpecs.png";
            	}
            	
            	break;

            case 2: // priest
            	
            	if ( GetCurrentTradeLevel() >= 5 )
            	{
            		return "/btwmodtex/fcPriestLvl.png";
            	}
            	
            	break;
            	
            case 3: // smith
            	
            	break;

            case 4: // butcher
            	
            	if ( GetCurrentTradeLevel() > 3 )
            	{
            		return "/btwmodtex/fcButcherLvl.png";
            	}
            	
            	break;
        }
        
        return super.getTexture();
    }
    
    @Override
    public void handleHealthUpdate( byte bUpdateType )
    {
        super.handleHealthUpdate( bUpdateType );
        
        if ( bUpdateType == 14 )
        {
            // item collect sound on villager restock
        	
            worldObj.playSound( posX, posY, posZ, "random.pop", 
        		0.25F, ( ( rand.nextFloat() - rand.nextFloat() ) * 0.7F + 1F ) * 2F );
        }        
    }
}