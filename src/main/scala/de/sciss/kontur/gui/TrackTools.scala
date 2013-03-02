/*
 *  TrackTools.scala
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
package gui

import java.awt.{ Cursor, Point, Toolkit }
import javax.swing.{ Box, JLabel, JOptionPane, SwingUtilities }
import javax.swing.event.MouseInputAdapter
import de.sciss.app.{ AbstractApplication, AbstractCompoundEdit }
import de.sciss.common.BasicWindowHandler
import de.sciss.dsp.Util.dbamp
import de.sciss.gui.GUIUtil
import de.sciss.util.{ DefaultUnitTranslator, ParamSpace }
import util.{Model, PrefsUtil}
import session.{Session, AudioRegion, MuteableStake, Stake}
import sc.{SCAudioTrackPlayer, SuperColliderClient}
import java.awt.event.{MouseAdapter, KeyEvent, KeyListener, MouseEvent}
import language.reflectiveCalls
import de.sciss.span.Span

object TrackTools {
   case class ToolChanged( oldTool: TrackTool, newTool: TrackTool )
   case class VisualBoostChanged( oldBoost: Float, newBoost: Float )                // XXX this should really be somewhere else
   case class FadeViewModeChanged( oldMode: FadeViewMode, newMode: FadeViewMode )   // XXX this should really be somewhere else
   case class StakeBorderViewModeChanged( oldMode: StakeBorderViewMode, newMode: StakeBorderViewMode )   // XXX this should really be somewhere else
}

object StakeBorderViewMode {
   /** No visual indicator for stake borders */
   case object None  extends StakeBorderViewMode { val id = 0 }
   /** Rounded box for stake borders */
   case object Box extends StakeBorderViewMode { val id = 1 }
   /** Rounded box with region name for stake borders */
   case object TitledBox extends StakeBorderViewMode { val id = 2 }

   def apply( id: Int ) : StakeBorderViewMode = id match {
      case None.id      => None
      case Box.id       => Box
      case TitledBox.id => TitledBox
   }
}
sealed trait StakeBorderViewMode { def id: Int }

object FadeViewMode {
   /** No visual indicator for fades */
   case object None  extends FadeViewMode { val id = 0 }
   /** Curve overlays to indicate fades */
   case object Curve extends FadeViewMode { val id = 1 }
   /** Gain adjustments to sonogram to indicate fades */
   case object Sonogram extends FadeViewMode { val id = 2 }

   def apply( id: Int ) : FadeViewMode = id match {
      case None.id      => None
      case Curve.id     => Curve
      case Sonogram.id  => Sonogram
   }
}
sealed trait FadeViewMode { def id: Int }

trait TrackTools extends Model {
   def currentTool: TrackTool
   def visualBoost: Float                       // XXX this should really be somewhere else
   def fadeViewMode: FadeViewMode               // XXX this should really be somewhere else
   def stakeBorderViewMode: StakeBorderViewMode // XXX this should really be somewhere else
}

//class DummyTrackTools extends TrackTools {
//    val currentTools = DummyTrackTool
//}

trait TrackTool extends Model {
    def defaultCursor : Cursor
    def name: String
    def handleSelect( e: MouseEvent, hitTrack: TrackListElement, pos: Long, stakeO: Option[ Stake[ _ ]]) : Unit
}

//object DummyTrackTool extends TrackTool {
//    val defaultCursor : Cursor = Cursor.getDefaultCursor
//    val name = "Dummy"
//    def handleSelect( e: MouseEvent, hitTrack: TrackListElement, pos: Long, stakeO: Option[ Stake[ _ ]]) {}
//}

trait TrackToolsListener {
    def registerTools( tools: TrackTools )
}

class TrackCursorTool( trackList: TrackList, timelineView: TimelineView ) extends TrackTool {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.TEXT_CURSOR )
   val name = "Cursor"

    def handleSelect( e: MouseEvent, hitTrack: TrackListElement, pos: Long, stakeO: Option[ Stake[ _ ]]) {

    }
}

object TrackStakeTool {
    case object DragBegin
    case class DragEnd( commit: AbstractCompoundEdit )
    case object DragCancel
}

trait TrackStakeTool extends TrackTool {
   tool =>

   protected def trackList: TrackList
   protected def timelineView: TimelineView

   def handleSelect( e: MouseEvent, tle: TrackListElement, pos: Long, stakeO: Option[ Stake[ _ ]]) {
      val track      = tle.track // "stable"
      val tvCast     = tle.trailView.asInstanceOf[ TrailView[ track.T ]]
      val stakeOCast = stakeO.asInstanceOf[ Option[ track.T ]]

      tvCast.editor.foreach( ed => {
         if( e.isShiftDown ) {
            stakeOCast.foreach( stake => {
               val ce = ed.editBegin( "editSelectStakes" )
               if( tvCast.isSelected( stake )) {
                  ed.editDeselect( ce, stake )
//                  if( linked ) newSelRecalc = spanMinus( newSelRecalc, newSel, stake.span )
               } else {
                  ed.editSelect( ce, stake )
//                  if( linked ) newSel = spanPlus( newSelRecalc, newSel, stake.span )
               }
               ed.editEnd( ce )
            })
         } else {
            if( stakeOCast.map( stake => !tvCast.isSelected( stake )) getOrElse true ) {
               val ce = ed.editBegin( "editSelectStakes" )
               trackList.foreach( tle2 => {
                  val track2 = tle2.track // "stable"
                  val tvCast2 = tle2.trailView.asInstanceOf[ TrailView[ track2.T ]]
                  tvCast2.editor.foreach( ed2 => {
                     ed2.editDeselect( ce, ed2.view.selectedStakes.toList: _* )
//                     if( linked && !newSelRecalc ) {
//                        newSelRecalc = spanMinus( newSelRecalc, newSel, stake.span )
//                     }
                  })
               })
               stakeOCast.foreach( stake => {
                  ed.editSelect( ce, stake )
               })
               ed.editEnd( ce )
            }
         }
      })

      // handle linked timeline selection
      if( AbstractApplication.getApplication.getUserPrefs.getBoolean( PrefsUtil.KEY_LINKOBJTIMELINESEL, false )) {
         val oldSel     = timelineView.selection.span
         val newSel     = trackList.toList.flatMap(_.trailView.selectedStakes.map(_.span)) match {
           case head :: tail => tail.foldLeft(head)(_ union _)
           case Nil => Span.Void
         }
         if( newSel != oldSel ) {
            timelineView.editor.foreach( ed => {
               val ce = ed.editBegin( "select" )
               ed.editSelect( ce, newSel )
               ed.editEnd( ce )
            })
         }
      }

      // now go on if stake is selected
      stakeOCast.foreach( stake => if( tvCast.isSelected( stake )) {
         handleSelect( e, tle, pos, stake )
      })
   }

   protected def handleSelect( e: MouseEvent, tle: TrackListElement, pos: Long, stake: Stake[ _ ]) : Unit

   protected def screenToVirtual( e: MouseEvent ) : Long = {
      val tlSpan   = timelineView.timeline.span
      val p_off    = -tlSpan.start
      val p_scale  = e.getComponent.getWidth.toDouble / tlSpan.length
      (e.getX.toLong / p_scale - p_off + 0.5).toLong
   }
}

abstract class BasicTrackStakeTool[ P <: AnyRef ](
   protected val trackList: TrackList, protected val timelineView: TimelineView )
extends TrackStakeTool {
   import TrackStakeTool._

   protected var currentParamVar: Option[ P ] = None

   protected def dragToParam( d: Drag ) : P

   protected def dragEnd() {
      // XXX it becomes a little arbitrary which editor
      // to use to initiate an edit... should change this somehow
      timelineView.timeline.editor.foreach( ed => {
         val ce = ed.editBegin( name )
         dispatch( DragEnd( ce ))
         ed.editEnd( ce )
      })
   }

   protected def dragCancel( d: this.Drag ) {
      dispatch( DragCancel )
   }
   
   protected def handleSelect( e: MouseEvent, tle: TrackListElement, pos: Long, stake: Stake[ _ ]) {
      if( e.getClickCount == 2 ) {
         handleDoubleClick()
      } else {
         new Drag( e, tle, pos, stake )
      }
   }

   protected def dragStarted( d: this.Drag ) : Boolean =
      d.currentEvent.getPoint.distanceSq( d.firstEvent.getPoint ) > 16
   
   protected def dragBegin( d: this.Drag ) {
       val p = dragToParam( d )
       currentParamVar = Some( p )
       dispatch( DragBegin )
       dispatch( p )
   }

   protected def dragAdjust( d: this.Drag ) {
      currentParamVar.foreach( oldP => {
          val p = dragToParam( d )
          if( p != oldP ) {
             currentParamVar = Some( p )
             dispatch( p )
          }
      })
   }

   protected def dialog: Option[ P ]

   protected def handleDoubleClick() {
      dialog.foreach( p => {
         timelineView.timeline.editor.foreach( ed => {
//println( "GOT " + p )
            val ce = ed.editBegin( name )
            dispatch( DragBegin )
            dispatch( p )
            dispatch( DragEnd( ce ))
            ed.editEnd( ce )
         })
      })
   }

   protected def showDialog( message: AnyRef ) : Boolean = {
      val op = new JOptionPane( message, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION )
      val result = BasicWindowHandler.showDialog( op, null, name )
      result == JOptionPane.OK_OPTION
   }

   protected class Drag( val firstEvent: MouseEvent, val firstTLE: TrackListElement,
                        val firstPos: Long, val firstStake: Stake[ _ ])
   extends MouseInputAdapter with KeyListener {
      private var started = false

      private var currentEventVar = firstEvent
      def currentEvent = currentEventVar

      private var currentTLEFVar = firstTLE
      def currentTLE = currentTLEFVar

      private var currentPosVar = firstPos
      def currentPos = currentPosVar

      // ---- constructor ----
      {
         val comp = firstEvent.getComponent
         comp.addMouseListener( this )
         comp.addMouseMotionListener( this )
//       comp.addKeyListener( this )
         comp.requestFocus()
      }

      override def mouseReleased( e: MouseEvent ) {
         unregister()
         if( started ) dragEnd()
      }

      private def unregister() {
         val comp = firstEvent.getComponent
         comp.removeMouseListener( this )
         comp.removeMouseMotionListener( this )
         comp.removeKeyListener( this )
      }

      private def calcCurrent( e: MouseEvent ) {
         currentEventVar = e
         currentTLEFVar  = firstTLE  // default assumption
         val comp = e.getComponent
         if( e.getX < 0 || e.getX >= comp.getWidth ||
             e.getY < 0 || e.getY >= comp.getHeight ) {

            val parent = comp.getParent
            val ptParent = SwingUtilities.convertPoint( comp, e.getX, e.getY, parent )
            val child    = parent.getComponentAt( ptParent )
            if( child != null ) {
               currentTLEFVar = trackList.find( _.renderer.trackComponent == child ).getOrElse( firstTLE )
            }
         }
         val convE = SwingUtilities.convertMouseEvent( comp, e, currentTLEFVar.renderer.trackComponent )
         currentPosVar   = screenToVirtual( convE )
      }

      override def mouseDragged( e: MouseEvent ) {
         calcCurrent( e )
         if( !started ) {
            started = dragStarted( this )
            if( !started ) return
            e.getComponent.addKeyListener( this )
            dragBegin( this )
         }
         dragAdjust( this )
      }

      def keyPressed( e: KeyEvent ) {
         if( e.getKeyCode == KeyEvent.VK_ESCAPE ) {
            unregister()
            dragCancel( this )
         }
      }
      def keyTyped( e: KeyEvent ) {}
      def keyReleased( e: KeyEvent ) {}
   }
}

object TrackMoveTool {
   case class Move( deltaTime: Long, deltaVertical: Int, copy: Boolean )
}

class TrackMoveTool( trackList: TrackList, timelineView: TimelineView )
extends BasicTrackStakeTool[ TrackMoveTool.Move ]( trackList, timelineView ) {
   import TrackMoveTool._

   def defaultCursor = Cursor.getPredefinedCursor( Cursor.HAND_CURSOR )
   val name = "Move"

   protected def dragToParam( d: Drag ) : Move = {
       Move( d.currentPos - d.firstPos,
             trackList.indexOf( d.currentTLE ) - trackList.indexOf( d.firstTLE ),
             d.currentEvent.isAltDown )
   }

   protected def dialog: Option[ Move ] = {
      val box        = Box.createHorizontalBox
      val timeTrans  = new DefaultUnitTranslator()
      val ggTime     = new ParamField( timeTrans )
      val spcTimeHHMMSSD	= new ParamSpace( Double.NegativeInfinity, Double.PositiveInfinity, 0.0, 1, 3, 0.0,
                                             ParamSpace.TIME | ParamSpace.SECS | ParamSpace.HHMMSS | ParamSpace.OFF )
      ggTime.addSpace( spcTimeHHMMSSD )
      ggTime.addSpace( ParamSpace.spcTimeSmpsD )
      ggTime.addSpace( ParamSpace.spcTimeMillisD )
      GUIUtil.setInitialDialogFocus( ggTime )
      box.add( new JLabel( "Move by:" ))
      box.add( Box.createHorizontalStrut( 8 ))
      box.add( ggTime )

      val tl = timelineView.timeline
      timeTrans.setLengthAndRate( tl.span.length, tl.rate )
      if( showDialog( box )) {
         val delta = timeTrans.translate( ggTime.getValue, ParamSpace.spcTimeSmpsD ).`val`.toLong
         Some( Move( delta, 0, copy = false ))
      } else None
   }
}

object TrackResizeTool {
    case class Resize( deltaStart: Long, deltaStop: Long )
}

class TrackResizeTool( trackList: TrackList, timelineView: TimelineView )
extends BasicTrackStakeTool[ TrackResizeTool.Resize ]( trackList, timelineView ) {
   import TrackResizeTool._

   def defaultCursor = Cursor.getPredefinedCursor( Cursor.W_RESIZE_CURSOR )
   val name = "Resize"

   protected def dialog: Option[ Resize ] = None // not yet supported

   protected def dragToParam( d: Drag ) : Resize = {
      val (deltaStart, deltaStop ) =
         if( math.abs( d.firstPos - d.firstStake.span.start ) <
             math.abs( d.firstPos - d.firstStake.span.stop )) {

            (d.currentPos - d.firstPos, 0L)
         } else {
            (0L, d.currentPos - d.firstPos)
         }
      Resize( deltaStart, deltaStop )
   }
}

object TrackGainTool {
    case class Gain( factor: Float )
}

class TrackGainTool( trackList: TrackList, timelineView: TimelineView )
extends BasicTrackStakeTool[ TrackGainTool.Gain ]( trackList, timelineView ) {
   import TrackGainTool._
   
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR )
   val name = "Gain"

   protected def dialog: Option[ Gain ] = None // not yet supported

   override protected def dragStarted( d: this.Drag ) : Boolean =
      d.currentEvent.getY != d.firstEvent.getY

   protected def dragToParam( d: Drag ) : Gain = {
      val dy = d.firstEvent.getY - d.currentEvent.getY
      // use 0.1 dB per pixel. eventually we could use modifier keys...
      val factor = (dbamp( dy / 10 )).toFloat
      Gain( factor )
   }
}

object TrackFadeTool {
    case class Fade( deltaFadeIn: Long, deltaFadeOut: Long,
                     deltaFadeInCurve: Float, deltaFadeOutCurve: Float )
}

class TrackFadeTool( trackList: TrackList, timelineView: TimelineView )
extends BasicTrackStakeTool[ TrackFadeTool.Fade ]( trackList, timelineView ) {
   import TrackFadeTool._

   def defaultCursor = Cursor.getPredefinedCursor( Cursor.NW_RESIZE_CURSOR )
   val name = "Fade"

   private var curvature = false

   protected def dialog: Option[ Fade ] = None // not yet supported

   protected def dragToParam( d: Drag ) : Fade = {
      val leftHand = math.abs( d.firstPos - d.firstStake.span.start ) <
                     math.abs( d.firstPos - d.firstStake.span.stop )
      val (deltaTime, deltaCurve) = if( curvature ) {
         val dc = (d.firstEvent.getY - d.currentEvent.getY) * 0.1f
         (0L, if( leftHand ) -dc else dc)
      } else {
         (if( leftHand ) d.currentPos - d.firstPos else d.firstPos - d.currentPos, 0f)
      }
      if( leftHand ) Fade( deltaTime, 0L, deltaCurve, 0f )
      else Fade( 0L, deltaTime, 0f, deltaCurve )
   }

   override protected def dragStarted( d: this.Drag ) : Boolean = {
      val result = super.dragStarted( d )
      if( result ) {
         curvature = math.abs( d.currentEvent.getX - d.firstEvent.getX ) <
                     math.abs( d.currentEvent.getY - d.firstEvent.getY )
      }
      result
   }
}

object TrackSlideTool {
   case class Slide( deltaOuter: Long, deltaInner: Long )
}

class TrackSlideTool( trackList: TrackList, timelineView: TimelineView )
extends BasicTrackStakeTool[ TrackSlideTool.Slide ]( trackList, timelineView ) {
   import TrackSlideTool._

   def defaultCursor = Cursor.getPredefinedCursor( Cursor.E_RESIZE_CURSOR )
   val name = "Slide"

   protected def dialog: Option[ Slide ] = None // not yet supported

   protected def dragToParam( d: Drag ) : Slide = {
      val amt = d.currentPos - d.firstPos
      if( d.firstEvent.isAltDown )
         Slide( 0L, -amt )
      else
         Slide( amt, 0L )
   }
}

object TrackMuteTool {
   case class Mute( ce: AbstractCompoundEdit, state: Boolean )

   private lazy val cursor = {
      val tk   = Toolkit.getDefaultToolkit
      val img  = tk.createImage( classOf[ TrackMuteTool ].getResource( "cursor-mute.png" ))
      tk.createCustomCursor( img, new Point( 4, 4 ), "Mute" )
   }
}

class TrackMuteTool(
   protected val trackList: TrackList, protected val timelineView: TimelineView )
extends TrackStakeTool {
   import TrackMuteTool._

   protected def handleSelect( e: MouseEvent, tle: TrackListElement, pos: Long, stake: Stake[ _ ]) {
      stake match {
         case mStake: MuteableStake[ _ ] => {
            timelineView.timeline.editor.foreach( ed => {
               val ce = ed.editBegin( name )
               dispatch( Mute( ce, !mStake.muted ))
               ed.editEnd( ce )
            })
         }
         case _ =>
      }
   }

   val defaultCursor = TrackMuteTool.cursor
   val name = "Mute"
}

object TrackAuditionTool {
   private lazy val cursor = {
      val tk   = Toolkit.getDefaultToolkit
      val img  = tk.createImage( classOf[ TrackMuteTool ].getResource( "cursor-audition.png" ))
      tk.createCustomCursor( img, new Point( 4, 4 ), "Audition" )
   }
}

class TrackAuditionTool( doc: Session,
   protected val trackList: TrackList, protected val timelineView: TimelineView )
extends TrackStakeTool {
   protected def handleSelect( e: MouseEvent, tle: TrackListElement, pos: Long, stake: Stake[ _ ]) {
      val fromStart = e.isAltDown
      if( !fromStart ) {
         timelineView.editor.foreach { ed =>
            val ce = ed.editBegin( "Adjust timeline position" )
            ed.editPosition( ce, pos )
            ed.editEnd( ce )
         }
      }
      val trackPlayerO = SuperColliderClient.instance.getPlayer( doc ).flatMap( _.session )
         .map( _.timeline( timelineView.timeline ).track( tle.track ))
      (stake, trackPlayerO) match {
         case (ar: AudioRegion, Some( atp: SCAudioTrackPlayer )) =>
            val frameOffset   = if( fromStart ) 0L else {
               ar.span.clip( pos - (ar.audioFile.sampleRate * 0.1).toLong ) - ar.span.start
            }
            val stopper       = atp.play( ar, frameOffset )
            val comp          = e.getComponent
            comp.addMouseListener( new MouseAdapter {
               override def mouseReleased( e2: MouseEvent ) {
                  stopper.stop()
                  comp.removeMouseListener( this )
               }
            })
            comp.requestFocus()

         case _ =>
      }
   }

   val name = "Audition"
   val defaultCursor = TrackAuditionTool.cursor
}