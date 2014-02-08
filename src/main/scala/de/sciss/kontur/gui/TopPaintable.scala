/*
 *  TopPaintable.scala
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

import java.awt.Graphics2D
import scala.collection.immutable.Queue

trait TopPaintable {
  private var topPainters: Queue[ Graphics2D => Unit ] = Queue.empty

  def addTopPainter( t: Graphics2D => Unit ): Unit =
      topPainters = topPainters.enqueue( t )

  protected def paintOnTop( g: Graphics2D ): Unit =
     topPainters.foreach( _.apply( g ))

  def removeTopPainter( t: Graphics2D => Unit ): Unit = {
    var filtered: Queue[ Graphics2D => Unit ] = Queue.empty
      topPainters.foreach( x => if( x != t )
        filtered = filtered.enqueue( x )) // ugly; no easier way??
      topPainters = filtered
  }
}