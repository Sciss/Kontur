/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import javax.swing.{ JComponent }
import de.sciss.kontur.session.{ Track }

trait TrackRendererFactory {
    def createTrackRenderer( t: Track, view: TracksView, timelineView: TimelineView ) : TrackRenderer
}

trait TrackRenderer {
    def trackHeaderComponent : JComponent
    def trackComponent : JComponent
}

object DefaultTrackRendererFactory
extends TrackRendererFactory {
    def createTrackRenderer( t: Track, tracksView: TracksView, timelineView: TimelineView ) : TrackRenderer =
      new DefaultTrackRenderer( t, tracksView, timelineView )
}

class DefaultTrackRenderer( t: Track, tracksView: TracksView, timelineView: TimelineView )
extends TrackRenderer {
  val trackHeaderComponent = new DefaultTrackHeaderComponent( t, tracksView )
  val trackComponent       = new DefaultTrackComponent( t, tracksView, timelineView )
}