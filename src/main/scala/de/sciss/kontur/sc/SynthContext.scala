/*
 *  SynthContext.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur
package sc

import collection.immutable.Queue
import java.io.{ File, IOException }
import de.sciss.synth._
import de.sciss.osc
import collection.immutable.{IndexedSeq => IIdxSeq}
import java.awt.EventQueue
import util.Model
import language.reflectiveCalls

trait AsyncAction {
   def asyncDone() : Unit
}

trait AsyncModel extends AsyncAction {
   private var collWhenOnline  = Queue[ () => Unit ]()
   private var collWhenOffline = Queue[ () => Unit ]()
   private var isOnlineVar = false
   private var wasOnline = false

   def asyncDone(): Unit = isOnline = true

   def isOnline: Boolean = isOnlineVar
   def isOnline_=( newState: Boolean ): Unit = {
      if( newState != isOnlineVar ) {
         isOnlineVar = newState
         if( newState ) {
            wasOnline = true
            val cpy = collWhenOnline
            collWhenOnline = Queue[ () => Unit ]()
            cpy.foreach( _.apply() )
         } else {
            val cpy = collWhenOffline
            collWhenOffline = Queue[ () => Unit ]()
            cpy.foreach( _.apply() )
         }
      }
   }

   def whenOnline( thunk: => Unit ): Unit = {
      val fun = () => thunk
      if( isOnline ) {
         thunk
      } else {
         collWhenOnline = collWhenOnline.enqueue( fun )
      }
   }

   def whenOffline( thunk: => Unit ): Unit = {
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

   override def whenOffline( thunk: => Unit ): Unit =
      throw new IllegalStateException( "OPERATION NOT YET SUPPORTED" )
}

trait RichNode extends AsyncModel {
   def node: Node

   def free(): Unit = {
      whenOnline {
         SynthContext.current.add( node.freeMsg )
      }
   }

   def set( pairs: ControlSetMap* ): Unit =
      SynthContext.current.add( node.setMsg( pairs: _* ))
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

   def free() : Unit

   def whenReady( thunk: => Unit ): Unit = {
      val fun = () => thunk
      collWhenReady = collWhenReady.enqueue( fun )
   }

   def asyncDone(): Unit = {
      val cpy = collWhenReady
      collWhenReady = Queue[ () => Unit ]()
      cpy.foreach( _.apply() )
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
                           leaveOpen: Boolean ): Unit =
      SynthContext.current.addAsync(
         buffer.readMsg( path, offsetI, numFrames, bufStartFrame, leaveOpen ), this )

   def free(): Unit = {
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

   protected def protRead( path: String, offsetI: Int, numFrames: Int, bufStartFrame: Int, leaveOpen: Boolean ): Unit = {
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

   def free(): Unit = {
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
   def free(): Unit = bus.free() // XXX

   def index: Int = bus.index
}

object SynthContext {
   private var currentVar : SynthContext = null
   def current: SynthContext = currentVar
//   private def current_=( c: SynthContext ) {
////      println( "SETTING CONTEXT TO " + c )
////      new Throwable().printStackTrace()
////      println( "-------------------------" )
//      currentVar = c
//   }

   def inGroup( g: RichGroup )( thunk: => Unit ): Unit = current.inGroup( g )( thunk )

   def graph( definingParams: Any* )( ugenThunk: => Unit ) : RichSynthDef =
      current.graph( definingParams :_* )( ugenThunk )

   def audioBus( numChannels: Int ) : RichBus =
      current.audioBus( numChannels )

   def group : RichGroup = current.group

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
   def timebase_=( newVal: Double ): Unit = current.timebase_=( newVal )

   def delayed( tb: Double, delay: Double )( thunk: => Unit ): Unit =
      current.delayed( tb, delay )( thunk )

   def invalidate( obj: AnyRef ): Unit =
      current.dispatch( Invalidation( obj ))

   case class Invalidation( obj: AnyRef )
}

abstract class SynthContext( val server: Server, val realtime: Boolean )
extends Model /* with Disposable */ {
   private var defMap      = Map[ List[ Any ], RichSynthDef ]()
   private var uniqueID    = 0
   private var bundleVar: AbstractBundle = null
   private var currentTarget: RichGroup = new RichGroup( server.defaultGroup ) // XXX make online

//   private def bundle : AbstractBundle = bundleVar

   def timebase : Double
   def timebase_=( newVal: Double ) : Unit

   protected def initBundle( delta: Double ) : AbstractBundle

   protected def deferIfNeeded( thunk: => Unit ): Unit =
      if( EventQueue.isDispatchThread ) thunk else EventQueue.invokeLater( new Runnable {
         def run(): Unit = thunk
      })

   def perform( thunk: => Unit ): Unit = perform( thunk, -1, send = true )

   /**
    * Like perform but ignores the messages silently. Use this to clean up after the server shut down.
    */
   def consume( thunk: => Unit ): Unit =
      perform( thunk, -1, send = false )

   def delayed( tb: Double, delay: Double )( thunk: => Unit ): Unit =
      perform( thunk, (tb - timebase) + delay, send = true )

   private def perform( thunk: => Unit, time: Double, send: Boolean ): Unit = {
      require( EventQueue.isDispatchThread, "Invoked 'perform' outside dispatch thread" )
      val savedContext  = SynthContext.current
      val savedBundle   = bundleVar
//println( " context perform >>>>> " + hashCode() )
      try {
         bundleVar = initBundle( time )
         SynthContext.currentVar = this
         thunk
         if( send ) bundleVar.send()
      }
      catch { case e: IOException => e.printStackTrace() }
      finally {
//println( " context perform <<<<< " + hashCode() )
         SynthContext.currentVar = savedContext
         bundleVar = savedBundle
      }
   }

//   private def sendBundle() {
//      try {
//         bundle.send() // sendBundle( bundle )
//      }
//      catch { case e: IOException => e.printStackTrace() }
//      finally {
//         bundle = null
//      }
//   }

   def sampleRate : Double = server.counts.sampleRate

   def group : RichGroup = {
      val group = Group( server )
      val rg    = new RichGroup( group )
      add( group.newMsg( currentTarget.group, addToHead ))
      rg
   }

   def groupAfter( n: RichNode ) : RichGroup = {
      val group = Group( server )
      val rg    = new RichGroup( group )
      add( group.newMsg( n.node, addAfter ))  // XXX should check g.online!
      rg
   }

   def audioBus( numChannels: Int ) =
      new RichBus( Bus.audio( server, numChannels ))

   def emptyBuffer( numFrames: Int, numChannels: Int ) : RichBuffer = {
      val buf  = Buffer( server )
      val rb   = new RichSingleBuffer( buf )
      addAsync( buf.allocMsg( numFrames, numChannels ))
      addAsync( buf.zeroMsg )
      rb
   }

   def cue( obj: { def numChannels: Int; def path: File }, fileStartFrame: Long, bufSize: Int ) : RichBuffer = {
      val buf  = Buffer( server )
      val rb   = new RichSingleBuffer( buf )
      addAsync( buf.allocMsg( bufSize, obj.numChannels ))
      rb.read( obj.path, fileStartFrame, leaveOpen = true )
      rb
   }

   def emptyMultiBuffer( numFrames: Int, numChannels: Int ) : RichBuffer = {
      val id    = server.allocBuffer( numChannels )
      val bufs  = (0 until numChannels).map( ch => Buffer( server, id + ch ))
      val rb = new RichMultiBuffer( bufs )
      bufs.foreach( buf => {
         addAsync( buf.allocMsg( numFrames ))
         addAsync( buf.zeroMsg )
      })
      rb
   }

   def inGroup( g: RichGroup )( thunk: => Unit ): Unit = {
      val saved = currentTarget
      try {
         currentTarget = g
         thunk
      }
      finally {
         currentTarget = saved
      }
   }

   def graph( definingParams: Any* )( ugenThunk: => Unit ) : RichSynthDef = {
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
         addAsync( sd.recvMsg, rsd )
         rsd
      }
   }

   def play( rsd: RichSynthDef, args: Seq[ ControlSetMap ]) : RichSynth = {
      val synth = Synth( server )
      val rs    = new RichSynth( synth )
      val tgt   = currentTarget  // freeze
      rsd.whenOnline {
         add( synth.newMsg( rsd.synthDef.name, tgt.node, args ))
//         rs.node.onGo  { perform { rs.isOnline = true }}
//         rs.node.onEnd { perform { rs.isOnline = false }}
         rs.node.onGo  { deferIfNeeded { perform { rs.isOnline = true }}}
         rs.node.onEnd { deferIfNeeded { perform { rs.isOnline = false }}}
      }
      rs
   }

   def addAsync( async: AsyncAction ): Unit =
      bundleVar.addAsync( async )

   def addAsync( msg: osc.Message ): Unit =
      bundleVar.addAsync( msg )

   def addAsync( msg: osc.Message, async: AsyncAction ): Unit =
      bundleVar.addAsync( msg, async )

   def add( msg: osc.Message ): Unit =
      bundleVar.add( msg )

   def endsAfter( rn: RichNode, dur: Double ) = ()

   trait AbstractBundle {
//      protected var msgs     = Queue[ osc.Message ]()
      protected var msgs      = IIdxSeq.empty[ osc.Message ]
      protected var asyncs    = Queue[ AsyncAction ]()
      private var hasAsyncVar = false

      @throws( classOf[ IOException ])
      def send(): Unit

      def add( msg: osc.Message ): Unit = {
//         msgs = msgs.enqueue( msg )
         msgs :+= msg
      }

      def addAsync( msg: osc.Message ): Unit = {
         hasAsyncVar = true
         add( msg )
      }

      def addAsync( msg: osc.Message, async: AsyncAction ): Unit = {
         addAsync( async )
         add( msg )
      }

      def addAsync( async: AsyncAction ): Unit = {
          hasAsyncVar = true
          asyncs   = asyncs.enqueue( async )
       }

      def hasAsync = hasAsyncVar

      def messages: Seq[ osc.Message ] = msgs

      def doAsync(): Unit =
         asyncs.foreach( _.asyncDone() )
   }
}

class RealtimeSynthContext(s: Server)
  extends SynthContext(s, true) {

  private var syncWait = Map[Int, Bundle]()

  private val resp = message.Responder(server) {
    // Warning: code is not thread safe, and Responder
    // is on an arbitrary thread, so defer to AWT
    // thread to conincide with the Swing Timer in
    // SCTimeline!
    case message.Synced(id) => deferIfNeeded {
      syncWait.get(id).foreach {
        bundle =>
          syncWait -= id
          perform {
            bundle.doAsync()
          } // we could use 'delayed', but we might want bundle 'immediate' times
      }
    }
  }
  private var timebaseSysRef  = System.currentTimeMillis()
  private var timebaseVar     = 0.0
  private var bundleCount     = 0

  // ---- constructor ----
  resp.add()

  def dispose(): Unit = resp.remove()

  override def toString = "Realtime(" + s.toString + ")"

  def timebase: Double = {
    timebaseVar
  }

  def timebase_=(newVal: Double): Unit = {
    if (newVal == 0.0) timebaseSysRef = System.currentTimeMillis
    timebaseVar = newVal
  }

  protected def initBundle(delta: Double): AbstractBundle = {
    val res = new Bundle(bundleCount, if (delta < 0) 0L else timebaseSysRef + ((timebase + delta) * 1000 + 0.5).toLong)
    bundleCount += 1
    res
  }

  private class Bundle(count: Int, ref: Long)
    extends AbstractBundle {

    def send(): Unit = {
      val cpy = if (hasAsync) {
        syncWait += count -> this
        // msgs.enqueue( new scosc.SyncedMessage( count ))
        msgs :+ new message.Sync(count)
      } else {
        msgs
      }
      if (cpy.nonEmpty) {
        val bndl = if (ref == 0L) osc.Bundle.now(cpy: _*) else osc.Bundle.millis(ref, cpy: _*)
        server ! bndl
      }
    }
  }
}