package lastcard.view.gui;

import javax.swing.Timer;

import lastcard.controller.TurnScheduler;

/**
 * Il TurnScheduler della GUI: un timer di Swing che scatta una volta sola. Il compito gira sul thread di Swing,
 * lo stesso dei clic e dei disegni, quindi la partita è toccata sempre da un thread solo.
 */
public final class SwingTurnScheduler implements TurnScheduler {

    private Timer timer;

    @Override
    public void schedule(int delayMillis, Runnable task) {
        cancel();
        timer = new Timer(delayMillis, e -> task.run());
        timer.setRepeats(false);
        timer.start();
    }

    @Override
    public void cancel() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
}
