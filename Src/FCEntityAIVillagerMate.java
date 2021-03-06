// FCMOD

package net.minecraft.src;

import java.util.Iterator;
import java.util.List;

public class FCEntityAIVillagerMate extends EntityAIBase
{
    static final double m_dDistanceToCheckForMate = 8F;
    
    private FCEntityVillager m_villager;
    private FCEntityVillager m_mate;
    private World m_world;

    private int m_iSpawnBabyDelay = 0;
    private int m_iThrustDelay = 0;
    
    public FCEntityAIVillagerMate( FCEntityVillager villager )
    {
        m_villager = villager;
        m_world = villager.worldObj;
        setMutexBits( 3 );
    }

    public boolean shouldExecute()
    {
        if ( m_villager.GetInLove() <= 0 )
        {
            return false;
        }
        else
        {
            m_mate = getNearbyMate();
            
            return m_mate != null;
        }
    }

    public void resetTask()
    {
        m_mate = null;
        m_iSpawnBabyDelay = 0;
    }

    public boolean continueExecuting()
    {
        return m_mate != null && m_mate.isEntityAlive() && m_mate.GetInLove() > 0 && 
        	m_iSpawnBabyDelay < 100;
    }

    public void updateTask()
    {
        m_villager.getLookHelper().setLookPositionWithEntity( m_mate, 10F, 30F );

        if ( m_villager.getDistanceSqToEntity( m_mate ) > 4D )
        {
        	m_villager.getNavigator().tryMoveToEntityLiving( m_mate, 0.25F );
        	
        	m_iSpawnBabyDelay = 0;
        	
        	m_iThrustDelay = m_villager.rand.nextInt( 5 ) + 15;
        }
        else
        {
            m_iSpawnBabyDelay++;
            
            if ( m_iSpawnBabyDelay == 100 )
            {
                giveBirth();
            }
            else
            {
            	m_iThrustDelay--;
            	
            	if ( m_iThrustDelay <= 0 )
            	{
            		m_world.playSoundAtEntity( m_villager, 
                		"random.classic_hurt", 1F +  m_villager.rand.nextFloat() * 0.25F, 
	            		m_villager.getSoundPitch() * 2F );
            		
                	m_iThrustDelay = m_villager.rand.nextInt( 5 ) + 15;
                	
                	if ( m_villager.onGround )
                	{
                		m_villager.jump();                		
                	}
            	}
            }
        }        
        
    }

    private void giveBirth()
    {
        FCEntityVillager babyVillager = m_villager.func_90012_b( m_mate );

    	int iBabyProfession = m_villager.getProfession();
    	
    	// 50% chance of baby inheriting profession from other parent
    	
        if ( babyVillager.rand.nextInt( 2 ) == 0 )
        {
        	babyVillager.setProfession( m_mate.getProfession() );
        }
        
        if ( iBabyProfession != 0 ) // 0 = peasant
        {
        	// 25% chance of baby being of same "caste" but different profession
        	
        	if ( babyVillager.rand.nextInt( 4 ) == 0 )
        	{
        		switch ( iBabyProfession )
        		{
        			case 1: // librarian
        				
        				iBabyProfession = 2; // priest
        				
        				break;
        				
        			case 2: // priest
        				
        				iBabyProfession = 1; // librarian
        				
        				break;
        				
        			case 3: // blacksmith
        				
        				iBabyProfession = 4; // butcher
        				
        				break;
        				
        			case 4: // butcher
        				
        				iBabyProfession = 3; // blacksmith
        				
        				break;        				
        		}
        	}
        }
        
    	babyVillager.setProfession( iBabyProfession );
    	
        m_mate.setGrowingAge( 6000 );
        m_villager.setGrowingAge( 6000 );
        
        m_mate.SetInLove( 0 );
        m_villager.SetInLove( 0 );
        
        babyVillager.setGrowingAge( -babyVillager.GetTicksForChildToGrow() );
        
        babyVillager.setLocationAndAngles( m_villager.posX, m_villager.posY, m_villager.posZ, 
        	0F, 0F );
        
        m_world.spawnEntityInWorld( babyVillager );
        
        m_world.setEntityState( babyVillager, (byte)12 );
        
        // birthing effects
        
        m_world.playAuxSFX( FCBetterThanWolves.m_iAnimalBirthingAuxFXID, 
    		MathHelper.floor_double( babyVillager.posX ), 
    		MathHelper.floor_double( babyVillager.posY ), 
    		MathHelper.floor_double( babyVillager.posZ ), 
    		0 );
    }
    
    private FCEntityVillager getNearbyMate()
    {        
        List potentialMateList = m_world.getEntitiesWithinAABB( FCEntityVillager.class, 
        	m_villager.boundingBox.expand( m_dDistanceToCheckForMate, m_dDistanceToCheckForMate, 
    		m_dDistanceToCheckForMate ) );
        
        Iterator mateIterator = potentialMateList.iterator();
        
        FCEntityVillager foundMate = null;

        while ( mateIterator.hasNext() )
        {
        	FCEntityVillager tempVillager = (FCEntityVillager)mateIterator.next();
        	
        	if ( CanMateWith( tempVillager ) )
        	{
        		return tempVillager;
        	}
        }

        return null;
    }
    
    private boolean CanMateWith( FCEntityVillager targetVillager )
    {
    	if ( targetVillager != m_villager && targetVillager.GetInLove() > 0 && 
    		!targetVillager.isLivingDead )
    	{
    		return true;
    	}
    	
    	return false;
    }
}