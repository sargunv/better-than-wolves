// FCMOD
// special cased full block with standard attributes (stuff like stone and dirt) to speed rendering

package net.minecraft.src;

public class FCBlockFullBlock extends Block
{
    protected FCBlockFullBlock( int iBlockID, Material material )
    {
    	super( iBlockID, material );
    }
    
	//----------- Client Side Functionality -----------//
    
    @Override
    public boolean RenderBlock( RenderBlocks renderer, int i, int j, int k )
    {
    	return renderer.RenderStandardFullBlock( this, i, j, k );
    }
    
    @Override
    public boolean DoesItemRenderAsBlock( int iItemDamage )
    {
    	return true;
    }
    
    @Override
    public void RenderBlockMovedByPiston( RenderBlocks renderBlocks, int i, int j, int k )
    {
    	renderBlocks.RenderStandardFullBlockMovedByPiston( this, i, j, k );
    }    
}