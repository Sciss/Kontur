/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Rectangle }
import javax.swing.{ JComponent }
import de.sciss.kontur.session.{ TrackElement }

trait TrackTable {
	def mainView: JComponent
	def getRowHeader( t: TrackElement[ _ ]) : TrackRowHeader
	def getTrackBounds( t: TrackElement[ _ ], r: Rectangle ) : Rectangle
	def numTracks: Int
	def getTrack( idx: Int ) : TrackElement[ _ ]
	def indexOf( t: TrackElement[ _ ]) : Int
}