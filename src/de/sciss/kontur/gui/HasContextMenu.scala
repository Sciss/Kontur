/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sciss.kontur.gui

import java.awt.{ Component }

trait HasContextMenu {
  def createContextMenu() : Option[ PopupRoot ]
}
