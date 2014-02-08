/*
 *  DiffusionSynth.scala
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

package de.sciss.kontur.sc

import de.sciss.kontur.session.{ ConvolutionDiffusion, Diffusion, MatrixDiffusion }

trait DiffusionSynth {
   def play() : Unit
   def stop() : Unit
   def dispose() : Unit
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
      case cdiff: ConvolutionDiffusion => Some( new ConvolutionDiffusionSynthFactory( cdiff ))
      case _ => None
   }
}

trait DiffusionSynthFactory {
   def create: DiffusionSynth
}