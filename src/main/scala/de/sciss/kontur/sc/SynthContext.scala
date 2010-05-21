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
import java.io.{ File, IOException }
import java.net.{ SocketAddress }
import de.sciss.synth._
import osc._
import SC._
import de.sciss.scalaosc.{ OSCBundle, OSCMessage }
import de.sciss.kontur.util.{ Model }
import de.sciss.util.{ Disposable }

/**
 *    @version 0.11, 21-May-10
 */
trait AsyncAction {
   def asyncDone : Unit
}

trait AsyncModel extends AsyncAction {
   private var collWhenOnline  = Queue[ () => Unit ]()
   private var collWhenOffline = Queue[ () => Unit ]()
   private var isOnlineVar = false
   private var wasOnline = false

   def asyncDone { isOnline = true }

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

class RichSynthDef( val synthDef: SynthDef )
extends AsyncModel {
   def play : RichSynth = play()
   def play( args: ControlSetMap* ) : RichSynth =
      SynthContext.current.play( this, args )

   override def whenOffline( thunk: => Unit ) {
      throw new IllegalStateException( "OPERATION NOT YET SUPPORTED" )
   }
}

trait RichNode extends AsyncModel {
   def node: Node

   def free {
      whenOnline {
         SynthContext.current.add( node.freeMsg )
      }
   }

   def set( pairs: ControlSetMap* ) {
      SynthContext.current.add( node.setMsg( pairs: _* ))
   }
}

class RichSynth( val synth: Synth )
extends RichNode {
   def node = synth

   def endsAfter( dur: Double ) : RichSynth = {
      SynthContext.current.endsAfter( this, dur )
      this
   }
}

class RichGroup( val group: Group )
extends RichNode {
   def node = group
}

trait RichBuffer
extends AsyncAction {
   protected var wasOpened = false
   private var collWhenReady = Queue[ () => Unit ]()
   
   def id : Int
   def numChannels : Int

   def free : Unit

   def whenReady( thunk: => Unit ) {
      val fun = () => thunk
      collWhenReady = collWhenReady.enqueue( fun )
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
   def id = buffer.id
   def numChannels = buffer.numChannels
   
   protected def protRead( path: String, offsetI: Int, numFrames: Int, bufStartFrame: Int,
                           leaveOpen: Boolean ) {
      SynthContext.current.addAsync(
         buffer.readMsg( path, offsetI, numFrames, bufStartFrame, leaveOpen ), this )
   }

   def free {
      if( wasOpened ) {
         SynthContext.current.addAsync( buffer.closeMsg )
         wasOpened = false
      }
      SynthContext.current.addAsync( buffer.freeMsg )
   }
}

class RichMultiBuffer( val buffers: Seq[ Buffer ])
extends RichBuffer {
   def id: Int = buffers.head.id
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
      whenReady {
         buffers.foreach( buf => {
            if( wasOpened ) SynthContext.current.addAsync( buf.closeMsg )
            SynthContext.current.addAsync( buf.freeMsg )
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

   def delayed( tb: Double, delay: Double )( thunk: => Unit ) : Unit =
      current.delayed( tb, delay )( thunk )

   def invalidate( obj: AnyRef ) {
      current.dispatch( Invalidation( obj ))
   }

   case class Invalidation( obj: AnyRef )
}

abstract class SynthContext( val server: Server, val realtime: Boolean )
extends Model with Disposable {
   private var defMap      = Map[ List[ Any ], RichSynthDef ]()
   private var uniqueID    = 0
   protected var bundle: AbstractBundle = null
   private var currentTarget: RichGroup = new RichGroup( server.defaultGroup ) // XXX make online

   def timebase : Double
   def timebase_=( newVal: Double ) : Unit

   protected def initBundle( delta: Double ) : Unit

   def perform( thunk: => Unit ) {
      perform( thunk, -1 )
   }

   def delayed( tb: Double, delay: Double )( thunk: => Unit ) {
      perform( thunk, (tb - timebase) + delay )
   }
   
   private def perform( thunk: => Unit, time: Double ) {
      val savedContext  = SynthContext.current
      val savedBundle   = bundle
      try {
         initBundle( time )
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
      try {
         bundle.send // sendBundle( bundle )
      }
      catch { case e: IOException => e.printStackTrace }
      finally {
         bundle = null
      }
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
      val buf  = Buffer( server )
      val rb   = new RichSingleBuffer( buf )
      bundle.addAsync( buf.allocMsg( numFrames, numChannels ))
      bundle.addAsync( buf.zeroMsg )
      rb
   }

   def cue( obj: { def numChannels: Int; def path: File }, fileStartFrame: Long, bufSize: Int ) : RichBuffer = {
      val buf  = Buffer( server )
      val rb   = new RichSingleBuffer( buf )
      bundle.addAsync( buf.allocMsg( bufSize, obj.numChannels ))
      rb.read( obj.path, fileStartFrame, leaveOpen = true )
      rb
   }

   def emptyMultiBuffer( numFrames: Int, numChannels: Int ) : RichBuffer = {
      val id    = server.buffers.alloc( numChannels )
      val bufs  = (0 until numChannels).map( ch => Buffer( server, id + ch ))
      val rb = new RichMultiBuffer( bufs )
      bufs.foreach( buf => {
         bundle.addAsync( buf.allocMsg( numFrames ))
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
//         val ugenFunc = () => ugenThunk
         val sd  = SynthDef( name )( ugenThunk )
         val rsd = new RichSynthDef( sd )
         defMap += defList -> rsd
         bundle.addAsync( sd.recvMsg, rsd )
         rsd
      }
   }

   def play( rsd: RichSynthDef, args: Seq[ ControlSetMap ]) : RichSynth = {
      val synth = Synth( server )
      val rs    = new RichSynth( synth )
      val tgt   = currentTarget  // freeze
      rsd.whenOnline {
         bundle.add( synth.newMsg( rsd.synthDef.name, tgt.node, args ))
         rs.node.onGo  { perform { rs.isOnline = true }}
         rs.node.onEnd { perform { rs.isOnline = false }}
      }
      rs
   }

   def addAsync( async: AsyncAction ) {
      bundle.addAsync( async )
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

   def endsAfter( rn: RichNode, dur: Double ) {}

   trait AbstractBundle {
      protected var msgs     = Queue[ OSCMessage ]()
      protected var asyncs   = Queue[ AsyncAction ]()
      private var hasAsyncVar = false

      @throws( classOf[ IOException ])
      def send: Unit

      def add( msg: OSCMessage ) {
         msgs = msgs.enqueue( msg )
      }

      def addAsync( msg: OSCMessage ) {
         hasAsyncVar = true
         add( msg )
      }

      def addAsync( msg: OSCMessage, async: AsyncAction ) {
         addAsync( async )
         add( msg )
      }

      def addAsync( async: AsyncAction ) {
          hasAsyncVar = true
          asyncs   = asyncs.enqueue( async )
       }

      def hasAsync = hasAsyncVar

      def messages: Seq[ OSCMessage ] = msgs

      def doAsync {
         asyncs.foreach( _.asyncDone )
      }
   }
}

class RealtimeSynthContext( s: Server )
extends SynthContext( s, true ) {
   private val resp = OSCResponder({
      case OSCSyncedMessage( id ) => syncWait.get( id ).foreach( bundle => {
         syncWait -= id
         perform { bundle.doAsync } // we could use 'delayed', but we might want bundle 'immediate' times
      })
   }, server )
   private var timebaseSysRef = System.currentTimeMillis
   private var timebaseVar = 0.0
   private var syncWait    = Map[ Int, Bundle ]()
   private var bundleCount = 0

   // ---- constructor ----
   {
      resp.add
   }

   def dispose {
      resp.remove
   }

   def timebase : Double = {
      timebaseVar
   }

   def timebase_=( newVal: Double ) {
      if( newVal == 0.0 ) timebaseSysRef = System.currentTimeMillis
      timebaseVar = newVal
   }

   protected def initBundle( delta: Double ) {
      bundle = new Bundle( bundleCount, if( delta < 0 ) 0L else timebaseSysRef + ((timebase + delta) * 1000 + 0.5).toLong )
      bundleCount += 1
   }

   private class Bundle( count: Int, ref: Long )
   extends AbstractBundle {
      @throws( classOf[ IOException ])
      def send {
         val cpy = if( hasAsync ) {
            syncWait += count -> this
            msgs.enqueue( new OSCSyncMessage( count ))
         } else {
            msgs
         }
         if( cpy.nonEmpty ) {
            val bndl = if( ref == 0L ) OSCBundle( cpy: _* ) else OSCBundle.millis( ref, cpy: _* )
            server ! bndl
         }
      }
   }
}