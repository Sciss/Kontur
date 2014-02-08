/*
 *  TrailPanel.scala
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

import java.awt.Graphics
import javax.swing.JComponent
import de.sciss.kontur.session.BasicTimeline

class TrailPanel( tl: BasicTimeline ) extends JComponent {
  override def paintComponent( g: Graphics ) = ()
}
