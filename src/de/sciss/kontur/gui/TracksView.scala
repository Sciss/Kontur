/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

  // ---- constructor ----
  {
      tracks.foreach( t => views += (t -> new TrackView) )
      tracks.addListener( tracksListener )
  }

  def dispose {
    tracks.removeListener( tracksListener )
  }

  private def tracksListener( msg: AnyRef ) {
    msg match {
      case tracks.ElementAdded( idx, elem ) => views += (elem -> new TrackView)
      case tracks.ElementRemoved( idx, elem ) => views -= elem
      case _ =>
    }
    dispatch( msg )
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
        val edit = new SimpleEdit( "editTrackSelection" ) {
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