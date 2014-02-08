/*
 *  SuperColliderClient.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
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
 */

package de.sciss.kontur
package sc

import de.sciss.app.{ AbstractApplication, DocumentEvent, DocumentListener }
import session.Session
import util.{Model, PrefsUtil}
import de.sciss.util.Param
import java.awt.EventQueue
import java.io.IOException
import de.sciss.{synth, osc}
import synth._

object SuperColliderClient {
   lazy val instance = new SuperColliderClient
//   val DEFAULT_PORT = 57108

//   case class ServerChanged( s: Option[ Server ])
   case class ServerBooting( s: ServerConnection )
   case class ServerRunning( s: Server )
   case object ServerTerminated
}

class SuperColliderClient extends Model {
   import SuperColliderClient._

   private final val VERBOSE  = false

   private val app            = AbstractApplication.getApplication
   private val audioPrefs     = app.getUserPrefs.node( PrefsUtil.NODE_AUDIO )
   private var bootingVar     = Option.empty[ ServerConnection ]
   private var serverVar      = Option.empty[ Server ]
   private var serverIsReady  = false
   private var shouldReboot   = false
   private var dumpMode : osc.Dump = if( VERBOSE ) osc.Dump.Text else osc.Dump.Off

   private var players = Map[ Session, SuperColliderPlayer ]()
   private val forward: Model.Listener = { case msg => defer( dispatch( msg ))}

   private var volumeVar      = 1.0f
   private var volumeBus      = Option.empty[ ControlBus ]
   private var limiterVar     = false
   private var limiterBus     = Option.empty[ ControlBus ]

    // ---- constructor ----
    {
//		val userPrefs	= app.getUserPrefs
		
//		oCfgListener = new PreferenceChangeListener() {
//			public void preferenceChange( PreferenceChangeEvent e )
//			{
//				outputConfigChanged();
//			}
//		};
      
      app.getDocumentHandler.addDocumentListener( new DocumentListener {
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
                     player.dispose()
                  }
                  case _ =>
               }
            }
            
        	def documentFocussed( e: DocumentEvent ) {}
      })
   }

   override def toString = "SuperColliderClient"

   def getPlayer( doc: Session ) : Option[ SuperColliderPlayer ] = players.get( doc )

   def volume: Float = volumeVar
   def volume_=( value: Float ) {
      if( volumeVar != value ) {
         volumeVar = value
         import Ops._
         volumeBus.foreach( _.set( value ))
      }
   }

   def limiter: Boolean = limiterVar
   def limiter_=( onOff: Boolean ) {
      if( limiterVar != onOff ) {
         limiterVar = onOff
         import Ops._
         limiterBus.foreach( _.set( if( onOff ) 1f else 0f ))
      }
   }

   private def defer( thunk: => Unit ) {
      EventQueue.invokeLater( new Runnable { def run() { thunk }})
   }

   def dumpOSC( mode: osc.Dump ) {
      if( mode != dumpMode ) {
         dumpMode = mode
         serverVar.foreach( _.dumpOSC( mode ))
      }
   }

	def quit() {
//		Server.quitAll
//        serverVar.foreach( _.quit )
      serverVar.foreach { s =>
         Runtime.getRuntime.addShutdownHook( new Thread {
            override def run() {
               if( s.condition != Server.Offline ) {
                  s.quit()
               }
               bootingVar.foreach { booting =>
                  booting.abort()
               }
            }
         })
      }
	}

	def reboot() {
		shouldReboot = true
		stop()
	}

   /**
    * Queries the current state of the SuperCollider server.
    *
    * @return  the current condition of the server, either of `ServerConnectino.Running` or `Server.Offline`
    */
    def serverCondition : AnyRef = {
      serverVar.map( s => s.condition ).getOrElse( Server.Offline )
    }

    def server = serverVar

    private def printError( name: String, t: Throwable ) {
		System.err.println( name + " : " + t.getClass.getName + " : " + t.getLocalizedMessage )
	}

	def stop() {
      bootingVar.foreach( _.abort() )
      serverVar.foreach( s => try { s.quit() } catch { case e1: IOException => printError( "stop", e1 )})
	}

	// @synchronization	must be called in the event thread!
	private def dispose() {
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

      volumeBus   = None
      limiterBus  = None

		serverVar.foreach( s => {
            s.removeListener( forward )
//          s.dispose
        })
        if( serverVar != None ) {
           serverVar = None
           dispatch( ServerTerminated )
        }
		serverIsReady = false
	}

   private def getResourceString( key: String ) = app.getResourceString( key )

   def boot() : Boolean = {
      if( bootingVar.isDefined || serverVar.map( _.isRunning ) == Some( true )) return false

		dispose()

      val so = Server.Config()

		val pRate = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_AUDIORATE, null )
		if( pRate != null ) so.sampleRate = pRate.`val`.toInt
      so.memorySize = 64 << 10

      val pAudioBuses = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_AUDIOBUSSES, null )
      if( pAudioBuses != null ) {
         so.audioBusChannels = pAudioBuses.`val`.toInt
      } else {
         so.audioBusChannels = 512  // XXX hack around the missing prefs GUI
      }

		val pBlockSize = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCBLOCKSIZE, null )
		if( pBlockSize != null ) so.blockSize = pBlockSize.`val`.toInt
   	so.loadSynthDefs  = false
	 	so.zeroConf       = audioPrefs.getBoolean( PrefsUtil.KEY_SCZEROCONF, false )

		val pPort = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCPORT, null )
		val serverPort = if( pPort == null ) 0 else pPort.`val`.toInt
      so.port = serverPort
		val proto = audioPrefs.get( PrefsUtil.KEY_SCPROTOCOL, "tcp" ) match {
         case "udp" => osc.UDP
         case "tcp" => osc.TCP
      }
		so.transport = proto
      if( serverPort == 0 ) so.pickPort()

		val appPath = audioPrefs.get( PrefsUtil.KEY_SUPERCOLLIDERAPP, null )
		if( appPath == null ) {
		   println( getResourceString( "errSCSynthAppNotFound" ))
			return false
		}
		so.programPath = appPath

if( VERBOSE ) {
println( ":: memorySize        = " + so.memorySize )
println( ":: audioBusChannels  = " + so.audioBusChannels )
println( ":: blockSize         = " + so.blockSize )
println( ":: zeroConf          = " + so.zeroConf )
println( ":: port              = " + so.port )
println( ":: transport         = " + so.transport )
println( ":: programPath       = " + so.programPath )
}

		try {
//		   // check for automatic port assignment
//			if( serverPort == 0 ) {
//			   if( so.transport == osc.TCP ) {
//				   val ss = new ServerSocket( 0 )
//					serverPort = ss.getLocalPort
//					ss.close()
//            } else if( so.transport == osc.UDP ) {
//				   val ds = new DatagramSocket()
//					serverPort = ds.getLocalPort
//					ds.close()
//		      } else {
//               throw new IllegalArgumentException( "Illegal protocol : " + so.transport )
//            }
//         }
//
         val b = Server.boot( app.getName, so ) {
            case ServerConnection.Aborted =>
               defer {
                  bootingVar = None
               }
            case ServerConnection.Running( s ) =>
               defer {
                  bootingVar = None
                  serverVar = Some( s )
                  s.addListener( forward )
                  s.dumpOSC( dumpMode )
                  initMaster( s )
                  dispatch( ServerRunning( s ))
               }
         }
         bootingVar = Some( b )
         dispatch( ServerBooting( b ))
			true
	   }
		catch { case e1: IOException =>
		   printError( "boot", e1 )
         false
      }
   }

   private def initMaster( s: Server ) {
      import synth._
      import synth.ugen._

      val volBus  = Bus.control( s, 1 )
      val limBus  = Bus.control( s, 1 )
      import Ops._
      volBus.set( volumeVar )
      limBus.set( if( limiterVar ) 1f else 0f )

      val df = SynthDef( "kontur-master" ) {
         val vol     = LagIn.kr( volBus.index )
         val limPos  = LagIn.kr( limBus.index ) * 2 - 1
         val in      = In.ar( 0, s.config.outputBusChannels ) * vol
         val lim     = Limiter.ar( in, -0.2.ampdb, 0.1 )
         val sig     = LinXFade2.ar( in, lim, limPos )
         ReplaceOut.ar( 0, sig )
      }
      df.play( s, addAction = addToTail )

      volumeBus   = Some( volBus )
      limiterBus  = Some( limBus )
   }

//   override protected def dispatch( change: AnyRef ) {
////println( "DISPATCH >>>>>>> " + change )
//      super.dispatch( change )
////println( "DISPATCH <<<<<<< " + change )
//   }
}