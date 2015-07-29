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
package net.sf.jabref;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

public abstract class AbstractWorker {

    public static class RunnableWorker extends AbstractWorker {

        private final Runnable runnable;

        public RunnableWorker(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }

    /**
     * Runs in EDT, updating the GUI **before** the actual task is run.
     */
    public void init() {

    }

    /**
     * Runs in separate thread outside of EDT, this is the long running task.
     */
    public abstract void run();

    /**
     * Runs in EDT, updating the GUI **after** the actual task has been run.
     */
    public void update() {

    }

    /**
     * Caller can either be the EDT or any other thread.
     * <p>
     * The init and update method MUST be executed on the EDT.
     * If the current thread is already the EDT, the methods are just executed directly.
     * In the other cases, we use SwingUtilities.invokeAndWait.
     * <p>
     * The run method MUST be executed on a separate thread.
     * As the previous library did block until the method returned, we need to implement a similar synchronous behaviour.
     * To get this behaviour, we encapsulate this action in its own thread and wait it to finish.
     */
    public void startInSwingWorker() {
        executeInEDTThread(new Runnable() {
            @Override
            public void run() {
                AbstractWorker.this.init();
            }

        });

        if(!SwingUtilities.isEventDispatchThread()) {
            AbstractWorker.this.run();
        } else {
            System.out.println("EDT THREAD -> run needs to run in separate thread");
            JabRefExecutorService.INSTANCE.executeAndWait(new Runnable() {
                @Override
                public void run() {
                    AbstractWorker.this.run();
                }
            });
        }
        executeInEDTThread(new Runnable() {
            @Override
            public void run() {
                AbstractWorker.this.update();
            }
        });
    }

    private void executeInEDTThread(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            System.out.println("NOT EDT THREAD -> needs to run in EDT!");
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

}
