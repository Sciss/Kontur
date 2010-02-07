/*
 *  DiffusionSynth.scala
 *  (Kontur)
 *
 *  Copyright (c) 2004-2010 Hanns Holger Rutz. All rights reserved.
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
 *
 *
 *  Changelog:
 */

package de.sciss.kontur.sc

import de.sciss.kontur.session.{ ConvolutionDiffusion, Diffusion, MatrixDiffusion }
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
      case cdiff: ConvolutionDiffusion => Some( new ConvolutionDiffusionSynthFactory( cdiff ))
      case _ => None
   }
}

trait DiffusionSynthFactory {
   def create: DiffusionSynth
}