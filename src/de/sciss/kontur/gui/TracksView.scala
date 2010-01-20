/*
 *  TracksView.scala
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

import javax.swing.undo.{ UndoManager }
import scala.collection.mutable.{ ArrayBuffer }
import de.sciss.app.{ AbstractCompoundEdit }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }
import de.sciss.kontur.session.{ Session, SessionElementSeq, Stake, Track }
import de.sciss.kontur.util.{ Model }

object TracksView {
//  case class SelectionChanged[ T <: Stake ]( tracks: Track[ T ]* )
//    case class SelectionChanged[ T ]( tracks: Track[ T ]* )
    case class SelectionChanged( tracks: Track* )
}

trait TracksView extends Model {
    def tracks: SessionElementSeq[ Track ]

//    def addToSelection( t: Track[ _ ]* ) : Unit
//    def removeFromSelection( t: Track[ _ ]* ) : Unit
    def isSelected( t: Track ) : Boolean

    def editor: Option[ TracksViewEditor ]
}

trait TracksViewEditor extends Editor {
   def editSelect( ce: AbstractCompoundEdit, tracks: Track* ) : Unit
   def editDeselect( ce: AbstractCompoundEdit, tracks: Track* ) : Unit
}

class BasicTracksView( doc: Session, val tracks: SessionElementSeq[ Track ])
extends TracksView with TracksViewEditor {
  import TracksView._

  private var views = Map[ Track, TrackView ]()

  private val tracksListener = (msg: AnyRef) => {
    msg match {
      case tracks.ElementAdded( idx, elem ) => views += (elem -> new TrackView)
      case tracks.ElementRemoved( idx, elem ) => views -= elem
      case _ =>
    }
    dispatch( msg )
  }
  
  // ---- constructor ----
  {
      tracks.foreach( t => views += (t -> new TrackView) )
      tracks.addListener( tracksListener )
  }

  def dispose {
    tracks.removeListener( tracksListener )
  }

  def select( tracks: Track* ) : Unit = setSelection( tracks, true )
  def deselect( tracks: Track* ) : Unit = setSelection( tracks, false )

  private def setSelection( tracks: Seq[ Track ], state: Boolean ) {
      val tf = tracks.filterNot( t => isSelected( t ) == state )
      if( !tf.isEmpty ) {
        val change = SelectionChanged( tracks: _* )
        tracks.foreach( t => views( t ).selected = state )
        dispatch( change )
      }
  }

  def isSelected( t: Track ) : Boolean = views( t ).selected

  def editor: Option[ TracksViewEditor ] = Some( this )
   // ---- TracksViewEditor ----

  def undoManager: UndoManager = doc.getUndoManager

  def editSelect( ce: AbstractCompoundEdit, tracks: Track* ) : Unit =
    editSetSelection( ce, tracks, true )

  def editDeselect( ce: AbstractCompoundEdit, tracks: Track* ) : Unit =
    editSetSelection( ce, tracks, false )

  private def editSetSelection( ce: AbstractCompoundEdit, tracks: Seq[ Track ], state: Boolean ) {
    val tf = tracks.filterNot( t => isSelected( t ) == state )
    if( !tf.isEmpty ) {
        val edit = new SimpleEdit( "editTrackSelection", false ) {
            def apply { setSelection( tf, state )}
            def unapply { setSelection( tf, !state )}
        }
        ce.addPerform( edit )
    }
  }

  private class TrackView {
    var selected = false
  }
}