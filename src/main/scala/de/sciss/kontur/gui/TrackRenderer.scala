/*
 *  TrackRenderer.scala
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

package de.sciss.kontur.gui

import javax.swing.JComponent
import de.sciss.kontur.session.{ AudioTrack, Session, Track }

//import Track.Tr

/*
trait TrackRendererFactory {
    def createTrackRenderer( doc: Session, t: Track, tracksView: TracksView,
                             timelineView: TimelineView ) :
     TrackRenderer
}
*/

trait TrackRenderer {
    def trackHeaderComponent : JComponent
    def trackComponent : JComponent with TrackComponent
//    def trackGUIEditor: Option[ TrackGUIEditor ]
}
/*
object DefaultTrackRendererFactory
extends TrackRendererFactory {
    def createTrackRenderer( doc: Session, t: Track, tracksView: TracksView,
                             timelineView: TimelineView ) :
     TrackRenderer =
      t match {
          case at: AudioTrack => new AudioTrackRenderer( doc, at, tracksView,
                                                         timelineView )
          case _ => new DefaultTrackRenderer( doc, t, tracksView, timelineView )
      }
}
*/
class DefaultTrackRenderer( doc: Session, t: Track, trackList: TrackList,
                            timelineView: TimelineView )
extends TrackRenderer {
   val trackHeaderComponent = new DefaultTrackHeaderComponent( t, trackList )
   val trackComponent       = new DefaultTrackComponent( doc, t, trackList, timelineView )
}

class AudioTrackRenderer( doc: Session, t: AudioTrack, trackList: TrackList,
                          timelineView: TimelineView )
extends TrackRenderer {
   val trackHeaderComponent = new AudioTrackHeaderComponent( t, trackList )
   val trackComponent       = new AudioTrackComponent( doc, t, trackList, timelineView )
}
