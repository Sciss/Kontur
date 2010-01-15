/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Graphics2D }
import scala.collection.immutable.{ Queue }

trait TopPaintable {
  private var topPainters: Queue[ Graphics2D => Unit ] = Queue.Empty

  def addTopPainter( t: Graphics2D => Unit ) {
      topPainters = topPainters.enqueue( t )
  }

  protected def paintOnTop( g: Graphics2D ) {
     topPainters.foreach( _.apply( g ))
  }

  def removeTopPainter( t: Graphics2D => Unit ) {
    var filtered: Queue[ Graphics2D => Unit ] = Queue.Empty
      topPainters.foreach( x => if( x != t )
        filtered = filtered.enqueue( x )) // ugly; no easier way??
      topPainters = filtered
  }
}