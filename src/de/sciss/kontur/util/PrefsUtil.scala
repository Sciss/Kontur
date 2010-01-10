/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.util

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
}
