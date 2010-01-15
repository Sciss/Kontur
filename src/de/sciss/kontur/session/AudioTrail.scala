/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.session

import de.sciss.io.{ Span }

class AudioRegion( afe: AudioFileElement, s: Span, n: String )
extends Region( s, n ) {
  
}

class AudioTrail( doc: Session ) extends BasicTrail[ AudioRegion ]( doc )
