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

import java.awt.{ Cursor }
import java.awt.event.{ MouseEvent }
import javax.swing.{ JPanel }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.kontur.session.{ Stake, Track, Trail }
import de.sciss.kontur.util.{ Model }

object TrackTools {
    case class ToolChanged( oldTool: TrackTool, newTool: TrackTool )
}

trait TrackTools extends Model {
    def currentTool: TrackTool
}

trait TrackTool extends Model {
    def defaultCursor : Cursor
    def name: String
    def handleSelect( e: MouseEvent, hitTrack: Track ) : Unit
}

trait TrackEditor {
    def registerTools( tools: TrackTools )
}

class TrackCursorTool( tt: TrackList ) extends TrackTool {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.TEXT_CURSOR )
   val name = "Cursor"

    def handleSelect( e: MouseEvent, hitTrack: Track ) {

    }
}

object TrackMoveTool {
    case class DragBegin( move: Move )
    case class DragAdjust( oldMove: Move, newMove: Move )
    case class dragEnd( move: Move, commit: AbstractCompoundEdit )
    case object DragCancel

    case class Move( deltaTime: Long, deltaVertical: Int )
}

trait TrackStakeTool extends TrackTool {
    protected def trackList: TrackList
    
    def handleSelect( e: MouseEvent, hitTrack: Track ) {

    }
}

abstract class BasicTrackStakeTool( protected val trackList: TrackList )
extends TrackStakeTool

class TrackMoveTool( tt: TrackList ) extends BasicTrackStakeTool( tt ) {
    import TrackMoveTool._

   def defaultCursor = Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR )
   val name = "Move"
   private var currentMoveVar: Option[ Move ] = None

//   def dragBegin( e: MouseEvent ) {
//
//   }

   def dragBegin( move: Move ) {
      currentMoveVar = Some( move )
      dispatch( DragBegin( move ))
   }

   def dragAdjust( move: Move ) {
      currentMoveVar.foreach( oldMove => {
          if( move != currentMoveVar )
          currentMoveVar = Some( move )
          dispatch( DragAdjust( oldMove, move ))
      })
   }

   def dragEnd {
      currentMoveVar.foreach( move => {
          dispatch( DragBegin( move ))
      })
   }

   def dragCancel {
      if( currentMoveVar.isDefined ) {
          currentMoveVar = None
          dispatch( DragCancel )
      }
   }
   
   def currentMove = currentMoveVar
}

class TrackResizeTool( tt: TrackList ) extends BasicTrackStakeTool( tt ) {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.W_RESIZE_CURSOR )
   val name = "Resize"
}

class TrackGainTool( tt: TrackList ) extends BasicTrackStakeTool( tt ) {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.N_RESIZE_CURSOR )
   val name = "Gain"
}

class TrackFadeTool( tt: TrackList ) extends BasicTrackStakeTool( tt ) {
   def defaultCursor = Cursor.getPredefinedCursor( Cursor.NW_RESIZE_CURSOR )
   val name = "Fade"
}

class BasicTrackTools( tt: TrackList )
extends JPanel with TrackTools {
    private val tools = List( new TrackCursorTool( tt ),
                              new TrackMoveTool( tt ))

    private var currentToolVar: TrackTool = tools.head

    // ---- constructor ----
    {
      
    }

    def currentTool: TrackTool = currentToolVar
}