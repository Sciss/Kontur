/*
 * Created by IntelliJ IDEA.
 * User: rutz
 * Date: 06.02.2010
 * Time: 17:53:45
 */
package de.sciss.kontur.sc

import de.sciss.kontur.session.{ Diffusion, MatrixDiffusion }
import de.sciss.tint.sc.{ Bus }

trait DiffusionSynth {
   def play : Unit
   def stop : Unit
   def dispose : Unit
   def inBus: RichBus
   def outBus: RichBus
   def diffusion: Diffusion
}

object DiffusionSynthFactory {
//   var registered = Map[ String, DiffusionSynthFactory ]()

   // ---- constructor ----
   // XXX eventually this needs to be decentralized
//   registered += MatrixDiffusion.factoryName -> MatrixDiffusionSynth

   def get( diff: Diffusion ) : Option[ DiffusionSynthFactory ] = diff match {
      // XXX eventually this needs to be decentralized
      case mdiff: MatrixDiffusion => Some( new MatrixDiffusionSynthFactory( mdiff ))
      case _ => None
   }
}

trait DiffusionSynthFactory {
   def create: DiffusionSynth
}