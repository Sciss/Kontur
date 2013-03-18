/*
 *  PrefCacheManager.scala
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
 *		16-Jul-05	created
 *		23-Sep-05	fixes a problem of cache folder not automatically been generated
 *		28-Jul-07	refactored from de.sciss.eisenkraut.io.CacheManager
 */

package de.sciss.kontur
package io

import java.io.File
import java.util.prefs.{ PreferenceChangeEvent, PreferenceChangeListener }
import legacy.{CacheManager, ParamSpace, Param}
import de.sciss.desktop.Preferences

object PrefCacheManager {
   /**
    *	Convenient name for preferences node
    */
   val KEY_ACTIVE		   = "active"		// boolean
   val KEY_FOLDER		   = "folder"		// String
   val KEY_CAPACITY		= "capacity"	// Param
}

class PrefCacheManager( val preferences: Preferences,
                        defaultActive: Boolean, defaultFolder: File, defaultCapacity: Int )
extends CacheManager
with PreferenceChangeListener {
   import PrefCacheManager._

  private val defaultCapacityP = new Param(defaultCapacity, ParamSpace.NONE | ParamSpace.ABS)

  // ---- constructor ---
  {
    import desktop.Implicits._

    val capacity  = preferences.getOrElse(KEY_CAPACITY, defaultCapacityP).value.toInt
    val folder    = preferences.getOrElse(KEY_FOLDER, defaultFolder)
    val active    = preferences.getOrElse(KEY_ACTIVE, defaultActive)
    setFolderAndCapacity(folder, capacity)
    setActive(active)
    // XXX TODO
//    preferences.addPreferenceChangeListener(this)
  }

  def dispose() {
    // XXX TODO
//    preferences.removePreferenceChangeListener(this)
  }

  override def setActive(onOff: Boolean) {
    super.setActive(onOff)
    preferences.put(KEY_ACTIVE, onOff)
  }

  override def setFolderAndCapacity(folder: File, capacity: Int) {
    super.setFolderAndCapacity(folder, capacity)
    import desktop.Implicits._
    preferences.put(KEY_FOLDER, folder)
    preferences.put(KEY_CAPACITY, new Param(capacity, ParamSpace.NONE | ParamSpace.ABS))
  }

// ------- PreferenceChangeListener interface -------

	def preferenceChange( e: PreferenceChangeEvent) {
		val key = e.getKey

    import desktop.Implicits._

		if( key == KEY_FOLDER ) {
			val f = new File( e.getNewValue )
			if( (getFolder == null) || (getFolder != f) ) {
				setFolder( f )
			}
      } else if( key == KEY_CAPACITY ) {
			val c = preferences.getOrElse(key, defaultCapacityP).value.toInt
			if( getCapacity != c ) {
				setCapacity( c )
			}
		} else if( key == KEY_ACTIVE ) {
			val b = java.lang.Boolean.valueOf( e.getNewValue ).booleanValue()
			if( isActive != b ) {
				setActive( b )
			}
		}
 	}
}