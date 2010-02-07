/*
 *  SynthContext.scala
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

import scala.collection.immutable.{ Queue }
import scala.collection.mutable.{ ListBuffer }
import java.io.{ File }
import java.net.{ SocketAddress }
import de.sciss.tint.sc.{ Buffer, Bus, GE, Group, OSCResponderNode, OSCSyncMessage, OSCSyncedMessage, SC, Server, Synth,
   SynthDef }
import SC._
import de.sciss.scalaosc.{ OSCMessage }
import de.sciss.kontur.util.{ Model }

trait AsyncModel {
   private var collWhenOnline  = Queue[ () => Unit ]()
   private var collWhenOffline = Queue[ () => Unit ]()
   private var isOnlineVar = false
   private var wasOnline = false

   def isOnline: Boolean = isOnlineVar
   def isOnline_=( newState: Boolean ) {
      if( newState != isOnlineVar ) {
         isOnlineVar = newState
         if( newState ) {
            wasOnline = true
            val cpy = collWhenOnline
            collWhenOnline = Queue[ () => Unit ]()
            cpy.foreach( _.apply )
         } else {
            val cpy = collWhenOffline
            collWhenOffline = Queue[ () => Unit ]()
            cpy.foreach( _.apply )
         }
      }
   }

   def whenOnline( thunk: => Unit ) {
      val fun = () => thunk
      if( isOnline ) {
         thunk
      } else {
         collWhenOnline = collWhenOnline.enqueue( fun )
      }
   }

   def whenOffline( thunk: => Unit ) {
      val fun = () => thunk
      if( !wasOnline || isOnline ) {
         collWhenOffline = collWhenOffline.enqueue( fun )
      } else {
         thunk
      }
   }
}

class RichSynthDef( val synthDef: SynthDef, val bundleCount: Int )
extends AsyncModel {
   def play : RichSynth = play()
   def play( args: Tuple2[ String, Float ]* ) : RichSynth =
      SynthContext.current.play( this, args )

   override def whenOffline( thunk: => Unit ) {
      throw new IllegalStateException( "OPERATION NOT YET SUPPORTED" )
   }
}

class RichSynth( val synth: Synth )
extends AsyncModel {
   synth.onEnd { isOnline = false }

   def free {
      SynthContext.current.free( this )
   }
}

trait RichBuffer
extends AsyncModel {
   def bufNum : Int
   def numChannels : Int
   def read( path: File, fileStartFrame: Int = 0, numFrames: Int = -1, bufStartFrame: Int = 0,
          leaveOpen: Boolean = false ) : RichBuffer
   def free : Unit

   override def whenOffline( thunk: => Unit ) {
      throw new IllegalStateException( "OPERATION NOT YET SUPPORTED" )
   }
}

class RichSingleBuffer( val buffer: Buffer )
extends RichBuffer {
   def bufNum = buffer.bufNum
   def numChannels = buffer.numChannels
   
   def read( path: File, fileStartFrame: Int = 0, numFrames: Int = -1, bufStartFrame: Int = 0,
             leaveOpen: Boolean = false ) : RichBuffer = {
      SynthContext.current.addAsync(
         buffer.readMsg( path.getCanonicalPath, fileStartFrame, numFrames, bufStartFrame, leaveOpen ))
      this
   }

   def free {
      val context = SynthContext.current
      whenOnline {
         context.addAsync( buffer.freeMsg )
      }
   }
}

class RichMultiBuffer( val buffers: Seq[ Buffer ])
extends RichBuffer {
   def bufNum: Int = buffers.head.bufNum
   val numChannels = buffers.size
   def read( path: File, fileStartFrame: Int = 0, numFrames: Int = -1, bufStartFrame: Int = 0,
             leaveOpen: Boolean = false ) : RichBuffer = {

      val context = SynthContext.current
      var ch = 0; buffers.foreach( buf => {
         context.addAsync(
            buf.readChannelMsg( path.getCanonicalPath, fileStartFrame, numFrames, bufStartFrame, leaveOpen, List( ch )))
      ch += 1 })
      this
   }

   def free {
      val context = SynthContext.current
      whenOnline {
         buffers.foreach( buf => {
            context.addAsync( buf.freeMsg )
         })
      }
   }
}

class RichBus( val bus: Bus ) {
   def free {
      bus.free // XXX
   }

   def index: Int = bus.index
}

object SynthContext {
   private[sc] var current: SynthContext = null

   def inGroup( g: Group)( thunk: => Unit ) {
      current.inGroup( g )( thunk )
   }

   def graph( definingParams: Any* )( ugenThunk: => GE ) : RichSynthDef =
      current.graph( definingParams :_* )( ugenThunk )

   def audioBus( numChannels: Int ) : RichBus =
      current.audioBus( numChannels )

   def emptyBuffer( numFrames: Int, numChannels: Int = 1 ) : RichBuffer =
      current.emptyBuffer( numFrames, numChannels )

   def emptyMultiBuffer( numFrames: Int, numChannels: Int ) : RichBuffer =
      current.emptyMultiBuffer( numFrames, numChannels )

   def invalidate( obj: AnyRef ) {
      current.dispatch( Invalidation( obj ))
   }

   case class Invalidation( obj: AnyRef )
}

class SynthContext( val server: Server ) extends Model {
   private var defMap      = Map[ List[ Any ], RichSynthDef ]()
   private var syncWait    = Map[ Int, Bundle ]()
   private var uniqueID    = 0
   private var bundleCount = 0
   private var bundle: Bundle = null
   private var currentTarget: Group = server.defaultGroup

   private val resp        = new OSCResponderNode( server, "/synced", syncedAction )

   def syncedAction( msg: OSCMessage, addr: SocketAddress, when: Long ) = msg match {
      case sm: OSCSyncedMessage => syncWait.get( sm.id ).foreach( bundle => {
//println( "synced : " + sm.id )
         syncWait -= sm.id
         perform { bundle.doAsync }
      })
      case _ =>
   }

   // ---- constructor ----
   {
      resp.add
   }

   def dispose {
      resp.remove
   }

   private def initBundle {
      bundle = new Bundle( bundleCount )
      bundleCount += 1
   }

   def perform( thunk: => Unit ) {
      val saved = SynthContext.current
      initBundle
      try {
         SynthContext.current = this
         thunk
      }
      finally {
         SynthContext.current = saved
         bundle.send( server )
      }
   }

   def audioBus( numChannels: Int ) =
      new RichBus( Bus.audio( server, numChannels ))

   def emptyBuffer( numFrames: Int, numChannels: Int ) : RichBuffer = {
      val buf  = new Buffer( server, numFrames, numChannels )
      val rb   = new RichSingleBuffer( buf )
      bundle.addAsync( buf.allocMsg )
      bundle.addAsync( buf.zeroMsg )
      rb
   }

   def emptyMultiBuffer( numFrames: Int, numChannels: Int ) : RichBuffer = {
      val bufNum = server.getBufferAllocator.alloc( numChannels )
      val bufs   = (0 until numChannels).map( ch => new Buffer( server, numFrames, 1, bufNum + ch ))
      val rb = new RichMultiBuffer( bufs )
      bufs.foreach( buf => {
         bundle.addAsync( buf.allocMsg )
         bundle.addAsync( buf.zeroMsg )
      })
      rb
   }

   def inGroup( g: Group)( thunk: => Unit ) {
      val saved = currentTarget
      try {
         currentTarget = g
         thunk
      }
      finally {
         currentTarget = saved
      }
   }

   def graph( definingParams: Any* )( ugenThunk: => GE ) : RichSynthDef = {
      val defList = definingParams.toList
      defMap.get( defList ) getOrElse {
         val name = (if( definingParams.nonEmpty ) definingParams else List[ Any ]( new AnyRef )).foldLeft( "" )(
            (n, elem) => n + "_" + (elem match {
               case d: Double    => d.toString
               case f: Float     => f.toString
               case l: Long      => l.toString
               case i: Int       => i.toString
               case b: Boolean   => if( b ) "1" else "0"
               case s: String    => s
               case _ => { uniqueID += 1; uniqueID.toString } // XXX sync
         })).substring( 1 )
         val ugenFunc = () => ugenThunk
         val sd  = new SynthDef( name )( ugenFunc )
         val rsd = new RichSynthDef( sd, bundleCount )
         defMap += defList -> rsd
         bundle.addAsync( sd.recvMsg, rsd )
         rsd
      }
   }

   def play( rsd: RichSynthDef, args: Seq[ Tuple2[ String, Float ]]) : RichSynth = {
      val synth = new Synth( rsd.synthDef.name, server )
      val rs    = new RichSynth( synth )
      val tgt   = currentTarget  // freeze
      rsd.whenOnline {
         bundle.add( synth.newMsg( tgt, args ))
      }
      rs
   }

   def free( rs: RichSynth ) {
      rs.whenOnline {
         bundle.add( rs.synth.freeMsg )
      }
   }

   def addAsync( msg: OSCMessage ) {
      bundle.addAsync( msg )
   }

   class Bundle( count: Int ) {
      private var msgs     = Queue[ OSCMessage ]()
      private var asyncs   = Queue[ AsyncModel ]()
      private var hasAsync = false

      def add( msg: OSCMessage ) {
         msgs = msgs.enqueue( msg )
      }

      def addAsync( msg: OSCMessage ) {
         hasAsync = true
         add( msg )
      }

      def addAsync( msg: OSCMessage, async: AsyncModel ) {
         hasAsync = true
         asyncs   = asyncs.enqueue( async )
         add( msg )
      }

      def send( server: Server ) {
         val cpy = if( hasAsync ) {
//println( "sync to " + count )
            syncWait += count -> this
            msgs.enqueue( new OSCSyncMessage( count ))
         } else {
            msgs
         }
         if( cpy.nonEmpty ) {
            server.sendBundle( -1, cpy: _* ) // XXX bundle clumping
         }
      }

      def doAsync {
//println( "asyncs : " + asyncs.size )
         asyncs.foreach( _.isOnline = true )
      }
   }
}