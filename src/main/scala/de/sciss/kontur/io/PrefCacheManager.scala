/*
 *  PrefCacheManager.scala
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

  def dispose(): Unit = {
    // XXX TODO
//    preferences.removePreferenceChangeListener(this)
  }

  override def setActive(onOff: Boolean): Unit = {
    super.setActive(onOff)
    preferences.put(KEY_ACTIVE, onOff)
  }

  override def setFolderAndCapacity(folder: File, capacity: Int): Unit = {
    super.setFolderAndCapacity(folder, capacity)
    import desktop.Implicits._
    preferences.put(KEY_FOLDER, folder)
    preferences.put(KEY_CAPACITY, new Param(capacity, ParamSpace.NONE | ParamSpace.ABS))
  }

// ------- PreferenceChangeListener interface -------

	def preferenceChange( e: PreferenceChangeEvent): Unit = {
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