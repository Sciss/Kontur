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
import de.sciss.tint.sc._
import SC._
import de.sciss.scalaosc.{ OSCMessage }
import de.sciss.kontur.util.{ Model }

trait AsyncAction {
   def asyncDone : Unit
}

//object FUCK {
//   private var count = 0
//   def next = {
//      val result = count
//      count += 1
//      result
//   }
//}

trait AsyncModel extends AsyncAction {
   private var collWhenOnline  = Queue[ () => Unit ]()
   private var collWhenOffline = Queue[ () => Unit ]()
   private var isOnlineVar = false
   private var wasOnline = false

   def asyncDone { isOnline = true }

//lazy val gaga = FUCK.next
//   println( "id = " + gaga + "; ASYNC created" )

   def isOnline: Boolean = isOnlineVar
   def isOnline_=( newState: Boolean ) {
//println( "id = " + gaga + "; isOnline_( " + newState+ " ) ; old was " + isOnlineVar )
      if( newState != isOnlineVar ) {
         isOnlineVar = newState
         if( newState ) {
//println( "...collWhenOnline.size = " + collWhenOnline.size )
            wasOnline = true
            val cpy = collWhenOnline
            collWhenOnline = Queue[ () => Unit ]()
            cpy.foreach( _.apply )
         } else {
//println( "...collWhenOffline.size = " + collWhenOffline.size )
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

trait RichNode extends AsyncModel {
   def node: Node

   node.onGo  { /* println( "id = " + gaga + " HUHU. ONLINE " + node.id );*/  isOnline = true }
   node.onEnd { /* println( "id = " + gaga + " HUHU. OFFLINE " + node.id );*/ isOnline = false }
//   println( "id = " + gaga + "; RICHNODE created" )

   def free {
      val context = SynthContext.current
      whenOnline {
         context.add( node.freeMsg )
      }
   }
}

class RichSynth( val synth: Synth )
extends RichNode {
   def node = synth
}

class RichGroup( val group: Group )
extends RichNode {
   def node = group
}

trait RichBuffer
extends AsyncAction {
   protected var wasOpened = false
   private var collWhenReady = Queue[ () => Unit ]()
   
   def index : Int
   def numChannels : Int

   def free : Unit

   def whenReady( thunk: => Unit ) {
      val fun = () => thunk
//      if( isOnline ) {
//         thunk
//      } else {
         collWhenReady = collWhenReady.enqueue( fun )
//      }
   }

   def asyncDone {
      val cpy = collWhenReady
      collWhenReady = Queue[ () => Unit ]()
      cpy.foreach( _.apply )
   }

   def read( path: File, fileStartFrame: Long = 0L, numFrames: Int = -1, bufStartFrame: Int = 0,
             leaveOpen: Boolean = false ) : RichBuffer = {
      val offsetI = castOffsetToInt( fileStartFrame )
      protRead( path.getAbsolutePath, offsetI, numFrames, bufStartFrame, leaveOpen )
      wasOpened = leaveOpen
      this
   }

   protected def protRead( path: String, offsetI: Int, numFrames: Int, bufStartFrame: Int, leaveOpen: Boolean ) : Unit

//   override def whenOffline( thunk: => Unit ) {
//      throw new IllegalStateException( "OPERATION NOT YET SUPPORTED" )
//   }

   protected def castOffsetToInt( off: Long ) : Int =
      if( off <= 0xFFFFFFFFL ) {
         off.toInt
      } else {
         println( "WARNING: Buffer.read: file offset exceeds 32bit" )
         0xFFFFFFFF
      }
}

class RichSingleBuffer( val buffer: Buffer )
extends RichBuffer {
   def index = buffer.bufNum
   def numChannels = buffer.numChannels
   
   protected def protRead( path: String, offsetI: Int, numFrames: Int, bufStartFrame: Int,
                           leaveOpen: Boolean ) {
      SynthContext.current.addAsync(
         buffer.readMsg( path, offsetI, numFrames, bufStartFrame, leaveOpen ), this )
   }

   def free {
      val context = SynthContext.current
      whenReady {
         if( wasOpened ) {
            context.addAsync( buffer.closeMsg )
            wasOpened = false
         }
         context.addAsync( buffer.freeMsg )
      }
   }
}

class RichMultiBuffer( val buffers: Seq[ Buffer ])
extends RichBuffer {
   def index: Int = buffers.head.bufNum
   val numChannels = buffers.size

   protected def protRead( path: String, offsetI: Int, numFrames: Int, bufStartFrame: Int, leaveOpen: Boolean ) {
      val context = SynthContext.current
      var ch = 0; buffers.foreach( buf => {
         val msg = buf.readChannelMsg( path, offsetI, numFrames, bufStartFrame, leaveOpen, List( ch ))
         if( ch == 0 ) {
            context.addAsync( msg, this ) // make sure asyncDone is called only once!
         } else {
            context.addAsync( msg )
         }
      ch += 1 })
   }

   def free {
      val context = SynthContext.current
      whenReady {
         buffers.foreach( buf => {
            if( wasOpened ) context.addAsync( buf.closeMsg )
            context.addAsync( buf.freeMsg )
         })
         wasOpened = false
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

   def inGroup( g: RichGroup )( thunk: => Unit ) {
      current.inGroup( g )( thunk )
   }

   def graph( definingParams: Any* )( ugenThunk: => GE ) : RichSynthDef =
      current.graph( definingParams :_* )( ugenThunk )

   def audioBus( numChannels: Int ) : RichBus =
      current.audioBus( numChannels )

   def group() : RichGroup =
      current.group

   def groupAfter( n: RichNode ) : RichGroup =
      current.groupAfter( n )

   def emptyBuffer( numFrames: Int, numChannels: Int = 1 ) : RichBuffer =
      current.emptyBuffer( numFrames, numChannels )

   def emptyMultiBuffer( numFrames: Int, numChannels: Int ) : RichBuffer =
      current.emptyMultiBuffer( numFrames, numChannels )

   def cue( obj: { def numChannels: Int; def path: File }, fileStartFrame: Long = 0L, bufSize: Int = 32768 ) : RichBuffer =
      current.cue( obj, fileStartFrame, bufSize )

   def realtime : Boolean = current.realtime

   def sampleRate : Double = current.sampleRate

   def timebase : Double = current.timebase
   def timebase_=( newVal: Double ) : Unit = current.timebase_=( newVal )

   def delayed( delay: Double )( thunk: => Unit ) : Unit =
      current.delayed( delay )( thunk )

   def invalidate( obj: AnyRef ) {
      current.dispatch( Invalidation( obj ))
   }

   case class Invalidation( obj: AnyRef )
}

class SynthContext( val server: Server, val realtime: Boolean ) extends Model {
   private var defMap      = Map[ List[ Any ], RichSynthDef ]()
   private var syncWait    = Map[ Int, Bundle ]()
   private var uniqueID    = 0
   private var bundleCount = 0
   private var bundle: Bundle = null
   private var currentTarget: RichGroup = new RichGroup( server.defaultGroup ) // XXX make online

   private val resp        = new OSCResponderNode( server, "/synced", syncedAction )

   private var startTime   = System.currentTimeMillis    // XXX eventually we need logical time!

   def timebase : Double = {
      val currentTime = System.currentTimeMillis
      (currentTime - startTime).toDouble / 1000
   }

   def timebase_=( newVal: Double ) {
      val currentTime = System.currentTimeMillis
      startTime = currentTime + (newVal * 1000 + 0.5).toLong
   }

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

   private def initBundle( time: Double = -1 ) {
      bundle = new Bundle( bundleCount, time )
      bundleCount += 1
   }

   def perform( thunk: => Unit ) {
      perform( thunk, -1 )
   }

   def delayed( delay: Double )( thunk: => Unit ) {
      perform( thunk, delay )
   }
   
   private def perform( thunk: => Unit, time: Double ) {
      val savedContext  = SynthContext.current
      val savedBundle   = bundle
      try {
         initBundle()
         SynthContext.current = this
         thunk
         sendBundle
      }
      finally {
         SynthContext.current = savedContext
         bundle = savedBundle
      }
   }

   private def sendBundle {
      bundle.send( server )
      bundle = null
   }

   def sampleRate : Double = server.counts.sampleRate

   def group() : RichGroup = {
      val group = new Group( server )
      val rg    = new RichGroup( group )
      bundle.add( group.newMsg( currentTarget.group, addToHead ))
      rg
   }

   def groupAfter( n: RichNode ) : RichGroup = {
      val group = new Group( server )
      val rg    = new RichGroup( group )
      bundle.add( group.newMsg( n.node, addAfter ))  // XXX should check g.online!
      rg
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

   def cue( obj: { def numChannels: Int; def path: File }, fileStartFrame: Long, bufSize: Int ) : RichBuffer = {
      val buf  = new Buffer( server, bufSize, obj.numChannels )
      val rb   = new RichSingleBuffer( buf )
      bundle.addAsync( buf.allocMsg )
      rb.read( obj.path, fileStartFrame, leaveOpen = true )
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

   def inGroup( g: RichGroup )( thunk: => Unit ) {
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
         bundle.add( synth.newMsg( tgt.node, args ))
      }
      rs
   }

   def addAsync( msg: OSCMessage ) {
      bundle.addAsync( msg )
   }

   def addAsync( msg: OSCMessage, async: AsyncAction ) {
      bundle.addAsync( msg, async )
   }

   def add( msg: OSCMessage ) {
      bundle.add( msg )
   }

   class Bundle( count: Int, val time: Double = -1 ) {
      private var msgs     = Queue[ OSCMessage ]()
      private var asyncs   = Queue[ AsyncAction ]()
      private var hasAsync = false

//println( "NEW BUNDLE : " + time )

      def add( msg: OSCMessage ) {
//println( "....add " + msg )
         msgs = msgs.enqueue( msg )
      }

      def addAsync( msg: OSCMessage ) {
         hasAsync = true
         add( msg )
      }

      def addAsync( msg: OSCMessage, async: AsyncAction ) {
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
            server.sendBundle( time, cpy: _* ) // XXX bundle clumping
         }
      }

      def doAsync {
//println( "asyncs : " + asyncs.size )
         asyncs.foreach( _.asyncDone )
      }
   }
}