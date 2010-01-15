/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import javax.swing.{ JComponent }
import de.sciss.kontur.session.{ Track }

trait TrackRendererFactory {
    def createTrackRenderer( t: Track, view: TracksView ) : TrackRenderer
}

trait TrackRenderer {
    def trackHeaderComponent : JComponent
    def trackComponent : JComponent
}

object DefaultTrackRendererFactory
extends TrackRendererFactory {
    def createTrackRenderer( t: Track, view: TracksView ) : TrackRenderer =
      new DefaultTrackRenderer( t, view )
}

class DefaultTrackRenderer( t: Track, view: TracksView )
extends TrackRenderer {
  val trackHeaderComponent = new DefaultTrackHeaderComponent( t, view )
  val trackComponent       = new DefaultTrackComponent( t, view )
}