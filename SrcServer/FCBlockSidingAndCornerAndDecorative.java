// FCMOD

package net.minecraft.src;

import java.util.List;
import java.util.Random;

//import org.lwjgl.opengl.GL11; // client only

public class FCBlockSidingAndCornerAndDecorative extends FCBlockSidingAndCorner
{
	public static final int m_iSubtypeBench = 12;
	public static final int m_iSubtypeFence = 14;
	
    private final static float m_fBenchTopHeight = ( 2F / 16F );
    private final static float m_fBenchLegHeight = ( 0.5F - m_fBenchTopHeight );
    private final static float m_fBenchLegWidth = ( 4F / 16F );
    private final static float m_fBenchLegHalfWidth = ( m_fBenchLegWidth / 2F );
    
    public final static int m_iOakBenchTopTextureID = 93;
    public final static int m_iOakBenchLegTextureID = 94;
    
	protected FCBlockSidingAndCornerAndDecorative( int iBlockID, Material material, String sTextureName, float fHardness, float fResistance, StepSound stepSound, String name )
	{
		super( iBlockID, material, sTextureName, fHardness, fResistance, stepSound, name );
	}
	
	@Override
    public void addCollisionBoxesToList( World world, int i, int j, int k, 
		AxisAlignedBB axisalignedbb, List list, Entity entity )
    {
    	int iSubtype = world.getBlockMetadata( i, j, k );
    	
		if ( iSubtype == m_iSubtypeFence )
		{
			AddCollisionBoxesToListForFence( world, i, j, k, axisalignedbb, list, entity );
		}
		else
		{
            super.addCollisionBoxesToList(world, i, j, k, axisalignedbb, list, entity);
		}
    }
	
    @Override
    public AxisAlignedBB GetBlockBoundsFromPoolBasedOnState( 
    	IBlockAccess blockAccess, int i, int j, int k )
    {
		int iSubtype = blockAccess.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeBench )
		{
			return GetBlockBoundsFromPoolForBench( blockAccess, i, j, k );
		}
		else if ( iSubtype == m_iSubtypeFence )
		{
			return GetBlockBoundsFromPoolForFence( blockAccess, i, j, k );
		}
		
		return super.GetBlockBoundsFromPoolBasedOnState( blockAccess, i, j, k );
    }
	
    @Override
    public MovingObjectPosition collisionRayTrace( World world, int i, int j, int k, Vec3 startRay, Vec3 endRay )
    {
        int iBlockID = world.getBlockId( i, j, k );

    	if ( IsBlockBench( world, i, j, k ) && DoesBenchHaveLeg( world, i, j, k ) )
    	{
	   		return CollisionRayTraceBenchWithLeg( world, i, j, k, startRay, endRay );
    	}
    	else if ( ( iBlockID == blockID && world.getBlockMetadata( i, j, k ) == m_iSubtypeFence ) || iBlockID == Block.fenceGate.blockID )
    	{
	   		return CollisionRayTraceFence( world, i, j, k, startRay, endRay );
    	}
    	
    	return super.collisionRayTrace( world, i, j, k, startRay, endRay );
    }
    
	@Override
    public int onBlockPlaced( World world, int i, int j, int k, int iFacing, float fClickX, float fClickY, float fClickZ, int iMetadata )
    {
		int iSubtype = world.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeBench || iSubtype == m_iSubtypeFence )
		{
			return iMetadata;
		}
		
		return super.onBlockPlaced( world, i, j, k, iFacing, fClickX, fClickY, fClickZ, iMetadata );
    }
	
    @Override
	public boolean HasCenterHardPointToFacing( IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency )
	{
		int iSubtype = blockAccess.getBlockMetadata( i, j, k );
		
    	if ( iSubtype == m_iSubtypeBench )
    	{
    		return iFacing == 0;
    	}
    	else if ( iSubtype == m_iSubtypeFence )
    	{
    		return iFacing == 0 || iFacing == 1;
    	}
    	
    	return super.HasCenterHardPointToFacing( blockAccess, i, j, k, iFacing, bIgnoreTransparency );
	}

    @Override
	public boolean HasLargeCenterHardPointToFacing( IBlockAccess blockAccess, int i, int j, int k, int iFacing, boolean bIgnoreTransparency )
    {
		int iSubtype = blockAccess.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeBench || iSubtype == m_iSubtypeFence )
		{
			return false;			
		}
		
    	return super.HasLargeCenterHardPointToFacing( blockAccess, i, j, k, iFacing, bIgnoreTransparency );
    }
    
	@Override
    public int damageDropped( int iMetadata )
    {
		if ( IsDecorativeFromMetadata( iMetadata ) )
		{
			return iMetadata;
		}
		
		return super.damageDropped( iMetadata );
    }

	@Override
    public boolean getBlocksMovement( IBlockAccess blockAccess, int i, int j, int k )
    {
		int iSubtype = blockAccess.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeFence )
		{			
			return false;
		}
		
		return super.getBlocksMovement( blockAccess, i, j, k );        
    }
    
    @Override
    public boolean CanGroundCoverRestOnBlock( World world, int i, int j, int k )
    {
    	int iMetadata = world.getBlockMetadata( i, j, k );
    	
		if ( iMetadata == m_iSubtypeBench )
		{
			return true;
		}		
		else if ( IsDecorativeFromMetadata( iMetadata ) )
		{
    		return world.doesBlockHaveSolidTopSurface( i, j, k );
		}
    	
    	return super.CanGroundCoverRestOnBlock( world, i, j, k );    		
    }
    
    @Override
    public float GroundCoverRestingOnVisualOffset( IBlockAccess blockAccess, int i, int j, int k )
    {
    	int iMetadata = blockAccess.getBlockMetadata( i, j, k );
    	
		if ( iMetadata == m_iSubtypeBench )
		{
			return -0.5F;
		}		
		else if ( IsDecorativeFromMetadata( iMetadata ) )
		{
    		return 0F;
		}
    	
    	return super.GroundCoverRestingOnVisualOffset( blockAccess, i, j, k );
    }
    
	@Override
    public int GetWeightOnPathBlocked( IBlockAccess blockAccess, int i, int j, int k )
    {
    	int iMetadata = blockAccess.getBlockMetadata( i, j, k );
    	
		if ( iMetadata == m_iSubtypeFence )
		{
			return -3;
		}
		else
		{
			return 0;
		}
    }	
   
	@Override
	public int GetFacing( int iMetadata )
	{
		if ( iMetadata == m_iSubtypeBench || iMetadata == m_iSubtypeFence )
		{
			return 0;
		}
		
		return super.GetFacing( iMetadata );
	}
	
	@Override
	public int SetFacing( int iMetadata, int iFacing )
	{
		if ( iMetadata == m_iSubtypeBench || iMetadata == m_iSubtypeFence )
		{
			return iMetadata;
		}
		
		return super.SetFacing( iMetadata, iFacing );		
	}
	
	@Override
	public boolean CanRotateOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		int iSubtype = blockAccess.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeBench || iSubtype == m_iSubtypeFence )
		{
			return true;
		}
		
		return super.CanRotateOnTurntable( blockAccess, i, j, k );
	}
	
	@Override
	public boolean CanTransmitRotationVerticallyOnTurntable( IBlockAccess blockAccess, int i, int j, int k )
	{
		int iSubtype = blockAccess.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeFence )
		{
			return true;
		}
		else if ( iSubtype == m_iSubtypeBench )
		{
			return false;
		}
		
		return super.CanTransmitRotationVerticallyOnTurntable( blockAccess, i, j, k );
	}
	
	@Override
	public int RotateMetadataAroundJAxis( int iMetadata, boolean bReverse )
	{
		if ( iMetadata == m_iSubtypeBench || iMetadata == m_iSubtypeFence )
		{
			return iMetadata;
		}
		
		return super.RotateMetadataAroundJAxis( iMetadata, bReverse );
	}
	
	@Override
	public boolean ToggleFacing( World world, int i, int j, int k, boolean bReverse )
	{
		int iSubtype = world.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeBench || iSubtype == m_iSubtypeFence )
		{
			return false;
		}
		
		return super.ToggleFacing( world, i, j, k, bReverse );
	}
	
	@Override
    public float MobSpawnOnVerticalOffset( World world, int i, int j, int k )
    {
		int iSubtype = world.getBlockMetadata( i, j, k );
		
		if ( iSubtype == m_iSubtypeFence )
		{
	    	// corresponds to the actual collision volume of the fence, which extends
	    	// half a block above it
	    	
	    	return 0.5F;
		}
		else if ( iSubtype == m_iSubtypeBench )
		{
			return -0.5F;
		}
		
		return super.MobSpawnOnVerticalOffset( world, i, j, k );
    }
    
    //------------- Class Specific Methods ------------//
	
	public boolean IsDecorative( IBlockAccess blockAccess, int i, int j, int k )
	{
		return IsDecorativeFromMetadata( blockAccess.getBlockMetadata( i, j, k ) );
	}
	
	static public boolean IsDecorativeFromMetadata( int iMetadata )
	{
		return iMetadata == m_iSubtypeBench || iMetadata == m_iSubtypeFence;
	}
    
    public AxisAlignedBB GetBlockBoundsFromPoolForBench( IBlockAccess blockAccess, int i, int j, int k )
    {
		if ( !DoesBenchHaveLeg( blockAccess, i, j, k ) )
		{
			return AxisAlignedBB.getAABBPool().getAABB( 
				0D, 0.5D - m_fBenchTopHeight, 0D, 
        		1D, 0.5D, 1D );
		}
		else
		{
			return AxisAlignedBB.getAABBPool().getAABB( 
				0D, 0D, 0D, 1D, 0.5D, 1D );
		}
    }
    
    public AxisAlignedBB GetBlockBoundsFromPoolForFence( IBlockAccess blockAccess, int i, int j, int k )
    {
    	AxisAlignedBB fenceBox = AxisAlignedBB.getAABBPool().getAABB(
    		0.375D, 0D, 0.375D, 
    		0.625D, 1D, 0.625D );

        if ( DoesFenceConnectTo( blockAccess, i, j, k - 1 ) )
        {
        	fenceBox.minZ = 0D;
        }

        if ( DoesFenceConnectTo( blockAccess, i, j, k + 1 ) )
        {
        	fenceBox.maxZ = 1D;
        }

        if ( DoesFenceConnectTo( blockAccess, i - 1, j, k ) )
        {
        	fenceBox.minX = 0D;
        }

        if ( DoesFenceConnectTo( blockAccess, i + 1, j, k ) )
        {
        	fenceBox.maxX = 1D;
        }

		return fenceBox;
    }
    
    public void AddCollisionBoxesToListForFence( World world, int i, int j, int k, 
		AxisAlignedBB intersectingBox, List list, Entity entity )
    {
        boolean bConnectsNegativeI = DoesFenceConnectTo( world, i - 1, j, k );
        boolean bConnectsPositiveI = DoesFenceConnectTo( world, i + 1, j, k );
        boolean bConnectsNegativeK = DoesFenceConnectTo( world, i, j, k - 1 );
        boolean bConnectsPositiveK = DoesFenceConnectTo( world, i, j, k + 1 );
        
        float fXMin = 0.375F;
        float fXMax = 0.625F;
        float fZMin = 0.375F;
        float fZMax = 0.625F;

        if ( bConnectsNegativeK )
        {
            fZMin = 0.0F;
        }

        if ( bConnectsPositiveK )
        {
            fZMax = 1.0F;
        }

        if ( bConnectsNegativeK || bConnectsPositiveK )
        {
        	AxisAlignedBB.getAABBPool().getAABB( fXMin, 0.0F, fZMin, fXMax, 1.5F, fZMax ).
        		offset( i, j, k ).AddToListIfIntersects( intersectingBox, list );
        }

        if ( bConnectsNegativeI )
        {
            fXMin = 0.0F;
        }

        if ( bConnectsPositiveI )
        {
            fXMax = 1.0F;
        }

        if ( bConnectsNegativeI || bConnectsPositiveI || ( !bConnectsNegativeK && !bConnectsPositiveK ) )
        {
        	AxisAlignedBB.getAABBPool().getAABB( fXMin, 0.0F, 0.375F, fXMax, 1.5F, 0.625F ).
    			offset( i, j, k ).AddToListIfIntersects( intersectingBox, list );
        }
    }
    
    public boolean DoesBenchHaveLeg( IBlockAccess blockAccess, int i, int j, int k )
    {
    	int iBlockBelowID = blockAccess.getBlockId( i, j - 1, k );
    	
    	if ( blockID == FCBetterThanWolves.fcBlockNetherBrickSidingAndCorner.blockID )
    	{
    		if ( iBlockBelowID == Block.netherFence.blockID )
    		{
    			return true;
    		}
    	}
    	else if ( blockID == iBlockBelowID )
    	{
    		int iBlockBelowMetadata = blockAccess.getBlockMetadata( i, j - 1, k );
    		
    		if ( iBlockBelowMetadata == FCBlockSidingAndCornerAndDecorative.m_iSubtypeFence )
    		{
    			return true;
    		}
    	}
    		
    	boolean positiveIBench = IsBlockBench( blockAccess, i + 1, j, k );
    	boolean negativeIBench = IsBlockBench( blockAccess, i - 1, j, k );
    	boolean positiveKBench = IsBlockBench( blockAccess, i, j, k + 1 );
    	boolean negativeKBench = IsBlockBench( blockAccess, i, j, k - 1 );
    	
    	if ( ( !positiveIBench && ( !positiveKBench || !negativeKBench ) ) ||
			( !negativeIBench && ( !positiveKBench || !negativeKBench ) ) )
    	{
    		return true;
    	}
    	
    	return false;
    }
    
    public boolean IsBlockBench( IBlockAccess blockAccess, int i, int j, int k )
    {
    	return blockAccess.getBlockId( i, j, k ) == blockID && blockAccess.getBlockMetadata( i, j, k ) == m_iSubtypeBench;  
    }
    
    public MovingObjectPosition CollisionRayTraceBenchWithLeg( World world, int i, int j, int k, Vec3 startRay, Vec3 endRay )
    {
    	FCUtilsRayTraceVsComplexBlock rayTrace = new FCUtilsRayTraceVsComplexBlock( world, i, j, k, startRay, endRay );
    	
    	// top
    	
		rayTrace.AddBoxWithLocalCoordsToIntersectionList( 0.0F, 0.5F - m_fBenchTopHeight, 0.0F, 1.0F, 0.5F, 1.0F );
		
		// leg
		
		rayTrace.AddBoxWithLocalCoordsToIntersectionList( 0.5F - m_fBenchLegHalfWidth, 0.0F, 0.5F - m_fBenchLegHalfWidth,
	   		0.5F + m_fBenchLegHalfWidth, m_fBenchLegHeight, 0.5F + m_fBenchLegHalfWidth );
    	
        return rayTrace.GetFirstIntersection();        
    }
    
    public MovingObjectPosition CollisionRayTraceFence( World world, int i, int j, int k, Vec3 startRay, Vec3 endRay )
    {
    	FCUtilsRayTraceVsComplexBlock rayTrace = new FCUtilsRayTraceVsComplexBlock( world, i, j, k, startRay, endRay );
    	
    	// post
    	
		rayTrace.AddBoxWithLocalCoordsToIntersectionList( 0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D );
        
        // supports
        
        boolean bConnectsAlongI = false;

        boolean bConnectsNegativeI = DoesFenceConnectTo( world, i - 1, j, k );
        boolean bConnectsPositiveI = DoesFenceConnectTo( world, i + 1, j, k );
        boolean bConnectsNegativeK = DoesFenceConnectTo( world, i, j, k - 1 );
        boolean bConnectsPositiveK = DoesFenceConnectTo( world, i, j, k + 1 );
        
        if ( bConnectsNegativeI || bConnectsPositiveI )
        {
            bConnectsAlongI = true;
        }

        boolean bConnectsAlongK = false;
        
        if ( bConnectsNegativeK || bConnectsPositiveK )
        {
            bConnectsAlongK = true;
        }

        if ( !bConnectsAlongI && !bConnectsAlongK )
        {
            bConnectsAlongI = true;
        }

        float var6 = 0.4375F;
        float var7 = 0.5625F;
        float var14 = 0.75F;
        float var15 = 0.9375F;
        
        float var16 = bConnectsNegativeI ? 0.0F : var6;
        float var17 = bConnectsPositiveI ? 1.0F : var7;
        float var18 = bConnectsNegativeK ? 0.0F : var6;
        float var19 = bConnectsPositiveK ? 1.0F : var7;

        if (bConnectsAlongI)
        {
        	rayTrace.AddBoxWithLocalCoordsToIntersectionList((double)var16, (double)var14, (double)var6, (double)var17, (double)var15, (double)var7);
        }

        if (bConnectsAlongK)
        {
        	rayTrace.AddBoxWithLocalCoordsToIntersectionList((double)var6, (double)var14, (double)var18, (double)var7, (double)var15, (double)var19);
        }

        var14 = 0.375F;
        var15 = 0.5625F;

        if (bConnectsAlongI)
        {
        	rayTrace.AddBoxWithLocalCoordsToIntersectionList((double)var16, (double)var14, (double)var6, (double)var17, (double)var15, (double)var7);
        }

        if (bConnectsAlongK)
        {
        	rayTrace.AddBoxWithLocalCoordsToIntersectionList((double)var6, (double)var14, (double)var18, (double)var7, (double)var15, (double)var19);
        }

        return rayTrace.GetFirstIntersection();        
    }
    
    public boolean DoesFenceConnectTo( IBlockAccess blockAccess, int i, int j, int k )
    {
        int iBlockID = blockAccess.getBlockId( i, j, k );

        if ( ( iBlockID == blockID && blockAccess.getBlockMetadata( i, j, k ) == m_iSubtypeFence ) || iBlockID == Block.fenceGate.blockID )
        {
        	return true;
        }
        
        Block block = Block.blocksList[iBlockID];
        
        return block != null && block.blockMaterial.isOpaque() && block.renderAsNormalBlock() && 
        	block.blockMaterial != Material.pumpkin;
    }
    
	//----------- Client Side Functionality -----------//
}