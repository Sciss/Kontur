/*
 *  EisenkrautClient.scala
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
package io

import java.io.{File, IOException}
import java.net.InetSocketAddress
import java.util.prefs.{PreferenceChangeListener, PreferenceChangeEvent}
import util.PrefsUtil
import actors.{Actor, TIMEOUT}
import de.sciss.osc
import de.sciss.span.Span
import Span.SpanOrVoid
import desktop.Application

//object EisenkrautClient {
//   val verbose = true
//   lazy val instance = new EisenkrautClient
//}

class EisenkrautClient()(implicit application: Application) {
//  import EisenkrautClient._

  private var oscVar        = Option.empty[osc.Client]
  private var actorVar      = Option.empty[OSCActor]
  private val prefs         = application.userPrefs.node(PrefsUtil.NODE_IO)
  private val prefs1        = prefs[Int]   (PrefsUtil.KEY_EISKOSCPORT    )
  private val prefs2        = prefs[String](PrefsUtil.KEY_EISKOSCPROTOCOL)
  private var queryIDCount  = 0

  private val prefsListener = { _: Option[Any] => shutDown() }

   private val receiveAction = (p: osc.Packet) => p match {
      case m: osc.Message => actorVar.foreach( _ ! m )
      case b: osc.Bundle  => actorVar.foreach { a => b.foreach( a ! _ )}
   }

   // ---- constructor ----
  prefs1.addListener(prefsListener)
  prefs2.addListener(prefsListener)

   def dispose() {
     prefs1.removeListener(prefsListener)
     prefs2.removeListener(prefsListener)
   }

   def openAudioFile( path: File, cursor: Option[ Long ] = None, selection: SpanOrVoid = Span.Void ) {
      spawn {
         send( "/doc", "open", path.getCanonicalPath )
         documentForPath( path ).foreach( addr => {
            send( addr, "activate" )
            val tAddr = addr + "/timeline"
            cursor.foreach( pos => {
               send( tAddr, "position", pos )
            })
           selection match {
             case Span(start, stop) => send( tAddr, "select", start, stop )
             case _ =>
           }
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
      val msg = osc.Message( path, args: _* )
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

   private def query[ A, B ]( path: String, propA: String, propB: String ) : Option[ (A, B) ] = {
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
          result = Actor.receiveWithin[ Option[ Seq[ Any ]]]( math.max( 0L, timeOut - (t2 - t1) )) {
             case osc.Message( "/query.reply", `queryID`, objects @ _* ) => {
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
      oscClient.foreach( c => {
         val a = new OSCActor( c, body )
         actorVar = Some( a )
         a.start()
      })
   }

   private def shutDown() {
      oscVar.foreach( c => {
         c.close()
         oscVar = None
         actorVar = None // XXX ?
      })
   }

   private def oscClient: Option[ osc.Client ] = {
      if( oscVar.isEmpty ) {
         // tcp is actual default, but scalaosc does not support it yet
         val proto = prefs.getOrElse( PrefsUtil.KEY_EISKOSCPROTOCOL, "tcp" ) match {
            case "udp" => osc.UDP
            case "tcp" => osc.TCP
         }
         val port  = prefs.getOrElse( PrefsUtil.KEY_EISKOSCPORT, 0x4549 )
         try {
            val target = new InetSocketAddress( "127.0.0.1", port )
            val c = proto match {
               case osc.UDP =>
                  val cfg = osc.UDP.Config()
                  cfg.codec = osc.PacketCodec().longs()
                  osc.UDP.Client( target, cfg )
               case osc.TCP =>
                  val cfg = osc.TCP.Config()
                  cfg.codec = osc.PacketCodec().longs()
                  osc.TCP.Client( target, cfg )
            }
            var success = false
            try {
//               if( verbose ) c.dump()
               println( "OSC Client talking " + proto.name + " to " + target.getHostName + ":" + target.getPort )
               c.connect()
               c.action = receiveAction
               oscVar = Some( c )
               success = true
            } finally {
               if( !success ) c.close()
            }
         }
         catch {
            case e: IOException => e.printStackTrace()
         }
      }
      oscVar
   }

   private class OSCActor( val c: osc.Client, body: => Unit ) extends Actor {
      def act() {
         body
         if( actorVar == Some( Actor.self )) actorVar = None
      }
   }
}