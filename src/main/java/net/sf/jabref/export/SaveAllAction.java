/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.export;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sf.jabref.*;

/**
 * @author alver
 */
public class SaveAllAction extends MnemonicAwareAction implements Runnable {

    private final JabRefFrame frame;
    private int databases = 0;


    /**
     * Creates a new instance of SaveAllAction
     */
    public SaveAllAction(JabRefFrame frame) {
        super(GUIGlobals.getImage("saveAll"));
        this.frame = frame;
        putValue(Action.ACCELERATOR_KEY, Globals.prefs.getKey("Save all"));
        putValue(Action.SHORT_DESCRIPTION, Globals.lang("Save all open databases"));
        putValue(Action.NAME, "Save all");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        databases = frame.getTabbedPane().getTabCount();
        frame.output(Globals.lang("Saving all databases..."));
        new AbstractWorker() {
            @Override
            public void run() {
                SaveAllAction.this.run();
            }

            @Override
            public void update() {
                frame.output(Globals.lang("Save all finished."));
            }
        }.startInSwingWorker();
    }

    @Override
    public void run() {
        for (int i = 0; i < databases; i++) {
            if (i < frame.getTabbedPane().getTabCount()) {
                BasePanel panel = frame.baseAt(i);
                if (panel.getFile() == null) {
                    frame.showBaseAt(i);
                }

                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                panel.runCommand("save");
            }
        }
    }

}
