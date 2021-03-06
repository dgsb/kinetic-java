/**
 * 
 * Copyright (C) 2014 Seagate Technology.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package com.seagate.kinetic.simulator.internal;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.seagate.kinetic.common.lib.KineticMessage;
import com.seagate.kinetic.simulator.io.provider.spi.MessageService;
import com.seagate.kinetic.simulator.io.provider.tcp.IoHandler;

/**
 *
 * Server network I/O message handler.
 * <p>
 *
 * @author James Hughes
 * @author chiaming Yang
 *
 */
public class MessageHandler implements Runnable {

	// my logger
	private final static Logger logger = Logger.getLogger(MessageHandler.class
			.getName());

	// my message queue
	private final LinkedBlockingQueue<KineticMessage> lbqueue = new LinkedBlockingQueue<KineticMessage>();

	// io handler
	private IoHandler ioHandler = null;

	// master server
	private MessageService messageService = null;

	// flag running flag
	private volatile boolean isRunning = false;

	// close flag
	private volatile boolean isClosed = false;

	// reference to current running flag
	private Thread currentThread = null;

	/**
	 * Constructor.
	 *
	 * @param ioHandler
	 *            the iohandler associated with this message handler.
	 */
	public MessageHandler(IoHandler ioHandler) {
		this.ioHandler = ioHandler;
		this.messageService = ioHandler.getIoService().getMessageService();
	}

	/**
	 * process request message from IoHandler.
	 *
	 * @param message
	 *            request message.
	 *
	 * @throws InterruptedException
	 *             if interrupted.
	 */
	public void processRequest(KineticMessage message)
			throws InterruptedException {
		// put to queue
		this.lbqueue.put(message);

		// check if there is a thread running. if not, submit to thread pool
		// for execution.
		checkRunning();
	}

	@Override
	public void run() {

		try {

			while (isRunning()) {

				// save the current thread reference
				this.currentThread = Thread.currentThread();

				// poll message from queue
				KineticMessage msg = this.lbqueue.poll(6, TimeUnit.SECONDS);

				if (msg != null) {
					// process message
					doProcessMessage(msg);
				} else {
					// break out of loop
					this.isRunning = false;
				}

			}
		} catch (InterruptedException ie) {
			// interrupted when closed
			;
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		} finally {
			// set running to false
			isRunning = false;

			// thread returning to pool
			this.currentThread = null;
		}

	}

	/**
	 * process request message.
	 *
	 * @param request
	 *            request message from IoHandler.
	 */
	private void doProcessMessage(KineticMessage request) {

		// delegate to server instance to process
		KineticMessage response = this.messageService.processRequest(request);

		// delegate to IoHandler to send the response message
		this.ioHandler.sendResponse(response);
	}

	/**
	 * Check if there is a thread running and processing the queue. If not,
	 * submit myself to the executor service.
	 */
	private void checkRunning() {

		if (this.isRunning == false) {

			synchronized (this) {

				if (this.isRunning == false) {

					if (this.isClosed) {
						return;
					}

					// set flag to true
					this.isRunning = true;

					// execute by the thread pool
					this.ioHandler.getIoService().getPool().execute(this);

					logger.fine("started message handler thread ..."
							+ this.ioHandler.getIoService().getName());
				}
			}

		}

	}

	/**
	 * check if there is a thread running.
	 *
	 * @return true if the thread is polling the message.
	 */
	private boolean isRunning() {
		return (isRunning && (!isClosed));
	}

	/**
	 * Close the message handler.
	 */
	public void close() {
		// set closed flag
		this.isClosed = true;

		// wake up lbqueue
		if (this.currentThread != null) {
			this.currentThread.interrupt();
		}
	}

}
