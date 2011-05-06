/*
 *  TrackList.scala
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

import java.awt.Rectangle
import javax.swing.JComponent
import scala.collection.mutable.{ ArrayBuffer, ListBuffer }
import de.sciss.app.{ AbstractCompoundEdit, UndoManager }
import de.sciss.kontur.edit.{ Editor, SimpleEdit }
import de.sciss.kontur.session.{ AudioTrack, BasicSessionElementSeq, Session,
                                Stake, Track }
import de.sciss.synth.Model

object TrackList {
    case class ElementAdded( idx: Int, e: TrackListElement )
    case class ElementRemoved( idx: Int, e: TrackListElement )
    case class SelectionChanged( e: TrackListElement* )
}

trait TrackListEditor extends Editor {
   def editSelect( ce: AbstractCompoundEdit, e: TrackListElement* ) : Unit
   def editDeselect( ce: AbstractCompoundEdit, e: TrackListElement* ) : Unit
}

trait TrackListElement {
    def track: Track
    def renderer: TrackRenderer
    def trailView: TrailView[ _ <: Stake[ _ ]]
    def selected: Boolean
}

trait TrackList extends Model {
//	def getRenderer( e: TrackListElement ) : TrackRenderer
	def getBounds( e: TrackListElement ) : Rectangle
	def numElements: Int
	def getElementAt( idx: Int ) : TrackListElement
	def getElement( t: Track ) : Option[ TrackListElement ]
   def editor : Option[ TrackListEditor ]

   // support these common methods
   def foreach[ U ]( f: TrackListElement => U ) : Unit =
      toList.foreach( f )

   def filter( p: (TrackListElement) => Boolean ): List[ TrackListElement ] =
      toList.filter( p )

   def find( p: (TrackListElement) => Boolean ): Option[ TrackListElement ] =
      toList.find( p )

   def foldLeft[B]( z: B )( op: (B, TrackListElement) => B ) : B =
      toList.foldLeft( z )( op )

   def toList: List[ TrackListElement ] = {
      val buf = new ListBuffer[ TrackListElement ]()
      var i = 0
      while( i < numElements ) {
         buf += getElementAt( i )
         i += 1
      }
      buf.toList
   }

   def indexOf( e: TrackListElement ) = toList.indexOf( e )
}

class DummyTrackList extends TrackList {
    def getBounds( e: TrackListElement ) : Rectangle = throw new NoSuchElementException()
    val numElements = 0
    def getElementAt( idx: Int ) : TrackListElement = throw new NoSuchElementException()
    def getElement( t: Track ) : Option[ TrackListElement ] = None
    def editor : Option[ TrackListEditor ] = None
}

//trait TrackListElementFactory {
//    def createTrackListElement( doc: Session, t: Track, trackList: TrackList,
//                                timelineView: TimelineView ) :
//     TrackListElement
//}

class BasicTrackListElement( val track: Track, val renderer: TrackRenderer,
                             val trailView: TrailView[ _ <: Stake[ _ ]])
extends TrackListElement {
    var selected = false
}

trait BasicTrackList // ( doc: Session, val timelineView: TimelineView )
extends TrackList with TrackListEditor {
  import TrackList._

  protected val doc: Session
  protected def timelineView: TimelineView

  /**
   *  Adds all tracks from the timeline
   *  to the list view
   */
  def addAllTracks {
      tracks.foreach( t => addTrack( t, true ))
  }

  private var following = false
  private var everAdded = Set[ Track ]()
  private var mapElem   = Map[ Track, TrackListElement  ]()
  private val elements  = new ArrayBuffer[ TrackListElement ]()
  private val tracks    = timelineView.timeline.tracks

  private val tracksListener: Model.Listener = {
      case tracks.ElementAdded( idx, t ) => addTrack( t, following )
      case tracks.ElementRemoved( idx, t ) => removeTrack( t )
  }

  // ---- constructor ----
  {
      tracks.addListener( tracksListener )
  }

  def followTracks {
     following = true
  }

   override def foreach[ U ]( f: TrackListElement => U ) : Unit =
       elements.foreach( f )

   override def filter( p: (TrackListElement) => Boolean ): List[ TrackListElement ] =
        elements.filter( p ).toList

   override def find( p: (TrackListElement) => Boolean ): Option[ TrackListElement ] =
        elements.find( p )

   override def indexOf( e: TrackListElement ) : Int = elements.indexOf( e )

   override def toList: List[ TrackListElement ] = elements.toList

   override def foldLeft[B]( z: B )( op: (B, TrackListElement) => B ) : B =
      elements.foldLeft( z )( op )

   protected def createTrailView( t: Track ) : TrailView[ _ <: Stake[ _ ]] =
      new BasicTrailView( doc, t.trail )

   protected def createRenderer( t: Track ) : TrackRenderer = t match {
      case at: AudioTrack => new AudioTrackRenderer( doc, at, this, timelineView )
      case _ => new DefaultTrackRenderer( doc, t, this, timelineView )
   }

   protected def createElement( t: Track, renderer: TrackRenderer,
                                trailView: TrailView[ _ <: Stake[ _ ]]) : TrackListElement =
      new BasicTrackListElement( t, renderer, trailView )

   private def addTrack( t: Track, force: Boolean ) {
      if( mapElem.contains( t )) return
      if( force || everAdded.contains( t )) {
         val trailView = createTrailView( t ) // must be before renderer!
         val renderer  = createRenderer( t )
         val elem      = createElement( t, renderer, trailView )
         mapElem += t -> elem
         everAdded += t
         val idx = elements.size // XXX
         elements.insert( idx, elem )
         dispatch( ElementAdded( idx, elem )) // XXX could redispatch SESeq stuff
      }
   }

   private def removeTrack( t: Track ) {
      if( following || everAdded.contains( t )) {
         println( "REMOVE TRACK : NOT YET IMPLEMENTED" )
      }
   }

   def dispose {
      tracks.removeListener( tracksListener )
      everAdded = Set[ Track ]()
      mapElem   = Map[ Track, TrackListElement ]()
   }

   def select( e: TrackListElement* ) : Unit = setSelection( e, true )
   def deselect( e: TrackListElement* ) : Unit = setSelection( e, false )

   private def setSelection( e: Seq[ TrackListElement ], state: Boolean ) {
      val ef = e.filterNot( elem => elem.selected == state )
      if( !ef.isEmpty ) {
        val change = SelectionChanged( ef: _* )
        ef.foreach( elem => elem match {
            case be: BasicTrackListElement => be.selected = state
            case _ => // what can we do...?
        })
        dispatch( change )
      }
   }

   // ---- TrackList trait ----
	def getBounds( e: TrackListElement ) : Rectangle =
      e.renderer.trackComponent.getBounds()

	def numElements: Int = elements.size
	def getElementAt( idx: Int ) : TrackListElement = elements( idx )
	def getElement( t: Track ) : Option[ TrackListElement ] = mapElem.get( t )

    def editor: Option[ TrackListEditor ] = Some( this )
   // ---- TrackListEditor trait ----

   def undoManager: UndoManager = doc.getUndoManager

   def editSelect( ce: AbstractCompoundEdit, e: TrackListElement* ) : Unit =
      editSetSelection( ce, e, true )

   def editDeselect( ce: AbstractCompoundEdit, e: TrackListElement* ) : Unit =
      editSetSelection( ce, e, false )

   private def editSetSelection( ce: AbstractCompoundEdit,
                                 e: Seq[ TrackListElement ], state: Boolean ) {
      val ef = e.filterNot( _.selected == state )
      if( !ef.isEmpty ) {
         val edit = new SimpleEdit( "editTrackSelection", false ) {
            def apply { setSelection( ef, state )}
            def unapply { setSelection( ef, !state )}
         }
         ce.addPerform( edit )
      }
   }
}

/*
object DefaultTrackListElementFactory
extends TrackListElementFactory {
    def createTrackListElement( doc: Session, t: Track, trackList: TrackList,
                                timelineView: TimelineView ) :
     TrackListElement = {

        val renderer = t match {
          case at: AudioTrack => new AudioTrackRenderer( doc, at, trackList,
                                                         timelineView )
          case _ => new DefaultTrackRenderer( doc, t, trackList, timelineView )
        }

        new BasicTrackListElement( t, renderer, trail )
     }
}
*/