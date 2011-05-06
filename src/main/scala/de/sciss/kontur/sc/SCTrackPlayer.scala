package de.sciss.kontur.sc

import de.sciss.io.{ Span }
import de.sciss.util.{ Disposable }
import de.sciss.kontur.session.{ Track }

trait SCTrackPlayer extends Disposable {
   def track: Track.Any
   def step( currentPos: Long, span: Span )
   def play
   def stop
}

class SCDummyPlayer( val track: Track.Any )
extends SCTrackPlayer {
   def dispose {}
   def step( currentPos: Long, span: Span ) {}
   def play {}
   def stop {}
}