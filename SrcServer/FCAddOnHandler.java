// FCMOD

package net.minecraft.src;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FCAddOnHandler
{
	public static List m_ModList = new LinkedList();
	public static boolean m_bModsInitialized = false;
	
    public static final Logger m_Logger = Logger.getLogger( "BetterThanWolves" );
    
	// Client version
    //private static final File m_LogFile = new File( Minecraft.getMinecraftDir(), "BTWLog.txt" );    
	// Server version
    private static final File m_LogFile = new File( new File("."), "BTWLog.txt" );
    
    private static FileHandler m_LogFileHandler = null;
    
	public static void AddMod( FCAddOn mod )
	{
		m_ModList.add( mod );
	}
	
	public static void InitializeMods()
	{
		if ( !m_bModsInitialized )
		{
			InitializeLogger();
			
			LogMessage( "...Add-On Handler Initializing..." );
			
			PreInitializeMods();
			
	    	Iterator modIterator = m_ModList.iterator();
	    	
	    	while ( modIterator.hasNext() )
	    	{
	    		FCAddOn tempMod = (FCAddOn)modIterator.next();
	    		
	    		tempMod.Initialize();
	    	}
			
			PostInitializeMods();
	    	
	    	m_bModsInitialized = true;
	    	
	    	OnLanguageLoaded( StringTranslate.getInstance() );
	    	
	    	LogMessage( "...Add-On Handler Initialization Complete..." );
		}
	}
	
	public static void InitializeLogger()
	{
        try
        {
	        if ( ( m_LogFile.exists() || m_LogFile.createNewFile() ) && m_LogFile.canWrite() )
	        {
	            m_LogFileHandler = new FileHandler( m_LogFile.getPath() );
	            m_LogFileHandler.setFormatter( new SimpleFormatter() );
	            m_Logger.addHandler( m_LogFileHandler );
	            
	        	m_Logger.setLevel( Level.FINE );
	        }
        }
        catch ( Throwable error )
        {
            throw new RuntimeException( error );
        }
	}
	
	public static void LogMessage( String string )
	{
		System.out.println( string );

		if ( net.minecraft.server.MinecraftServer.getServer() != null )
		{
			// client
	    	//net.minecraft.server.MinecraftServer.getServer().getLogAgent().logInfo( string );
			// server
	    	net.minecraft.server.MinecraftServer.getServer().getLogAgent().func_98233_a( string );
		}
    	
		m_Logger.fine( string );
	}
	
	public static void LogWarning( String string )
	{
		System.out.println( string );

		if ( net.minecraft.server.MinecraftServer.getServer() != null )
		{
			// client
	    	//net.minecraft.server.MinecraftServer.getServer().getLogAgent().logWarning( string );
			// server
	    	net.minecraft.server.MinecraftServer.getServer().getLogAgent().func_98236_b( string );
		}
    	
		m_Logger.fine( string );
	}
	
	public static void PreInitializeMods()
	{
    	Iterator modIterator = m_ModList.iterator();
    	
    	while ( modIterator.hasNext() )
    	{
    		FCAddOn tempMod = (FCAddOn)modIterator.next();
    		
    		tempMod.PreInitialize();
    	}
	}
	
	public static void PostInitializeMods()
	{
    	Iterator modIterator = m_ModList.iterator();
    	
    	while ( modIterator.hasNext() )
    	{
    		FCAddOn tempMod = (FCAddOn)modIterator.next();
    		
    		tempMod.PostInitialize();
    	}
	}
	
	public static void OnLanguageLoaded( StringTranslate translator )
	{
		// only call on language loaded after mods have been initialized to prevent funkiness due to static instance variable of 
		// StringTranslate creating ambiguous initialization order.
		
		if ( m_bModsInitialized )
		{
	    	Iterator modIterator = m_ModList.iterator();
	    	
	    	while ( modIterator.hasNext() )
	    	{
	    		FCAddOn tempMod = (FCAddOn)modIterator.next();
	    		
	    		tempMod.OnLanguageLoaded( translator );
	    		
	    		String sPrefix = tempMod.GetLanguageFilePrefix();
	    		
	    		if ( sPrefix != null )
	    		{
	    			LogMessage( "...Add-On Handler Loading Custom Language File With Prefix: " + sPrefix + "..."  );
	    			
	    			translator.LoadAddonLanguageExtension( sPrefix );
	    		}
	    	}	    	
		}
	}
	
    public static void ServerCustomPacketReceived( NetServerHandler handler, Packet250CustomPayload packet )
    {
    	Iterator modIterator = m_ModList.iterator();
    	
    	while ( modIterator.hasNext() )
    	{
    		FCAddOn tempMod = (FCAddOn)modIterator.next();
    		
    		if ( tempMod.ServerCustomPacketReceived( handler, packet ) )
    		{
    			return;
    		}    		
    	}    	
    }
    
	//----------- Client Side Functionality -----------//
}
