/*
 *  EisenkrautClient.scala
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

package de.sciss.kontur.io

import java.io.{ File, IOException }
import java.net.{ InetSocketAddress, SocketAddress }
import java.util.prefs.{ PreferenceChangeListener, PreferenceChangeEvent }
import de.sciss.io.{ Span }
import de.sciss.app.{ AbstractApplication }
import de.sciss.kontur.util.{ PrefsUtil }
import scala.actors.{ Actor, TIMEOUT }
import scala.math._
import de.sciss.osc.{UDP, TCP, OSCClient, OSCMessage}

object EisenkrautClient {
   val verbose = true
   lazy val instance = new EisenkrautClient
}

class EisenkrautClient {
   import EisenkrautClient._ 

   private var oscVar: Option[ OSCClient ]   = None
   private var actorVar: Option[ OSCActor ]  = None
   private val prefs                         = AbstractApplication.getApplication.getUserPrefs.node( PrefsUtil.NODE_IO )
   private var queryIDCount                  = 0

   private val prefsListener = new PreferenceChangeListener {
      def preferenceChange( e: PreferenceChangeEvent ) = e.getKey match {
         case PrefsUtil.KEY_EISKOSCPORT     => shutDown
         case PrefsUtil.KEY_EISKOSCPROTOCOL => shutDown
         case _ =>
      }
   }

   private val receiveAction = (msg: OSCMessage, addr: SocketAddress, when: Long ) => {
      actorVar.foreach( _ ! msg )
   }

   // ---- constructor ----
   {
      prefs.addPreferenceChangeListener( prefsListener )
   }

   def dispose {
      prefs.removePreferenceChangeListener( prefsListener )
   }

   def openAudioFile( path: File, cursor: Option[ Long ] = None, selection: Option[ Span ] = None ) {
      spawn {
         send( "/doc", "open", path.getCanonicalPath )
         documentForPath( path ).foreach( addr => {
            send( addr, "activate" )
            var tAddr = addr + "/timeline"
            cursor.foreach( pos => {
               send( tAddr, "position", pos )
            })
            selection.foreach( span => {
               send( tAddr, "select", span.start, span.stop )
            })
         })
      }
   }

   def documentForPath( path: File ) : Option[ String ] = {
      val pathStr = path.getCanonicalPath
//      println( "AQUI 1" )
      query[ Int ]( "/doc", "count" ).map( numDocs => {
         var addrO: Option[ String ] = None
         var i = 0; while( addrO.isEmpty && (i < numDocs) ) {
            val addr = "/doc/index/" + i
            query[ String, Int ]( addr, "file", "id" ).foreach( tup => {
               val (path2, id) = tup
               if( path2 == pathStr ) addrO = Some( "/doc/id/" + id )
            })
         i += 1 }
         addrO
      }) getOrElse None
   }

   private def send( path: String, args: Any* ) {
//      println( "AQUI 5" )
      val a = Actor.self.asInstanceOf[ OSCActor ]
//      println( "AQUI 6 " + a.c )
      val msg = OSCMessage( path, args: _* )
//      println( "AQUI 7 " + msg )
      a.c ! msg // .send( msg )
//      println( "AQUI 8" )
   }

   private def query[ T ]( path: String, property: String ) : Option[ T ] = {
//      println( "AQUI 2" )
      query( path, 4000L, property ).map( seq => {
         seq( 0 ).asInstanceOf[ T ]
      })
   }

   private def query[ A, B ]( path: String, propA: String, propB: String ) : Option[ Tuple2[ A, B ]] = {
      query( path, 4000L, propA, propB ).map( seq => {
         Tuple2( seq( 0 ).asInstanceOf[ A ], seq( 1 ).asInstanceOf[ B ])
      })
   }

   private def query( path: String, timeOut: Long, properties: String* ) : Option[ Seq[ Any ]] = {
       val queryID = queryIDCount
       queryIDCount += 1
       val args = List( "query", queryID ) ::: properties.toList
//      println( "AQUI 3 " + args )
       send( path, args: _* )
//      println( "AQUI 4" )
       var result: Option[ Seq[ Any ]] = None
       var keepGoing = true
       val t1 = System.currentTimeMillis
       while( keepGoing ) {
          val t2 = System.currentTimeMillis
          result = Actor.receiveWithin[ Option[ Seq[ Any ]]]( max( 0L, timeOut - (t2 - t1) )) {
             case OSCMessage( "/query.reply", queryID, objects @ _* ) => {
                keepGoing = false; Some( objects )
             }
             case TIMEOUT => { println( "TIMEOUT" ); keepGoing = false; None }
             case _ => None // ignore other messages
          }
       }
       result
    }

   private def spawn( body: => Unit ) {
      if( actorVar.isDefined ) {
         println( "ACTOR BUSY!" )
         return
      }
      osc.foreach( c => {
         val a = new OSCActor( c, body )
         actorVar = Some( a )
         a.start
      })
   }

   private def shutDown {
      oscVar.foreach( c => {
         c.dispose
         oscVar = None
         actorVar = None // XXX ?
      })
   }

   private def osc: Option[ OSCClient ] = {
      if( oscVar.isEmpty ) {
         // tcp is actual default, but scalaosc does not support it yet
         val proto = prefs.get( PrefsUtil.KEY_EISKOSCPROTOCOL, "udp" ) match {
            case "udp" => UDP
            case "tcp" => TCP
         }
         val port  = prefs.getInt( PrefsUtil.KEY_EISKOSCPORT, 0x4549 )
         try {
            val c = OSCClient( proto )
            try {
               if( verbose ) c.dumpOSC()
               val target = new InetSocketAddress( "127.0.0.1", port )
               c.target = target
               println( "OSC Client talking " + proto.name + " to " + target.getHostName + ":" + target.getPort )
               c.start
               c.action = receiveAction
            } catch { case e: IOException => { c.dispose; throw e }}
            oscVar = Some( c )
         }
         catch { case e: IOException => e.printStackTrace }
      }
      oscVar
   }

   private class OSCActor( val c: OSCClient, body: => Unit ) extends Actor {
      def act {
         body
         if( actorVar == Some( Actor.self )) actorVar = None
      }
   }
}