/*
 *  TrackTools.scala
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

package de.sciss.kontur.gui

import java.awt.{ Cursor, Insets }
import java.awt.event.{ ActionEvent, KeyEvent, KeyListener, MouseEvent }
import javax.swing.{ AbstractAction, AbstractButton, BoxLayout, ImageIcon,
                     JButton, JPanel, JToggleButton, SwingUtilities }
import javax.swing.event.{ MouseInputAdapter }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.kontur.session.{ Stake, Track, Trail }
import de.sciss.kontur.util.{ Model }

object TrackTools {
    case class ToolChanged( oldTool: TrackTool, newTool: TrackTool )
}

trait TrackTools extends Model {
    def currentTool: TrackTool
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

object TrackMoveTool {
    case class DragBegin( move: Move )
    case class DragAdjust( oldMove: Move, newMove: Move )
    case class DragEnd( move: Move, commit: AbstractCompoundEdit )
    case object DragCancel

    case class Move( deltaTime: Long, deltaVertical: Int )
}

trait TrackStakeTool extends TrackTool {
    tool =>

    protected def trackList: TrackList
    protected def timelineView: TimelineView
    
    def handleSelect( e: MouseEvent, tle: TrackListElement, pos: Long, stakeO: Option[ Stake[ _ ]]) {
        val track = tle.track // "stable"
        val tvCast = tle.trailView.asInstanceOf[ TrailView[ track.T ]]
        val stakeOCast = stakeO.asInstanceOf[ Option[ track.T ]]
        tvCast.editor.foreach( ed => {
            if( e.isShiftDown ) {
                stakeOCast.foreach( stake => {
                    val ce = ed.editBegin( "editSelectStakes" )
                    if( tvCast.isSelected( stake )) {
                        ed.editDeselect( ce, stake )
                    } else {
                        ed.editSelect( ce, stake )
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
                        })
                    })
                    stakeOCast.foreach( stake => {
                        ed.editSelect( ce, stake )
                    })
                    ed.editEnd( ce )
                }
            }
        })

        // now go on if stake is selected
        stakeOCast.foreach( stake => if( tvCast.isSelected( stake ))
            new Drag( e, tle, pos, stake )
        )
    }

    protected def dragEnd( d: Drag ) : Unit
    protected def dragCancel( d: Drag ) : Unit
    protected def dragStarted( d: Drag ) : Boolean
    protected def dragBegin( d: Drag )
    protected def dragAdjust( d: Drag )

    protected def screenToVirtual( e: MouseEvent ) : Long = {
        val tlSpan   = timelineView.timeline.span
        val p_off    = -tlSpan.start
        val p_scale  = e.getComponent.getWidth.toDouble / tlSpan.getLength
        (e.getX.toLong / p_scale - p_off + 0.5).toLong
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
            val comp = firstEvent.getComponent()
            comp.addMouseListener( this )
            comp.addMouseMotionListener( this )
//          comp.addKeyListener( this )
            comp.requestFocus
        }

        override def mouseReleased( e: MouseEvent ) {
            unregister
            if( started ) dragEnd( this )
        }

        private def unregister {
            val comp = firstEvent.getComponent()
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
                    currentTLEFVar = trackList.find( _.renderer.trackComponent == child ).
                        getOrElse( firstTLE )
                }
            }
            val convE = SwingUtilities.convertMouseEvent(
                comp, e, currentTLEFVar.renderer.trackComponent )
            currentPosVar   = screenToVirtual( convE )
        }

        override def mouseDragged( e: MouseEvent ) {
            calcCurrent( e )
            if( !started ) {
                started = dragStarted( this )
                if( !started ) return
                e.getComponent().addKeyListener( this )
                dragBegin( this )
            }
            dragAdjust( this )
        }

        def keyPressed( e: KeyEvent ) {
            if( e.getKeyCode == KeyEvent.VK_ESCAPE ) {
                unregister
                dragCancel( this )
            }
        }
        def keyTyped( e: KeyEvent ) {}
        def keyReleased( e: KeyEvent ) {}
    }
}

abstract class BasicTrackStakeTool( protected val trackList: TrackList,
                                    protected val timelineView: TimelineView )
extends TrackStakeTool

class TrackMoveTool( trackList: TrackList, timelineView: TimelineView )
extends BasicTrackStakeTool( trackList, timelineView ) {
    import TrackMoveTool._

   def defaultCursor = Cursor.getPredefinedCursor( Cursor.HAND_CURSOR )
   val name = "Move"
   private var currentMoveVar: Option[ Move ] = None

   protected def dragStarted( d: this.Drag ) : Boolean =
       d.currentEvent.getPoint().distanceSq( d.firstEvent.getPoint() ) > 4

   private def dragToMove( d: Drag ) : Move = {
       TrackMoveTool.Move( d.currentPos - d.firstPos,
            trackList.indexOf( d.currentTLE ) - trackList.indexOf( d.firstTLE ))
   }
   
   protected def dragBegin( d: this.Drag ) {
       val move = dragToMove( d )
       currentMoveVar = Some( move )
       dispatch( DragBegin( move ))
   }

   protected def dragAdjust( d: this.Drag ) {
      currentMoveVar.foreach( oldMove => {
          val move = dragToMove( d )
          if( move != currentMoveVar )
          currentMoveVar = Some( move )
          dispatch( DragAdjust( oldMove, move ))
      })
   }

   protected def dragEnd( d: this.Drag ) {
      currentMoveVar.foreach( move => {
          dispatch( DragEnd( move, null ))  // XXX
      })
   }

   protected def dragCancel( d: this.Drag ) {
      if( currentMoveVar.isDefined ) {
          currentMoveVar = None
          dispatch( DragCancel )
      }
   }
   
   def currentMove = currentMoveVar
}

/*
class TrackResizeTool( trackList: TrackList ) extends BasicTrackStakeTool( trackList ) {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.W_RESIZE_CURSOR )
   val name = "Resize"
}

class TrackGainTool( trackList: TrackList ) extends BasicTrackStakeTool( trackList ) {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR )
   val name = "Gain"
}

class TrackFadeTool( trackList: TrackList ) extends BasicTrackStakeTool( trackList ) {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.NW_RESIZE_CURSOR )
   val name = "Fade"
}
*/