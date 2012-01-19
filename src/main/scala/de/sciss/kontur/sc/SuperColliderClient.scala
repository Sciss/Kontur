/*
 *  SuperColliderClient.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2011 Hanns Holger Rutz. All rights reserved.
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
import de.sciss.kontur.session.Session
import de.sciss.kontur.util.PrefsUtil
import de.sciss.util.Param
import java.awt.EventQueue
import java.io.IOException
import java.net.{ DatagramSocket, ServerSocket }
import de.sciss.osc
import de.sciss.synth.{ServerConnection, Model, Server}

object SuperColliderClient {
   lazy val instance = new SuperColliderClient
   val DEFAULT_PORT = 57108

//   case class ServerChanged( s: Option[ Server ])
   case class ServerBooting( s: ServerConnection )
   case class ServerRunning( s: Server )
   case object ServerTerminated
}

class SuperColliderClient extends Model {
   import SuperColliderClient._

   private val app            = AbstractApplication.getApplication
   private val audioPrefs     = app.getUserPrefs.node( PrefsUtil.NODE_AUDIO )
   private val so             = Server.Config()
   private var bootingVar     = Option.empty[ ServerConnection ]
	 private var serverVar      = Option.empty[ Server ]
   private var serverIsReady  = false
   private var shouldReboot   = false
   private var dumpMode       = osc.Dump.Off: osc.Dump

   private var players = Map[ Session, SuperColliderPlayer ]()
   private val forward: Model.Listener = { case msg => defer( dispatch( msg ))}

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
                     player.dispose
                  }
                  case _ =>
               }
            }
            
        	def documentFocussed( e: DocumentEvent ) {}
        })
    }

   override def toString = "SuperColliderClient"

   def getPlayer( doc: Session ) : Option[ SuperColliderPlayer ] = players.get( doc )

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
                  s.quit
               }
               bootingVar.foreach { booting =>
                  booting.abort
               }
            }
         })
      }
	}

	def reboot() {
		shouldReboot = true
		stop()
	}

    def serverCondition : AnyRef = {
      serverVar.map( _.condition ) getOrElse Server.Offline
    }

    def server = serverVar

    private def printError( name: String, t: Throwable ) {
		System.err.println( name + " : " + t.getClass.getName + " : " + t.getLocalizedMessage )
	}

	def stop() {
      bootingVar.foreach( _.abort )
      serverVar.foreach( s => try { s.quit } catch { case e1: IOException => printError( "stop", e1 )})
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

		  val pRate = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_AUDIORATE, null )
		  if( pRate != null ) so.sampleRate = pRate.`val`.toInt
      so.memorySize = 64 << 10

		  val pBlockSize = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCBLOCKSIZE, null )
		  if( pBlockSize != null ) so.blockSize = pBlockSize.`val`.toInt
   		so.loadSynthDefs  = false
	 	  so.zeroConf       = audioPrefs.getBoolean( PrefsUtil.KEY_SCZEROCONF, true )

		  val pPort = Param.fromPrefs( audioPrefs, PrefsUtil.KEY_SCPORT, null )
		  var serverPort = if( pPort == null ) DEFAULT_PORT else pPort.`val`.toInt
		  val proto = audioPrefs.get( PrefsUtil.KEY_SCPROTOCOL, "udp" ) match {
         case "udp" => osc.UDP
         case "tcp" => osc.TCP
      }
		  so.transport = proto

		  val appPath = audioPrefs.get( PrefsUtil.KEY_SUPERCOLLIDERAPP, null )
		  if( appPath == null ) {
			   println( getResourceString( "errSCSynthAppNotFound" ))
			   return false
		  }
		  so.programPath = appPath

		  try {
			   // check for automatic port assignment
			   if( serverPort == 0 ) {
				    if( so.transport == osc.TCP ) {
					     val ss = new ServerSocket( 0 )
					     serverPort = ss.getLocalPort
					     ss.close()
				    } else if( so.transport == osc.UDP ) {
					     val ds = new DatagramSocket()
					     serverPort = ds.getLocalPort
					     ds.close()
				    } else {
					     throw new IllegalArgumentException( "Illegal protocol : " + so.transport )
				    }
			   }

         val b = Server.boot( app.getName, so.build ) {
            case ServerConnection.Aborted =>
defer {
//println( "---ABORTED" )
               bootingVar = None
}
            case ServerConnection.Running( s ) =>
defer {
//println( "---RUNNING" )
               bootingVar = None
               serverVar = Some( s )
               s.addListener( forward )
               s.dumpOSC( dumpMode )
//               dispatch( ServerChanged( serverVar ))
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

   override protected def dispatch( change: AnyRef ) {
//println( "DISPATCH >>>>>>> " + change )
      super.dispatch( change )
//println( "DISPATCH <<<<<<< " + change )
   }
}