/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.sc

import de.sciss.app.{ AbstractApplication }
import de.sciss.kontur.util.{ Model, PrefsUtil }
import de.sciss.tint.sc.{ Server, ServerOptions }
import de.sciss.util.{ Param }
import java.io.{ IOException }
import java.net.{ DatagramSocket, ServerSocket }

object SuperColliderClient {
   lazy val instance = new SuperColliderClient
   val DEFAULT_PORT = 57108

   case class ServerChanged( s: Option[ Server ])
}

class SuperColliderClient extends Model {
    import SuperColliderClient._

    private val app         = AbstractApplication.getApplication()
    private val audioPrefs  = app.getUserPrefs.node( PrefsUtil.NODE_AUDIO )
    private val so          = new ServerOptions
	private var server: Option[ Server ] = None
    private var serverIsReady = false
    private var shouldReboot = false

    // ---- constructor ----
    {
		val userPrefs	= app.getUserPrefs
		
//		oCfgListener = new PreferenceChangeListener() {
//			public void preferenceChange( PreferenceChangeEvent e )
//			{
//				outputConfigChanged();
//			}
//		};
    }

	def quit {
//		Server.quitAll
        server.foreach( _.quit )
	}

	def reboot {
		shouldReboot = true
		stop
	}

    def serverCondition : AnyRef = {
      server.map( _.condition ) getOrElse Server.Offline
    }

    private def printError( name: String, t: Throwable ) {
		System.err.println( name + " : " + t.getClass().getName() + " : " + t.getLocalizedMessage() )
	}

	def stop {
        server.foreach( s => {
          if( s.isRunning || s.isBooting ) {
			try {
				s.quit // quitAndWait
			}
			catch { case e1: IOException => printError( "stop", e1 )}
          }
      })
	}

	// @synchronization	must be called in the event thread!
	private def dispose {
//		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException();

//		grpLimiter = null;

//		try {
//			if( (grpMaster != null) && (server != null) && server.isRunning() ) grpMaster.free();
//		}
//		catch( IOException e1 ) {
//			printError( "dispose", e1 );
//		}
//		grpMaster = null;

//		if( nw != null ) {
//			nw.dispose();
//			nw		= null;
//		}

		server.foreach( s => {
            s.removeListener( dispatch( _ ))
//          s.dispose
        })
        if( server != None ) {
           server = None
           dispatch( ServerChanged( server ))
        }
		serverIsReady = false
	}

  	private def getResourceString( key: String ) = app.getResourceString( key )

    def boot : Boolean = {

//		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException();

        server.foreach( s => {
            if( s.isRunning || s.isBooting ) return false
        })

		dispose

//		final String			abCfgID		= audioPrefs.get( PrefsUtil.KEY_AUDIOBOX, AudioBoxConfig.ID_DEFAULT );
//		final AudioBoxConfig	abCfg		= new AudioBoxConfig( audioPrefs.node( PrefsUtil.NODE_AUDIOBOXES ).node( abCfgID ));

		val pRate = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_AUDIORATE, null )
		if( pRate != null ) so.sampleRate.value = pRate.`val`.toInt
//		so.setNumInputBusChannels( abCfg.numInputChannels )
//		so.setNumOutputBusChannels( abCfg.numOutputChannels )
//		val pBusses = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_AUDIOBUSSES, null )
//		if( pBusses != null ) so.setNumAudioBusChannels( max( abCfg.numInputChannels + abCfg.numOutputChannels, (int) p.val ));
		val pMemSize = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCMEMSIZE, null )
		if( pMemSize != null ) so.memSize.value = pMemSize.`val`.toInt << 10
		val pBlockSize = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCBLOCKSIZE, null )
		if( pBlockSize != null ) so.blockSize.value = pBlockSize.`val`.toInt
//		if( !abCfg.name.equals( "Default" )) so.setDevice( abCfg.name );
		so.loadSynthDefs.value = false
		so.zeroConf.value = audioPrefs.getBoolean( PrefsUtil.KEY_SCZEROCONF, true )

		val pPort = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCPORT, null )
		var serverPort = if( pPort == null ) DEFAULT_PORT else pPort.`val`.toInt
		val proto = Symbol( audioPrefs.get( PrefsUtil.KEY_SCPROTOCOL, "udp" /* tcp"*/ ))
		so.protocol.value = proto

//		so.setEnv( "SC_JACK_NAME", "Eisenkraut" )

		val appPath = audioPrefs.get( PrefsUtil.KEY_SUPERCOLLIDERAPP, null )
		if( appPath == null ) {
			println( getResourceString( "errSCSynthAppNotFound" ))
			return false
		}
		so.program.value = appPath

		try {
			// check for automatic port assignment
			if( serverPort == 0 ) {
				if( so.protocol == 'tcp ) {
					val ss = new ServerSocket( 0 )
					serverPort = ss.getLocalPort()
					ss.close()
				} else if( so.protocol == 'udp ) {
					val ds = new DatagramSocket()
					serverPort = ds.getLocalPort()
					ds.close()
				} else {
					throw new IllegalArgumentException( "Illegal protocol : " + so.protocol )
				}
			}

			// loopback is sufficient here
			val s = new Server( app.getName, so )
			s.addListener( dispatch( _ ))

//			if( dumpMode != kDumpOff ) dumpOSC( dumpMode )
//			nw	= NodeWatcher.newFrom( server )

            server = Some( s )
            dispatch( ServerChanged( server ))
			s.boot
			true
		}
		catch { case e1: IOException => {
			printError( "boot", e1 )
            false
		}}
    }
}
