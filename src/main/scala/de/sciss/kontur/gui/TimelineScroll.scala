/*
 *  TimelineScroll.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2013 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU General Public License
 *	as published by the Free Software Foundation; either
 *	version 2, june 1991 of the License, or (at your option) any later version.
 *
 *	This software is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *	General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public
 *	License (gpl.txt) along with this software; if not, write to the Free Software
 *	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.kontur
package gui

import java.awt.{Adjustable, BasicStroke, Color, Graphics, Graphics2D, Insets, Shape}
import java.awt.event.{AdjustmentEvent, AdjustmentListener}
import java.awt.geom.{Line2D, Rectangle2D}
import javax.swing.{JScrollBar, UIManager}

import session.Timeline
import util.Model
import de.sciss.span.Span
import desktop.impl.DynamicComponentImpl

/**
 *  A GUI element for allowing
 *  horizontal timeline scrolling.
 *  Subclasses <code>JScrollBar</code>
 *  simply to override the <code>paintComponent</code>
 *  method: an additional hairline is drawn
 *  to visualize the current timeline position.
 *  also a translucent blue rectangle is drawn
 *  to show the current timeline selection.
 *	<p>
 *	This class tracks the catch preferences
 *
 *  @todo		the display properties work well
 *				with the Aqua look+and+feel, however
 *				are slightly wrong on Linux with platinum look+feel
 *				because the scroll gadgets have different positions.
 */
object TimelineScroll {
  val TYPE_UNKNOWN    = 0
  val TYPE_DRAG       = 1
  val TYPE_TRANSPORT  = 2

  private val colrSelection = new Color(0x00, 0x00, 0xFF, 0x2F) // GraphicsUtil.colrSelection;
  private val colrPosition  = Color.red
  private val strkPosition  = new BasicStroke(0.5f)
}

class TimelineScroll(timelineView: TimelineView)
  extends JScrollBar(Adjustable.HORIZONTAL)
  with AdjustmentListener with DynamicComponentImpl {

  import TimelineScroll._

  private var recentSize = getMinimumSize
  private var shpSelection: Option[Shape] = None
  private var shpPosition: Option[Shape] = None
  private var timelineSel = timelineView.selection.span
  private var timelineSpan = timelineView.timeline.span
  private var timelineLenShift = 0
  private var timelinePos = 0L
  private var timelineVis = timelineView.span
  private var prefCatch = false

  protected def dynamicComponent = this

  private val trackMargin = {
    val lafName = UIManager.getLookAndFeel.getName
    //println( "lafName = " + lafName )
    if (lafName.contains("Aqua") || lafName.contains("Quaqua") ||
      lafName.contains("Mac OS X")) {
      new Insets(0, 6, 0, 33)
    } else {
      new Insets(0, 17, 0, 17)
    }
  }

  private var wasAdjusting          = false
  private var adjustCatchBypass     = false
  private var catchBypassCount      = 0
  private var catchBypassWasSynced  = false

  // ---- constructor ----
  //    {
  calcLenShift()
  recalcTransforms()
  recalcBoundedRange()

  // --- Listener ---

  addAdjustmentListener(this)
  setFocusable(false)

  private def calcLenShift(): Unit = {
    var i = 0
    while ((timelineSpan.length >> i) > 0x3FFFFFFF) i += 1
    timelineLenShift = i
  }

  /**
	 *  Paints the normal scroll bar using
	 *  the super class's method. Additionally
	 *  paints timeline position and selection cues
	 */
  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)

    val d = getSize()
    val g2 = g.asInstanceOf[Graphics2D]
    val strkOrig = g2.getStroke
    val pntOrig = g2.getPaint

    if (d.width != recentSize.width || d.height != recentSize.height) {
      recentSize = d
      recalcTransforms()
    }

    shpSelection.foreach(shp => {
      g2.setColor(colrSelection)
      g2.fill(shp)
    })
    shpPosition.foreach(shp => {
      g2.setColor(colrPosition)
      g2.setStroke(strkPosition)
      g2.draw(shp)
    })

    g2.setStroke(strkOrig)
    g2.setPaint(pntOrig)
  }

  private def recalcBoundedRange(): Unit = {
    val len = (timelineSpan.length >> timelineLenShift).toInt
    val len2 = (timelineVis.length >> timelineLenShift).toInt
    // println( "recalcBoundedRange: len = " + len + "; len2 = " + len2 )
    if ((len > 0) && (len2 > 0)) {
      if (!isEnabled) setEnabled(true)
      setValues((timelineVis.start >> timelineLenShift).toInt, len2, 0, len) // val, extent, min, max
      setUnitIncrement(math.max(1, len2 >> 5)) // 1/32 extent
      setBlockIncrement(math.max(1, (len2 * 3) >> 2)) // 3/4 extent
    } else {
      if (isEnabled) setEnabled(false)
      setValues(0, 100, 0, 100) // full view will hide the scrollbar knob
    }
  }

  /*
   *  Calculates virtual->screen coordinates
   *  for timeline position and selection
   */
  private def recalcTransforms(): Unit = {
    if (!timelineSpan.isEmpty) {
      val scale = (recentSize.width - trackMargin.left - trackMargin.right).toDouble / timelineSpan.length
      timelineSel match {
        case sp@Span(tlStart, _) =>
          shpSelection = Some(new Rectangle2D.Double(
            tlStart * scale + trackMargin.left, 0,
            sp.length * scale, recentSize.height))
        case _ =>
          shpSelection = None
      }
      val x = timelinePos * scale + trackMargin.left
      shpPosition = Some(new Line2D.Double(x, 0, x, recentSize.height))
    } else {
      shpSelection = None
      shpPosition = None
    }
  }

  /**
   * Updates the red hairline representing
   * the current timeline position in the
   * overall timeline span.
   * Called directly from TimelineFrame
   * to improve performance. Don't use
   * elsewhere.
   *
   * @param  pos			new position in absolute frames
   * @param  patience	allowed graphic update interval
   *
   * @see	java.awt.Component#repaint( long )
   */
  def setPosition(pos: Long, patience: Long, typ: Int): Unit = {
    if (prefCatch && (catchBypassCount == 0) /* && timelineVis.contains( timelinePos ) */ &&
      //			(timelineVis.getStop() < timelineLen) &&
      !timelineVis.contains(pos + (if (typ == TYPE_TRANSPORT) timelineVis.length >> 3 else 0))) {

      timelinePos = pos

      var start = timelinePos
      if (typ == TYPE_TRANSPORT) {
        start -= timelineVis.length >> 3
      } else if (typ == TYPE_DRAG) {
        if (timelineVis.stop <= timelinePos) {
          start -= timelineVis.length
        }
      } else {
        start -= timelineVis.length >> 2
      }
      val stop = math.min(timelineSpan.stop, math.max(timelineSpan.start, start) + timelineVis.length)
      start = math.max(timelineSpan.start, stop - timelineVis.length)
      if (stop > start) {
        // it's crucial to update internal var timelineVis here because
        // otherwise the delay between emitting the edit and receiving the
        // change via timelineScrolled might be two big, causing setPosition
        // to fire more than one edit!
        timelineVis = Span(start, stop)
        timelineView.editor.foreach(ed => {
          val ce = ed.editBegin("scroll")
          ed.editScroll(ce, timelineVis)
          ed.editEnd(ce)
        })
        return
      }
    }
    timelinePos = pos
    recalcTransforms()
    repaint(patience)
  }

  def addCatchBypass(): Unit = {
    catchBypassCount += 1
    if (catchBypassCount == 1) {
      catchBypassWasSynced = timelineVis.contains(timelinePos)
    }
  }

  def removeCatchBypass(): Unit = {
    catchBypassCount -= 1
    if ((catchBypassCount == 0) && catchBypassWasSynced) {
      catchBypassWasSynced = false
      if (prefCatch && !timelineVis.contains(timelinePos)) {
        var start = timelinePos - (timelineVis.length >> 2)
        val stop = math.min(timelineSpan.stop, math.max(timelineSpan.start, start) + timelineVis.length)
        start = math.max(timelineSpan.start, stop - timelineVis.length)
        if (stop > start) {
          // it's crucial to update internal var timelineVis here because
          // otherwise the delay between emitting the edit and receiving the
          // change via timelineScrolled might be two big, causing setPosition
          // to fire more than one edit!
          timelineVis = Span(start, stop)
          timelineView.editor.foreach(ed => {
            val ce = ed.editBegin("scroll")
            ed.editScroll(ce, timelineVis)
            ed.editEnd(ce)
          })
        }
      }
    }
  }

  // ---------------- DynamicListening interface ----------------

  protected def componentShown(): Unit = {
    timelineView.addListener(timelineListener)
    recalcTransforms()
    repaint()
  }

  protected def componentHidden(): Unit = {
    timelineView.removeListener(timelineListener)
  }

  // ---------------- PreferenceChangeListener interface ----------------

  def setCatch(onOff: Boolean): Unit = {
    if (onOff == prefCatch) return

    prefCatch = onOff
    if (!prefCatch) return

    catchBypassCount = 0
    adjustCatchBypass = false
    if (!timelineVis.contains(timelinePos)) {
      var start = math.max(0, timelinePos - (timelineVis.length >> 2))
      val stop = math.min(timelineSpan.stop, start + timelineVis.length)
      start = math.max(timelineSpan.start, stop - timelineVis.length)
      if (stop > start) {
        timelineVis = Span(start, stop)
        timelineView.editor.foreach(ed => {
          val ce = ed.editBegin("scroll")
          ed.editScroll(ce, timelineVis)
          ed.editEnd(ce)
        })
      }
    }
  }

  // ---------------- TimelineListener interface ----------------

  private val timelineListener: Model.Listener = {
    case TimelineSelection.SpanChanged(_, newSpan) => {
      timelineSel = newSpan
      recalcTransforms()
      repaint()
    }
    case Timeline.SpanChanged(_, newSpan) => {
      timelineSpan = newSpan
      calcLenShift()
      recalcTransforms()
      recalcBoundedRange()
      repaint()
    }
    case TimelineView.SpanChanged(_, newSpan) => {
      if (newSpan != timelineVis) {
        timelineVis = newSpan
        recalcBoundedRange()
      }
    }
  }

  // ---------------- AdjustmentListener interface ----------------
  // we're listening to ourselves

  def adjustmentValueChanged(e: AdjustmentEvent): Unit = {
    if (!isEnabled) return

    val isAdjusting = e.getValueIsAdjusting
    val oldVisi = timelineView.span
    val newVisi = Span(getValue << timelineLenShift,
      (getValue + getVisibleAmount) << timelineLenShift)

    //println( "oldVisi " + oldVisi + "; newVisi " + newVisi )

    if (prefCatch && isAdjusting && !wasAdjusting) {
      adjustCatchBypass = true
      addCatchBypass()
    } else if (wasAdjusting && !isAdjusting && adjustCatchBypass) {
      if (prefCatch && !newVisi.contains(timelinePos)) {
        // we need to set prefCatch here even though laterInvocation will handle it,
        // because removeCatchBypass might look at it!
        prefCatch = false
      }
      adjustCatchBypass = false
      removeCatchBypass()
    }

    if (!newVisi.equals(oldVisi)) {
      timelineVis = newVisi
      timelineView.editor.foreach(ed => {
        val ce = ed.editBegin("scroll")
        ed.editScroll(ce, timelineVis)
        ed.editEnd(ce)
      })
    }

    wasAdjusting = isAdjusting
  }
}