/*
 *  AudioTrail.scala
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

package de.sciss.kontur.session

import de.sciss.io.{ Span }

class AudioRegion( s: Span, n: String, val audioFile: AudioFileElement,
                   val offset: Long )
extends Region( s, n ) {
  def toXML = <stake>
  <name>{name}</name>
  <span start={span.start.toString} stop={span.stop.toString}/>
  <audioFile idref={audioFile.id.toString}/>
  <offset>offset</offset>
</stake>
}

class AudioTrail( doc: Session ) extends BasicTrail[ AudioRegion ]( doc ) {
  def toXML = <trail>
  {getAll().map(_.toXML)}
</trail>
}
