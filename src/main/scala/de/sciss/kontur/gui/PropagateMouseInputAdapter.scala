/*
 *  PropagateMouseInputAdapter.scala
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

import java.awt.{ Component, Container }
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.event.MouseInputAdapter

class PropagateMouseInputAdapter private(parent: Option[Container])
  extends MouseInputAdapter {

  def this(parent: Container) = this(Some(parent))

  def this() = this(None)

  override def mousePressed (e: MouseEvent): Unit = propagate(e)
  override def mouseReleased(e: MouseEvent): Unit = propagate(e)
  override def mouseMoved   (e: MouseEvent): Unit = propagate(e)
  override def mouseDragged (e: MouseEvent): Unit = propagate(e)

  def propagate(e: MouseEvent): Unit = {
    val p = parent getOrElse e.getComponent.getParent
    val ec = SwingUtilities.convertMouseEvent(e.getComponent, e, p)
    p.dispatchEvent(ec)
  }

  def removeFrom(c: Component): Unit = {
    c.addMouseListener      (this)
    c.addMouseMotionListener(this)
  }

  def addTo(c: Component): Unit = {
    c.removeMouseListener      (this)
    c.removeMouseMotionListener(this)
  }
}