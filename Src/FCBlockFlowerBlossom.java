// FCMOD

package net.minecraft.src;

public class FCBlockFlowerBlossom extends BlockFlower
{
    protected FCBlockFlowerBlossom( int iBlockID )
    {
        super( iBlockID );
        
        setHardness( 0F );
        
        SetBuoyant();
    	SetFurnaceBurnTime( FCEnumFurnaceBurnTime.DAMP_VEGETATION );
    	SetFilterableProperties( Item.m_iFilterable_Small );
        
        setStepSound( soundGrassFootstep );
    }
    
    @Override
    public boolean CanBeGrazedOn( IBlockAccess access, int i, int j, int k, EntityAnimal animal )
    {
		return true;
    }
    
    //------------- Class Specific Methods ------------//
    
	//----------- Client Side Functionality -----------//
}
