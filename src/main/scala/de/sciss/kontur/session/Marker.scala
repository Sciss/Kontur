/*
 *  Marker.scala
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

package de.sciss.kontur.session

import de.sciss.span.Span

case class Marker( pos: Long, name: String ) extends Stake[ Marker ] {
    val span = Span( pos, pos )

    def move( delta: Long ): Marker = copy( pos = pos + delta )
}
