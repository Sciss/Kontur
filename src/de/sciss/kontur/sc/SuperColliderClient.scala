/*
 *  SuperColliderClient.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.sc

import de.sciss.app.{ AbstractApplication, DocumentEvent, DocumentListener }
import de.sciss.kontur.session.{ Session }
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
	private var serverVar: Option[ Server ] = None
    private var serverIsReady = false
    private var shouldReboot = false

    private var players = Map[ Session, SuperColliderPlayer ]()

    // ---- constructor ----
    {
		val userPrefs	= app.getUserPrefs
		
//		oCfgListener = new PreferenceChangeListener() {
//			public void preferenceChange( PreferenceChangeEvent e )
//			{
//				outputConfigChanged();
//			}
//		};
      
        app.getDocumentHandler().addDocumentListener( new DocumentListener {
        	def documentAdded( e: DocumentEvent ) {
               e.getDocument match {
                  case doc: Session => {
                     players += doc -> new SuperColliderPlayer( SuperColliderClient.this, doc )
                  }
                  case _ =>
               }
            }

        	def documentRemoved( e: DocumentEvent ) {
               e.getDocument match {
                  case doc: Session => {
                     val player = players( doc )
                     players -= doc
                     player.dispose
                  }
                  case _ =>
               }
            }
            
        	def documentFocussed( e: DocumentEvent ) {}
        })
    }

	def quit {
//		Server.quitAll
        serverVar.foreach( _.quit )
	}

	def reboot {
		shouldReboot = true
		stop
	}

    def serverCondition : AnyRef = {
      serverVar.map( _.condition ) getOrElse Server.Offline
    }

    def server = serverVar

    private def printError( name: String, t: Throwable ) {
		System.err.println( name + " : " + t.getClass().getName() + " : " + t.getLocalizedMessage() )
	}

	def stop {
        serverVar.foreach( s => {
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

		serverVar.foreach( s => {
            s.removeListener( dispatch( _ ))
//          s.dispose
        })
        if( serverVar != None ) {
           serverVar = None
           dispatch( ServerChanged( serverVar ))
        }
		serverIsReady = false
	}

  	private def getResourceString( key: String ) = app.getResourceString( key )

    def boot : Boolean = {

//		if( !EventQueue.isDispatchThread() ) throw new IllegalMonitorStateException();

        serverVar.foreach( s => {
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

            serverVar = Some( s )
            dispatch( ServerChanged( serverVar ))
			s.boot
			true
		}
		catch { case e1: IOException => {
			printError( "boot", e1 )
            false
		}}
    }
}
