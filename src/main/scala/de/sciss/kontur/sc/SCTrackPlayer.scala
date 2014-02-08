/*
 *  SCTrackPlayer.scala
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

package de.sciss.kontur
package sc

import session.Track
import de.sciss.span.Span

trait SCTrackPlayer /* extends Disposable */ {
  def track: Track

  def step(currentPos: Long, span: Span): Unit

  def play(): Unit
  def stop(): Unit
  def dispose(): Unit
}

final class SCDummyPlayer(val track: Track)
  extends SCTrackPlayer {

  def step(currentPos: Long, span: Span) = ()

  def play() = ()
  def stop() = ()
  def dispose() = ()
}
