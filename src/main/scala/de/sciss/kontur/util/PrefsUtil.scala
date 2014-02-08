/*
 *  PrefsUtil.scala
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

package de.sciss.kontur.util

/**
 *    @version 0.11, 10-Feb-10
 */
object PrefsUtil {
	/**
	 *  Value: String representing the path of
	 *  the last used directory for opening a file.<br>
	 *  Has default value: no!<br>
	 *  Node: root
	 */
	val KEY_FILEOPENDIR = "fileopendir"	// string : path
	/**
	 *  Value: String representing the path of
	 *  the last used directory for saving a file.<br>
	 *  Has default value: no!<br>
	 *  Node: root
	 */
	val KEY_FILESAVEDIR = "filesavedir"	// string : path
	/**
	 *  Value: Boolean stating whether frame bounds
	 *  should be recalled a session file when it's
	 *  loaded. Has default value: yes!<br>
	 *  Node: root
	 */
	val KEY_LOOKANDFEEL = "lookandfeel"

   val KEY_LINKOBJTIMELINESEL = "linkobjtimelinesel"

   val KEY_FADEVIEWMODE             = "fadeviewmode"
//   val VALUE_FADEVIEWMODE_NONE      = 0
//   val VALUE_FADEVIEWMODE_CURVE     = 1
//   val VALUE_FADEVIEWMODE_SONOGRAM  = 2

   val KEY_STAKEBORDERVIEWMODE             = "stakeborderviewmode"

	/**
	 *  Child node of root prefs
	 */
	val NODE_IO = "io"

	val NODE_SONACACHE = "sonacache" // child of I/O

   val KEY_EISKOSCPORT     = "eiskoscport"
   val KEY_EISKOSCPROTOCOL = "eiskoscprotocol"

	/**
	 *  Child node of audio prefs
	 */
	val NODE_AUDIO	= "audio"

	/**
	 *  Value: String representing the osc port
	 *  of the supercollider application.<br>
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
//	val KEY_SUPERCOLLIDEROSC	= "supercolliderosc";	// string : "ip:port"

	val KEY_SCPROTOCOL		= "scprotocol"			// string : osc protocol
	val KEY_SCPORT			= "scport"				// param : osc port

	/**
	 *  Value: String representing the pathname
	 *  of the supercollider application.<br>
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
	val KEY_SUPERCOLLIDERAPP	= "supercolliderapp"	// string : pathname
	/**
	 *  Value: Param representing the sample rate
	 *  at which supercollider shall run (0 = default system rate)
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
	val KEY_AUDIORATE	= "audiorate"		// Param : sample rate for sc
	/**
	 *  Value: Param representing the number of
	 *  internal supercollider busses
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
	val KEY_AUDIOBUSSES	= "audiobusses"	// Param : integer number of busses
	/**
	 *  Value: Param representing the amount of
	 *  realtime memory for supercollider (in MB)
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
	val KEY_SCMEMSIZE	= "scmemsize"		// Param : integer number MB realtime mem for supercollider
	/**
	 *  Value: Param representing the number of
	 *  audio samples per control period in supercollider
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
	val KEY_SCBLOCKSIZE	= "scblocksize"	// Param : integer number samples per control period in supercollider
	/**
	 *  Value: Boolean determining whether scsynth should be booted
	 *  with ZeroConf enabled or not.
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
	val KEY_SCZEROCONF = "sczeroconf"	// Boolean
	/**
	 *  Value: Boolean indicating whether scsynth shall
	 *  be booted automatically when Eisenkraut launched or not
	 *  Has default value: yes!<br>
	 *  Node: audio
	 */
	val KEY_AUTOBOOT		= "autoboot"	// boolean : automatic scsynth booting
//	/**
//	 *  Value: String representing the name
//	 *  of the currently active output routine configuration
//	 *  Has default value: yes!<br>
//	 *  Node: audio
//	 */
//	val KEY_OUTPUTCONFIG	= "outputconfig"	// string : active output configuration
	/**
	 *  Value: String representing the name
	 *  of the currently active audio box
	 *  Has default value: ??<br>
	 *  Node: audio
	 */
	val KEY_AUDIOBOX		= "audiobox"	// string : active audio box configuration

	// ------------ audio->audioboxes node level ------------

	/**
	 *  Child node of audio prefs
	 */
	val NODE_AUDIOBOXES		= "audioboxes"

	// ------------

   val NODE_GUI         = "gui"

   val KEY_NUDGEAMOUNT  = "nudgeamount"
}
