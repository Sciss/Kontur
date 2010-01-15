/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Color, Graphics, Graphics2D }
import javax.swing.{ JComponent }
import de.sciss.kontur.session.{ Track }

class DefaultTrackComponent( t: Track, view: TracksView )
extends JComponent {

    {
        val rnd = new java.util.Random()
        setBackground( Color.getHSBColor( rnd.nextFloat(), 0.5f, 0.5f ))
    }

//    override def paintComponent( g: Graphics ) {
//
//    }
}
