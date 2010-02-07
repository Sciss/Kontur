/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 06.02.2010
 * Time: 18:23:16
 */
package de.sciss.kontur.sc

import scala.collection.immutable.{ Queue }
import scala.collection.mutable.{ ListBuffer }
import java.net.{ SocketAddress }
import de.sciss.tint.sc.{ Bus, GE, Group, OSCResponderNode, OSCSyncMessage, OSCSyncedMessage, Server, Synth, SynthDef }
import de.sciss.scalaosc.{ OSCMessage }
import de.sciss.kontur.util.{ Model }

trait AsyncModel {
   private var collWhenOnline = Queue[ () => Unit ]()
   private var isOnlineVar = false

   def isOnline: Boolean = isOnlineVar
   def isOnline_=( newState: Boolean ) {
      if( newState != isOnlineVar ) {
         isOnlineVar = newState
         if( newState ) {
            val cpy = collWhenOnline
            collWhenOnline = Queue[ () => Unit ]()
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
}

class RichSynthDef( val synthDef: SynthDef, val bundleCount: Int )
extends AsyncModel {
   def play : RichSynth = play()
   def play( args: Tuple2[ String, Float ]* ) : RichSynth =
      SynthContext.current.play( this, args )
}

class RichSynth( val synth: Synth )
extends AsyncModel {
   def free {
      SynthContext.current.free( this )
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

   def graph( definingParams: Any* )( ugenThunk: => GE ) : RichSynthDef =
      current.graph( definingParams :_* )( ugenThunk )

   def audioBus( numChannels: Int ) : RichBus =
      current.audioBus( numChannels )

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
         bundle.add( sd.recvMsg, rsd )
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

   class Bundle( count: Int ) {
      private var msgs     = Queue[ OSCMessage ]()
      private var asyncs   = Queue[ AsyncModel ]()

      def add( msg: OSCMessage ) {
         msgs = msgs.enqueue( msg )
      }

      def add( msg: OSCMessage, async: AsyncModel ) {
         asyncs   = asyncs.enqueue( async )
         add( msg )
      }

      def send( server: Server ) {
         val cpy = if( asyncs.nonEmpty ) {
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