/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Rectangle }
import javax.swing.{ JComponent }
import de.sciss.kontur.session.{ Track }

trait TracksTable {
	def mainView: JComponent
	def getRowHeader( t: Track ) : TrackRowHeader
	def getTrackBounds( t: Track /*, r: Rectangle*/ ) : Rectangle
	def numTracks: Int
	def getTrack( idx: Int ) : Option[ Track ]
	def indexOf( t: Track ) : Int
}