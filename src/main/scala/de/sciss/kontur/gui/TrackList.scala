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
    case class ElementAdded[ T <: Stake[ T ]]( idx: Int, e: TrackListElement[ T ])
    case class ElementRemoved[ T <: Stake[ T ]]( idx: Int, e: TrackListElement[ T ])
    case class SelectionChanged( e: TrackListElement.Any* )
}

trait TrackListEditor extends Editor {
   def editSelect( ce: AbstractCompoundEdit, e: TrackListElement.Any* ) : Unit
   def editDeselect( ce: AbstractCompoundEdit, e: TrackListElement.Any* ) : Unit
}

object TrackListElement {
   type Any = TrackListElement[ T ] forSome { type T <: Stake[ T ]}
}
trait TrackListElement[ +T ] {
    def track: Track[ T ]
    def renderer: TrackRenderer
    def trailView: TrailView[ T ] // .Any // [ T <: Stake[ T ] forSome { type T }]
    def selected: Boolean
}

trait TrackList extends Model {
//	def getRenderer( e: TrackListElement ) : TrackRenderer
	def getBounds( e: TrackListElement.Any ) : Rectangle
	def numElements: Int
	def getElementAt( idx: Int ) : TrackListElement.Any
	def getElement[ T <: Stake[ T ]]( t: Track[ T ]) : Option[ TrackListElement[ T ]]
   def editor : Option[ TrackListEditor ]

   // support these common methods
   def foreach[ U ]( f: TrackListElement[ Stake[ _ ]] => U ) : Unit =
      toList.foreach( f )

   def filter( p: (TrackListElement[ Stake[ _ ]]) => Boolean ): List[ TrackListElement[ Stake[ _ ]]] =
      toList.filter( p )

   def find( p: (TrackListElement[ Stake[ _ ]]) => Boolean ): Option[ TrackListElement[ Stake[ _ ]]] =
      toList.find( p )

   def foldLeft[B]( z: B )( op: (B, TrackListElement[ Stake[ _ ]]) => B ) : B =
      toList.foldLeft( z )( op )

   def toList: List[ TrackListElement[ Stake[ _ ]]] = {
      val buf = new ListBuffer[ TrackListElement[ Stake[ _ ]]]()
      var i = 0
      while( i < numElements ) {
         buf += getElementAt( i )
         i += 1
      }
      buf.toList
   }

   def indexOf( e: TrackListElement.Any ) = toList.indexOf( e )
}

class DummyTrackList extends TrackList {
    def getBounds( e: TrackListElement.Any ) : Rectangle = throw new NoSuchElementException()
    val numElements = 0
    def getElementAt( idx: Int ) : TrackListElement.Any = throw new NoSuchElementException()
    def getElement[ T <: Stake[ T ]]( t: Track[ T ]) : Option[ TrackListElement[ T ]] = None
    def editor : Option[ TrackListEditor ] = None
}

//trait TrackListElementFactory {
//    def createTrackListElement( doc: Session, t: Track, trackList: TrackList,
//                                timelineView: TimelineView ) :
//     TrackListElement
//}

class BasicTrackListElement[ T <: Stake[ T ]]( val track: Track[ T ], val renderer: TrackRenderer,
                             val trailView: TrailView[ T ])
extends TrackListElement[ T ] {
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
      tracks.foreach( t => addTrack( t.asInstanceOf[ Track.Any ], true ))
  }

  private var following = false
  private var everAdded = Set[ Track[ Stake[ _ ]]]()
  private var mapElem   = Map[ Track[ Stake[ _ ]], TrackListElement[ Stake[ _ ]]]()
  private val elements  = new ArrayBuffer[ TrackListElement[ Stake[ _ ]]]()
  private val tracks    = timelineView.timeline.tracks

  private val tracksListener: Model.Listener = {
     case "never" =>
// PPP
//      case tracks.ElementAdded( idx, t )   => addTrack( t, following )
//      case tracks.ElementRemoved( idx, t ) => removeTrack( t )
  }

  // ---- constructor ----
  {
      tracks.addListener( tracksListener )
  }

  def followTracks {
     following = true
  }

   override def foreach[ U ]( f: TrackListElement[ Stake[ _ ]] => U ) : Unit =
       elements.foreach( f )

   override def filter( p: (TrackListElement[ Stake[ _ ]]) => Boolean ): List[ TrackListElement[ Stake[ _ ]]] =
        elements.filter( p ).toList

   override def find( p: (TrackListElement[ Stake[ _ ]]) => Boolean ): Option[ TrackListElement[ Stake[ _ ]]] =
        elements.find( p )

   override def indexOf( e: TrackListElement.Any ) : Int = elements.indexOf( e )

   override def toList: List[ TrackListElement[ Stake[ _ ]]] = elements.toList

   override def foldLeft[B]( z: B )( op: (B, TrackListElement[ Stake[ _ ]]) => B ) : B =
      elements.foldLeft( z )( op )

   protected def createTrailView[ T <: Stake[ T ]]( t: Track[ T ]) : TrailView[ T ] =
      new BasicTrailView( doc, t.trail )

   protected def createRenderer( t: Track.Any ) : TrackRenderer = t match {
      case at: AudioTrack => new AudioTrackRenderer( doc, at, this, timelineView )
      case _ => new DefaultTrackRenderer( doc, t, this, timelineView )
   }

   protected def createElement[ T <: Stake[ T ]]( t: Track[ T ], renderer: TrackRenderer,
                                trailView: TrailView[ T ]) : TrackListElement[ T ] =
      new BasicTrackListElement[ T ]( t, renderer, trailView )

   private def addTrack[ T <: Stake[ T ]]( t: Track[ T ], force: Boolean ) {
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

   private def removeTrack( t: Track.Any ) {
      if( following || everAdded.contains( t )) {
         println( "REMOVE TRACK : NOT YET IMPLEMENTED" )
      }
   }

   def dispose {
      tracks.removeListener( tracksListener )
      everAdded = everAdded.empty
      mapElem   = mapElem.empty
   }

   def select( e: TrackListElement.Any* ) : Unit = setSelection( e, true )
   def deselect( e: TrackListElement.Any* ) : Unit = setSelection( e, false )

   private def setSelection( e: Seq[ TrackListElement.Any ], state: Boolean ) {
      val ef = e.filterNot( elem => elem.selected == state )
      if( !ef.isEmpty ) {
        val change = SelectionChanged( ef: _* )
        ef.foreach( elem => elem match {
            case be: BasicTrackListElement[ _ ] => be.selected = state
            case _ => // what can we do...?
        })
        dispatch( change )
      }
   }

   // ---- TrackList trait ----
	def getBounds( e: TrackListElement.Any ) : Rectangle =
      e.renderer.trackComponent.getBounds()

	def numElements: Int = elements.size
	def getElementAt( idx: Int ) : TrackListElement.Any = elements( idx ).asInstanceOf[ TrackListElement.Any ]
	def getElement[ T <: Stake[ T ]]( t: Track[ T ]) : Option[ TrackListElement[ T ]] = mapElem.get( t ).asInstanceOf[ Option[ TrackListElement[ T ]]]

    def editor: Option[ TrackListEditor ] = Some( this )
   // ---- TrackListEditor trait ----

   def undoManager: UndoManager = doc.getUndoManager

   def editSelect( ce: AbstractCompoundEdit, e: TrackListElement.Any* ) : Unit =
      editSetSelection( ce, e, true )

   def editDeselect( ce: AbstractCompoundEdit, e: TrackListElement.Any* ) : Unit =
      editSetSelection( ce, e, false )

   private def editSetSelection( ce: AbstractCompoundEdit,
                                 e: Seq[ TrackListElement.Any ], state: Boolean ) {
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