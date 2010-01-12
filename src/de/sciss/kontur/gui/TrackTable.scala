/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Rectangle }
import javax.swing.{ JComponent }
import de.sciss.kontur.session.{ Track }

trait TrackTable {
	def mainView: Option[ JComponent ]
	def getRowHeader( t: Track[ _ ]) : TrackRowHeader
	def getTrackBounds( t: Track[ _ ], r: Rectangle ) : Rectangle
	def numTracks: Int
	def getTrack( idx: Int ) : Option[ Track[ _ ]]
	def indexOf( t: Track[ _ ]) : Int
}