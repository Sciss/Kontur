/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.io.{ Span }

object TimelineElement {
  case class SpanChanged( oldSpan: Span, newSpan: Span )
  case class RateChanged( oldRate: Double, newRate: Double )
}

class TimelineElement
extends SessionElement with Renameable {
  import TimelineElement._

  private var spanVar = new Span()
  private var rateVar = 44100.0
  protected var nameVar = "Untitled"
//  private val sync = new AnyRef

  val tracks   = new SessionElementSeq[ TrackElement[ _ ]]( "Tracks" )

  def span: Span = spanVar
  def span_=( newSpan: Span ) {
//    sync.synchronized {
      if( newSpan != spanVar ) {
        val change = SpanChanged( spanVar, newSpan )
        spanVar = newSpan
        dispatch( change )
      }
//    }
  }

  def rate: Double = // sync.synchronized {
    rateVar
  // } // sync necessary?

  def rate_=( newRate: Double ) {
//    sync.synchronized {
      if( newRate != rateVar ) {
        val change = RateChanged( rateVar, newRate )
        rateVar = newRate
        dispatch( change )
      }
//    }
  }
}
